package edu.gkg.service;

public record ImportResult(int total, int success, int skipped, long elapsedMs) {}