/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.support;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public final class GraphSnapshot {

    private static final Path GOLDEN_DIR = Path.of("src", "test", "resources", "golden");
    private static final boolean UPDATE = Boolean.getBoolean("golden.update");

    private GraphSnapshot() {
    }

    public static String take(Driver driver) {
        List<String> lines = new ArrayList<>();
        try (Session session = driver.session()) {
            for (Record record : session.run("MATCH (n) RETURN n").list()) {
                lines.add(renderNode(record.get("n").asNode()));
            }
            for (Record record : session.run("MATCH (a)-[r]->(b) RETURN a, r, b").list()) {
                Relationship r = record.get("r").asRelationship();
                lines.add(key(record.get("a").asNode()) + " -[:" + r.type() + " " + render(r.asMap()) + "]-> "
                        + key(record.get("b").asNode()));
            }
        }
        return lines.stream().sorted().collect(Collectors.joining("\n")) + "\n";
    }

    public static void assertMatchesGolden(Driver driver, String name) {
        String actual = take(driver);
        Path file = GOLDEN_DIR.resolve(name + ".txt");
        try {
            if (UPDATE || !Files.exists(file)) {
                Files.createDirectories(GOLDEN_DIR);
                Files.writeString(file, actual, StandardCharsets.UTF_8);
            }
            assertThat(actual).isEqualTo(Files.readString(file, StandardCharsets.UTF_8).replace("\r\n", "\n"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String renderNode(Node node) {
        return key(node) + " " + render(node.asMap());
    }

    private static String key(Node node) {
        String labels = String.join(":", node.labels());
        Map<String, Object> props = node.asMap();
        Object id = labels.equals("SoftwareSystem") ? props.get("cmdb") : props.get("name");
        if (id == null) {
            id = "external_name=" + props.get("external_name");
        }
        return "(" + labels + " '" + id + "' @" + props.get("graphTag") + ")";
    }

    private static String render(Map<String, Object> props) {
        return new TreeMap<>(props).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
