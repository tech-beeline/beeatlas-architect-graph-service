package ru.beeline.architecting_graph.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Decision {

    private String id;
    private String date;
    private Status status;
    private String title;
    private String content;
    private Format format;
    private String elementId;

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
