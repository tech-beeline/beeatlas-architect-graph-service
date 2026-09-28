/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.service.graph;

import lombok.Getter;
import ru.beeline.architecting_graph.model.Connection;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.model.RelationshipEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RelationshipBatch {

    private final Map<List<String>, Row> rows = new LinkedHashMap<>();

    public void add(RelationshipEntity relationship, Connection connection) {
        GraphObject source = connection.getSource();
        GraphObject destination = connection.getDestination();
        List<String> key = List.of(connection.getRelationshipType(),
                source.getType(), source.getKey(), source.getValue(),
                destination.getType(), destination.getKey(), destination.getValue(),
                Objects.toString(connection.getCmdb()), relationship.getDescription());
        rows.computeIfAbsent(key, k -> new Row(connection, relationship.getDescription()))
                .merge(relationship, connection);
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public Collection<Row> drain() {
        List<Row> drained = new ArrayList<>(rows.values());
        rows.clear();
        return drained;
    }

    @Getter
    public static class Row {

        private final String relationshipType;
        private final GraphObject source;
        private final GraphObject destination;
        private final String sourceWorkspace;
        private final String description;
        private final Map<String, Object> properties = new LinkedHashMap<>();
        private Integer connects;
        private String tags;
        private String url;
        private String technology;
        private Object interactionStyle;
        private String level;

        private Row(Connection connection, String description) {
            this.relationshipType = connection.getRelationshipType();
            this.source = connection.getSource();
            this.destination = connection.getDestination();
            this.sourceWorkspace = connection.getCmdb();
            this.description = description;
        }

        private void merge(RelationshipEntity relationship, Connection connection) {
            if ("None".equals(description)) {
                connects = connects == null ? 1 : connects + 1;
            }
            tags = relationship.getTags();
            url = relationship.getUrl();
            technology = relationship.getTechnology();
            interactionStyle = relationship.getInteractionStyle();
            level = connection.getLevel();
            if (relationship.getProperties() != null) {
                for (Map.Entry<String, Object> entry : relationship.getProperties().entrySet()) {
                    properties.put(entry.getKey().replaceAll("[^a-zA-Z0-9]", "_"), entry.getValue());
                }
            }
        }

        public Map<String, Object> toParameters() {
            Map<String, Object> row = new HashMap<>();
            row.put("source", source.getValue());
            row.put("destination", destination.getValue());
            row.put("sourceWorkspace", sourceWorkspace);
            row.put("description", description);
            row.put("connects", connects);
            row.put("tags", tags);
            row.put("url", url);
            row.put("technology", technology);
            row.put("interactionStyle", interactionStyle);
            row.put("level", level);
            row.put("properties", properties);
            return row;
        }
    }
}
