package com.company.platform.response;

import java.util.List;

/**
 * 分页响应包装（机制层）：与具体业务的分页参数命名解耦。
 *
 * @param items   当前页数据
 * @param total   总条数
 * @param page    当前页码（1 起）
 * @param size    每页条数
 */
public record PageResponse<T>(List<T> items, long total, int page, int size) {

    public static <T> PageResponse<T> of(List<T> items, long total, int page, int size) {
        return new PageResponse<>(items == null ? List.of() : List.copyOf(items), total, page, size);
    }
}
