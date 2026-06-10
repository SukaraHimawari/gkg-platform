package edu.gkg.service.impl;

import edu.gkg.common.SharedRecords.*;
import edu.gkg.service.QueryService;

import java.time.LocalDate;
import java.util.List;

/**
 * F-06~F-10 真实查询实现骨架。
 *
 * 现状：等 B 推 DAO（GkgRecordDao / PersonDao / OrgDao / ThemeDao）。
 * DAO 到位前，本类全部方法抛 UnsupportedOperationException —— 启动期间
 * App 应注入 MockQueryService 而非本类。
 *
 * DAO 到位后逐方法替换：
 *   search          → 动态 SQL + PreparedStatement + LIMIT/OFFSET
 *   getPersonProfile→ Person JOIN GkgRecord 聚合 + 关联实体 TOP10
 *   getThemeTrend   → GROUP BY publish_date
 *   suggest*        → LIKE 'prefix%' LIMIT N
 */
public class QueryServiceImpl implements QueryService {

    // TODO(W3): 注入 GkgRecordDao / PersonDao / OrgDao / ThemeDao（B 交付后）

    @Override
    public Page<SearchRow> search(QueryCondition cond, int page, int pageSize) {
        throw new UnsupportedOperationException("等 B 交付 DAO 后实现 (W2 末)");
    }

    @Override
    public EntityProfile getPersonProfile(String name) {
        throw new UnsupportedOperationException("等 B 交付 PersonDao 后实现");
    }

    @Override
    public EntityProfile getOrgProfile(String name) {
        throw new UnsupportedOperationException("等 B 交付 OrganizationDao 后实现");
    }

    @Override
    public List<DateTone> getThemeTrend(String themeCode, LocalDate from, LocalDate to) {
        throw new UnsupportedOperationException("等 B 交付 ThemeDao 后实现");
    }

    @Override
    public List<String> suggestPerson(String prefix, int limit) {
        throw new UnsupportedOperationException("等 B 交付 PersonDao 后实现");
    }

    @Override
    public List<String> suggestOrg(String prefix, int limit) {
        throw new UnsupportedOperationException("等 B 交付 OrganizationDao 后实现");
    }

    @Override
    public long countAll() {
        throw new UnsupportedOperationException("等 B 交付 GkgRecordDao 后实现");
    }
}
