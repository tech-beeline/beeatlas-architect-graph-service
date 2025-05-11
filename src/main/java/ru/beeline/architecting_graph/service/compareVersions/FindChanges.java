package ru.beeline.architecting_graph.service.compareVersions;

import org.neo4j.driver.Record;
import org.neo4j.driver.*;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
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

    public void checkDeploymentNodes(String name, Session session, Integer v1, Integer v2,
                                     Integer curVersion, String cmdb, Boolean flag) {
        processRelationships("DeploymentNode", name, session, v1, v2, curVersion, cmdb, flag);
        processChildren("DeploymentNode", "DeploymentNode", name, session, v1, v2, curVersion, cmdb, flag);
        processChildren("DeploymentNode", "InfrastructureNode", name, session, v1, v2, curVersion, cmdb, flag);
        processChildren("DeploymentNode", "ContainerInstance", name, session, v1, v2, curVersion, cmdb, flag);
    }

    private boolean isVersionInRange(int start, int end, int v1, int v2, boolean flag) {
        return flag ? (start <= v1 && v1 <= end && end < v2)
                : (v1 < start && start <= v2 && v2 <= end);
    }

    private int parseVersion(Value value, int fallback) {
        String str = value.toString();
        if (str.equals("NULL")) return fallback;
        str = str.substring(1, str.length() - 1);
        return Integer.parseInt(str);
    }

    private void processRelationships(String label, String name, Session session, int v1, int v2, int curVersion,
                                      String cmdb, boolean flag) {
        String query = "MATCH (n:" + label + " {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) " +
                "RETURN n, m, r.startVersion, r.endVersion, r.description";
        Value params = Values.parameters("cmdb", cmdb, "val1", name);
        Result result = session.run(query, params);
        while (result.hasNext()) {
            Record record = result.next();
            int start = parseVersion(record.get("r.startVersion"), 0);
            int end = parseVersion(record.get("r.endVersion"), curVersion);
            if (isVersionInRange(start, end, v1, v2, flag)) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }
    }

    private void processChildren(String parentType, String childType, String parentName, Session session,
                                 int v1, int v2, int curVersion, String cmdb, boolean flag) {
        String query = "MATCH (n:" + parentType + " {graphTag: \"Global\", name: $val1})-[r:Child]->(m:" + childType + ") " +
                "RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value params = Values.parameters("val1", parentName);
        Result result = session.run(query, params);
        while (result.hasNext()) {
            Record record = result.next();
            int start = parseVersion(record.get("m.startVersion"), 0);
            int end = parseVersion(record.get("m.endVersion"), curVersion);
            String name = record.get("m.name").asString();
            if (isVersionInRange(start, end, v1, v2, flag)) {
                out.add(new Pair(name, childType));
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"), "Relationship"));
            }
            if (childType.equals("DeploymentNode")) {
                checkDeploymentNodes(name, session, v1, v2, curVersion, cmdb, flag);
            }
            if (!childType.equals("DeploymentNode")) {
                processRelationships(childType, name, session, v1, v2, curVersion, cmdb, flag);
            }
        }
    }

    public Set<Pair> EarlierChanges(Integer v1, Integer v2, Integer cur_version, String cmdb, Session session) {
        out = new HashSet<>();
        processSystemRelationships(v1, v2, cur_version, cmdb, session);
        processContainers(v1, v2, cur_version, cmdb, session);
        processSoftwareSystemInstances(v1, v2, cur_version, cmdb, session);
        processDeploymentNodes(v1, v2, cur_version, cmdb, session);
        return out;
    }

    private void processSystemRelationships(Integer v1, Integer v2, Integer cur_version, String cmdb, Session session) {
        // Обработка прямых связей системы
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion, r.description";
        Value parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        Result result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer startVersionRelationship = getVersion(record.get("r.startVersion").toString(), cur_version);
            Integer endVersionRelationship = getVersion(record.get("r.endVersion").toString(), cur_version);
            if (startVersionRelationship <= v1 && v1 <= endVersionRelationship && endVersionRelationship < v2) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }
        // Обработка обратных связей системы
        query = "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1}) RETURN n, m, r.startVersion, r.endVersion, r.description";
        result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer startVersionRelationship = getVersion(record.get("r.startVersion").toString(), cur_version);
            Integer endVersionRelationship = getVersion(record.get("r.endVersion").toString(), cur_version);
            if (startVersionRelationship <= v1 && v1 <= endVersionRelationship && endVersionRelationship < v2) {
                out.add(new Pair(descriptionRelation(record.get("m"), record.get("n"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }
    }

    private void processContainers(Integer v1, Integer v2, Integer cur_version, String cmdb, Session session) {
        // Обработка контейнеров и их связей
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Child]->(m:Container) RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", cmdb);
        Result result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer startVersionContainer = getVersion(record.get("m.startVersion").toString(), cur_version);
            Integer endVersionContainer = getVersion(record.get("m.endVersion").toString(), cur_version);
            String containerName = record.get("m.name").asString();
            if (startVersionContainer <= v1 && v1 <= endVersionContainer && endVersionContainer < v2) {
                out.add(new Pair(containerName, "Container"));
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"), "Relationship"));
            }
            processContainerRelationships(containerName, v1, v2, cur_version, cmdb, session);
            processComponents(containerName, v1, v2, cur_version, cmdb, session);
        }
    }

    private void processContainerRelationships(String containerName, Integer v1, Integer v2, Integer cur_version, String cmdb, Session session) {
        // Обработка связей контейнера
        String query = "MATCH (n:Container {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion, r.description";
        Value parameters = Values.parameters("val1", containerName, "cmdb", cmdb);
        Result containerResult = session.run(query, parameters);
        while (containerResult.hasNext()) {
            Record record = containerResult.next();
            Integer startVersionContainerRel = getVersion(record.get("r.startVersion").toString(), cur_version);
            Integer endVersionContainerRel = getVersion(record.get("r.endVersion").toString(), cur_version);
            if (startVersionContainerRel <= v1 && v1 <= endVersionContainerRel && endVersionContainerRel < v2) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }
        // Обработка обратных связей контейнера
        query = "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:Container {graphTag: \"Global\", name: $val1}) RETURN n, m, r.startVersion, r.endVersion, r.description";
        containerResult = session.run(query, parameters);
        while (containerResult.hasNext()) {
            Record record = containerResult.next();
            Integer startVersionContainerRelRev = getVersion(record.get("r.startVersion").toString(), cur_version);
            Integer endVersionContainerRelRev = getVersion(record.get("r.endVersion").toString(), cur_version);
            if (startVersionContainerRelRev <= v1 && v1 <= endVersionContainerRelRev && endVersionContainerRelRev < v2) {
                out.add(new Pair(descriptionRelation(record.get("m"), record.get("n"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }
    }

    private void processComponents(String containerName, Integer v1, Integer v2, Integer cur_version, String cmdb, Session session) {
        // Обработка компонентов и их связей
        String query = "MATCH (n:Container {graphTag: \"Global\", name: $val1})-[r:Child]->(m:Component) RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", containerName);
        Result componentResult = session.run(query, parameters);
        while (componentResult.hasNext()) {
            Record record = componentResult.next();
            Integer startVersionComponent = getVersion(record.get("m.startVersion").toString(), cur_version);
            Integer endVersionComponent = getVersion(record.get("m.endVersion").toString(), cur_version);
            String componentName = record.get("m.name").asString();
            if (startVersionComponent <= v1 && v1 <= endVersionComponent && endVersionComponent < v2) {
                out.add(new Pair(componentName, "Component"));
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"), "Relationship"));
            }
            // Обработка связей компонента
            query = "MATCH (n:Component {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion, r.description";
            parameters = Values.parameters("val1", componentName, "cmdb", cmdb);
            Result componentRelResult = session.run(query, parameters);
            while (componentRelResult.hasNext()) {
                record = componentRelResult.next();
                Integer startVersionComponentRel = getVersion(record.get("r.startVersion").toString(), cur_version);
                Integer endVersionComponentRel = getVersion(record.get("r.endVersion").toString(), cur_version);

                if (startVersionComponentRel <= v1 && v1 <= endVersionComponentRel && endVersionComponentRel < v2) {
                    out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                            record.get("r.description").asString()), "Relationship"));
                }
            }
            // Обработка обратных связей компонента
            query = "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:Component {graphTag: \"Global\", name: $val1}) RETURN n, m, r.startVersion, r.endVersion, r.description";
            componentRelResult = session.run(query, parameters);
            while (componentRelResult.hasNext()) {
                record = componentRelResult.next();
                Integer startVersionComponentRelRev = getVersion(record.get("r.startVersion").toString(), cur_version);
                Integer endVersionComponentRelRev = getVersion(record.get("r.endVersion").toString(), cur_version);
                if (startVersionComponentRelRev <= v1 && v1 <= endVersionComponentRelRev && endVersionComponentRelRev < v2) {
                    out.add(new Pair(descriptionRelation(record.get("m"), record.get("n"), "Relationship",
                            record.get("r.description").asString()), "Relationship"));
                }
            }
        }
    }

    private void processSoftwareSystemInstances(Integer v1, Integer v2, Integer cur_version, String cmdb, Session session) {
        // Обработка экземпляров программных систем
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Deploy {sourceWorkspace: $cmdb}]->(m) RETURN n, m, r.startVersion, r.endVersion";
        Value parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        Result result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer startVersionDeploy = getVersion(record.get("r.startVersion").toString(), cur_version);
            Integer endVersionDeploy = getVersion(record.get("r.endVersion").toString(), cur_version);
            if (startVersionDeploy <= v1 && v1 <= endVersionDeploy && endVersionDeploy < v2) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "SoftwareSystemInstance",
                        "Deploy"), "SoftwareSystemInstance"));
            }
        }
    }

    private void processDeploymentNodes(Integer v1, Integer v2, Integer cur_version, String cmdb, Session session) {
        // Обработка узлов развертывания
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Child]->(m:DeploymentNode) RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", cmdb);
        Result result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer startVersionDeploymentNode = getVersion(record.get("m.startVersion").toString(), cur_version);
            Integer endVersionDeploymentNode = getVersion(record.get("m.endVersion").toString(), cur_version);
            String deploymentNodeName = record.get("m.name").asString();
            if (startVersionDeploymentNode <= v1 && v1 <= endVersionDeploymentNode && endVersionDeploymentNode < v2) {
                out.add(new Pair(deploymentNodeName, "DeploymentNode"));
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"), "Relationship"));
            }
            checkDeploymentNodes(deploymentNodeName, session, v1, v2, cur_version, cmdb, true);
        }
    }

    // Метод для извлечения и преобразования версии
    private Integer getVersion(String versionString, Integer cur_version) {
        if (versionString.equals("NULL")) {
            return cur_version;
        }
        versionString = versionString.substring(1, versionString.length() - 1); // Убираем кавычки
        return Integer.parseInt(versionString); // Преобразуем в Integer
    }

    public Set<Pair> LaterChanges(Integer v1, Integer v2, Integer cur_version, String cmdb, Session session) {
        out = new HashSet<>();
        processSystemRelationships(cmdb, v1, v2, cur_version, session);
        processContainers(cmdb, v1, v2, cur_version, session);
        processSoftwareSystemInstances(cmdb, v1, v2, cur_version, session);
        processDeploymentNodes(cmdb, v1, v2, cur_version, session);
        return out;
    }

    private Integer getParsedVersion(String versionString, Integer cur_version) {
        if (versionString.equals("NULL")) {
            return cur_version;
        }
        versionString = versionString.substring(1, versionString.length() - 1);
        return Integer.parseInt(versionString);
    }

    private void processSystemRelationships(String cmdb, Integer v1, Integer v2, Integer cur_version, Session session) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) " +
                "RETURN n, m, r.startVersion, r.endVersion, r.description";
        Value parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        Result result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer start = getParsedVersion(record.get("r.startVersion").toString(), cur_version);
            Integer end = getParsedVersion(record.get("r.endVersion").toString(), cur_version);
            if (v1 < start && start <= v2 && v2 <= end) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }
        query = "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1}) " +
                "RETURN n, m, r.startVersion, r.endVersion, r.description";
        result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer start = getParsedVersion(record.get("r.startVersion").toString(), cur_version);
            Integer end = getParsedVersion(record.get("r.endVersion").toString(), cur_version);
            if (v1 < start && start <= v2 && v2 <= end) {
                out.add(new Pair(descriptionRelation(record.get("m"), record.get("n"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }
    }

    private void processContainers(String cmdb, Integer v1, Integer v2, Integer cur_version, Session session) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Child]->(m:Container) " +
                "RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", cmdb);
        Result result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer start = getParsedVersion(record.get("m.startVersion").toString(), cur_version);
            Integer end = getParsedVersion(record.get("m.endVersion").toString(), cur_version);
            String containerName = record.get("m.name").asString();
            if (v1 < start && start <= v2 && v2 <= end) {
                out.add(new Pair(containerName, "Container"));
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"), "Relationship"));
            }
            processNodeRelationships("Container", containerName, cmdb, v1, v2, cur_version, session);
            processComponents(containerName, cmdb, v1, v2, cur_version, session);
        }
    }

    private void processComponents(String containerName, String cmdb, Integer v1, Integer v2, Integer cur_version, Session session) {
        String query = "MATCH (n:Container {graphTag: \"Global\", name: $val1})-[r:Child]->(m:Component) " +
                "RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", containerName);
        Result result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer start = getParsedVersion(record.get("m.startVersion").toString(), cur_version);
            Integer end = getParsedVersion(record.get("m.endVersion").toString(), cur_version);
            String componentName = record.get("m.name").asString();
            if (v1 < start && start <= v2 && v2 <= end) {
                out.add(new Pair(componentName, "Component"));
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"), "Relationship"));
            }
            processNodeRelationships("Component", componentName, cmdb, v1, v2, cur_version, session);
        }
    }

    private void processNodeRelationships(String label, String name, String cmdb, Integer v1, Integer v2, Integer cur_version, Session session) {
        String query = String.format("MATCH (n:%s {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) " +
                "RETURN n, m, r.startVersion, r.endVersion, r.description", label);
        Value parameters = Values.parameters("val1", name, "cmdb", cmdb);
        Result result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer start = getParsedVersion(record.get("r.startVersion").toString(), cur_version);
            Integer end = getParsedVersion(record.get("r.endVersion").toString(), cur_version);
            if (v1 < start && start <= v2 && v2 <= end) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }
        query = String.format("MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:%s {graphTag: \"Global\", name: $val1}) " +
                "RETURN n, m, r.startVersion, r.endVersion, r.description", label);
        result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer start = getParsedVersion(record.get("r.startVersion").toString(), cur_version);
            Integer end = getParsedVersion(record.get("r.endVersion").toString(), cur_version);
            if (v1 < start && start <= v2 && v2 <= end) {
                out.add(new Pair(descriptionRelation(record.get("m"), record.get("n"), "Relationship",
                        record.get("r.description").asString()), "Relationship"));
            }
        }
    }

    private void processSoftwareSystemInstances(String cmdb, Integer v1, Integer v2, Integer cur_version, Session session) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Deploy {sourceWorkspace: $cmdb}]->(m) " +
                "RETURN n, m, r.startVersion, r.endVersion";
        Value parameters = Values.parameters("cmdb", cmdb, "val1", cmdb);
        Result result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer start = getParsedVersion(record.get("r.startVersion").toString(), cur_version);
            Integer end = getParsedVersion(record.get("r.endVersion").toString(), cur_version);
            if (v1 < start && start <= v2 && v2 <= end) {
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "SoftwareSystemInstance",
                        "Deploy"), "SoftwareSystemInstance"));
            }
        }
    }

    private void processDeploymentNodes(String cmdb, Integer v1, Integer v2, Integer cur_version, Session session) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Child]->(m:DeploymentNode) " +
                "RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", cmdb);
        Result result = session.run(query, parameters);
        while (result.hasNext()) {
            Record record = result.next();
            Integer start = getParsedVersion(record.get("m.startVersion").toString(), cur_version);
            Integer end = getParsedVersion(record.get("m.endVersion").toString(), cur_version);
            String name = record.get("m.name").asString();
            if (v1 < start && start <= v2 && v2 <= end) {
                out.add(new Pair(name, "DeploymentNode"));
                out.add(new Pair(descriptionRelation(record.get("n"), record.get("m"), "Child", "Child"), "Relationship"));
            }
            checkDeploymentNodes(name, session, v1, v2, cur_version, cmdb, false);
        }
    }
}
