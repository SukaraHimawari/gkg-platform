package edu.gkg.service;

public record CleanResult(int duplicateRemoved, int nullThemeRemoved, int invalidRowRemoved) {}