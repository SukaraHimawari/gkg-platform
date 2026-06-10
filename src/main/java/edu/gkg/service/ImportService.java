package edu.gkg.service;

import edu.gkg.common.SharedRecords.ImportResult;
import edu.gkg.common.SharedRecords.ProgressTick;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * F-01 / F-03 数据导入与清理。
 *
 * 临时占位：本接口归属同学 B，A 这边先放一份本地副本让 UI / Controller 编译；
 * B 推上来后直接合并。
 */
public interface ImportService {

    /**
     * 导入多份 GKG / GDELT 文件（或目录递归）。
     * 进度通过 sink 回调（百分比 + 当前文件 + 信息）。
     * 返回成功/跳过/失败计数 + 用时。
     */
    ImportResult importFiles(List<File> files, Consumer<ProgressTick> sink);

    /**
     * F-03：扫描 gkg_record 删除"V2Tone 缺失"、"日期非法"等无效行，
     * 返回删除条数。
     */
    int cleanInvalidData();
}
