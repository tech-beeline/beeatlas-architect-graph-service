package ru.beeline.architecting_graph.otherObjects;

public class DocumentationSection {

    private String content;
    private Format format;
    private Integer order;

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

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public enum Format {
        Markdown,
        AsciiDoc
    }
}
