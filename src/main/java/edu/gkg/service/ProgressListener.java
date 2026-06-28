package edu.gkg.service;

@FunctionalInterface
public interface ProgressListener {
    void onProgress(int progress, String message);
}