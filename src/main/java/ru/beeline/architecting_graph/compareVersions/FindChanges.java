package ru.beeline.architecting_graph.compareVersions;

import java.util.HashSet;
import java.util.Set;
import org.neo4j.driver.Value;
import org.neo4j.driver.Result;
import org.neo4j.driver.Values;
import org.neo4j.driver.Session;

public class FindChanges {

    private static Set<Pair> out;

    public static String descriptionRelation(Value LeftNode, Value RightNode, String rel_type, String description) {
        String type = LeftNode.asNode().labels().toString();
        type = type.substring(1, type.length() - 1);

        String key = "name";
        if (type.equals("SoftwareSystem")) {
            key = "cmdb";
        }

        String val = LeftNode.asNode().asMap().get(key).toString();

        String res = "\t\t{\n" + "\t\t\t\"type\": \"Relation\",\n";
        res = res + "\t\t\t\"relation_type\": \"" + rel_type + "\",\n";
        res = res + "\t\t\t\"description\": \"" + description + "\",\n";
        res = res + "\t\t\t\"from\": {\n\t\t\t\t\"" + key + "\": \"" + val + "\",\n";
        res = res + "\t\t\t\t\"type\": \"" + type + "\",\n\t\t\t},\n";

        type = RightNode.asNode().labels().toString();
        type = type.substring(1, type.length() - 1);

        key = "name";
        if (type.equals("SoftwareSystem")) {
            key = "cmdb";
        }

        val = RightNode.asNode().asMap().get(key).toString();

        res = res + "\t\t\t\"to\": {\n\t\t\t\t\"" + key + "\": \"" + val + "\",\n";
        res = res + "\t\t\t\t\"type\": \"" + type + "\",\n\t\t\t}\n\t\t}";

        return res;
    }

    public static void CheckDeploymentNodes(String name, Session session, Integer v1, Integer v2, Integer cur_version,
            String cmdb, Boolean flag) {

        // Проход по прямым связям
        String query = "MATCH (n:DeploymentNode {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion, r.description";
        Value parameters = Values.parameters("cmdb", cmdb, "val1", name);
        Result result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("r.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("r.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            if (flag) {
                if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                            record.get("r.description").asString()), "Relationship"));
                }
            } else {
                if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                            record.get("r.description").asString()), "Relationship"));
                }
            }
        }

        // Проход по DeploymentNodes
        query = "MATCH (n:DeploymentNode {graphTag: \"Global\", name: $val1})-[r:Child]->(m:DeploymentNode) RETURN n, m, m.name, m.startVersion, m.endVersion";
        parameters = Values.parameters("val1", name);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("m.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("m.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            String depl_name = record.get("m.name").asString();

            if (flag) {
                if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                    out.add(new Pair(depl_name, "DeploymentNode"));
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"),
                            "Relationship"));
                }
            } else {
                if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                    out.add(new Pair(depl_name, "DeploymentNode"));
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"),
                            "Relationship"));
                }
            }

            CheckDeploymentNodes(depl_name, session, v1, v2, cur_version, cmdb, flag);
        }

        // Проход по InfrastructureNodes
        query = "MATCH (n:DeploymentNode {graphTag: \"Global\", name: $val1})-[r:Child]->(m:InfrastructureNode) RETURN n, m, m.name, m.startVersion, m.endVersion";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("m.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("m.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            String infr_name = record.get("m.name").asString();

            if (flag) {
                if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                    out.add(new Pair(infr_name, "InfrastructureNode"));
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"),
                            "Relationship"));
                }
            } else {
                if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                    out.add(new Pair(infr_name, "InfrastructureNode"));
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"),
                            "Relationship"));
                }
            }

            // Проход по прямым связям
            query = "MATCH (n:InfrastructureNode {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion, r.description";
            parameters = Values.parameters("cmdb", cmdb, "val1", infr_name);
            Result result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();

                startVersionString = record.get("r.startVersion").toString();
                startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
                startVersion = Integer.parseInt(startVersionString);

                endVersionString = record.get("r.endVersion").toString();
                endVersion = cur_version;
                if (!endVersionString.equals("NULL")) {
                    endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                    endVersion = Integer.parseInt(endVersionString);
                }

                if (flag) {
                    if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                        out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                                record.get("r.description").asString()), "Relationship"));
                    }
                } else {
                    if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                        out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                                record.get("r.description").asString()), "Relationship"));
                    }
                }
            }
        }

        // Проход по ContainerInstances
        query = "MATCH (n:DeploymentNode {graphTag: \"Global\", name: $val1})-[r:Child]->(m:ContainerInstance) RETURN n, m, m.name, m.startVersion, m.endVersion";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("m.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("m.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            String cont_ins_name = record.get("m.name").asString();

            if (flag) {
                if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                    out.add(new Pair(cont_ins_name, "ContainerInstance"));
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"),
                            "Relationship"));
                }
            } else {
                if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                    out.add(new Pair(cont_ins_name, "ContainerInstance"));
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"),
                            "Relationship"));
                }
            }

            // Проход по прямым связям
            query = "MATCH (n:ContainerInstance {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion, r.description";
            parameters = Values.parameters("cmdb", cmdb, "val1", cont_ins_name);
            Result result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();

                startVersionString = record.get("r.startVersion").toString();
                startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
                startVersion = Integer.parseInt(startVersionString);

                endVersionString = record.get("r.endVersion").toString();
                endVersion = cur_version;
                if (!endVersionString.equals("NULL")) {
                    endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                    endVersion = Integer.parseInt(endVersionString);
                }

                if (flag) {
                    if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                        out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                                record.get("r.description").asString()), "Relationship"));
                    }
                } else {
                    if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                        out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                                record.get("r.description").asString()), "Relationship"));
                    }
                }
            }
        }
    }

    public static Set<Pair> EarlierChanges(Integer v1, Integer v2, Integer cur_version, String cmdb, Session session) {
        out = new HashSet<>();

        // Проход по прямым связям системы
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion, r.description";
        Value parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        Result result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("r.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("r.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }

        // Проход по обратным связям системы
        query = "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1}) RETURN n, m, r.startVersion, r.endVersion, r.description";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("r.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("r.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                out.add(new Pair(descriptionRelation(record.get("m"), record.get("n"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }

        // Проход по контейнерам
        query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Child]->(m:Container) RETURN n, m, m.name, m.startVersion, m.endVersion";
        parameters = Values.parameters("val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("m.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("m.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            String cont_name = record.get("m.name").asString();

            if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                out.add(new Pair(cont_name, "Container"));
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child",
                        "Child"), "Relationship"));
            }

            // Проход по прямым связям контейнера
            query = "MATCH (n:Container {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion, r.description";
            parameters = Values.parameters("val1", cont_name, "cmdb", cmdb);
            Result result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();

                startVersionString = record.get("r.startVersion").toString();
                startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
                startVersion = Integer.parseInt(startVersionString);

                endVersionString = record.get("r.endVersion").toString();
                endVersion = cur_version;
                if (!endVersionString.equals("NULL")) {
                    endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                    endVersion = Integer.parseInt(endVersionString);
                }

                if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                            record.get("r.description").asString()), "Relationship"));
                }
            }

            // Проход по обратным связям контейнера
            query = "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:Container {graphTag: \"Global\", name: $val1}) RETURN n, m, r.startVersion, r.endVersion, r.description";
            result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();

                startVersionString = record.get("r.startVersion").toString();
                startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
                startVersion = Integer.parseInt(startVersionString);

                endVersionString = record.get("r.endVersion").toString();
                endVersion = cur_version;
                if (!endVersionString.equals("NULL")) {
                    endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                    endVersion = Integer.parseInt(endVersionString);
                }

                if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                    out.add(new Pair(descriptionRelation(record.get("m"), record.get("n"), "Relationship",
                            record.get("r.description").asString()), "Relationship"));
                }
            }

            // Добавление компонентов
            query = "MATCH (n:Container {graphTag: \"Global\", name: $val1})-[r:Child]->(m:Component) RETURN n, m, m.name, m.startVersion, m.endVersion";
            parameters = Values.parameters("val1", cont_name);
            result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();

                startVersionString = record.get("m.startVersion").toString();
                startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
                startVersion = Integer.parseInt(startVersionString);

                endVersionString = record.get("m.endVersion").toString();
                endVersion = cur_version;
                if (!endVersionString.equals("NULL")) {
                    endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                    endVersion = Integer.parseInt(endVersionString);
                }

                String comp_name = record.get("m.name").asString();

                if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                    out.add(new Pair(comp_name, "Component"));
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child",
                            "Child"), "Relationship"));
                }

                // Проход по прямым связям компонента
                query = "MATCH (n:Component {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion, r.description";
                parameters = Values.parameters("val1", comp_name, "cmdb", cmdb);
                Result result2 = session.run(query, parameters);

                while (result2.hasNext()) {
                    record = result2.next();

                    startVersionString = record.get("r.startVersion").toString();
                    startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
                    startVersion = Integer.parseInt(startVersionString);

                    endVersionString = record.get("r.endVersion").toString();
                    endVersion = cur_version;
                    if (!endVersionString.equals("NULL")) {
                        endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                        endVersion = Integer.parseInt(endVersionString);
                    }

                    if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                        out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                                record.get("r.description").asString()), "Relationship"));
                    }
                }

                // Проход по обратным связям компонента
                query = "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:Component {graphTag: \"Global\", name: $val1}) RETURN n, m, r.startVersion, r.endVersion, r.description";
                parameters = Values.parameters("val1", comp_name, "cmdb", cmdb);
                result2 = session.run(query, parameters);

                while (result2.hasNext()) {
                    record = result2.next();

                    startVersionString = record.get("r.startVersion").toString();
                    startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
                    startVersion = Integer.parseInt(startVersionString);

                    endVersionString = record.get("r.endVersion").toString();
                    endVersion = cur_version;
                    if (!endVersionString.equals("NULL")) {
                        endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                        endVersion = Integer.parseInt(endVersionString);
                    }

                    if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                        out.add(new Pair(descriptionRelation(record.get("m"), record.get("n"), "Relationship",
                                record.get("r.description").asString()), "Relationship"));
                    }
                }
            }
        }

        // Проход по SoftwareSystemInstances
        query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Deploy {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion";
        parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("r.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("r.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "SoftwareSystemInstance",
                        "Deploy"), "SoftwareSystemInstance"));
            }
        }

        // Проход по DeploymentNodes
        query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Child]->(m:DeploymentNode) RETURN n, m, m.name, m.startVersion, m.endVersion";
        parameters = Values.parameters("val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("m.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("m.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            String depl_name = record.get("m.name").asString();

            if (startVersion <= v1 && v1 <= endVersion && endVersion < v2) {
                out.add(new Pair(depl_name, "DeploymentNode"));
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child",
                        "Child"), "Relationship"));
            }

            CheckDeploymentNodes(depl_name, session, v1, v2, cur_version, cmdb, true);
        }

        return out;
    }

    public static Set<Pair> LaterChanges(Integer v1, Integer v2, Integer cur_version, String cmdb, Session session) {
        out = new HashSet<>();

        // Проход по прямым связям системы
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion, r.description";
        Value parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        Result result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("r.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("r.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }

        // Проход по обратным связям системы
        query = "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1}) RETURN n, m, r.startVersion, r.endVersion, r.description";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("r.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("r.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                out.add(new Pair(descriptionRelation(record.get("m"), record.get("n"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }

        // Проход по контейнерам
        query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Child]->(m:Container) RETURN n, m, m.name, m.startVersion, m.endVersion";
        parameters = Values.parameters("val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("m.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("m.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            String cont_name = record.get("m.name").asString();

            if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                out.add(new Pair(cont_name, "Container"));
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child",
                        "Child"), "Relationship"));
            }

            // Проход по прямым связям контейнера
            query = "MATCH (n:Container {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion, r.description";
            parameters = Values.parameters("val1", cont_name, "cmdb", cmdb);
            Result result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();

                startVersionString = record.get("r.startVersion").toString();
                startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
                startVersion = Integer.parseInt(startVersionString);

                endVersionString = record.get("r.endVersion").toString();
                endVersion = cur_version;
                if (!endVersionString.equals("NULL")) {
                    endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                    endVersion = Integer.parseInt(endVersionString);
                }

                if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                            record.get("r.description").asString()), "Relationship"));
                }
            }

            // Проход по обратным связям контейнера
            query = "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:Container {graphTag: \"Global\", name: $val1}) RETURN n, m, r.startVersion, r.endVersion, r.description";
            result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();

                startVersionString = record.get("r.startVersion").toString();
                startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
                startVersion = Integer.parseInt(startVersionString);

                endVersionString = record.get("r.endVersion").toString();
                endVersion = cur_version;
                if (!endVersionString.equals("NULL")) {
                    endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                    endVersion = Integer.parseInt(endVersionString);
                }

                if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                    out.add(new Pair(descriptionRelation(record.get("m"), record.get("n"), "Relationship",
                            record.get("r.description").asString()), "Relationship"));
                }
            }

            // Добавление компонентов
            query = "MATCH (n:Container {graphTag: \"Global\", name: $val1})-[r:Child]->(m:Component) RETURN n, m, m.name, m.startVersion, m.endVersion";
            parameters = Values.parameters("val1", cont_name);
            result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();

                startVersionString = record.get("m.startVersion").toString();
                startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
                startVersion = Integer.parseInt(startVersionString);

                endVersionString = record.get("m.endVersion").toString();
                endVersion = cur_version;
                if (!endVersionString.equals("NULL")) {
                    endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                    endVersion = Integer.parseInt(endVersionString);
                }

                String comp_name = record.get("m.name").asString();

                if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                    out.add(new Pair(comp_name, "Component"));
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child",
                            "Child"), "Relationship"));
                }

                // Проход по прямым связям компонента
                query = "MATCH (n:Component {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion, r.description";
                parameters = Values.parameters("val1", comp_name, "cmdb", cmdb);
                Result result2 = session.run(query, parameters);

                while (result2.hasNext()) {
                    record = result2.next();

                    startVersionString = record.get("r.startVersion").toString();
                    startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
                    startVersion = Integer.parseInt(startVersionString);

                    endVersionString = record.get("r.endVersion").toString();
                    endVersion = cur_version;
                    if (!endVersionString.equals("NULL")) {
                        endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                        endVersion = Integer.parseInt(endVersionString);
                    }

                    if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                        out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                                record.get("r.description").asString()), "Relationship"));
                    }
                }

                // Проход по обратным связям компонента
                query = "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:Component {graphTag: \"Global\", name: $val1}) RETURN n, m, r.startVersion, r.endVersion, r.description";
                parameters = Values.parameters("val1", comp_name, "cmdb", cmdb);
                result2 = session.run(query, parameters);

                while (result2.hasNext()) {
                    record = result2.next();

                    startVersionString = record.get("r.startVersion").toString();
                    startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
                    startVersion = Integer.parseInt(startVersionString);

                    endVersionString = record.get("r.endVersion").toString();
                    endVersion = cur_version;
                    if (!endVersionString.equals("NULL")) {
                        endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                        endVersion = Integer.parseInt(endVersionString);
                    }

                    if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                        out.add(new Pair(descriptionRelation(record.get("m"), record.get("n"), "Relationship",
                                record.get("r.description").asString()), "Relationship"));
                    }
                }
            }
        }

        // Проход по SoftwareSystemInstances
        query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Deploy {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion";
        parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("r.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("r.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "SoftwareSystemInstance",
                        "Deploy"), "SoftwareSystemInstance"));
            }
        }

        // Проход по DeploymentNodes
        query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Child]->(m:DeploymentNode) RETURN n, m, m.name, m.startVersion, m.endVersion";
        parameters = Values.parameters("val1", cmdb);
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String startVersionString = record.get("m.startVersion").toString();
            startVersionString = startVersionString.substring(1, startVersionString.length() - 1);
            Integer startVersion = Integer.parseInt(startVersionString);

            String endVersionString = record.get("m.endVersion").toString();
            Integer endVersion = cur_version;
            if (!endVersionString.equals("NULL")) {
                endVersionString = endVersionString.substring(1, endVersionString.length() - 1);
                endVersion = Integer.parseInt(endVersionString);
            }

            String depl_name = record.get("m.name").asString();

            if (v1 < startVersion && startVersion <= v2 && v2 <= endVersion) {
                out.add(new Pair(depl_name, "DeploymentNode"));
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child",
                        "Child"), "Relationship"));
            }

            CheckDeploymentNodes(depl_name, session, v1, v2, cur_version, cmdb, false);
        }

        return out;
    }
}
