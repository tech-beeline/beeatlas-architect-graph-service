package com.example.architecting_graph;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;

import com.example.architecting_graph.AutomaticLayout.LayoutImplementation;
import com.example.architecting_graph.AutomaticLayout.RankDirection;

public class GetObjects {
    private static Long id_obj;
    private static Map<String, Long> map_id;
    private static Map<String, SoftwareSystem> systems;
    private static Map<String, Container> containers;
    private static Map<String, Component> components;
    private static Map<String, DeploymentNode> deploymentNodes;
    private static Map<String, InfrastructureNode> infrastructureNodes;

    public static SoftwareSystem getSystem(Node node) {
        SoftwareSystem system = new SoftwareSystem();
        system.setProperties(new HashMap<>());
        system.setRelationships(new ArrayList<>());
        system.setContainers(new ArrayList<>());
        system.setId(String.valueOf(id_obj));

        // Добавление property
        for (String key : node.keys()) {
            try {
                Field field = SoftwareSystem.class.getDeclaredField(key);
                field.setAccessible(true); // Разрешение доступа к приватным полям
                // Установка значения поля
                field.set(system, node.get(key).asObject());
            } catch (Exception e) {
                system.getProperties().put(key, node.get(key).asObject());
            }
        }

        map_id.put(system.getProperties().get("structurizr_dsl_identifier").toString(), id_obj);
        systems.put(system.getProperties().get("structurizr_dsl_identifier").toString(), system);
        id_obj = id_obj + 1;

        return system;
    }

    public static Container getContainer(Node node, Session session) {
        Container container = new Container();
        container.setProperties(new HashMap<>());
        container.setRelationships(new ArrayList<>());
        container.setComponents(new ArrayList<>());
        container.setId(String.valueOf(id_obj));

        // Добавление property
        for (String key : node.keys()) {
            try {
                Field field = Container.class.getDeclaredField(key);
                field.setAccessible(true); // Разрешение доступа к приватным полям
                // Установка значения поля
                field.set(container, node.get(key).asObject());
            } catch (Exception e) {
                container.getProperties().put(key, node.get(key).asObject());
            }
        }

        map_id.put(container.getProperties().get("structurizr_dsl_identifier").toString(), id_obj);
        containers.put(container.getProperties().get("structurizr_dsl_identifier").toString(), container);
        id_obj = id_obj + 1;

        // Добавление системы
        String query = "MATCH (m:SoftwareSystem)-[r:Child]->(n:Container {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", container.getProperties().get("structurizr_dsl_identifier"));
        Result result = session.run(query, parameters);

        org.neo4j.driver.Record record = result.next();
        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
            getSystem(record.get("m").asNode());
        }

        SoftwareSystem system = systems.get(record.get("m.structurizr_dsl_identifier").asString());
        system.getContainers().add(container);
        systems.put(record.get("m.structurizr_dsl_identifier").asString(), system);

        return container;
    }

    public static void getComponent(Node node, Session session) {
        Component component = new Component();
        component.setProperties(new HashMap<>());
        component.setRelationships(new ArrayList<>());
        component.setId(String.valueOf(id_obj));

        // Добавление property
        for (String key : node.keys()) {
            try {
                Field field = Component.class.getDeclaredField(key);
                field.setAccessible(true); // Разрешение доступа к приватным полям
                // Установка значения поля
                field.set(component, node.get(key).asObject());
            } catch (Exception e) {
                component.getProperties().put(key, node.get(key).asObject());
            }
        }

        map_id.put(component.getProperties().get("structurizr_dsl_identifier").toString(), id_obj);
        components.put(component.getProperties().get("structurizr_dsl_identifier").toString(), component);
        id_obj = id_obj + 1;

        // Добавление контейнера
        String query = "MATCH (m:Container)-[r:Child]->(n:Component {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", component.getProperties().get("structurizr_dsl_identifier"));
        Result result = session.run(query, parameters);

        org.neo4j.driver.Record record = result.next();
        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
            getContainer(record.get("m").asNode(), session);
        }

        Container container = containers.get(record.get("m.structurizr_dsl_identifier").asString());
        container.getComponents().add(component);
        containers.put(record.get("m.structurizr_dsl_identifier").asString(), container);
    }

    public static Relationship getRelation(org.neo4j.driver.types.Relationship relation, String source,
            String destination) {
        Relationship relationship = new Relationship();
        relationship.setProperties(new HashMap<>());
        relationship.setId(String.valueOf(id_obj));
        id_obj = id_obj + 1;
        relationship.setSourceId(source);
        relationship.setDestinationId(destination);

        // Добавление property
        for (Map.Entry<String, Object> entry : relation.asMap().entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            try {
                Field field = Relationship.class.getDeclaredField(key);
                field.setAccessible(true); // Разрешение доступа к приватным полям
                // Установка значения поля
                field.set(relationship, val);
            } catch (Exception e) {
                relationship.getProperties().put(key, val);
            }
        }

        return relationship;
    }

    public static void getInfrastructureNode(Node node) {
        InfrastructureNode infrastructureNode = new InfrastructureNode();
        infrastructureNode.setProperties(new HashMap<>());
        infrastructureNode.setRelationships(new ArrayList<>());
        infrastructureNode.setId(String.valueOf(id_obj));

        // Добавление property
        for (String key : node.keys()) {
            try {
                Field field = InfrastructureNode.class.getDeclaredField(key);
                field.setAccessible(true); // Разрешение доступа к приватным полям
                // Установка значения поля
                field.set(infrastructureNode, node.get(key).asObject());
            } catch (Exception e) {
                infrastructureNode.getProperties().put(key, node.get(key).asObject());
            }
        }

        map_id.put(infrastructureNode.getProperties().get("structurizr_dsl_identifier").toString(), id_obj);
        id_obj = id_obj + 1;

        infrastructureNodes.put(infrastructureNode.getProperties().get("structurizr_dsl_identifier").toString(),
                infrastructureNode);
    }

    public static void getDeploymentNode(Node node, Session session) {
        DeploymentNode deploymentNode = new DeploymentNode();
        deploymentNode.setId(String.valueOf(id_obj));

        deploymentNode.setProperties(new HashMap<>());
        // Добавление property
        for (String key : node.keys()) {
            try {
                Field field = DeploymentNode.class.getDeclaredField(key);
                field.setAccessible(true); // Разрешение доступа к приватным полям
                // Установка значения поля
                field.set(deploymentNode, node.get(key).asObject());
            } catch (Exception e) {
                deploymentNode.getProperties().put(key, node.get(key).asObject());
            }
        }

        map_id.put(deploymentNode.getProperties().get("structurizr_dsl_identifier").toString(), id_obj);
        id_obj = id_obj + 1;

        // Добавление Environment
        String query = "MATCH (n:Environment)-[r:Child]->(n:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.name";
        Value parameters = Values.parameters("val1", deploymentNode.getProperties().get("structurizr_dsl_identifier"));
        Result result = session.run(query, parameters);

        if (result.hasNext()) {
            deploymentNode.setEnvironment(result.next().get("n.name").toString());
        }

        // Добавление InfrastructureNode
        query = "MATCH (n:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:InfrastructureNode) RETURN m";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            getInfrastructureNode(record.get("m").asNode());
        }

        // Добавление дочерних DeploymentNode

        query = "MATCH (n:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:DeploymentNode) RETURN m";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            getDeploymentNode(record.get("m").asNode(), session);
        }

        deploymentNodes.put(deploymentNode.getProperties().get("structurizr_dsl_identifier").toString(),
                deploymentNode);
    }

    public static SoftwareSystemInstance getSoftwareSystemInstance(org.neo4j.driver.types.Relationship relation,
            String instanceId, String softwareSystemId) {
        SoftwareSystemInstance softwareSystemInstance = new SoftwareSystemInstance();
        softwareSystemInstance.setProperties(new HashMap<>());
        softwareSystemInstance.setId(String.valueOf(id_obj));
        id_obj = id_obj + 1;
        softwareSystemInstance.setInstanceId(Integer.parseInt(instanceId));
        softwareSystemInstance.setSoftwareSystemId(softwareSystemId);

        // Добавление property
        for (Map.Entry<String, Object> entry : relation.asMap().entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            try {
                Field field = SoftwareSystemInstance.class.getDeclaredField(key);
                field.setAccessible(true); // Разрешение доступа к приватным полям
                // Установка значения поля
                field.set(softwareSystemInstance, val);
            } catch (Exception e) {
                softwareSystemInstance.getProperties().put(key, val);
            }
        }

        map_id.put(softwareSystemInstance.getProperties().get("structurizr_dsl_identifier").toString(), id_obj);
        id_obj = id_obj + 1;
        return softwareSystemInstance;
    }

    public static ContainerInstance getContainerInstance(org.neo4j.driver.types.Relationship relation,
            String instanceId, String containerId) {
        ContainerInstance containerInstance = new ContainerInstance();
        containerInstance.setProperties(new HashMap<>());
        containerInstance.setId(String.valueOf(id_obj));
        id_obj = id_obj + 1;
        containerInstance.setInstanceId(Integer.parseInt(instanceId));
        containerInstance.setContainerId(containerId);

        // Добавление property
        for (Map.Entry<String, Object> entry : relation.asMap().entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            try {
                Field field = ContainerInstance.class.getDeclaredField(key);
                field.setAccessible(true); // Разрешение доступа к приватным полям
                // Установка значения поля
                field.set(containerInstance, val);
            } catch (Exception e) {
                containerInstance.getProperties().put(key, val);
            }
        }

        map_id.put(containerInstance.getProperties().get("structurizr_dsl_identifier").toString(), id_obj);
        id_obj = id_obj + 1;
        return containerInstance;
    }

    public static DeploymentNode getDeploymentNodeRelations(DeploymentNode deploymentNode, Session session) {

        // Добавление связей
        deploymentNode.setRelationships(new ArrayList<>());
        String query = "MATCH (n:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship]->(m) RETURN r, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", deploymentNode.getProperties().get("structurizr_dsl_identifier"));
        Result result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();

            String second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString();
            deploymentNode.getRelationships()
                    .add(getRelation(record.get("r").asRelationship(), deploymentNode.getId(), second_id));
        }

        // Добавление InfrastructureNode
        deploymentNode.setInfrastructureNodes(new ArrayList<>());

        query = "MATCH (n:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:InfrastructureNode) RETURN m.structurizr_dsl_identifier";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            InfrastructureNode infrastructureNode = infrastructureNodes
                    .get(record.get("m.structurizr_dsl_identifier").asString());

            query = "MATCH (n:InfrastructureNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship]->(m) RETURN r, m.structurizr_dsl_identifier";
            parameters = Values.parameters("val1",
                    infrastructureNode.getProperties().get("structurizr_dsl_identifier"));
            Result result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();

                String second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString();
                infrastructureNode.getRelationships()
                        .add(getRelation(record.get("r").asRelationship(), infrastructureNode.getId(), second_id));
            }

            deploymentNode.getInfrastructureNodes().add(infrastructureNode);
        }

        // Добавление SoftwareSystemInstance
        deploymentNode.setSoftwareSystemInstances(new ArrayList<>());

        query = "MATCH (n:SoftwareSystem)-[r:Deploy]->(m:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.structurizr_dsl_identifier, r";
        parameters = Values.parameters("val1", deploymentNode.getProperties().get("structurizr_dsl_identifier"));
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            String second_id = map_id.get(record.get("n.structurizr_dsl_identifier").asString()).toString();
            deploymentNode.getSoftwareSystemInstances()
                    .add(getSoftwareSystemInstance(record.get("r").asRelationship(), deploymentNode.getId(),
                            second_id));
        }

        // Добавление ContainerInstance
        deploymentNode.setContainerInstances(new ArrayList<>());

        query = "MATCH (n:Container)-[r:Deploy]->(m:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.structurizr_dsl_identifier, r";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            String second_id = map_id.get(record.get("n.structurizr_dsl_identifier").asString()).toString();
            deploymentNode.getContainerInstances()
                    .add(getContainerInstance(record.get("r").asRelationship(), deploymentNode.getId(), second_id));
        }

        // Добавление дочерних DeploymentNode
        deploymentNode.setChildren(new ArrayList<>());

        query = "MATCH (n:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:DeploymentNode) RETURN m.structurizr_dsl_identifier";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            deploymentNode.getChildren()
                    .add(getDeploymentNodeRelations(
                            deploymentNodes.get(record.get("m.structurizr_dsl_identifier").asString()), session));
        }

        return deploymentNode;
    }

    public static Workspace GetWorkspace(String softwareSystemMnemonic, String containerMnemonic, String uri,
            String user, String password) {

        // Начальная инициализация
        id_obj = 2L;
        map_id = new HashMap<>();
        systems = new HashMap<>();
        containers = new HashMap<>();
        components = new HashMap<>();
        deploymentNodes = new HashMap<>();
        infrastructureNodes = new HashMap<>();

        // Подключение к БД
        Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        Session session = driver.session();

        // Возвращаем воркспейс
        Workspace workspace = new Workspace();
        Model model = new Model();
        model.setSoftwareSystems(new ArrayList<>());
        model.setDeploymentNodes(new ArrayList<>());
        workspace.setId(1L);

        String query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n";
        Value parameters = Values.parameters("val1", softwareSystemMnemonic);
        Result result = session.run(query, parameters);
        org.neo4j.driver.Record record = result.next();

        // Добавление системы
        SoftwareSystem system = getSystem(record.get("n").asNode());

        if (containerMnemonic != null) {

            // Добавление контейнера
            query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:Container {graph: \"Global\", structurizr_dsl_identifier: $val2}) RETURN m";
            parameters = Values.parameters("val1", softwareSystemMnemonic, "val2", containerMnemonic);
            result = session.run(query, parameters);
            Container container = getContainer(result.next().get("m").asNode(), session);

            // Добавление компонентов
            query = "MATCH (n:Container {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m) RETURN m";
            parameters = Values.parameters("val1", containerMnemonic);
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                getComponent(record.get("m").asNode(), session);
            }

            // Добавление прямых связей
            query = "MATCH (n:Container{graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship]->(m) RETURN r, m, m.structurizr_dsl_identifier";
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                String label = record.get("m").asNode().labels().toString();
                label = label.substring(1, label.length() - 1);
                if (label.equals("SoftwareSystem")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getSystem(record.get("m").asNode());
                    }
                } else if (label.equals("Container")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getContainer(record.get("m").asNode(), session);
                    }
                } else if (label.equals("Component")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getComponent(record.get("m").asNode(), session);
                    }
                }

                String second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString();
                container.getRelationships()
                        .add(getRelation(record.get("r").asRelationship(), container.getId(), second_id));
            }

            // Добавление обратных связей
            query = "MATCH (m)-[r:Relationship]->(n:Container {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN r, m, m.structurizr_dsl_identifier";
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                String label = record.get("m").asNode().labels().toString();
                label = label.substring(1, label.length() - 1);
                if (label.equals("SoftwareSystem")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getSystem(record.get("m").asNode());
                    }
                    SoftwareSystem system1 = systems.get(record.get("m.structurizr_dsl_identifier").asString());
                    system1.getRelationships()
                            .add(getRelation(record.get("r").asRelationship(), system1.getId(),
                                    container.getId()));
                    systems.put(record.get("m.structurizr_dsl_identifier").asString(), system1);
                } else if (label.equals("Container")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getContainer(record.get("m").asNode(), session);
                    }
                    Container container1 = containers
                            .get(record.get("m.structurizr_dsl_identifier").asString());
                    container1.getRelationships()
                            .add(getRelation(record.get("r").asRelationship(), container1.getId(),
                                    container.getId()));
                    containers.put(record.get("m.structurizr_dsl_identifier").asString(), container1);
                } else if (label.equals("Component")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getComponent(record.get("m").asNode(), session);
                    }
                    Component component1 = components
                            .get(record.get("m.structurizr_dsl_identifier").asString());
                    component1.getRelationships()
                            .add(getRelation(record.get("r").asRelationship(), component1.getId(),
                                    container.getId()));
                    components.put(record.get("m.structurizr_dsl_identifier").asString(), component1);
                }
            }

            containers.put(container.getProperties().get("structurizr_dsl_identifier").toString(), container);

            // Добавление связей для компонентов
            query = "MATCH (n:Container {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m) RETURN m, m.structurizr_dsl_identifier";
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();

                Component component = components.get(record.get("m.structurizr_dsl_identifier").asString());

                // Добавление прямых связей
                query = "MATCH (n:Component{graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship]->(m) RETURN r, m, m.structurizr_dsl_identifier";
                parameters = Values.parameters("val1",
                        component.getProperties().get("structurizr_dsl_identifier"));
                Result result1 = session.run(query, parameters);

                while (result1.hasNext()) {
                    record = result1.next();
                    String label = record.get("m").asNode().labels().toString();
                    label = label.substring(1, label.length() - 1);
                    if (label.equals("SoftwareSystem")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getSystem(record.get("m").asNode());
                        }
                    } else if (label.equals("Container")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getContainer(record.get("m").asNode(), session);
                        }
                    } else if (label.equals("Component")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getComponent(record.get("m").asNode(), session);
                        }
                    }

                    String second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString())
                            .toString();
                    component.getRelationships()
                            .add(getRelation(record.get("r").asRelationship(), component.getId(), second_id));
                }

                // Добавление обратных связей
                query = "MATCH (m)-[r:Relationship]->(n:Component {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN r, m, m.structurizr_dsl_identifier";
                result1 = session.run(query, parameters);

                while (result1.hasNext()) {
                    record = result1.next();
                    String label = record.get("m").asNode().labels().toString();
                    label = label.substring(1, label.length() - 1);
                    if (label.equals("SoftwareSystem")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getSystem(record.get("m").asNode());
                        }
                        SoftwareSystem system1 = systems
                                .get(record.get("m.structurizr_dsl_identifier").asString());
                        system1.getRelationships()
                                .add(getRelation(record.get("r").asRelationship(), system1.getId(),
                                        component.getId()));
                        systems.put(record.get("m.structurizr_dsl_identifier").asString(), system1);
                    } else if (label.equals("Container")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getContainer(record.get("m").asNode(), session);
                        }
                        Container container1 = containers
                                .get(record.get("m.structurizr_dsl_identifier").asString());
                        container1.getRelationships()
                                .add(getRelation(record.get("r").asRelationship(), container1.getId(),
                                        component.getId()));
                        containers.put(record.get("m.structurizr_dsl_identifier").asString(), container1);
                    } else if (label.equals("Component")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getComponent(record.get("m").asNode(), session);
                        }
                        Component component1 = components
                                .get(record.get("m.structurizr_dsl_identifier").asString());
                        component1.getRelationships()
                                .add(getRelation(record.get("r").asRelationship(), component1.getId(),
                                        component.getId()));
                        components.put(record.get("m.structurizr_dsl_identifier").asString(), component1);
                    }
                }

                components.put(component.getProperties().get("structurizr_dsl_identifier").toString(),
                        component);
            }

        } else {

            // Добавление контейнеров
            query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:Container) RETURN m, m.structurizr_dsl_identifier";
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                getContainer(record.get("m").asNode(), session);

                // Добавление компонентов
                query = "MATCH (n:Container {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m) RETURN m";
                parameters = Values.parameters("val1", record.get("m.structurizr_dsl_identifier").asString());
                Result result1 = session.run(query, parameters);

                while (result1.hasNext()) {
                    record = result1.next();
                    getComponent(record.get("m").asNode(), session);
                }
            }

            // Добавление DeploymentNode
            // query = "MATCH (n:SoftwareSystem {graph: \"Global\",
            // structurizr_dsl_identifier: $val1})-[r:Child]->(m:DeploymentNode) RETURN m";
            // parameters = Values.parameters("val1", softwareSystemMnemonic);
            // result = session.run(query, parameters);

            // while (result.hasNext()) {
            // record = result.next();
            // getDeploymentNode(record.get("m").asNode(), session);
            // }

            // Добавление прямых связей
            query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship]->(m) RETURN r, m, m.structurizr_dsl_identifier";
            parameters = Values.parameters("val1", softwareSystemMnemonic);
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                String label = record.get("m").asNode().labels().toString();
                label = label.substring(1, label.length() - 1);
                if (label.equals("SoftwareSystem")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getSystem(record.get("m").asNode());
                    }
                } else if (label.equals("Container")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getContainer(record.get("m").asNode(), session);
                    }
                } else if (label.equals("Component")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getComponent(record.get("m").asNode(), session);
                    }
                }

                String second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString();
                system.getRelationships()
                        .add(getRelation(record.get("r").asRelationship(), system.getId(), second_id));
            }

            // Добавление обратных связей
            query = "MATCH (m)-[r:Relationship]->(n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN r, m, m.structurizr_dsl_identifier";
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                String label = record.get("m").asNode().labels().toString();
                label = label.substring(1, label.length() - 1);
                if (label.equals("SoftwareSystem")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getSystem(record.get("m").asNode());
                    }
                    SoftwareSystem system1 = systems.get(record.get("m.structurizr_dsl_identifier").asString());
                    system1.getRelationships()
                            .add(getRelation(record.get("r").asRelationship(), system1.getId(), system.getId()));
                    systems.put(record.get("m.structurizr_dsl_identifier").asString(), system1);
                } else if (label.equals("Container")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getContainer(record.get("m").asNode(), session);
                    }
                    Container container1 = containers.get(record.get("m.structurizr_dsl_identifier").asString());
                    container1.getRelationships()
                            .add(getRelation(record.get("r").asRelationship(), container1.getId(), system.getId()));
                    containers.put(record.get("m.structurizr_dsl_identifier").asString(), container1);
                } else if (label.equals("Component")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getComponent(record.get("m").asNode(), session);
                    }
                    Component component1 = components.get(record.get("m.structurizr_dsl_identifier").asString());
                    component1.getRelationships()
                            .add(getRelation(record.get("r").asRelationship(), component1.getId(), system.getId()));
                    components.put(record.get("m.structurizr_dsl_identifier").asString(), component1);
                }
            }

            systems.put(system.getProperties().get("structurizr_dsl_identifier").toString(), system);

            // Добавление связей для контейнеров
            query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:Container) RETURN m.structurizr_dsl_identifier";
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                Container container = containers.get(record.get("m.structurizr_dsl_identifier").asString());

                // Добавление прямых связей
                query = "MATCH (n:Container{graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship]->(m) RETURN r, m, m.structurizr_dsl_identifier";
                parameters = Values.parameters("val1", container.getProperties().get("structurizr_dsl_identifier"));
                Result result1 = session.run(query, parameters);

                while (result1.hasNext()) {
                    record = result1.next();
                    String label = record.get("m").asNode().labels().toString();
                    label = label.substring(1, label.length() - 1);
                    if (label.equals("SoftwareSystem")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getSystem(record.get("m").asNode());
                        }
                    } else if (label.equals("Container")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getContainer(record.get("m").asNode(), session);
                        }
                    } else if (label.equals("Component")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getComponent(record.get("m").asNode(), session);
                        }
                    }

                    String second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString();
                    container.getRelationships()
                            .add(getRelation(record.get("r").asRelationship(), container.getId(), second_id));
                }

                // Добавление обратных связей
                query = "MATCH (m)-[r:Relationship]->(n:Container {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN r, m, m.structurizr_dsl_identifier";
                result1 = session.run(query, parameters);

                while (result1.hasNext()) {
                    record = result1.next();
                    String label = record.get("m").asNode().labels().toString();
                    label = label.substring(1, label.length() - 1);
                    if (label.equals("SoftwareSystem")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getSystem(record.get("m").asNode());
                        }
                        SoftwareSystem system1 = systems.get(record.get("m.structurizr_dsl_identifier").asString());
                        system1.getRelationships()
                                .add(getRelation(record.get("r").asRelationship(), system1.getId(),
                                        container.getId()));
                        systems.put(record.get("m.structurizr_dsl_identifier").asString(), system1);
                    } else if (label.equals("Container")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getContainer(record.get("m").asNode(), session);
                        }
                        Container container1 = containers
                                .get(record.get("m.structurizr_dsl_identifier").asString());
                        container1.getRelationships()
                                .add(getRelation(record.get("r").asRelationship(), container1.getId(),
                                        container.getId()));
                        containers.put(record.get("m.structurizr_dsl_identifier").asString(), container1);
                    } else if (label.equals("Component")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getComponent(record.get("m").asNode(), session);
                        }
                        Component component1 = components
                                .get(record.get("m.structurizr_dsl_identifier").asString());
                        component1.getRelationships()
                                .add(getRelation(record.get("r").asRelationship(), component1.getId(),
                                        container.getId()));
                        components.put(record.get("m.structurizr_dsl_identifier").asString(), component1);
                    }
                }

                containers.put(container.getProperties().get("structurizr_dsl_identifier").toString(), container);

                // Добавление связей для компонентов
                query = "MATCH (n:Container {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m) RETURN m, m.structurizr_dsl_identifier";
                result1 = session.run(query, parameters);

                while (result1.hasNext()) {
                    record = result1.next();

                    Component component = components.get(record.get("m.structurizr_dsl_identifier").asString());

                    // Добавление прямых связей
                    query = "MATCH (n:Component{graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship]->(m) RETURN r, m, m.structurizr_dsl_identifier";
                    parameters = Values.parameters("val1",
                            component.getProperties().get("structurizr_dsl_identifier"));
                    Result result2 = session.run(query, parameters);

                    while (result2.hasNext()) {
                        record = result2.next();
                        String label = record.get("m").asNode().labels().toString();
                        label = label.substring(1, label.length() - 1);
                        if (label.equals("SoftwareSystem")) {
                            if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                                getSystem(record.get("m").asNode());
                            }
                        } else if (label.equals("Container")) {
                            if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                                getContainer(record.get("m").asNode(), session);
                            }
                        } else if (label.equals("Component")) {
                            if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                                getComponent(record.get("m").asNode(), session);
                            }
                        }

                        String second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString())
                                .toString();
                        component.getRelationships()
                                .add(getRelation(record.get("r").asRelationship(), component.getId(), second_id));
                    }

                    // Добавление обратных связей
                    query = "MATCH (m)-[r:Relationship]->(n:Component {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN r, m, m.structurizr_dsl_identifier";
                    result1 = session.run(query, parameters);

                    while (result1.hasNext()) {
                        record = result1.next();
                        String label = record.get("m").asNode().labels().toString();
                        label = label.substring(1, label.length() - 1);
                        if (label.equals("SoftwareSystem")) {
                            if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                                getSystem(record.get("m").asNode());
                            }
                            SoftwareSystem system1 = systems
                                    .get(record.get("m.structurizr_dsl_identifier").asString());
                            system1.getRelationships()
                                    .add(getRelation(record.get("r").asRelationship(), system1.getId(),
                                            component.getId()));
                            systems.put(record.get("m.structurizr_dsl_identifier").asString(), system1);
                        } else if (label.equals("Container")) {
                            if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                                getContainer(record.get("m").asNode(), session);
                            }
                            Container container1 = containers
                                    .get(record.get("m.structurizr_dsl_identifier").asString());
                            container1.getRelationships()
                                    .add(getRelation(record.get("r").asRelationship(), container1.getId(),
                                            component.getId()));
                            containers.put(record.get("m.structurizr_dsl_identifier").asString(), container1);
                        } else if (label.equals("Component")) {
                            if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                                getComponent(record.get("m").asNode(), session);
                            }
                            Component component1 = components
                                    .get(record.get("m.structurizr_dsl_identifier").asString());
                            component1.getRelationships()
                                    .add(getRelation(record.get("r").asRelationship(), component1.getId(),
                                            component.getId()));
                            components.put(record.get("m.structurizr_dsl_identifier").asString(), component1);
                        }
                    }

                    components.put(component.getProperties().get("structurizr_dsl_identifier").toString(),
                            component);
                }
            }

            // Добавление связей DeploymentNode
            // query = "MATCH (n:SoftwareSystem {graph: \"Global\",
            // structurizr_dsl_identifier: $val1})-[r:Child]->(m:DeploymentNode) RETURN
            // m.structurizr_dsl_identifier";
            // parameters = Values.parameters("val1", softwareSystemMnemonic);
            // result = session.run(query, parameters);

            // while (result.hasNext()) {
            // record = result.next();
            // model.getDeploymentNodes().add(getDeploymentNodeRelations(
            // deploymentNodes.get(record.get("m.structurizr_dsl_identifier").asString()),
            // session));
            // }
        }

        // Обновление данных
        for (Map.Entry<String, SoftwareSystem> entry1 : systems.entrySet()) {
            SoftwareSystem system1 = entry1.getValue();
            List<Container> containers_list = new ArrayList<>();
            for (Container container : system1.getContainers()) {
                List<Component> components_list = new ArrayList<>();
                for (Component component : container.getComponents()) {
                    components_list.add(components
                            .get(component.getProperties().get("structurizr_dsl_identifier").toString()));
                }
                Container real_container = containers
                        .get(container.getProperties().get("structurizr_dsl_identifier").toString());
                real_container.setComponents(components_list);
                containers_list.add(real_container);
            }
            system1.setContainers(containers_list);
            systems.put(system1.getProperties().get("structurizr_dsl_identifier").toString(), system1);
        }

        for (Map.Entry<String, SoftwareSystem> entry : systems.entrySet()) {
            model.getSoftwareSystems().add(entry.getValue());
        }
        workspace.setModel(model);

        // Создание views
        workspace.setViews(new Views());

        // Заполнение containerViews
        List<ContainerView> containerViews = new ArrayList<>();
        ContainerView containerView = new ContainerView();
        containerView.setElements(new ArrayList<>());
        containerView.setRelationships(new ArrayList<>());

        system = systems.get(softwareSystemMnemonic);

        // Проставление параметров
        containerView.setSoftwareSystemId(system.getId());
        containerView.setExternalSoftwareSystemBoundariesVisible(false);
        containerView.setKey("containers");
        containerView.setOrder(2);

        // Создание automaticLayout
        AutomaticLayout automaticLayout = new AutomaticLayout();
        automaticLayout.setApplied(false);
        automaticLayout.setImplementation(LayoutImplementation.Graphviz);
        automaticLayout.setNodeSeparation(300);
        automaticLayout.setRankDirection(RankDirection.TopBottom);
        automaticLayout.setRankSeparation(300);
        automaticLayout.setVertices(false);

        containerView.setAutomaticLayout(automaticLayout);

        // Проставление Elements и Relationships
        for (Container container : system.getContainers()) {
            ElementView cont = new ElementView();
            cont.setId(container.getId());
            cont.setX(0);
            cont.setY(0);
            containerView.getElements().add(cont);
            for (Relationship relationship : container.getRelationships()) {
                RelationshipView rel = new RelationshipView();
                rel.setId(relationship.getId());
                containerView.getRelationships().add(rel);
            }
        }

        containerViews.add(containerView);
        workspace.getViews().setContainerViews(containerViews);

        return workspace;
    }

}
