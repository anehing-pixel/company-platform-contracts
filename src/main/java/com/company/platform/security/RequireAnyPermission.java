package com.company.platform.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注接口需要<b>任一</b>所列权限点（any-of）。
 * 对应 floorplan 旧 {@code @RequireAdmin} 的「持有任一管理域权限点即放行」语义的泛化。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAnyPermission {

    String[] value();
}
