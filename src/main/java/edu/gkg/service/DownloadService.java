package edu.gkg.service;

import edu.gkg.common.SharedRecords.DownloadResult;
import edu.gkg.common.SharedRecords.ProgressTick;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.function.Consumer;

public interface DownloadService {

    DownloadResult downloadDate(LocalDate date, Path destDir, Consumer<ProgressTick> sink);
}
