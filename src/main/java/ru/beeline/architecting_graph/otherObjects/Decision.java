package ru.beeline.architecting_graph.otherObjects;

public class Decision {

    private String id;
    private String date;
    private Status status;
    private String title;
    private String content;
    private Format format;
    private String elementId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public enum Status {
        Proposed,
        Accepted,
        Superseded,
        Deprecated,
        Rejected,
        Creating,
        Draft
    }

    public enum Format {
        Markdown,
        AsciiDoc
    }
}
