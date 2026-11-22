package com.company.platform.calendar;

import java.time.LocalDate;

/**
 * 营业日历端口（机制层）：计息、结算窗口、宽限期一律按营业日计算。
 * 实现由各项目基础设施层提供（数据来自 holidays 表；表结构范式见 CONVENTIONS.md）。
 *
 * <p>与 floorplan 版同构。</p>
 */
public interface BusinessCalendar {

    boolean isBusinessDay(LocalDate date);

    /** 返回自 from 起的第 n 个营业日（n >= 0 时向后，n &lt; 0 时向前）。 */
    LocalDate plusBusinessDays(LocalDate from, int n);

    /** 下一个营业日（若当日已是营业日则返回当日）。 */
    default LocalDate nextBusinessDay(LocalDate date) {
        LocalDate cursor = date;
        int guard = 0;
        while (!isBusinessDay(cursor) && guard++ < 365) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }
}
