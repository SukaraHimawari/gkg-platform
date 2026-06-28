package edu.gkg.service.impl;

import edu.gkg.common.SharedRecords.*;
import edu.gkg.dao.GkgRecordDao;
import edu.gkg.dao.OrganizationDao;
import edu.gkg.dao.PersonDao;
import edu.gkg.model.GkgRecord;
import edu.gkg.service.QueryService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class QueryServiceImpl implements QueryService {

    private final GkgRecordDao  recordDao = new GkgRecordDao();
    private final PersonDao     personDao = new PersonDao();
    private final OrganizationDao orgDao  = new OrganizationDao();

    @Override
    public Page<GkgRecord> search(QueryCondition cond, int page, int pageSize) {
        try {
            // TODO: 等 B 补充带条件过滤的 DAO 方法后替换为真正 SQL 查询
            List<GkgRecord> all = recordDao.findAll();
            long total = recordDao.count();
            int fromIdx = Math.max(0, (page - 1) * pageSize);
            List<GkgRecord> rows = all.stream()
                    .skip(fromIdx)
                    .limit(pageSize)
                    .collect(Collectors.toList());
            return new Page<>(rows, page, pageSize, total);
        } catch (SQLException e) {
            return new Page<>(List.of(), page, pageSize, 0);
        }
    }

    @Override
    public EntityProfile getPersonProfile(String name) {
        // TODO: 等 B 补充 PersonDao 联表查询方法后实现
        return new EntityProfile(name, 0, 0.0, 0, List.of());
    }

    @Override
    public EntityProfile getOrgProfile(String name) {
        // TODO: 等 B 补充 OrganizationDao 联表查询方法后实现
        return new EntityProfile(name, 0, 0.0, 0, List.of());
    }

    @Override
    public List<DateTone> getThemeTrend(String themeCode, LocalDate from, LocalDate to) {
        // TODO: 等 B 补充 ThemeDao 按日期过滤查询方法后实现
        return List.of();
    }

    @Override
    public List<String> suggestPerson(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) return List.of();
        try {
            return personDao.findAll().stream()
                    .map(p -> p.getName())
                    .filter(n -> n != null && n.toLowerCase().startsWith(prefix.toLowerCase()))
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            return List.of();
        }
    }

    @Override
    public List<String> suggestOrg(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) return List.of();
        try {
            return orgDao.findAll().stream()
                    .map(o -> o.getName())
                    .filter(n -> n != null && n.toLowerCase().startsWith(prefix.toLowerCase()))
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            return List.of();
        }
    }

    @Override
    public long countAll() {
        try {
            return recordDao.count();
        } catch (SQLException e) {
            return 0;
        }
    }
}
