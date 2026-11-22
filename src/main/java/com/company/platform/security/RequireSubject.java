package com.company.platform.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注接口允许的主体类型。<b>切断点 ③ 的执行点</b>：
 * 未标注 = 不限主体；标注后 token 的 subjectType 不在集合内即拒（403），
 * 例如员工端点标 {@code @RequireSubject(SubjectType.EMPLOYEE)} 后，
 * 客户 token 即使验签通过也会被拒——主体隔离在机制层强制，不靠约定。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSubject {

    SubjectType[] value();
}
