package com.company.platform.security;

import com.company.platform.exception.BusinessException;
import com.company.platform.exception.ErrorCode;
import com.company.platform.exception.StandardErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 切断点 ③ 的可执行证据：主体隔离与权限校验在机制层强制。 */
class PlatformJwtAuthInterceptorTest {

    static class DummyController {
        @RequireSubject(SubjectType.EMPLOYEE)
        @RequirePermission({"RF_APP_APPROVE"})
        public void employeeApprove() {
        }

        @RequireAnyPermission({"CONFIG_MAINTAIN", "JOB_TRIGGER"})
        public void adminDomain() {
        }

        public void open() {
        }
    }

    static class FakeVerifier implements TokenVerifier {
        final VerifiedToken token;

        FakeVerifier(VerifiedToken token) {
            this.token = token;
        }

        @Override
        public VerifiedToken verify(String bearerToken) {
            if ("good-token".equals(bearerToken)) {
                return token;
            }
            throw new BusinessException(StandardErrorCode.UNAUTHORIZED, "invalid token");
        }
    }

    static final ErrorCode UNAUTHORIZED = StandardErrorCode.UNAUTHORIZED;
    static final ErrorCode FORBIDDEN = StandardErrorCode.FORBIDDEN;
    PlatformJwtAuthInterceptor interceptor;
    ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        interceptor = new PlatformJwtAuthInterceptor(
                new FakeVerifier(new VerifiedToken(
                        "EMP-001", SubjectType.EMPLOYEE,
                        Set.of("UNDERWRITER"), Set.of("RF_APP_APPROVE", "CONFIG_MAINTAIN"), Map.of())),
                om, UNAUTHORIZED, FORBIDDEN);
    }

    private HandlerMethod handler(String method) throws NoSuchMethodException {
        return new HandlerMethod(new DummyController(), DummyController.class.getMethod(method));
    }

    @Test
    void 员工token带权限_通过员工端点() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer good-token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, res, handler("employeeApprove")));
        assertEquals("EMP-001", req.getAttribute(PlatformJwtAuthInterceptor.ATTR_SUBJECT));
        assertEquals(SubjectType.EMPLOYEE, req.getAttribute(PlatformJwtAuthInterceptor.ATTR_SUBJECT_TYPE));
    }

    @Test
    void 客户token访问员工端点_主体隔离拒绝() throws Exception {
        // 切断点 ③ / RB-6：验签通过的客户 token 也不得访问员工端点
        PlatformJwtAuthInterceptor customerInterceptor = new PlatformJwtAuthInterceptor(
                new FakeVerifier(new VerifiedToken(
                        "CUS-001", SubjectType.CUSTOMER,
                        Set.of(), Set.of("RF_APP_APPROVE"), Map.of())),
                om, UNAUTHORIZED, FORBIDDEN);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer good-token");
        BusinessException ex = assertThrowsB(customerInterceptor, req, "employeeApprove");
        assertTrue(ex.getDetails().containsKey("subjectType"));
    }

    @Test
    void 有任一anyOf权限_通过管理域端点() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer good-token");
        assertTrue(interceptor.preHandle(req, new MockHttpServletResponse(), handler("adminDomain")));
    }

    @Test
    void 缺allOf权限_拒绝() throws Exception {
        PlatformJwtAuthInterceptor poorInterceptor = new PlatformJwtAuthInterceptor(
                new FakeVerifier(new VerifiedToken(
                        "EMP-002", SubjectType.EMPLOYEE, Set.of(), Set.of(), Map.of())),
                om, UNAUTHORIZED, FORBIDDEN);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer good-token");
        assertThrowsB(poorInterceptor, req, "employeeApprove");
    }

    @Test
    void 无token_401拒并写JSON响应() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(req, res, handler("open")));
        assertEquals(401, res.getStatus());
        assertTrue(res.getContentAsString().contains("code"));
    }

    @Test
    void 未标注注解的端点_验签通过即放行() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer good-token");
        assertTrue(interceptor.preHandle(req, new MockHttpServletResponse(), handler("open")));
    }

    private BusinessException assertThrowsB(PlatformJwtAuthInterceptor ic,
                                            HttpServletRequest req, String method) throws NoSuchMethodException {
        try {
            ic.preHandle(req, new MockHttpServletResponse(), handler(method));
            throw new AssertionError("期望抛 BusinessException 但没有抛");
        } catch (BusinessException e) {
            return e;
        } catch (Exception e) {
            throw new AssertionError("抛出了非 BusinessException 的异常：" + e, e);
        }
    }
}
