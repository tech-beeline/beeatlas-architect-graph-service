package com.example.architecting_graph;

import java.util.HashSet;
import java.util.Set;
import org.neo4j.driver.*;

public class FindChanges {

    private static Set<Pair> out;

    public static String descriptionRelation(Value LeftNode, Value RightNode) {
        String type = LeftNode.asNode().labels().toString();
        type = type.substring(1, type.length() - 1);

        String key = "name";
        if (type.equals("SoftwareSystem")) {
            key = "cmdb";
        }

        String val = LeftNode.asNode().asMap().get(key).toString();

        String res = type + "(" + key + ": " + val + ") -> ";

        type = RightNode.asNode().labels().toString();
        type = type.substring(1, type.length() - 1);

        key = "name";
        if (type.equals("SoftwareSystem")) {
            key = "cmdb";
        }

        val = RightNode.asNode().asMap().get(key).toString();

        res = res + type + "(" + key + ": " + val + ")";

        return res;
    }

    public static void CheckDeploymentNodes(String name, Session session, Integer v1, Integer v2, Integer cur_version,
            String cmdb, Boolean flag) {

        // Проход по прямым связям
        String query = "MATCH (n:DeploymentNode {graph: \"Global\", name: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN n, m, r.start_version, r.end_version";
        Value parameters = Values.parameters("cmdb", cmdb, "val1", name);
        Result result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("r.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("r.end_version").asString().equals("null")) {
                end_version = record.get("r.end_version").asInt();
            }

            if (flag) {
                if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "Relationship"));
                }
            } else {
                if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "Relationship"));
                }
            }
        }

        // Проход по DeploymentNodes
        query = "MATCH (n:DeploymentNode {graph: \"Global\", name: $val1})-[r:Child]->(m:DeploymentNode) RETURN m.name, m.start_version, m.end_version";
        parameters = Values.parameters("val1", name);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("m.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("m.end_version").asString().equals("null")) {
                end_version = record.get("m.end_version").asInt();
            }
            String depl_name = record.get("m.name").asString();

            if (flag) {
                if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                    out.add(new Pair(depl_name, "DeploymentNode"));
                }
            } else {
                if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                    out.add(new Pair(depl_name, "DeploymentNode"));
                }
            }

            CheckDeploymentNodes(depl_name, session, v1, v2, cur_version, cmdb, flag);
        }

        // Проход по InfrastructureNodes
        query = "MATCH (n:DeploymentNode {graph: \"Global\", name: $val1})-[r:Child]->(m:InfrastructureNode) RETURN m.name, m.start_version, m.end_version";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("m.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("m.end_version").asString().equals("null")) {
                end_version = record.get("m.end_version").asInt();
            }
            String infr_name = record.get("m.name").asString();

            if (flag) {
                if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                    out.add(new Pair(infr_name, "InfrastructureNode"));
                }
            } else {
                if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                    out.add(new Pair(infr_name, "InfrastructureNode"));
                }
            }

            // Проход по прямым связям
            query = "MATCH (n:InfrastructureNode {graph: \"Global\", name: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN n, m, r.start_version, r.end_version";
            parameters = Values.parameters("cmdb", cmdb, "val1", infr_name);
            Result result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();
                start_version = record.get("r.start_version").asInt();
                end_version = cur_version;
                if (!record.get("r.end_version").asString().equals("null")) {
                    end_version = record.get("r.end_version").asInt();
                }

                if (flag) {
                    if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                        out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "Relationship"));
                    }
                } else {
                    if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                        out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "Relationship"));
                    }
                }
            }
        }
    }

    public static Set<Pair> EarlierChanges(Integer v1, Integer v2, Integer cur_version, String cmdb, Session session) {
        out = new HashSet<>();

        // Проход по прямым связям системы
        String query = "MATCH (n:SoftwareSystem {graph: \"Global\", cmdb: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN n, m, r.start_version, r.end_version";
        Value parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        Result result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("r.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("r.end_version").asString().equals("null")) {
                end_version = record.get("r.end_version").asInt();
            }

            if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "Relationship"));
            }
        }

        // Проход по обратным связям системы
        query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:SoftwareSystem {graph: \"Global\", cmdb: $val1}) RETURN n, m, r.start_version, r.end_version";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("r.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("r.end_version").asString().equals("null")) {
                end_version = record.get("r.end_version").asInt();
            }

            if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                out.add(new Pair(descriptionRelation(record.get("m"), record.get("n")), "Relationship"));
            }
        }

        // Проход по контейнерам
        query = "MATCH (n:SoftwareSystem {graph: \"Global\", cmdb: $val1})-[r:Child]->(m:Container) RETURN m.name, m.start_version, m.end_version";
        parameters = Values.parameters("val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("m.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("m.end_version").asString().equals("null")) {
                end_version = record.get("m.end_version").asInt();
            }
            String cont_name = record.get("m.name").asString();

            if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                out.add(new Pair(cont_name, "Container"));
            }

            // Проход по прямым связям контейнера
            query = "MATCH (n:Container {graph: \"Global\", name: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN n, m, r.start_version, r.end_version";
            parameters = Values.parameters("val1", cont_name, "cmdb", cmdb);
            Result result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();
                start_version = record.get("r.start_version").asInt();
                end_version = cur_version;
                if (!record.get("r.end_version").asString().equals("null")) {
                    end_version = record.get("r.end_version").asInt();
                }

                if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "Relationship"));
                }
            }

            // Проход по обратным связям контейнера
            query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:Container {graph: \"Global\", name: $val1}) RETURN n, m, r.start_version, r.end_version";
            result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();
                start_version = record.get("r.start_version").asInt();
                end_version = cur_version;
                if (!record.get("r.end_version").asString().equals("null")) {
                    end_version = record.get("r.end_version").asInt();
                }

                if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                    out.add(new Pair(descriptionRelation(record.get("m"), record.get("n")), "Relationship"));
                }
            }

            // Проход по ContainerInctances
            query = "MATCH (n:Container {graph: \"Global\", name: $val1})-[r:Deploy {source_workspace: $cmdb}]->(m) RETURN n, m, r.start_version, r.end_version";
            parameters = Values.parameters("val1", cont_name, "cmdb", cmdb);
            result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();
                start_version = record.get("r.start_version").asInt();
                end_version = cur_version;
                if (!record.get("r.end_version").asString().equals("null")) {
                    end_version = record.get("r.end_version").asInt();
                }

                if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "ContainerInctance"));
                }
            }

            // Добавление компонентов
            query = "MATCH (n:Container {graph: \"Global\", name: $val1})-[r:Child]->(m:Component) RETURN m.name, m.start_version, m.end_version";
            parameters = Values.parameters("val1", cont_name);
            result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();
                start_version = record.get("m.start_version").asInt();
                end_version = cur_version;
                if (!record.get("m.end_version").asString().equals("null")) {
                    end_version = record.get("m.end_version").asInt();
                }
                String comp_name = record.get("m.name").asString();

                if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                    out.add(new Pair(comp_name, "Component"));
                }

                // Проход по прямым связям компонента
                query = "MATCH (n:Component {graph: \"Global\", name: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN n, m, r.start_version, r.end_version";
                parameters = Values.parameters("val1", comp_name, "cmdb", cmdb);
                Result result2 = session.run(query, parameters);

                while (result2.hasNext()) {
                    record = result2.next();
                    start_version = record.get("r.start_version").asInt();
                    end_version = cur_version;
                    if (!record.get("r.end_version").asString().equals("null")) {
                        end_version = record.get("r.end_version").asInt();
                    }

                    if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                        out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "Relationship"));
                    }
                }

                // Проход по обратным связям компонента
                query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:Component {graph: \"Global\", name: $val1}) RETURN n, m, r.start_version, r.end_version";
                parameters = Values.parameters("val1", comp_name, "cmdb", cmdb);
                result2 = session.run(query, parameters);

                while (result2.hasNext()) {
                    record = result2.next();
                    start_version = record.get("r.start_version").asInt();
                    end_version = cur_version;
                    if (!record.get("r.end_version").asString().equals("null")) {
                        end_version = record.get("r.end_version").asInt();
                    }

                    if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                        out.add(new Pair(descriptionRelation(record.get("m"), record.get("n")), "Relationship"));
                    }
                }
            }
        }

        // Проход по SoftwareSystemInctances
        query = "MATCH (n:SoftwareSystem {graph: \"Global\", cmdb: $val1})-[r:Deploy {source_workspace: $cmdb}]->(m) RETURN n, m, r.start_version, r.end_version";
        parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("r.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("r.end_version").asString().equals("null")) {
                end_version = record.get("r.end_version").asInt();
            }

            if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "SoftwareSystemInctance"));
            }
        }

        // Проход по DeploymentNodes
        query = "MATCH (n:SoftwareSystem {graph: \"Global\", cmdb: $val1})-[r:Child]->(m:DeploymentNode) RETURN m.name, m.start_version, m.end_version";
        parameters = Values.parameters("val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("m.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("m.end_version").asString().equals("null")) {
                end_version = record.get("m.end_version").asInt();
            }
            String depl_name = record.get("m.name").asString();

            if (start_version <= v1 && v1 <= end_version && end_version < v2) {
                out.add(new Pair(depl_name, "DeploymentNode"));
            }

            CheckDeploymentNodes(depl_name, session, v1, v2, cur_version, cmdb, true);
        }

        return out;
    }

    public static Set<Pair> LaterChanges(Integer v1, Integer v2, Integer cur_version, String cmdb, Session session) {
        out = new HashSet<>();

        // Проход по прямым связям системы
        String query = "MATCH (n:SoftwareSystem {graph: \"Global\", cmdb: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN n, m, r.start_version, r.end_version";
        Value parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        Result result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("r.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("r.end_version").asString().equals("null")) {
                end_version = record.get("r.end_version").asInt();
            }

            if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "Relationship"));
            }
        }

        // Проход по обратным связям системы
        query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:SoftwareSystem {graph: \"Global\", cmdb: $val1}) RETURN n, m, r.start_version, r.end_version";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("r.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("r.end_version").asString().equals("null")) {
                end_version = record.get("r.end_version").asInt();
            }

            if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                out.add(new Pair(descriptionRelation(record.get("m"), record.get("n")), "Relationship"));
            }
        }

        // Проход по контейнерам
        query = "MATCH (n:SoftwareSystem {graph: \"Global\", cmdb: $val1})-[r:Child]->(m:Container) RETURN m.name, m.start_version, m.end_version";
        parameters = Values.parameters("val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("m.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("m.end_version").asString().equals("null")) {
                end_version = record.get("m.end_version").asInt();
            }
            String cont_name = record.get("m.name").asString();

            if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                out.add(new Pair(cont_name, "Container"));
            }

            // Проход по прямым связям контейнера
            query = "MATCH (n:Container {graph: \"Global\", name: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN n, m, r.start_version, r.end_version";
            parameters = Values.parameters("val1", cont_name, "cmdb", cmdb);
            Result result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();
                start_version = record.get("r.start_version").asInt();
                end_version = cur_version;
                if (!record.get("r.end_version").asString().equals("null")) {
                    end_version = record.get("r.end_version").asInt();
                }

                if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "Relationship"));
                }
            }

            // Проход по обратным связям контейнера
            query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:Container {graph: \"Global\", name: $val1}) RETURN n, m, r.start_version, r.end_version";
            result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();
                start_version = record.get("r.start_version").asInt();
                end_version = cur_version;
                if (!record.get("r.end_version").asString().equals("null")) {
                    end_version = record.get("r.end_version").asInt();
                }

                if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                    out.add(new Pair(descriptionRelation(record.get("m"), record.get("n")), "Relationship"));
                }
            }

            // Проход по ContainerInctances
            query = "MATCH (n:Container {graph: \"Global\", name: $val1})-[r:Deploy {source_workspace: $cmdb}]->(m) RETURN n, m, r.start_version, r.end_version";
            parameters = Values.parameters("val1", cont_name, "cmdb", cmdb);
            result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();
                start_version = record.get("r.start_version").asInt();
                end_version = cur_version;
                if (!record.get("r.end_version").asString().equals("null")) {
                    end_version = record.get("r.end_version").asInt();
                }

                if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "ContainerInctance"));
                }
            }

            // Добавление компонентов
            query = "MATCH (n:Container {graph: \"Global\", name: $val1})-[r:Child]->(m:Component) RETURN m.name, m.start_version, m.end_version";
            parameters = Values.parameters("val1", cont_name);
            result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();
                start_version = record.get("m.start_version").asInt();
                end_version = cur_version;
                if (!record.get("m.end_version").asString().equals("null")) {
                    end_version = record.get("m.end_version").asInt();
                }
                String comp_name = record.get("m.name").asString();

                if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                    out.add(new Pair(comp_name, "Component"));
                }

                // Проход по прямым связям компонента
                query = "MATCH (n:Component {graph: \"Global\", name: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN n, m, r.start_version, r.end_version";
                parameters = Values.parameters("val1", comp_name, "cmdb", cmdb);
                Result result2 = session.run(query, parameters);

                while (result2.hasNext()) {
                    record = result2.next();
                    start_version = record.get("r.start_version").asInt();
                    end_version = cur_version;
                    if (!record.get("r.end_version").asString().equals("null")) {
                        end_version = record.get("r.end_version").asInt();
                    }

                    if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                        out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "Relationship"));
                    }
                }

                // Проход по обратным связям компонента
                query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:Component {graph: \"Global\", name: $val1}) RETURN n, m, r.start_version, r.end_version";
                parameters = Values.parameters("val1", comp_name, "cmdb", cmdb);
                result2 = session.run(query, parameters);

                while (result2.hasNext()) {
                    record = result2.next();
                    start_version = record.get("r.start_version").asInt();
                    end_version = cur_version;
                    if (!record.get("r.end_version").asString().equals("null")) {
                        end_version = record.get("r.end_version").asInt();
                    }

                    if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                        out.add(new Pair(descriptionRelation(record.get("m"), record.get("n")), "Relationship"));
                    }
                }
            }
        }

        // Проход по SoftwareSystemInctances
        query = "MATCH (n:SoftwareSystem {graph: \"Global\", cmdb: $val1})-[r:Deploy {source_workspace: $cmdb}]->(m) RETURN n, m, r.start_version, r.end_version";
        parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("r.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("r.end_version").asString().equals("null")) {
                end_version = record.get("r.end_version").asInt();
            }

            if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m")), "SoftwareSystemInctance"));
            }
        }

        // Проход по DeploymentNodes
        query = "MATCH (n:SoftwareSystem {graph: \"Global\", cmdb: $val1})-[r:Child]->(m:DeploymentNode) RETURN m.name, m.start_version, m.end_version";
        parameters = Values.parameters("val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Integer start_version = record.get("m.start_version").asInt();
            Integer end_version = cur_version;
            if (!record.get("m.end_version").asString().equals("null")) {
                end_version = record.get("m.end_version").asInt();
            }
            String depl_name = record.get("m.name").asString();

            if (v1 < start_version && start_version <= v2 && v2 <= end_version) {
                out.add(new Pair(depl_name, "DeploymentNode"));
            }

            CheckDeploymentNodes(depl_name, session, v1, v2, cur_version, cmdb, false);
        }

        return out;
    }
}
