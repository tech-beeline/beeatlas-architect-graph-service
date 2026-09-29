/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public final class Workspaces {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Workspaces() {
    }

    public static ObjectNode load(String name) {
        try (InputStream in = Workspaces.class.getResourceAsStream("/workspaces/" + name + ".json")) {
            if (in == null) {
                throw new IllegalArgumentException("Нет фикстуры " + name);
            }
            return (ObjectNode) MAPPER.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String json(JsonNode workspace) {
        return workspace.toString();
    }

    public static ObjectNode element(JsonNode workspace, String id) {
        ObjectNode found = find(workspace, id);
        if (found == null) {
            throw new IllegalArgumentException("Нет элемента с id " + id);
        }
        return found;
    }

    public static void remove(JsonNode workspace, String id) {
        removeWhere(workspace, id);
    }

    private static ObjectNode find(JsonNode node, String id) {
        if (node.isObject() && node.path("id").isTextual() && id.equals(node.path("id").asText()) && !node.has("sourceId")) {
            return (ObjectNode) node;
        }
        for (JsonNode child : node) {
            ObjectNode found = find(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static void removeWhere(JsonNode node, String id) {
        if (node.isArray()) {
            Iterator<JsonNode> it = node.iterator();
            while (it.hasNext()) {
                JsonNode item = it.next();
                if (references(item, id)) {
                    it.remove();
                }
            }
        }
        for (JsonNode child : node) {
            removeWhere(child, id);
        }
    }

    private static boolean references(JsonNode item, String id) {
        return id.equals(item.path("id").asText(null))
                || id.equals(item.path("sourceId").asText(null))
                || id.equals(item.path("destinationId").asText(null))
                || id.equals(item.path("containerId").asText(null));
    }

    public static ArrayNode array(ObjectNode element, String field) {
        return element.has(field) ? (ArrayNode) element.get(field) : element.putArray(field);
    }
}
