package com.company.platform.security;

import com.company.platform.exception.BusinessException;
import com.company.platform.exception.ErrorCode;
import com.company.platform.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * JWT 认证拦截器机制（机制层）：认证（验签）→ 主体类型校验 → 授权（权限点）三道关。
 *
 * <p>与 floorplan 版的差异（切断点 ③ 参数化）：</p>
 * <ul>
 *   <li>验签通过 {@link TokenVerifier} 端口注入——平台层不依赖任何项目的认证服务；</li>
 *   <li>权限点用字符串，内容由各项目定义；</li>
 *   <li>新增 {@link RequireSubject} 主体类型关——客户 token 不得访问员工端点，反之亦然，
 *       在机制层强制而不是靠约定。</li>
 * </ul>
 *
 * <p>验签通过后把主体信息放入请求属性（{@code authSubject} / {@code authSubjectType} /
 * {@code authRoles} / {@code authPermissions}），供 {@code AuditActor} 与审计兜底。</p>
 */
public class PlatformJwtAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(PlatformJwtAuthInterceptor.class);

    public static final String ATTR_SUBJECT = "authSubject";
    public static final String ATTR_SUBJECT_TYPE = "authSubjectType";
    public static final String ATTR_ROLES = "authRoles";
    public static final String ATTR_PERMISSIONS = "authPermissions";

    private final TokenVerifier verifier;
    private final ObjectMapper objectMapper;
    private final ErrorCode unauthorizedCode;
    private final ErrorCode forbiddenCode;

    public PlatformJwtAuthInterceptor(TokenVerifier verifier, ObjectMapper objectMapper,
                                      ErrorCode unauthorizedCode, ErrorCode forbiddenCode) {
        this.verifier = verifier;
        this.objectMapper = objectMapper;
        this.unauthorizedCode = unauthorizedCode;
        this.forbiddenCode = forbiddenCode;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(response, unauthorizedCode, Map.of("reason", "NO_TOKEN"));
        }

        VerifiedToken token;
        try {
            token = verifier.verify(header.substring(7));
        } catch (BusinessException ex) {
            return reject(response, ex.getErrorCode(),
                    ex.getDetails() == null ? Map.of("reason", "VERIFY_FAILED") : ex.getDetails());
        }

        request.setAttribute(ATTR_SUBJECT, token.subject());
        request.setAttribute(ATTR_SUBJECT_TYPE, token.subjectType());
        request.setAttribute(ATTR_ROLES, token.roles());
        request.setAttribute(ATTR_PERMISSIONS, token.permissions());
        if (request.getHeader("X-Operator-Id") == null) {
            request.setAttribute("operatorId", token.subject());
        }

        // 关二：主体类型（切断点 ③ 的执行点）
        SubjectType[] allowed = requiredSubjects(handler);
        if (allowed.length > 0 && !hasAnySubject(token.subjectType(), allowed)) {
            log.warn("主体类型越权被拒：subject={} type={} allowed={} uri={}",
                    token.subject(), token.subjectType(), Arrays.toString(allowed), request.getRequestURI());
            throw new BusinessException(forbiddenCode, "主体类型不允许访问该接口", Map.of(
                    "subjectType", String.valueOf(token.subjectType()),
                    "allowedSubjects", Arrays.toString(allowed),
                    "uri", request.getRequestURI()));
        }

        // 关三：权限点
        String[] required = requiredPermissions(handler);
        if (required.length > 0 && !hasAllPermissions(token.permissions(), required)) {
            log.warn("越权访问被拒：subject={} type={} uri={} required={}",
                    token.subject(), token.subjectType(), request.getRequestURI(), Arrays.toString(required));
            throw new BusinessException(forbiddenCode, "无该操作权限", Map.of(
                    "requiredPermissions", Arrays.toString(required),
                    "currentPermissions", token.permissions().toString(),
                    "uri", request.getRequestURI()));
        }

        String[] anyOf = requiredAnyPermissions(handler);
        if (anyOf.length > 0 && !hasAnyPermission(token.permissions(), anyOf)) {
            log.warn("越权访问被拒（任一域）：subject={} type={} uri={} anyOf={}",
                    token.subject(), token.subjectType(), request.getRequestURI(), Arrays.toString(anyOf));
            throw new BusinessException(forbiddenCode, "无该操作权限", Map.of(
                    "anyOfPermissions", Arrays.toString(anyOf),
                    "currentPermissions", token.permissions().toString(),
                    "uri", request.getRequestURI()));
        }

        return true;
    }

    private boolean reject(HttpServletResponse response, ErrorCode code, Map<String, Object> details)
            throws Exception {
        response.setStatus(code.getHttpStatus());
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> body = ApiResponse.fail(code.getCode(), code.getMessage(),
                UUID.randomUUID().toString(), details);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }

    private SubjectType[] requiredSubjects(Object handler) {
        if (handler instanceof HandlerMethod method) {
            RequireSubject ann = method.getMethodAnnotation(RequireSubject.class);
            if (ann == null) {
                ann = method.getBeanType().getAnnotation(RequireSubject.class);
            }
            return ann == null ? new SubjectType[0] : ann.value();
        }
        return new SubjectType[0];
    }

    private String[] requiredPermissions(Object handler) {
        if (handler instanceof HandlerMethod method) {
            RequirePermission ann = method.getMethodAnnotation(RequirePermission.class);
            if (ann == null) {
                ann = method.getBeanType().getAnnotation(RequirePermission.class);
            }
            return ann == null ? new String[0] : ann.value();
        }
        return new String[0];
    }

    private String[] requiredAnyPermissions(Object handler) {
        if (handler instanceof HandlerMethod method) {
            RequireAnyPermission ann = method.getMethodAnnotation(RequireAnyPermission.class);
            if (ann == null) {
                ann = method.getBeanType().getAnnotation(RequireAnyPermission.class);
            }
            return ann == null ? new String[0] : ann.value();
        }
        return new String[0];
    }

    private boolean hasAnySubject(SubjectType actual, SubjectType[] allowed) {
        for (SubjectType s : allowed) {
            if (s == actual) return true;
        }
        return false;
    }

    private boolean hasAllPermissions(Set<String> owned, String[] required) {
        for (String p : required) {
            if (!owned.contains(p)) return false;
        }
        return true;
    }

    private boolean hasAnyPermission(Set<String> owned, String[] candidates) {
        for (String p : candidates) {
            if (owned.contains(p)) return true;
        }
        return false;
    }
}
