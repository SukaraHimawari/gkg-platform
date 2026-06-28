package edu.gkg.service;

import java.io.File;

public interface ImportService {
    ImportResult importFile(File csvFile, ProgressListener listener);
    ImportResult importDirectory(File dir, ProgressListener listener);
    CleanResult cleanInvalidData();
}