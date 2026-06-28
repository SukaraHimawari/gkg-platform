package edu.gkg.model;

public class CooccurEdge {
    private Long e1Id;
    private String e1Type;  // "PERSON" 或 "ORG"
    private Long e2Id;
    private String e2Type;
    private Integer coCount;

    public CooccurEdge() {}

    public CooccurEdge(Long e1Id, String e1Type, Long e2Id, String e2Type, Integer coCount) {
        this.e1Id = e1Id;
        this.e1Type = e1Type;
        this.e2Id = e2Id;
        this.e2Type = e2Type;
        this.coCount = coCount;
    }

    public Long getE1Id() { return e1Id; }
    public void setE1Id(Long e1Id) { this.e1Id = e1Id; }
    public String getE1Type() { return e1Type; }
    public void setE1Type(String e1Type) { this.e1Type = e1Type; }
    public Long getE2Id() { return e2Id; }
    public void setE2Id(Long e2Id) { this.e2Id = e2Id; }
    public String getE2Type() { return e2Type; }
    public void setE2Type(String e2Type) { this.e2Type = e2Type; }
    public Integer getCoCount() { return coCount; }
    public void setCoCount(Integer coCount) { this.coCount = coCount; }
}