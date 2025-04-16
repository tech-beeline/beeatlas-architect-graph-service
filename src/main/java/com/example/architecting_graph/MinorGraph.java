package com.example.architecting_graph;

import org.neo4j.driver.*;

import java.util.HashMap;
import java.util.Map;

public class MinorGraph {

    private static HashMap<String, Object> cont = new HashMap<>();
    private static HashMap<String, Object> comp = new HashMap<>();
    private static HashMap<String, Object> deplNode = new HashMap<>();
    private static HashMap<String, Object> infNode = new HashMap<>();
    private static HashMap<String, Object> contIns = new HashMap<>();
    private static HashMap<String, Object> env_id = new HashMap<>();
    private static HashMap<String, String> env_name = new HashMap<>();

    public static boolean checkIfObjectExists(Session session, String label, String propertyKey, Object propertyValue) {
        String query = "MATCH (n:" + label + " {" + propertyKey + ": $value, graph: \"Local\"}) RETURN n";
        Result result = session.run(query, Values.parameters("value", propertyValue));
        return result.hasNext();
    }

    public static void createSoftware(Session session, SoftwareSystem softwareSystem, Object cmdb) {
        String createNodeQuery = "CREATE (n:SoftwareSystem {graph: \"Local\", cmdb: $cmdb1, name: $name1, description: $description1, tags: $tags1, url: $url1, group: $group1}) RETURN n";
        Value parameters = Values.parameters("cmdb1", cmdb, "name1", softwareSystem.getName(),
                "description1", softwareSystem.getDescription(), "tags1", softwareSystem.getTags(),
                "url1", softwareSystem.getUrl(), "group1", softwareSystem.getGroup());
        session.run(createNodeQuery, parameters);

        // Добавление property
        if (softwareSystem.getProperties() != null) {
            for (Map.Entry<String, Object> entry : softwareSystem.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replace(' ', '_');
                key = key.replace('.', '_');
                createNodeQuery = "MATCH (n:SoftwareSystem {graph: \"Local\", cmdb: $cmdb1}) SET n." + key
                        + " = $value RETURN n";
                parameters = Values.parameters("cmdb1", cmdb, "value", entry.getValue());
                session.run(createNodeQuery, parameters);
            }
        }
    }

    public static void createContainer(Session session, Container container) {
        String createNodeQuery = "CREATE (n:Container {graph: \"Local\", name: $name1, description: $description1, technology: $technology1, tags: $tags1, url: $url1, group: $group1}) RETURN n";
        Value parameters = Values.parameters("name1", container.getName(), "description1",
                container.getDescription(), "technology1", container.getTechnology(), "tags1",
                container.getTags(), "url1", container.getUrl(), "group1", container.getGroup());
        session.run(createNodeQuery, parameters);

        // Добавление property
        if (container.getProperties() != null) {
            for (Map.Entry<String, Object> entry : container.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replace(' ', '_');
                key = key.replace('.', '_');
                createNodeQuery = "MATCH (n:Container {graph: \"Local\", name: $name1}) SET n." + key
                        + " = $value RETURN n";
                parameters = Values.parameters("name1", container.getName(), "value", entry.getValue());
                session.run(createNodeQuery, parameters);
            }
        }
    }

    public static void createComponent(Session session, Component component) {
        String createNodeQuery = "CREATE (n:Component {graph: \"Local\", name: $name1, description: $description1, technology: $technology1, tags: $tags1, url: $url1, group: $group1}) RETURN n";
        Value parameters = Values.parameters("name1", component.getName(), "description1",
                component.getDescription(), "technology1", component.getTechnology(), "tags1",
                component.getTags(), "url1", component.getUrl(), "group1", component.getGroup());
        session.run(createNodeQuery, parameters);

        // Добавление property
        if (component.getProperties() != null) {
            for (Map.Entry<String, Object> entry : component.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replace(' ', '_');
                key = key.replace('.', '_');
                createNodeQuery = "MATCH (n:Component {graph: \"Local\", name: $name1}) SET n." + key
                        + " = $value RETURN n";
                parameters = Values.parameters("name1", component.getName(), "value", entry.getValue());
                session.run(createNodeQuery, parameters);
            }
        }
    }

    public static void updateRelation(Session session, Relationship rel, String rel_type, String type1, String key1,
            Object val1, String type2, String key2, Object val2, String level, Object cmdb) {

        if (rel_type.equals("Child")) {
            return;
        }

        String updateNode = "MATCH (a:" + type1 + " {graph: \"Local\", " + key1 + ": $val1})-[r:"
                + rel_type + " {graph: \"Local\", source_workspace: $cmdb, description: $description1}]->(b:" + type2
                + " {graph: \"Local\", " + key2
                + ": $val2}) SET r.tags = $tags1,  r.url = $url1, r.technology = $technology1,  r.interactionStyle = $interactionStyle1, r.level = $level1 RETURN r";
        Value parameters = Values.parameters("val1", val1, "cmdb", cmdb, "description1", rel.getDescription(), "val2",
                val2, "tags1", rel.getTags(), "url1", rel.getUrl(), "technology1",
                rel.getTechnology(), "interactionStyle1", rel.getInteractionStyle(), "level1", level);
        session.run(updateNode, parameters);

        if (rel.getDescription().equals("None")) {
            Integer number = 0;

            // Вычисление текущего количества связей
            updateNode = "MATCH (a:" + type1 + " {graph: \"Local\", " + key1 + ": $val1})-[r:" + rel_type
                    + " {graph: \"Local\", source_workspace: $cmdb, description: $description1}]->(b:" + type2
                    + " {graph: \"Local\", " + key2 + ": $val2}) RETURN r";
            parameters = Values.parameters("val1", val1, "cmdb", cmdb, "description1", rel.getDescription(),
                    "val2", val2);
            Result result = session.run(updateNode, parameters);

            org.neo4j.driver.Record record = result.next();

            String cur_id = record.get("r").asRelationship().get("cur_id").toString();
            number = record.get("r").asRelationship().get("number_of_connects").asInt();

            if (!cur_id.equals(rel.getId())) {
                number = number + 1;
            }

            updateNode = "MATCH (a:" + type1 + " {graph: \"Local\", " + key1 + ": $val1})-[r:" + rel_type
                    + " {graph: \"Local\", source_workspace: $cmdb, description: $description1}]->(b:" + type2
                    + " {graph: \"Local\", " + key2
                    + ": $val2}) SET r.number_of_connects = $number_of_connects1, r.cur_id = $cur_id1";
            parameters = Values.parameters("val1", val1, "cmdb", cmdb, "description1", rel.getDescription(), "val2",
                    val2, "number_of_connects1", number, "cur_id1", rel.getId());
            session.run(updateNode, parameters);
        }

        // Обновление property
        if (rel.getProperties() != null) {
            for (Map.Entry<String, Object> entry : rel.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replace(' ', '_');
                key = key.replace('.', '_');
                updateNode = "MATCH (a:" + type1 + " {graph: \"Local\", " + key1 + ": $val1})-[r:" + rel_type
                        + " {graph: \"Local\", source_workspace: $cmdb, description: $description1}]->(b:" + type2 +
                        " {graph: \"Local\", " + key2 + ": $val2}) SET r." + key + " = $value";
                parameters = Values.parameters("val1", val1, "cmdb", cmdb, "description1", rel.getDescription(), "val2",
                        val2, "value", entry.getValue());
                session.run(updateNode, parameters);
            }
        }
    }

    public static String[] findObject(Session session, Model model, String id) {

        // Проход по всем системам
        for (SoftwareSystem system : model.getSoftwareSystems()) {
            String cmdb = null;
            if (system.getProperties() != null && system.getProperties().containsKey("cmdb")) {
                cmdb = system.getProperties().get("cmdb").toString();
            } else {
                continue;
            }

            if (id.equals(system.getId())) {

                // Проверка на существование
                boolean exists = checkIfObjectExists(session, "SoftwareSystem", "cmdb", cmdb);

                if (!exists) {
                    String createNodeQuery = "CREATE (n:SoftwareSystem {graph: \"Local\", cmdb: $cmdb1, name: $name1, description: $description1, tags: $tags1, url: $url1, group: $group1}) RETURN n";
                    Value parameters = Values.parameters("cmdb1", cmdb, "name1", system.getName(),
                            "description1", system.getDescription(), "tags1", system.getTags(),
                            "url1", system.getUrl(), "group1", system.getGroup());
                    session.run(createNodeQuery, parameters);

                    // Добавление property
                    if (system.getProperties() != null) {
                        for (Map.Entry<String, Object> entry : system.getProperties().entrySet()) {
                            String key = entry.getKey();
                            key = key.replace(' ', '_');
                            key = key.replace('.', '_');
                            createNodeQuery = "MATCH (n:SoftwareSystem {graph: \"Local\", cmdb: $cmdb1}) SET n." + key
                                    + " = $value RETURN n";
                            parameters = Values.parameters("cmdb1", cmdb, "value", entry.getValue());
                            session.run(createNodeQuery, parameters);
                        }
                    }
                }

                String[] answer = { "SoftwareSystem", "cmdb", cmdb };
                return answer;
            }

            // Проход по всем контейнерам системы
            if (system.getContainers() == null) {
                continue;
            }
            for (Container cont : system.getContainers()) {
                String cont_name = cmdb;
                String clear_cont_name = null;
                if (cont.getProperties() != null && cont.getProperties().containsKey("external_name")) {
                    clear_cont_name = cont.getProperties().get("external_name").toString();
                    cont_name = clear_cont_name + "." + cont_name;
                }

                if (id.equals(cont.getId())) {

                    // Проверка на существование
                    boolean pre_exists = checkIfObjectExists(session, "Container", "external_name", clear_cont_name);
                    if (pre_exists) {
                        String[] answer = { "Container", "external_name", clear_cont_name };
                        return answer;
                    }

                    boolean exists = checkIfObjectExists(session, "Container", "external_name", cont_name);

                    if (!exists) {
                        String createNodeQuery = "CREATE (n:Container {graph: \"Local\", external_name: $external_name1, name: $name1, description: $description1, technology: $technology1, tags: $tags1, url: $url1, group: $group1}) RETURN n";
                        Value parameters = Values.parameters("external_name1", cont_name, "name1", cont.getName(),
                                "description1", cont.getDescription(), "technology1", cont.getTechnology(), "tags1",
                                cont.getTags(), "url1", cont.getUrl(), "group1", cont.getGroup());
                        session.run(createNodeQuery, parameters);

                        // Добавление property
                        if (cont.getProperties() != null) {
                            for (Map.Entry<String, Object> entry : cont.getProperties().entrySet()) {
                                String key = entry.getKey();
                                key = key.replace(' ', '_');
                                key = key.replace('.', '_');
                                if (key.equals("external_name")) {
                                    continue;
                                }
                                createNodeQuery = "MATCH (n:Container {graph: \"Local\", external_name: $external_name1}) SET n."
                                        + key + " = $value RETURN n";
                                parameters = Values.parameters("external_name1", cont_name, "value", entry.getValue());
                                session.run(createNodeQuery, parameters);
                            }
                        }

                        // Проверка на существование системы
                        findObject(session, model, system.getId());

                        String createRelationshipQuery = "MATCH (a:SoftwareSystem {graph: \"Local\", cmdb: $val1}), (b:Container {graph: \"Local\", name: $val2}) CREATE (a)-[r:Child {graph: \"Local\", source_workspace: $cmdb, description: $description1}]->(b) RETURN a, b";
                        parameters = Values.parameters("val1", cmdb, "cmdb", cmdb, "val2", cont.getName(),
                                "description1", "Child");
                        session.run(createRelationshipQuery, parameters);
                    }

                    String[] answer = { "Container", "external_name", cont_name };
                    return answer;
                }

                // Проход по всем компонентам системы
                if (cont.getComponents() == null) {
                    continue;
                }
                for (Component comp : cont.getComponents()) {
                    String comp_name = cont_name;
                    String clear_comp_name = null;
                    if (comp.getProperties() != null && comp.getProperties().containsKey("external_name")) {
                        clear_comp_name = comp.getProperties().get("external_name").toString();
                        comp_name = clear_comp_name + "." + comp_name;
                    }
                    if (id.equals(comp.getId())) {

                        // Проверка на существование
                        boolean pre_exists = checkIfObjectExists(session, "Component", "external_name",
                                clear_comp_name);
                        if (pre_exists) {
                            String[] answer = { "Component", "external_name", clear_comp_name };
                            return answer;
                        }

                        boolean exists = checkIfObjectExists(session, "Component", "external_name", comp_name);

                        if (!exists) {
                            String createNodeQuery = "CREATE (n:Component {external_name: $external_name1, name: $name1, description: $description1, technology: $technology1, tags: $tags1, url: $url1, group: $group1}) RETURN n";
                            Value parameters = Values.parameters("external_name1", comp_name, "name1", comp.getName(),
                                    "description1", comp.getDescription(), "technology1", comp.getTechnology(), "tags1",
                                    comp.getTags(), "url1", comp.getUrl(), "group1", comp.getGroup());
                            session.run(createNodeQuery, parameters);

                            // Добавление property
                            if (comp.getProperties() != null) {
                                for (Map.Entry<String, Object> entry : comp.getProperties().entrySet()) {
                                    String key = entry.getKey();
                                    key = key.replace(' ', '_');
                                    key = key.replace('.', '_');
                                    if (key.equals("external_name")) {
                                        continue;
                                    }
                                    createNodeQuery = "MATCH (n:Component {graph: \"Local\", external_name: $external_name1}) SET n."
                                            + key + " = $value RETURN n";
                                    parameters = Values.parameters("external_name1", comp_name, "value",
                                            entry.getValue());
                                    session.run(createNodeQuery, parameters);
                                }
                            }

                            // Проверка на существование контейнера
                            findObject(session, model, cont.getId());

                            String createRelationshipQuery = "MATCH (a:Container {graph: \"Local\", name: $val1}), (b:Component {graph: \"Local\", name: $val2}) CREATE (a)-[r:Child {graph: \"Local\", source_workspace: $cmdb, description: $description1}]->(b) RETURN a, b";
                            parameters = Values.parameters("val1", cont.getName(), "cmdb", cmdb, "val2", comp.getName(),
                                    "description1", "Child");
                            session.run(createRelationshipQuery, parameters);
                        }

                        String[] answer = { "Component", "external_name", comp_name };
                        return answer;
                    }
                }
            }
        }

        String[] answer = { null, null, null };
        return answer;
    }

    public static void createRelation(Session session, Relationship rel, SoftwareSystem softwareSystem, String rel_type,
            Object cmdb, Model model, String level) {

        String type1;
        String key1;
        Object val1;

        String type2;
        String key2;
        Object val2;

        String source = rel.getSourceId();
        String destination = rel.getDestinationId();

        // Проверка на принадлежность текущей системе
        if (source.equals(softwareSystem.getId())) {
            type1 = "SoftwareSystem";
            key1 = "cmdb";
            val1 = cmdb;
        }
        if (cont.containsKey(source)) {
            type1 = "Container";
            key1 = "name";
            val1 = cont.get(source);
        } else if (comp.containsKey(source)) {
            type1 = "Component";
            key1 = "name";
            val1 = comp.get(source);
        } else if (deplNode.containsKey(source)) {
            type1 = "DeploymentNode";
            key1 = "name";
            val1 = deplNode.get(source);
        } else if (infNode.containsKey(source)) {
            type1 = "InfrastructureNode";
            key1 = "name";
            val1 = infNode.get(source);
        } else if (contIns.containsKey(source)) {
            type1 = "ContainerInstance";
            key1 = "name";
            val1 = contIns.get(source);
        } else if (env_id.containsKey(source)) {
            type1 = "Environment";
            key1 = "name";
            val1 = env_id.get(source);
        } else {
            String[] answer = findObject(session, model, source);
            type1 = answer[0];
            key1 = answer[1];
            val1 = answer[2];
        }

        if (val1 == null) {
            return;
        }

        // Проверка на принадлежность текущей системе
        if (destination.equals(softwareSystem.getId())) {
            type2 = "SoftwareSystem";
            key2 = "cmdb";
            val2 = cmdb;
        }
        if (cont.containsKey(destination)) {
            type2 = "Container";
            key2 = "name";
            val2 = cont.get(destination);
        } else if (comp.containsKey(destination)) {
            type2 = "Component";
            key2 = "name";
            val2 = comp.get(destination);
        } else if (deplNode.containsKey(destination)) {
            type2 = "DeploymentNode";
            key2 = "name";
            val2 = deplNode.get(destination);
        } else if (infNode.containsKey(destination)) {
            type2 = "InfrastructureNode";
            key2 = "name";
            val2 = infNode.get(destination);
        } else if (contIns.containsKey(destination)) {
            type2 = "ContainerInstance";
            key2 = "name";
            val2 = contIns.get(destination);
        } else if (env_id.containsKey(destination)) {
            type2 = "Environment";
            key2 = "name";
            val2 = env_id.get(destination);
        } else {
            String[] answer = findObject(session, model, destination);
            type2 = answer[0];
            key2 = answer[1];
            val2 = answer[2];
        }

        if (val2 == null) {
            return;
        }

        // Проверка на существование связи
        String createRelationshipQuery = "MATCH (a:" + type1 + " {graph: \"Local\", " + key1 + ": $val1})-[r:"
                + rel_type + "]->(b:" + type2 + " {graph: \"Local\", " + key2
                + ": $val2}) WHERE r.source_workspace = $cmdb AND r.description = $description1 AND r.graph = \"Local\"  RETURN EXISTS((a)-->(b)) AS relationship_exists";
        Value parameters = Values.parameters("val1", val1, "val2", val2, "cmdb", cmdb, "description1",
                rel.getDescription());
        Result result = session.run(createRelationshipQuery, parameters);
        if (result.hasNext()) {
            updateRelation(session, rel, rel_type, type1, key1, val1, type2, key2, val2, level, cmdb);
            return;
        }

        // Создание соединения
        createRelationshipQuery = "MATCH (a:" + type1 + " {graph: \"Local\", " + key1 + ": $val1}), (b:" + type2
                + " {graph: \"Local\", " + key2 + ": $val2}) CREATE (a)-[r:" + rel_type
                + " {graph: \"Local\", source_workspace: $cmdb, description: $description1}]->(b) RETURN a, b";
        parameters = Values.parameters("val1", val1, "cmdb", cmdb, "val2", val2,
                "description1", rel.getDescription());
        session.run(createRelationshipQuery, parameters);

        if (rel.getDescription().equals("None")) {
            String updateNode = "MATCH (a:" + type1 + " {graph: \"Local\", " + key1 + ": $val1})-[r:" + rel_type
                    + " {graph: \"Local\", source_workspace: $cmdb, description: $description1}]->(b:" + type2
                    + " {graph: \"Local\", " + key2
                    + ": $val2}) SET r.number_of_connects = $number_of_connects1, r.cur_id = $cur_id1";
            parameters = Values.parameters("val1", val1, "cmdb", cmdb, "description1", rel.getDescription(), "val2",
                    val2, "number_of_connects1", 1, "cur_id1", rel.getId());
            session.run(updateNode, parameters);
        }

        // Добавление характеристик
        updateRelation(session, rel, rel_type, type1, key1, val1, type2, key2, val2, level, cmdb);
    }

    public static void createContainerInstance(Session session, Model model, ContainerInstance containerInstance) {

        String cur_name = null;

        for (SoftwareSystem system : model.getSoftwareSystems()) {
            if (system.getContainers() == null) {
                continue;
            }
            for (Container cur : system.getContainers()) {
                if (cur.getId().equals(containerInstance.getContainerId())) {
                    cur_name = cur.getName().toString();
                    break;
                }
            }
        }

        if (cur_name == null) {
            return;
        }

        cur_name = "ContainerInstance." + cur_name;

        // Создание ContainerInstance
        String createNodeQuery = "CREATE (n:ContainerInstance {graph: \"Local\", name: $name1, instanceId: $instanceId1, tags: $tags1}) RETURN n";
        Value parameters = Values.parameters("name1", cur_name, "instanceId1", containerInstance.getInstanceId(),
                "tags1", containerInstance.getTags());
        session.run(createNodeQuery, parameters);

        // Добавление property
        if (containerInstance.getProperties() != null) {
            for (Map.Entry<String, Object> entry : containerInstance.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replace(' ', '_');
                key = key.replace('.', '_');
                createNodeQuery = "MATCH (n:ContainerInstance {graph: \"Local\", name: $val1}) SET n." + key
                        + " = $value RETURN n";
                parameters = Values.parameters("val1", cur_name, "value", entry.getValue());
                session.run(createNodeQuery, parameters);
            }
        }

        contIns.put(containerInstance.getId(), cur_name);
    }

    public static void createEnvironment(Session session, String environment) {

        // Проверка на существование
        if (!checkIfObjectExists(session, "Environment", "name", environment)) {
            // Создание Environment
            String createNodeQuery = "CREATE (n:Environment {graph: \"Local\", name: $name1}) RETURN n";
            Value parameters = Values.parameters("name1", environment);
            session.run(createNodeQuery, parameters);
        }

        if (!env_name.containsKey(environment)) {
            String id = String.valueOf(90000 + env_id.size());
            env_id.put(id, environment);
            env_name.put(environment, id);
        }
    }

    public static void createInfrastructureNode(Session session, InfrastructureNode infrastructureNode) {

        String createNodeQuery = "CREATE (n:InfrastructureNode {graph: \"Local\", name: $name1, description: $description1, technology: $technology1, tags: $tags1, url: $url1}) RETURN n";
        Value parameters = Values.parameters("name1", infrastructureNode.getName(), "description1",
                infrastructureNode.getDescription(), "technology1", infrastructureNode.getTechnology(),
                "tags1", infrastructureNode.getTags(), "url1",
                infrastructureNode.getUrl());
        session.run(createNodeQuery, parameters);

        // Обновление property
        if (infrastructureNode.getProperties() != null) {
            for (Map.Entry<String, Object> entry : infrastructureNode.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replace(' ', '_');
                key = key.replace('.', '_');
                createNodeQuery = "MATCH (n:InfrastructureNode {graph: \"Local\", name: $name1}) SET n." + key
                        + " = $value RETURN n";
                parameters = Values.parameters("name1", infrastructureNode.getName(), "value", entry.getValue());
                session.run(createNodeQuery, parameters);
            }
        }
    }

    public static void createDeploymentNode(Session session, DeploymentNode deploymentNode,
            Object cmdb, SoftwareSystem softwareSystem, Model model) {

        String createNodeQuery = "CREATE (n:DeploymentNode {graph: \"Local\", name: $name1, description: $description1, technology: $technology1, instances: $instances1, tags: $tags1, url: $url1}) RETURN n";
        Value parameters = Values.parameters("name1", deploymentNode.getName(), "description1",
                deploymentNode.getDescription(), "technology1", deploymentNode.getTechnology(), "instances1",
                deploymentNode.getInstances(), "tags1", deploymentNode.getTags(),
                "url1", deploymentNode.getUrl());
        session.run(createNodeQuery, parameters);

        // Обновление property
        if (deploymentNode.getProperties() != null) {
            for (Map.Entry<String, Object> entry : deploymentNode.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replace(' ', '_');
                key = key.replace('.', '_');
                createNodeQuery = "MATCH (n:DeploymentNode {graph: \"Local\", name: $name1}) SET n." + key
                        + " = $value RETURN n";
                parameters = Values.parameters("name1", deploymentNode.getName(), "value", entry.getValue());
                session.run(createNodeQuery, parameters);
            }
        }

        // Создание окружения и связи с ним
        if (deploymentNode.getEnvironment() != null) {
            createEnvironment(session, deploymentNode.getEnvironment());
            Relationship rel = new Relationship();
            rel.setSourceId(env_name.get(deploymentNode.getEnvironment()));
            rel.setDestinationId(deploymentNode.getId());
            rel.setDescription("Child");
            createRelation(session, rel, softwareSystem, "Child", cmdb, model, "");
        }

        // Проход по всем InfrastructureNodes
        if (deploymentNode.getInfrastructureNodes() != null) {
            for (InfrastructureNode infrastructureNode : deploymentNode.getInfrastructureNodes()) {
                infrastructureNode.setName(infrastructureNode.getName() + "." + deploymentNode.getName().toString());
                infNode.put(infrastructureNode.getId(), infrastructureNode.getName());
                createInfrastructureNode(session, infrastructureNode);

                Relationship rel = new Relationship();
                rel.setSourceId(deploymentNode.getId());
                rel.setDestinationId(infrastructureNode.getId());
                rel.setDescription("Child");
                createRelation(session, rel, softwareSystem, "Child", cmdb, model, "");

                // Создание окружения и связи с ним
                if (infrastructureNode.getEnvironment() != null) {
                    createEnvironment(session, infrastructureNode.getEnvironment());
                    rel = new Relationship();
                    rel.setSourceId(env_name.get(deploymentNode.getEnvironment()));
                    rel.setDestinationId(infrastructureNode.getId());
                    rel.setDescription("Child");
                    createRelation(session, rel, softwareSystem, "Child", cmdb, model, "");
                }
            }
        }

        // Проход по всем ContainerInstances
        if (deploymentNode.getContainerInstances() != null) {
            for (ContainerInstance containerInstance : deploymentNode.getContainerInstances()) {
                // Добавление в map происходит внутри функции
                createContainerInstance(session, model, containerInstance);

                // Deploy связь с контейнером
                Relationship rel = new Relationship();
                rel.setSourceId(containerInstance.getContainerId());
                rel.setDestinationId(containerInstance.getId());
                rel.setDescription("Deploy");
                createRelation(session, rel, softwareSystem, "Deploy", cmdb, model, "");

                rel = new Relationship();
                rel.setSourceId(deploymentNode.getId());
                rel.setDestinationId(containerInstance.getId());
                rel.setDescription("Child");
                createRelation(session, rel, softwareSystem, "Child", cmdb, model, "");

                // Создание окружения и связи с ним
                if (containerInstance.getEnvironment() != null) {
                    createEnvironment(session, containerInstance.getEnvironment());
                    rel = new Relationship();
                    rel.setSourceId(env_name.get(deploymentNode.getEnvironment()));
                    rel.setDestinationId(containerInstance.getId());
                    rel.setDescription("Child");
                    createRelation(session, rel, softwareSystem, "Child", cmdb, model, "");
                }
            }
        }

        // Проход по всем дочерним элементам
        if (deploymentNode.getChildren() != null) {
            for (DeploymentNode deploymentNodeChild : deploymentNode.getChildren()) {
                deploymentNodeChild.setName(deploymentNodeChild.getName() + "." + deploymentNode.getName().toString());
                deplNode.put(deploymentNodeChild.getId(), deploymentNodeChild.getName());
                createDeploymentNode(session, deploymentNodeChild, cmdb, softwareSystem, model);

                Relationship rel = new Relationship();
                rel.setSourceId(deploymentNode.getId());
                rel.setDestinationId(deploymentNodeChild.getId());
                rel.setDescription("Child");
                createRelation(session, rel, softwareSystem, "Child", cmdb, model, "");
            }
        }
    }

    public static void createDeploymentNodeRelations(Session session, DeploymentNode deploymentNode,
            Object cmdb, SoftwareSystem softwareSystem, Model model) {

        // Проход по всем Relationship
        if (deploymentNode.getRelationships() != null) {
            for (Relationship rel : deploymentNode.getRelationships()) {

                if (rel.getLinkedRelationshipId() != null) {
                    continue;
                }
                if (rel.getDescription() == null) {
                    rel.setDescription("None");
                }

                createRelation(session, rel, softwareSystem, "Relationship", cmdb, model, "C");
            }
        }

        // Проход по всем InfrastructureNode
        if (deploymentNode.getInfrastructureNodes() != null) {
            for (InfrastructureNode infrastructureNode : deploymentNode.getInfrastructureNodes()) {
                if (infrastructureNode.getRelationships() == null) {
                    continue;
                }
                for (Relationship rel : infrastructureNode.getRelationships()) {

                    if (rel.getLinkedRelationshipId() != null) {
                        continue;
                    }
                    if (rel.getDescription() == null) {
                        rel.setDescription("None");
                    }

                    createRelation(session, rel, softwareSystem, "Relationship", cmdb, model, "C");
                }
            }
        }

        // Проход по всем ContainerInstance
        if (deploymentNode.getContainerInstances() != null) {
            for (ContainerInstance containerInstance : deploymentNode.getContainerInstances()) {
                if (containerInstance.getRelationships() == null) {
                    continue;
                }
                for (Relationship rel : containerInstance.getRelationships()) {

                    if (rel.getDescription() == null) {
                        rel.setDescription("None");
                    }

                    createRelation(session, rel, softwareSystem, "Relationship", cmdb, model, "C");
                }
            }
        }

        // Проход по всем дочерним элементам
        if (deploymentNode.getChildren() != null) {
            for (DeploymentNode deploymentNodeChild : deploymentNode.getChildren()) {
                createDeploymentNodeRelations(session, deploymentNodeChild, cmdb, softwareSystem, model);
            }
        }

    }

    public static void createGraph(Workspace workspace, String uri, String user, String password) throws Exception {

        // Получение нужной SoftwareSystem
        Model model = workspace.getModel();
        Object cmdb = model.getProperties().get("workspace_cmdb");
        SoftwareSystem softwareSystem = new SoftwareSystem();
        for (SoftwareSystem cur : model.getSoftwareSystems()) {
            if (cur.getProperties() != null && cur.getProperties().get("cmdb") != null
                    && cur.getProperties().get("cmdb").equals(cmdb)) {
                softwareSystem = cur;
            }
        }

        // Подключение к БД
        Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        try (Session session = driver.session()) {

            // Очистка графа
            String createNodeQuery = "MATCH (n) WHERE n.graph = \"Local\" DETACH DELETE n";
            session.run(createNodeQuery);

            // Создание/Обновление системы
            createSoftware(session, softwareSystem, cmdb);

            cont = new HashMap<>();
            comp = new HashMap<>();
            deplNode = new HashMap<>();
            infNode = new HashMap<>();
            contIns = new HashMap<>();
            env_id = new HashMap<>();
            env_name = new HashMap<>();

            // Создание/Обновление контейнеров
            if (softwareSystem.getContainers() != null) {
                for (Container container : softwareSystem.getContainers()) {

                    // Изменение имён контейнера в соответствии с архитектурой
                    container.setName(container.getName() + "." + cmdb.toString());
                    String cont_ext_name = cmdb.toString();
                    if (container.getProperties() != null && container.getProperties().containsKey("external_name")) {
                        cont_ext_name = container.getProperties().get("external_name").toString() + "." + cont_ext_name;
                        container.getProperties().put("external_name", cont_ext_name);
                    }

                    createContainer(session, container);
                    boolean exists = checkIfObjectExists(session, "Container", "name", container.getName());
                    if (!exists) {
                        continue;
                    }
                    cont.put(container.getId(), container.getName());

                    Relationship rel = new Relationship();
                    rel.setSourceId(softwareSystem.getId());
                    rel.setDestinationId(container.getId());
                    rel.setDescription("Child");
                    createRelation(session, rel, softwareSystem, "Child", cmdb, model, "");

                    // Создание/Обновление компонентов конейнера
                    if (container.getComponents() == null) {
                        continue;
                    }
                    for (Component component : container.getComponents()) {

                        // Изменение имён контейнера в соответствии с архитектурой
                        component.setName(component.getName() + "." + container.getName());
                        String comp_ext_name = cont_ext_name;
                        if (component.getProperties() != null
                                && component.getProperties().containsKey("external_name")) {
                            comp_ext_name = component.getProperties().get("external_name").toString() + "."
                                    + comp_ext_name;
                            component.getProperties().put("external_name", comp_ext_name);
                        }

                        createComponent(session, component);
                        exists = checkIfObjectExists(session, "Component", "name", component.getName());
                        if (!exists) {
                            continue;
                        }

                        comp.put(component.getId(), component.getName());

                        rel.setSourceId(container.getId());
                        rel.setDestinationId(component.getId());
                        rel.setDescription("Child");
                        createRelation(session, rel, softwareSystem, "Child", cmdb, model, "");
                    }
                }
            }

            // Удаление лишних дублей
            if (softwareSystem.getContainers() != null) {
                for (Container container : softwareSystem.getContainers()) {
                    boolean exists = checkIfObjectExists(session, "Container", "name", container.getName());

                    if (!exists) {
                        cont.remove(container.getId());
                    }

                    if (container.getComponents() == null) {
                        continue;
                    }
                    for (Component component : container.getComponents()) {
                        exists = checkIfObjectExists(session, "Component", "name", component.getName());

                        if (!exists) {
                            comp.remove(component.getId());
                        }
                    }
                }
            }

            // Создание Relationship связей
            for (SoftwareSystem system : model.getSoftwareSystems()) {

                // Создание/Обновление Relationships у системы
                if (system.getRelationships() == null) {
                    continue;
                }
                for (Relationship rel_sys : system.getRelationships()) {
                    if (rel_sys.getLinkedRelationshipId() != null) {
                        continue;
                    }
                    if (rel_sys.getDescription() == null) {
                        rel_sys.setDescription("None");
                    }
                    createRelation(session, rel_sys, softwareSystem, "Relationship", cmdb, model, "C1");
                }

                // Создание/Обновление Relationships у контейнеров
                if (system.getContainers() == null) {
                    continue;
                }
                for (Container container : system.getContainers()) {

                    if (!cont.containsKey(container.getId())) {
                        continue;
                    }

                    if (container.getRelationships() == null) {
                        continue;
                    }
                    for (Relationship rel_cont : container.getRelationships()) {
                        if (rel_cont.getLinkedRelationshipId() != null) {
                            continue;
                        }
                        if (rel_cont.getDescription() == null) {
                            rel_cont.setDescription("None");
                        }
                        createRelation(session, rel_cont, softwareSystem, "Relationship", cmdb, model,
                                "C2");
                    }

                    // Создание/Обновление Relationships у компонентов контейнера
                    if (container.getComponents() == null) {
                        continue;
                    }
                    for (Component component : container.getComponents()) {

                        if (!comp.containsKey(component.getId())) {
                            continue;
                        }

                        if (component.getRelationships() == null) {
                            continue;
                        }
                        for (Relationship rel_comp : component.getRelationships()) {
                            if (rel_comp.getLinkedRelationshipId() != null) {
                                continue;
                            }
                            if (rel_comp.getDescription() == null) {
                                rel_comp.setDescription("None");
                            }
                            createRelation(session, rel_comp, softwareSystem, "Relationship", cmdb, model,
                                    "C3");
                        }
                    }
                }
            }

            // Создание DeploymentNodes
            if (model.getDeploymentNodes() != null) {
                for (DeploymentNode deploymentNode : model.getDeploymentNodes()) {
                    deploymentNode.setName(deploymentNode.getName() + "." + cmdb.toString());
                    deplNode.put(deploymentNode.getId(), deploymentNode.getName());
                    createDeploymentNode(session, deploymentNode, cmdb, softwareSystem, model);

                    Relationship rel = new Relationship();
                    rel.setSourceId(softwareSystem.getId());
                    rel.setDestinationId(deploymentNode.getId());
                    rel.setDescription("Child");
                    createRelation(session, rel, softwareSystem, "Child", cmdb, model, "");
                }
            }

            // Создание Relationship для DeploymentNodes
            if (model.getDeploymentNodes() != null) {
                for (DeploymentNode deploymentNode : model.getDeploymentNodes()) {
                    createDeploymentNodeRelations(session, deploymentNode, cmdb, softwareSystem, model);
                }
            }

        } catch (Exception e) {
            throw e;
        } finally {
            driver.close();
        }
    }
}