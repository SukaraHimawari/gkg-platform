package edu.gkg.service;

import edu.gkg.common.SharedRecords.*;

import java.time.LocalDate;
import java.util.List;

/**
 * F-06~F-10 查询接口。
 *
 * 临时占位：本接口归属同学 B，A 这边先放一份本地副本让 UI / Controller 编译；
 * B 推上来后直接合并（字段顺序、方法签名应保持一致）。
 */
public interface QueryService {

    /** 综合查询：日期 + 主题 + 人物 + 组织 + 地点的组合条件 + 分页 */
    Page<SearchRow> search(QueryCondition cond, int page, int pageSize);

    /** 人物档案：频次、平均 tone、关联组织/人物 TOP10、按月分布 */
    EntityProfile getPersonProfile(String name);

    /** 组织档案：同上 */
    EntityProfile getOrgProfile(String name);

    /** 主题频次随时间变化（用于 F-09 / F-21 折线图） */
    List<DateTone> getThemeTrend(String themeCode, LocalDate from, LocalDate to);

    /** 人物名前缀建议（F-10 自动补全） */
    List<String> suggestPerson(String prefix, int limit);

    /** 组织名前缀建议 */
    List<String> suggestOrg(String prefix, int limit);

    /** 数据库 gkg_record 总条数（状态栏用） */
    long countAll();
}
