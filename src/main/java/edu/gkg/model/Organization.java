package edu.gkg.model;

public class Organization {
    private Long orgId;
    private String name;

    public Organization() {}

    public Organization(String name) {
        this.name = name;
    }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name;
    }
}