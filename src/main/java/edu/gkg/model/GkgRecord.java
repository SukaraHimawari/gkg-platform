package edu.gkg.model;

public class GkgRecord {
    private String recordId;
    private String publishDate;
    private Integer sourceCollection;
    private String sourceCommonName;
    private String documentId;
    private Double tone;
    private Double positiveScore;
    private Double negativeScore;
    private Double polarity;
    private Integer wordCount;

    public GkgRecord() {}

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }

    public String getPublishDate() { return publishDate; }
    public void setPublishDate(String publishDate) { this.publishDate = publishDate; }

    public Integer getSourceCollection() { return sourceCollection; }
    public void setSourceCollection(Integer sourceCollection) { this.sourceCollection = sourceCollection; }

    public String getSourceCommonName() { return sourceCommonName; }
    public void setSourceCommonName(String sourceCommonName) { this.sourceCommonName = sourceCommonName; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public Double getTone() { return tone; }
    public void setTone(Double tone) { this.tone = tone; }

    public Double getPositiveScore() { return positiveScore; }
    public void setPositiveScore(Double positiveScore) { this.positiveScore = positiveScore; }

    public Double getNegativeScore() { return negativeScore; }
    public void setNegativeScore(Double negativeScore) { this.negativeScore = negativeScore; }

    public Double getPolarity() { return polarity; }
    public void setPolarity(Double polarity) { this.polarity = polarity; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
}