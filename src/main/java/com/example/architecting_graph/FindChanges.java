package com.example.architecting_graph;

import java.util.HashSet;
import java.util.Set;
import org.neo4j.driver.*;

public class FindChanges {

    public static Set<Pair> EarlierChanges(Integer v1, Integer v2, String cmdb, Session session) {
        Set<Pair> out = new HashSet<>();
        // out.add(new Pair("A", "B"));

        // Проход по прямым связям системы
        String query = "MATCH (n:SoftwareSystem {graph: \"Global\", cmdb: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN r.description, r.start_version, r.end_version";
        Value parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        Result result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("r.start_version").asInt();
            Integer end_version = record.get("r.end_version").asInt();
            String description = record.get("r.descriprion").asString();

            if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                out.add(new Pair(description, "Relationship"));
            }
        }

        // Проход по обратным связям системы
        query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:SoftwareSystem {graph: \"Global\", cmdb: $val1}) RETURN r.description, r.start_version, r.end_version";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("r.start_version").asInt();
            Integer end_version = record.get("r.end_version").asInt();
            String description = record.get("r.descriprion").asString();

            if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                out.add(new Pair(description, "Relationship"));
            }
        }

        // Проход по контейнерам
        query = "MATCH (n:SoftwareSystem {graph: \"Global\", cmdb: $val1})-[r:Child]->(m:Container) RETURN m.name, m.start_version, m.end_version";
        parameters = Values.parameters("val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("m.start_version").asInt();
            Integer end_version = record.get("m.end_version").asInt();
            String cont_name = record.get("m.name").asString();

            if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                out.add(new Pair(cont_name, "Container"));
            }

            // Проход по прямым связям контейнера
            query = "MATCH (n:Container {graph: \"Global\", name: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN r.description, r.start_version, r.end_version";
            parameters = Values.parameters("val1", cont_name, "cmdb", cmdb);
            Result result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();
                start_version = record.get("r.start_version").asInt();
                end_version = record.get("r.end_version").asInt();
                String description = record.get("r.descriprion").asString();

                if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                    out.add(new Pair(description, "Relationship"));
                }
            }

            // Проход по обратным связям контейнера
            query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:Container {graph: \"Global\", name: $val1}) RETURN r.description, r.start_version, r.end_version";
            result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();
                start_version = record.get("r.start_version").asInt();
                end_version = record.get("r.end_version").asInt();
                String description = record.get("r.descriprion").asString();

                if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                    out.add(new Pair(description, "Relationship"));
                }
            }

            // Добавление компонентов
            query = "MATCH (n:Container {graph: \"Global\", name: $val1})-[r:Child]->(m:Component) RETURN m.name, m.start_version, m.end_version";
            parameters = Values.parameters("val1", cont_name);
            result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();
                start_version = record.get("m.start_version").asInt();
                end_version = record.get("m.end_version").asInt();
                String comp_name = record.get("m.name").asString();

                if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                    out.add(new Pair(comp_name, "Component"));
                }

                // Проход по прямым связям компонента
                query = "MATCH (n:Component {graph: \"Global\", name: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN r.description, r.start_version, r.end_version";
                parameters = Values.parameters("val1", comp_name, "cmdb", cmdb);
                Result result2 = session.run(query, parameters);

                while (result2.hasNext()) {
                    record = result2.next();
                    start_version = record.get("r.start_version").asInt();
                    end_version = record.get("r.end_version").asInt();
                    String description = record.get("r.descriprion").asString();

                    if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                        out.add(new Pair(description, "Relationship"));
                    }
                }

                // Проход по обратным связям компонента
                query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:Component {graph: \"Global\", name: $val1}) RETURN r.description, r.start_version, r.end_version";
                parameters = Values.parameters("val1", comp_name, "cmdb", cmdb);
                result2 = session.run(query, parameters);

                while (result2.hasNext()) {
                    record = result2.next();
                    start_version = record.get("r.start_version").asInt();
                    end_version = record.get("r.end_version").asInt();
                    String description = record.get("r.descriprion").asString();

                    if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                        out.add(new Pair(description, "Relationship"));
                    }
                }
            }
        }

        return out;
    }

    public static Set<Pair> LaterChanges(Integer v1, Integer v2, String cmdb, Session session) {
        Set<Pair> out = new HashSet<>();

        return out;
    }
}
