package edu.gkg.service;

import edu.gkg.model.GkgRecord;
import java.util.List;

public interface QueryService {
    // 临时占位，让A能编译
    default List<GkgRecord> search(String keyword) {
        return List.of();
    }
}