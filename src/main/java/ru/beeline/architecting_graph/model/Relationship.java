package ru.beeline.architecting_graph.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.architecting_graph.model.Perspective;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Relationship {

    private String id;
    private String description;
    private String tags;
    private String url;
    private Map<String, Object> properties;
    private List<Perspective> perspectives;
    private String sourceId;
    private String destinationId;
    private String technology;
    private InteractionStyle interactionStyle;
    private String linkedRelationshipId;

    public enum InteractionStyle {
        Synchronous,
        Asynchronous
    }
}