package edu.gkg.service;

import edu.gkg.common.SharedRecords.DateTone;
import edu.gkg.common.SharedRecords.EntityProfile;
import edu.gkg.common.SharedRecords.Page;
import edu.gkg.common.SharedRecords.QueryCondition;
import edu.gkg.model.GkgRecord;

import java.time.LocalDate;
import java.util.List;

public interface QueryService {

    /** F-06 组合查询，返回分页结果 */
    Page<GkgRecord> search(QueryCondition cond, int page, int pageSize);

    /** F-07 人物档案 */
    EntityProfile getPersonProfile(String name);

    /** F-08 组织档案 */
    EntityProfile getOrgProfile(String name);

    /** F-10 主题时序趋势 */
    List<DateTone> getThemeTrend(String themeCode, LocalDate from, LocalDate to);

    List<DateTone> getEntityTrend(String entityType, String name, LocalDate from, LocalDate to);

    /** F-09 人物名前缀联想 */
    List<String> suggestPerson(String prefix, int limit);

    /** F-09 组织名前缀联想 */
    List<String> suggestOrg(String prefix, int limit);

    /** 地点名模糊联想 */
    List<String> suggestLocation(String prefix, int limit);

    /** 状态栏：数据库记录总数 */
    long countAll();
}
