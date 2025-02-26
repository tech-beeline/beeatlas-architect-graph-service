package com.example.demo;

import java.io.IOException;
import org.neo4j.driver.*;
import java.util.HashMap;
import java.util.Map;

public class MinorGraph {

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
                createNodeQuery = "MATCH (n:SoftwareSystem {cmdb: $cmdb1, graph: \"Local\"}) SET n." + key
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

        String updateNode = "MATCH (a:" + type1 + " {" + key1 + ": $val1, graph: \"Local\"})-[r:" + rel_type
                + " {source_workspace: $cmdb, graph: \"Local\", description: $description1}]->(b:" + type2 + " {" + key2
                + ": $val2, graph: \"Local\"}) SET r.tags = $tags1,  r.url = $url1, r.technology = $technology1,  r.interactionStyle = $interactionStyle1,  r.linkedRelationshipId = $linkedRelationshipId1, r.level = $level1 RETURN r";
        Value parameters = Values.parameters("val1", val1, "cmdb", cmdb, "description1", rel.getDescription(), "val2",
                val2, "tags1", rel.getTags(), "url1", rel.getUrl(), "technology1",
                rel.getTechnology(), "interactionStyle1", rel.getInteractionStyle(), "linkedRelationshipId1",
                rel.getLinkedRelationshipId(), "level1", level);
        session.run(updateNode, parameters);

        if (rel.getDescription().equals("None")) {
            Integer number = 0;

            // Вычисление текущего количества связей
            updateNode = "MATCH (a:" + type1 + " {" + key1 + ": $val1, graph: \"Local\"})-[r:" + rel_type
                    + " {source_workspace: $cmdb, graph: \"Local\", description: $description1}]->(b:" + type2 + " {"
                    + key2 + ": $val2, graph: \"Local\"}) RETURN r";
            parameters = Values.parameters("val1", val1, "cmdb", cmdb, "description1", rel.getDescription(),
                    "val2", val2);
            Result result = session.run(updateNode, parameters);

            org.neo4j.driver.types.Relationship relation = result.next().get("r").asRelationship();

            String cur_id = relation.get("cur_id").toString();
            number = relation.get("number_of_connects").asInt();

            if (!cur_id.equals(rel.getId())) {
                number = number + 1;
            }

            updateNode = "MATCH (a:" + type1 + " {" + key1 + ": $val1, graph: \"Local\"})-[r:" + rel_type
                    + " {source_workspace: $cmdb, graph: \"Local\", description: $description1}]->(b:"
                    + type2 + " {" + key2
                    + ": $val2, graph: \"Local\"}) SET r.number_of_connects = $number_of_connects1, r.cur_id = $cur_id1";
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
                updateNode = "MATCH (a:" + type1 + " {" + key1 + ": $val1, graph: \"Local\"})-[r:" + rel_type
                        + " {source_workspace: $cmdb, graph: \"Local\", description: $description1}]->(b:" + type2 +
                        " {" + key2 + ": $val2, graph: \"Local\"}) SET r." + key + " = $value";
                parameters = Values.parameters("val1", val1, "cmdb", cmdb, "description1", rel.getDescription(), "val2",
                        val2, "value", entry.getValue());
                session.run(updateNode, parameters);
            }
        }
    }

    public static void createRelation(Session session, Relationship rel, HashMap<String, Object> cont,
            HashMap<String, Object> comp, HashMap<String, Object> deplNode, HashMap<String, Object> infNode,
            SoftwareSystem softwareSystem, String rel_type, Object cmdb, Model model,
            String level) {

        String type1 = null;
        String key1 = null;
        Object val1 = null;

        String type2 = null;
        String key2 = null;
        Object val2 = null;

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
        }

        if (val2 == null) {
            return;
        }

        // Создание соединения
        String createRelationshipQuery = "MATCH (a:" + type1 + " {" + key1 + ": $val1, graph: \"Local\"}), (b:" + type2
                + " {" + key2 + ": $val2, graph: \"Local\"}) CREATE (a)-[r:" + rel_type
                + " {source_workspace: $cmdb, graph: \"Local\", description: $description1}]->(b) RETURN a, b";
        Value parameters = Values.parameters("val1", val1, "cmdb", cmdb, "val2", val2, "description1",
                rel.getDescription());
        session.run(createRelationshipQuery, parameters);

        if (rel.getDescription().equals("None")) {
            String updateNode = "MATCH (a:" + type1 + " {" + key1 + ": $val1, graph: \"Local\"})-[r:" + rel_type
                    + " {source_workspace: $cmdb, graph: \"Local\", description: $description1}]->(b:" + type2 + " {"
                    + key2
                    + ": $val2, graph: \"Local\"}) SET r.number_of_connects = $number_of_connects1, r.cur_id = $cur_id1";
            parameters = Values.parameters("val1", val1, "cmdb", cmdb, "description1", rel.getDescription(), "val2",
                    val2, "number_of_connects1", 1, "cur_id1", rel.getId());
            session.run(updateNode, parameters);
        }

        // Добавление характеристик
        updateRelation(session, rel, rel_type, type1, key1, val1, type2, key2, val2, level, cmdb);
    }

    public static void createDeployRelationSystem(Session session, Model model, DeploymentNode deploymentNode,
            SoftwareSystemInstance softwareSystemInstance, Object cmdb) {

        Object cur_cmdb = null;

        for (SoftwareSystem cur : model.getSoftwareSystems()) {
            if (cur.getId().equals(softwareSystemInstance.getSoftwareSystemId())) {
                cmdb = cur.getProperties().get("cmdb");
                break;
            }
        }

        if (cur_cmdb == null) {
            return;
        }

        // Создание соединения
        String createRelationshipQuery = "MATCH (a:SoftwareSystem {name: $val2, graph: \"Local\"}), (b:DeploymentNode {cmdb: $val1, graph: \"Local\"}) CREATE (a)-[r:Deploy {description: $description1, graph: \"Local\", cur_id: $cur_id1, source_workspace: $cmdb, number_of_connects: $number_of_connects1, environment: $environment1, tags: $tags1}]->(b) RETURN a, b";
        Value parameters = Values.parameters("description1", "Deploy", "val1", deploymentNode.getName(), "cmdb", cmdb,
                "val2", cur_cmdb, "cur_id1", softwareSystemInstance.getId(), "number_of_connects1", 1, "environment1",
                softwareSystemInstance.getEnvironment(), "tags1", softwareSystemInstance.getTags());
        session.run(createRelationshipQuery, parameters);

        // Обновление property
        if (softwareSystemInstance.getProperties() != null) {
            for (Map.Entry<String, Object> entry : softwareSystemInstance.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replace(' ', '_');
                key = key.replace('.', '_');
                createRelationshipQuery = "MATCH (a:SoftwareSystem  {name: $val2, graph: \"Local\"})-[r:Deploy {source_workspace: $cmdb, graph: \"Local\"}]->(b:DeploymentNode {cmdb: $val1, graph: \"Local\"}) SET r."
                        + key + " = $value";
                parameters = Values.parameters("val1", deploymentNode.getName(), "cmdb", cmdb, "val2",
                        cur_cmdb, "value", entry.getValue());
                session.run(createRelationshipQuery, parameters);
            }
        }
    }

    public static void createDeployRelationContainer(Session session, Model model, DeploymentNode deploymentNode,
            ContainerInstance containerInstance, Object cmdb) {

        Object cur_name = null;

        for (SoftwareSystem system : model.getSoftwareSystems()) {
            if (system.getContainers() == null) {
                continue;
            }
            for (Container cur : system.getContainers()) {
                if (cur.getId().equals(containerInstance.getContainerId())) {
                    cur_name = cur.getName();
                    break;
                }
            }
        }

        if (cur_name == null) {
            return;
        }

        // Создание соединения
        String createRelationshipQuery = "MATCH (a:Container {name: $val2, graph: \"Local\"}), (b:DeploymentNode  {name: $val1, graph: \"Local\"}) CREATE (a)-[r:Deploy {graph: \"Local\", description: $description1, cur_id: $cur_id1, source_workspace: $cmdb, number_of_connects: $number_of_connects1, environment: $environment1, tags: $tags1}]->(b) RETURN a, b";
        Value parameters = Values.parameters("description1", "Deploy", "val1", deploymentNode.getName(), "cmdb", cmdb,
                "val2", cur_name, "cur_id1", containerInstance.getId(), "number_of_connects1", 1, "environment1",
                containerInstance.getEnvironment(), "tags1", containerInstance.getTags());
        session.run(createRelationshipQuery, parameters);

        // Обновление property
        if (containerInstance.getProperties() != null) {
            for (Map.Entry<String, Object> entry : containerInstance.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replace(' ', '_');
                key = key.replace('.', '_');
                createRelationshipQuery = "MATCH (a:Container {name: $val2, graph: \"Local\"})-[r:Deploy {source_workspace: $cmdb, graph: \"Local\"}]->(b:DeploymentNode  {name: $val1, graph: \"Local\"}) SET r."
                        + key + " = $value";
                parameters = Values.parameters("val1", deploymentNode.getName(), "cmdb", cmdb, "val2",
                        cur_name, "value", entry.getValue());
                session.run(createRelationshipQuery, parameters);
            }
        }
    }

    public static void createEnvironmentDeployment(Session session, DeploymentNode deploymentNode, Object cmdb) {
        boolean exists = checkIfObjectExists(session, "Environment", "name", deploymentNode.getEnvironment());

        if (!exists) {
            String createNodeQuery = "CREATE (n:Environment {name: $name1, graph: \"Local\"}) RETURN n";
            Value parameters = Values.parameters("name1", deploymentNode.getEnvironment());
            session.run(createNodeQuery, parameters);
        }

        String createRelationshipQuery = "MATCH (a:Environment {name: $val1, graph: \"Local\"}), (b:DeploymentNode {name: $val2, graph: \"Local\"}) CREATE (a)-[r:Child {graph: \"Local\", source_workspace: $cmdb, description: $description1}]->(b) RETURN a, b";
        Value parameters = Values.parameters("val1", deploymentNode.getEnvironment(), "cmdb", cmdb, "val2",
                deploymentNode.getName(), "description1", "Child");
        session.run(createRelationshipQuery, parameters);
    }

    public static void createEnvironmentInfrastructure(Session session, InfrastructureNode infrastructureNode,
            Object cmdb) {

        boolean exists = checkIfObjectExists(session, "Environment", "name", infrastructureNode.getEnvironment());

        if (!exists) {
            String createNodeQuery = "CREATE (n:Environment {name: $name1, graph: \"Local\"}) RETURN n";
            Value parameters = Values.parameters("name1", infrastructureNode.getEnvironment());
            session.run(createNodeQuery, parameters);
        }

        String createRelationshipQuery = "MATCH (a:Environment {name: $val1, graph: \"Local\"}), (b:InfrastructureNode {name: $val2, graph: \"Local\"}) CREATE (a)-[r:Child {graph: \"Local\", source_workspace: $cmdb, description: $description1}]->(b) RETURN a, b";
        Value parameters = Values.parameters("val1", infrastructureNode.getEnvironment(), "cmdb", cmdb, "val2",
                infrastructureNode.getName(), "description1", "Child");
        session.run(createRelationshipQuery, parameters);
    }

    public static void createInfrastructureNode(Session session, InfrastructureNode infrastructureNode) {
        String createNodeQuery = "CREATE (n:InfrastructureNode {graph: \"Local\", name: $name1, description: $description1, technology: $technology1, tags: $tags1, url: $url1}) RETURN n";
        Value parameters = Values.parameters("name1", infrastructureNode.getName(), "description1",
                infrastructureNode.getDescription(), "technology1", infrastructureNode.getTechnology(),
                "tags1", infrastructureNode.getTags(), "url1", infrastructureNode.getUrl());
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

    public static void createDeploymentNode(Session session, DeploymentNode deploymentNode, Object cmdb,
            SoftwareSystem softwareSystem, Model model, HashMap<String, Object> cont, HashMap<String, Object> comp,
            HashMap<String, Object> deplNode, HashMap<String, Object> infNode) {

        String createNodeQuery = "CREATE (n:DeploymentNode {graph: \"Local\", name: $name1, description: $description1, technology: $technology1, instances: $instances1, tags: $tags1, url: $url1}) RETURN n";
        Value parameters = Values.parameters("name1", deploymentNode.getName(), "description1",
                deploymentNode.getDescription(), "technology1", deploymentNode.getTechnology(), "instances1",
                deploymentNode.getInstances(), "tags1", deploymentNode.getTags(), "url1", deploymentNode.getUrl());
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
            createEnvironmentDeployment(session, deploymentNode, cmdb);
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
                createRelation(session, rel, cont, comp, deplNode, infNode, softwareSystem, "Child", cmdb,
                        model, "");

                // Создание окружения и связи с ним
                if (infrastructureNode.getEnvironment() != null) {
                    createEnvironmentInfrastructure(session, infrastructureNode, cmdb);
                }
            }
        }

        // Проход по всем дочерним элементам
        if (deploymentNode.getChildren() != null) {
            for (DeploymentNode deploymentNodeChild : deploymentNode.getChildren()) {
                deploymentNodeChild.setName(deploymentNodeChild.getName() + "." + deploymentNode.getName().toString());
                deplNode.put(deploymentNodeChild.getId(), deploymentNodeChild.getName());
                createDeploymentNode(session, deploymentNodeChild, cmdb, softwareSystem, model, cont, comp, deplNode,
                        infNode);

                Relationship rel = new Relationship();
                rel.setSourceId(deploymentNode.getId());
                rel.setDestinationId(deploymentNodeChild.getId());
                rel.setDescription("Child");
                createRelation(session, rel, cont, comp, deplNode, infNode, softwareSystem, "Child", cmdb,
                        model, "");
            }
        }
    }

    public static void createDeploymentNodeRelations(Session session, DeploymentNode deploymentNode, Object cmdb,
            SoftwareSystem softwareSystem, Model model, HashMap<String, Object> cont, HashMap<String, Object> comp,
            HashMap<String, Object> deplNode, HashMap<String, Object> infNode) {

        // Проход по всем Relationship
        if (deploymentNode.getRelationships() != null) {
            for (Relationship rel : deploymentNode.getRelationships()) {

                if (rel.getLinkedRelationshipId() != null) {
                    continue;
                }
                if (rel.getDescription() == null) {
                    rel.setDescription("None");
                }

                createRelation(session, rel, cont, comp, deplNode, infNode, softwareSystem, "Relationship", cmdb, model,
                        "C");
            }
        }

        // Проход по всем InfrastructureNode
        if (deploymentNode.getInfrastructureNodes() != null) {
            for (InfrastructureNode infrastructureNode : deploymentNode.getInfrastructureNodes()) {
                for (Relationship rel : infrastructureNode.getRelationships()) {

                    if (rel.getLinkedRelationshipId() != null) {
                        continue;
                    }
                    if (rel.getDescription() == null) {
                        rel.setDescription("None");
                    }

                    createRelation(session, rel, cont, comp, deplNode, infNode, softwareSystem, "Relationship", cmdb,
                            model, "C");
                }
            }
        }

        // Проход по всем SoftwareSystemInstance
        if (deploymentNode.getSoftwareSystemInstances() != null) {
            for (SoftwareSystemInstance softwareSystemInstance : deploymentNode.getSoftwareSystemInstances()) {
                createDeployRelationSystem(session, model, deploymentNode, softwareSystemInstance, cmdb);
            }
        }

        // Проход по всем ContainerInstance
        if (deploymentNode.getContainerInstances() != null) {
            for (ContainerInstance containerInstance : deploymentNode.getContainerInstances()) {
                createDeployRelationContainer(session, model, deploymentNode, containerInstance, cmdb);
            }
        }

        // Проход по всем дочерним элементам
        if (deploymentNode.getChildren() != null) {
            for (DeploymentNode deploymentNodeChild : deploymentNode.getChildren()) {
                createDeploymentNodeRelations(session, deploymentNodeChild, cmdb, softwareSystem, model, cont, comp,
                        deplNode, infNode);
            }
        }

    }

    public static void createGraph(Workspace workspace, String uri, String user, String password) throws IOException {

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

            HashMap<String, Object> cont = new HashMap<>();
            HashMap<String, Object> comp = new HashMap<>();
            HashMap<String, Object> deplNode = new HashMap<>();
            HashMap<String, Object> infNode = new HashMap<>();

            // Создание/Обновление контейнеров
            if (softwareSystem.getContainers() != null) {
                for (Container container : softwareSystem.getContainers()) {

                    // Изменение имён контейнера в соответствии с архитектурой
                    container.setName(container.getName() + "." + cmdb.toString());
                    createContainer(session, container);
                    cont.put(container.getId(), container.getName());

                    Relationship rel = new Relationship();
                    rel.setSourceId(softwareSystem.getId());
                    rel.setDestinationId(container.getId());
                    rel.setDescription("Child");
                    createRelation(session, rel, cont, comp, deplNode, infNode, softwareSystem, "Child", cmdb, model,
                            "");

                    // Создание/Обновление компонентов конейнера
                    if (container.getComponents() == null) {
                        continue;
                    }
                    for (Component component : container.getComponents()) {

                        // Изменение имён контейнера в соответствии с архитектурой
                        component.setName(component.getName() + "." + container.getName());
                        createComponent(session, component);
                        comp.put(component.getId(), component.getName());

                        rel.setSourceId(container.getId());
                        rel.setDestinationId(component.getId());
                        rel.setDescription("Child");
                        createRelation(session, rel, cont, comp, deplNode, infNode, softwareSystem, "Child", cmdb,
                                model, "");
                    }
                }
            }

            // Создание Relationship связей
            for (Relationship rel_sys : softwareSystem.getRelationships()) {
                if (rel_sys.getLinkedRelationshipId() != null) {
                    continue;
                }
                if (rel_sys.getDescription() == null) {
                    rel_sys.setDescription("None");
                }
                createRelation(session, rel_sys, cont, comp, deplNode, infNode, softwareSystem, "Relationship",
                        cmdb, model, "C1");
            }

            // Создание/Обновление Relationships у контейнеров
            if (softwareSystem.getContainers() != null) {
                for (Container container : softwareSystem.getContainers()) {

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
                        createRelation(session, rel_cont, cont, comp, deplNode, infNode, softwareSystem, "Relationship",
                                cmdb, model, "C2");
                    }

                    // Создание/Обновление Relationships у компонентов контейнера
                    if (container.getComponents() == null) {
                        continue;
                    }
                    for (Component component : container.getComponents()) {
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
                            createRelation(session, rel_comp, cont, comp, deplNode, infNode, softwareSystem,
                                    "Relationship", cmdb, model, "C3");
                        }
                    }
                }
            }

            // Создание DeploymentNodes
            if (model.getDeploymentNodes() != null) {
                for (DeploymentNode deploymentNode : model.getDeploymentNodes()) {
                    deploymentNode.setName(deploymentNode.getName() + "." + cmdb.toString());
                    deplNode.put(deploymentNode.getId(), deploymentNode.getName());
                    createDeploymentNode(session, deploymentNode, cmdb, softwareSystem, model, cont, comp,
                            deplNode, infNode);

                    Relationship rel = new Relationship();
                    rel.setSourceId(softwareSystem.getId());
                    rel.setDestinationId(deploymentNode.getId());
                    rel.setDescription("Child");
                    createRelation(session, rel, cont, comp, deplNode, infNode, softwareSystem, "Child", cmdb, model,
                            "");
                }
            }

            // Создание Relationship для DeploymentNodes
            if (model.getDeploymentNodes() != null) {
                for (DeploymentNode deploymentNode : model.getDeploymentNodes()) {
                    createDeploymentNodeRelations(session, deploymentNode, cmdb, softwareSystem, model, cont, comp,
                            deplNode, infNode);
                }
            }

        } catch (Exception e) {
            throw e;
        } finally {
            driver.close();
        }
    }
}