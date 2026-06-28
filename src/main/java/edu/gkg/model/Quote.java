package edu.gkg.model;

public class Quote {
    private Long quoteId;
    private String recordId;
    private Integer charOffset;
    private Integer length;
    private String verb;
    private String content;
    private Integer sentiment; // -1, 0, 1

    public Quote() {}

    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public Integer getCharOffset() { return charOffset; }
    public void setCharOffset(Integer charOffset) { this.charOffset = charOffset; }
    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length; }
    public String getVerb() { return verb; }
    public void setVerb(String verb) { this.verb = verb; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getSentiment() { return sentiment; }
    public void setSentiment(Integer sentiment) { this.sentiment = sentiment; }
}