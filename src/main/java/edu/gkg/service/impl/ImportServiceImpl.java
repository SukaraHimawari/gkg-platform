package edu.gkg.service.impl;

import edu.gkg.service.CleanResult;
import edu.gkg.service.ImportResult;
import edu.gkg.service.ProgressListener;
import edu.gkg.service.ImportService;

import java.io.File;

public class ImportServiceImpl implements ImportService {
    @Override
    public ImportResult importFile(File csvFile, ProgressListener listener) {
        if (listener != null) {
            listener.onProgress(100, "暂未实现");
        }
        return new ImportResult(0, 0, 0, 0);
    }

    @Override
    public ImportResult importDirectory(File dir, ProgressListener listener) {
        if (listener != null) {
            listener.onProgress(100, "暂未实现");
        }
        return new ImportResult(0, 0, 0, 0);
    }

    @Override
    public CleanResult cleanInvalidData() {
        return new CleanResult(0, 0, 0);
    }
}
