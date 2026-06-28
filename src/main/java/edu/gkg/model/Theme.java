package edu.gkg.model;

public class Theme {
    private Long themeId;
    private String code;

    public Theme() {}

    public Theme(String code) {
        this.code = code;
    }

    public Long getThemeId() { return themeId; }
    public void setThemeId(Long themeId) { this.themeId = themeId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    @Override
    public String toString() {
        return code;
    }
}