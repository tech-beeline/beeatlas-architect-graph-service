package com.example.architecting_graph;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;

import com.example.architecting_graph.AutomaticLayout.LayoutImplementation;
import com.example.architecting_graph.AutomaticLayout.RankDirection;

public class GetObjects {
    private static Long id_obj;
    private static Map<String, Long> map_id;
    private static Map<String, String> parents;
    private static Set<String> childs;
    private static Set<Edge> rels;
    private static Map<String, SoftwareSystem> systems;
    private static Map<String, Container> containers;
    private static Map<String, Component> components;
    private static Map<String, DeploymentNode> deploymentNodes;
    private static Map<String, InfrastructureNode> infrastructureNodes;
    private static Map<String, ContainerInstance> containerInstances;

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
        parents.put(container.getProperties().get("structurizr_dsl_identifier").toString(),
                record.get("m.structurizr_dsl_identifier").asString());

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
        parents.put(component.getProperties().get("structurizr_dsl_identifier").toString(),
                record.get("m.structurizr_dsl_identifier").asString());
    }

    public static Relationship getRelation(org.neo4j.driver.types.Relationship relation, String source,
            String destination) {
        Relationship relationship = new Relationship();
        relationship.setProperties(new HashMap<>());
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

        Edge edge = new Edge();
        edge.setParams(source, destination, relationship.getDescription());
        if (rels.contains(edge)) {
            return null;
        }
        rels.add(edge);

        relationship.setId(String.valueOf(id_obj));
        id_obj = id_obj + 1;

        return relationship;
    }

    public static void getInfrastructureNode(Node node, Session session, String environment) {
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

        // Добавление Environment
        String query = "MATCH (n:Environment)-[r:Child]->(m:InfrastructureNode {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.name";
        Value parameters = Values.parameters("val1",
                infrastructureNode.getProperties().get("structurizr_dsl_identifier"));
        Result result = session.run(query, parameters);
        infrastructureNode.setEnvironment(result.next().get("n.name").asString());

        if (!infrastructureNode.getEnvironment().equals(environment)) {
            return;
        }

        map_id.put(infrastructureNode.getProperties().get("structurizr_dsl_identifier").toString(), id_obj);
        id_obj = id_obj + 1;

        infrastructureNodes.put(infrastructureNode.getProperties().get("structurizr_dsl_identifier").toString(),
                infrastructureNode);
    }

    public static void getContainerInstance(Node node, Session session, String environment) {
        ContainerInstance containerInstance = new ContainerInstance();
        containerInstance.setProperties(new HashMap<>());
        containerInstance.setRelationships(new ArrayList<>());
        containerInstance.setId(String.valueOf(id_obj));

        // Добавление property
        for (String key : node.keys()) {
            try {
                Field field = ContainerInstance.class.getDeclaredField(key);
                field.setAccessible(true); // Разрешение доступа к приватным полям
                // Установка значения поля
                field.set(containerInstance, node.get(key).asObject());
            } catch (Exception e) {
                containerInstance.getProperties().put(key, node.get(key).asObject());
            }
        }

        // Добавление Environment
        String query = "MATCH (n:Environment)-[r:Child]->(m:ContainerInstance {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.name";
        Value parameters = Values.parameters("val1",
                containerInstance.getProperties().get("structurizr_dsl_identifier"));
        Result result = session.run(query, parameters);
        containerInstance.setEnvironment(result.next().get("n.name").asString());

        if (!containerInstance.getEnvironment().equals(environment)) {
            return;
        }

        // Добавление id контейнера
        query = "MATCH (n:Container)-[r:Deploy]->(m:ContainerInstance {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.structurizr_dsl_identifier";
        result = session.run(query, parameters);
        String contIdentifier = result.next().get("n.structurizr_dsl_identifier").asString();
        containerInstance.setContainerId(map_id.get(contIdentifier).toString());

        map_id.put(containerInstance.getProperties().get("structurizr_dsl_identifier").toString(), id_obj);
        id_obj = id_obj + 1;

        containerInstances.put(containerInstance.getProperties().get("structurizr_dsl_identifier").toString(),
                containerInstance);
    }

    public static void getDeploymentNode(Node node, Session session, String environment) {
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

        // Добавление Environment
        String query = "MATCH (n:Environment)-[r:Child]->(m:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.name";
        Value parameters = Values.parameters("val1", deploymentNode.getProperties().get("structurizr_dsl_identifier"));
        Result result = session.run(query, parameters);
        deploymentNode.setEnvironment(result.next().get("n.name").asString());

        if (!deploymentNode.getEnvironment().equals(environment)) {
            return;
        }

        map_id.put(deploymentNode.getProperties().get("structurizr_dsl_identifier").toString(), id_obj);
        id_obj = id_obj + 1;

        // Добавление InfrastructureNode
        query = "MATCH (n:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:InfrastructureNode) RETURN m";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            getInfrastructureNode(record.get("m").asNode(), session, environment);
        }

        // Добавление ContainerInstance
        query = "MATCH (n:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:ContainerInstance) RETURN m";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            getContainerInstance(record.get("m").asNode(), session, environment);
        }

        // Добавление дочерних DeploymentNode
        query = "MATCH (n:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:DeploymentNode) RETURN m";
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            getDeploymentNode(record.get("m").asNode(), session, environment);
        }

        deploymentNodes.put(deploymentNode.getProperties().get("structurizr_dsl_identifier").toString(),
                deploymentNode);
    }

    public static DeploymentNode getDeploymentNodeRelations(DeploymentNode deploymentNode, Session session,
            String cmdb) {

        // Добавление связей
        deploymentNode.setRelationships(new ArrayList<>());
        String query = "MATCH (n:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN r, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", deploymentNode.getProperties().get("structurizr_dsl_identifier"),
                "cmdb", cmdb);
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
        parameters = Values.parameters("val1", deploymentNode.getProperties().get("structurizr_dsl_identifier"));
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            InfrastructureNode infrastructureNode = infrastructureNodes
                    .get(record.get("m.structurizr_dsl_identifier").asString());

            query = "MATCH (n:InfrastructureNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN r, m.structurizr_dsl_identifier";
            parameters = Values.parameters("val1",
                    infrastructureNode.getProperties().get("structurizr_dsl_identifier"), "cmdb", cmdb);
            Result result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();

                String second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString();
                infrastructureNode.getRelationships()
                        .add(getRelation(record.get("r").asRelationship(), infrastructureNode.getId(), second_id));
            }

            deploymentNode.getInfrastructureNodes().add(infrastructureNode);
        }

        // Добавление ContainerInstance
        deploymentNode.setContainerInstances(new ArrayList<>());

        query = "MATCH (n:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:ContainerInstance) RETURN m.structurizr_dsl_identifier";
        parameters = Values.parameters("val1", deploymentNode.getProperties().get("structurizr_dsl_identifier"));
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            ContainerInstance containerInstance = containerInstances
                    .get(record.get("m.structurizr_dsl_identifier").asString());

            query = "MATCH (n:ContainerInstance {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN r, m.structurizr_dsl_identifier";
            parameters = Values.parameters("val1", containerInstance.getProperties().get("structurizr_dsl_identifier"),
                    "cmdb", cmdb);
            Result result1 = session.run(query, parameters);

            while (result1.hasNext()) {
                record = result1.next();

                String second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString();
                containerInstance.getRelationships()
                        .add(getRelation(record.get("r").asRelationship(), containerInstance.getId(), second_id));
            }

            deploymentNode.getContainerInstances().add(containerInstance);
        }

        // Добавление дочерних DeploymentNode
        deploymentNode.setChildren(new ArrayList<>());

        query = "MATCH (n:DeploymentNode {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:DeploymentNode) RETURN m.structurizr_dsl_identifier";
        parameters = Values.parameters("val1", deploymentNode.getProperties().get("structurizr_dsl_identifier"));
        result = session.run(query, parameters);

        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            deploymentNode.getChildren()
                    .add(getDeploymentNodeRelations(
                            deploymentNodes.get(record.get("m.structurizr_dsl_identifier").asString()), session, cmdb));
        }

        return deploymentNode;
    }

    public static void getDeploymentElements(DeploymentNode deploymentNode, DeploymentView deploymentView,
            Set<String> objects) {

        // Добавление текущего элемента
        if (!objects.contains(deploymentNode.getId())) {
            ElementView depl = new ElementView();
            depl.setId(deploymentNode.getId());
            depl.setX(0);
            depl.setY(0);
            deploymentView.getElements().add(depl);
            objects.add(deploymentNode.getId());
        }

        // Проход по всем связям
        for (Relationship relationship : deploymentNode.getRelationships()) {
            if (!objects.contains(relationship.getId())) {
                RelationshipView rel = new RelationshipView();
                rel.setId(relationship.getId());
                deploymentView.getRelationships().add(rel);
                objects.add(relationship.getId());
            }
            if (!objects.contains(relationship.getDestinationId())) {
                ElementView el = new ElementView();
                el.setId(relationship.getDestinationId());
                el.setX(0);
                el.setY(0);
                deploymentView.getElements().add(el);
                objects.add(relationship.getDestinationId());
            }
        }

        // Проход по всем InfrastructureNodes
        for (InfrastructureNode infrastructureNode : deploymentNode.getInfrastructureNodes()) {
            if (!objects.contains(infrastructureNode.getId())) {
                ElementView el = new ElementView();
                el.setId(infrastructureNode.getId());
                el.setX(0);
                el.setY(0);
                deploymentView.getElements().add(el);
                objects.add(infrastructureNode.getId());
            }

            // Проход по всем связям
            for (Relationship relationship : infrastructureNode.getRelationships()) {
                if (!objects.contains(relationship.getId())) {
                    RelationshipView rel = new RelationshipView();
                    rel.setId(relationship.getId());
                    deploymentView.getRelationships().add(rel);
                    objects.add(relationship.getId());
                }
                if (!objects.contains(relationship.getDestinationId())) {
                    ElementView el = new ElementView();
                    el.setId(relationship.getDestinationId());
                    el.setX(0);
                    el.setY(0);
                    deploymentView.getElements().add(el);
                    objects.add(relationship.getDestinationId());
                }
            }
        }

        // Проход по всем ContainerInstances
        for (ContainerInstance containerInstance : deploymentNode.getContainerInstances()) {
            if (!objects.contains(containerInstance.getId())) {
                ElementView el = new ElementView();
                el.setId(containerInstance.getId());
                el.setX(0);
                el.setY(0);
                deploymentView.getElements().add(el);
                objects.add(containerInstance.getId());
            }

            // Проход по всем связям
            for (Relationship relationship : containerInstance.getRelationships()) {
                if (!objects.contains(relationship.getId())) {
                    RelationshipView rel = new RelationshipView();
                    rel.setId(relationship.getId());
                    deploymentView.getRelationships().add(rel);
                    objects.add(relationship.getId());
                }
                if (!objects.contains(relationship.getDestinationId())) {
                    ElementView el = new ElementView();
                    el.setId(relationship.getDestinationId());
                    el.setX(0);
                    el.setY(0);
                    deploymentView.getElements().add(el);
                    objects.add(relationship.getDestinationId());
                }
            }
        }

        // Проход по всем дочерним DeploymentNodes
        for (DeploymentNode childDeploymentNode : deploymentNode.getChildren()) {
            getDeploymentElements(childDeploymentNode, deploymentView, objects);
        }
    }

    public static Workspace GetWorkspace(String softwareSystemMnemonic, String containerMnemonic, String environment,
            String uri,
            String user, String password) {

        // Начальная инициализация
        id_obj = 2L;
        map_id = new HashMap<>();
        parents = new HashMap<>();
        childs = new HashSet<>();
        rels = new HashSet<>();
        systems = new HashMap<>();
        containers = new HashMap<>();
        components = new HashMap<>();
        deploymentNodes = new HashMap<>();
        infrastructureNodes = new HashMap<>();
        containerInstances = new HashMap<>();

        // Подключение к БД
        Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        Session session = driver.session();

        // Возвращаем воркспейс
        Workspace workspace = new Workspace();
        Model model = new Model();
        model.setSoftwareSystems(new ArrayList<>());
        model.setDeploymentNodes(new ArrayList<>());
        workspace.setId(1L);

        // Создание views
        workspace.setViews(new Views());

        String query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n";
        Value parameters = Values.parameters("val1", softwareSystemMnemonic);
        Result result = session.run(query, parameters);
        org.neo4j.driver.Record record = result.next();

        // Добавление системы
        SoftwareSystem system = getSystem(record.get("n").asNode());
        String cmdb = system.getProperties().get("cmdb").toString();

        if (containerMnemonic != null) {

            // Добавление контейнера
            query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:Container {graph: \"Global\", structurizr_dsl_identifier: $val2}) RETURN m";
            parameters = Values.parameters("val1", softwareSystemMnemonic, "val2", containerMnemonic);
            result = session.run(query, parameters);
            record = result.next();

            getContainer(record.get("m").asNode(), session);

            // Добавление компонентов
            query = "MATCH (n:Container {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m) RETURN m";
            parameters = Values.parameters("val1", containerMnemonic);
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                childs.add(id_obj.toString());
                getComponent(record.get("m").asNode(), session);
            }

            // Добавление связей для компонентов
            query = "MATCH (n:Container {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m) RETURN m, m.structurizr_dsl_identifier";
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();

                Component component = components.get(record.get("m.structurizr_dsl_identifier").asString());

                // Добавление прямых связей
                query = "MATCH (n:Component{graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN r, m, m.structurizr_dsl_identifier";
                parameters = Values.parameters("val1",
                        component.getProperties().get("structurizr_dsl_identifier"), "cmdb", cmdb);
                Result result1 = session.run(query, parameters);

                while (result1.hasNext()) {
                    record = result1.next();
                    String label = record.get("m").asNode().labels().toString();
                    label = label.substring(1, label.length() - 1);
                    String second_sys_id = null;
                    if (label.equals("SoftwareSystem")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getSystem(record.get("m").asNode());
                        }
                        second_sys_id = systems.get(record.get("m.structurizr_dsl_identifier").asString()).getId();
                    } else if (label.equals("Container")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getContainer(record.get("m").asNode(), session);
                        }
                        second_sys_id = systems.get(parents.get(record.get("m.structurizr_dsl_identifier").asString()))
                                .getId();
                    } else if (label.equals("Component")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getComponent(record.get("m").asNode(), session);
                        }
                        second_sys_id = systems.get(parents.get(parents.get(record.get("m.structurizr_dsl_identifier")
                                .asString()))).getId();
                    }

                    if (!second_sys_id.equals(system.getId())) {
                        Relationship rel = getRelation(record.get("r").asRelationship(), component.getId(),
                                second_sys_id);

                        if (rel != null) {
                            component.getRelationships().add(rel);
                        }
                    } else {
                        String second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString();
                        Relationship rel = getRelation(record.get("r").asRelationship(), component.getId(), second_id);

                        if (rel != null) {
                            component.getRelationships().add(rel);
                        }
                    }
                }

                // Добавление обратных связей
                query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:Component {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN r, m, m.structurizr_dsl_identifier";
                result1 = session.run(query, parameters);

                while (result1.hasNext()) {
                    record = result1.next();
                    if (map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString()) && childs
                            .contains(map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString())) {
                        continue;
                    }
                    String label = record.get("m").asNode().labels().toString();
                    label = label.substring(1, label.length() - 1);
                    SoftwareSystem system1 = null;
                    if (label.equals("SoftwareSystem")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getSystem(record.get("m").asNode());
                        }
                        system1 = systems.get(record.get("m.structurizr_dsl_identifier").asString());
                    } else if (label.equals("Container")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getContainer(record.get("m").asNode(), session);
                        }
                        system1 = systems.get(parents.get(record.get("m.structurizr_dsl_identifier").asString()));
                    } else if (label.equals("Component")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getComponent(record.get("m").asNode(), session);
                        }
                        system1 = systems
                                .get(parents.get(parents.get(record.get("m.structurizr_dsl_identifier").asString())));
                    }

                    Relationship rel = getRelation(record.get("r").asRelationship(), system1.getId(),
                            component.getId());

                    if (rel != null) {
                        system1.getRelationships().add(rel);
                        systems.put(record.get("m.structurizr_dsl_identifier").asString(), system1);
                    }
                }

                components.put(component.getProperties().get("structurizr_dsl_identifier").toString(),
                        component);
            }

        } else if (environment == null) {

            // Добавление контейнеров
            query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:Container) RETURN m, m.structurizr_dsl_identifier";
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                childs.add(String.valueOf(id_obj));
                getContainer(record.get("m").asNode(), session);

                // Добавление компонентов
                query = "MATCH (n:Container {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:Component) RETURN m, m.structurizr_dsl_identifier";
                parameters = Values.parameters("val1", record.get("m.structurizr_dsl_identifier").asString());
                Result result1 = session.run(query, parameters);

                while (result1.hasNext()) {
                    record = result1.next();
                    childs.add(String.valueOf(id_obj));
                    getComponent(record.get("m").asNode(), session);
                }
            }

            // Добавление прямых связей
            query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN r, m, m.structurizr_dsl_identifier";
            parameters = Values.parameters("val1", softwareSystemMnemonic, "cmdb", cmdb);
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                String label = record.get("m").asNode().labels().toString();
                label = label.substring(1, label.length() - 1);
                String second_id = null;
                if (label.equals("SoftwareSystem")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getSystem(record.get("m").asNode());
                    }
                    second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString();
                } else if (label.equals("Container")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getContainer(record.get("m").asNode(), session);
                    }
                    second_id = systems.get(parents.get(record.get("m.structurizr_dsl_identifier").asString())).getId();
                } else if (label.equals("Component")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getComponent(record.get("m").asNode(), session);
                    }
                    second_id = systems
                            .get(parents.get(parents.get(record.get("m.structurizr_dsl_identifier").asString())))
                            .getId();
                }

                Relationship rel = getRelation(record.get("r").asRelationship(), system.getId(), second_id);

                if (rel != null) {
                    system.getRelationships().add(rel);
                }
            }

            // Добавление обратных связей
            query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN r, m, m.structurizr_dsl_identifier";
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                if (map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString()) && childs
                        .contains(map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString())) {
                    continue;
                }
                String label = record.get("m").asNode().labels().toString();
                label = label.substring(1, label.length() - 1);
                SoftwareSystem system1 = null;
                if (label.equals("SoftwareSystem")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getSystem(record.get("m").asNode());
                    }
                    system1 = systems.get(record.get("m.structurizr_dsl_identifier").asString());
                } else if (label.equals("Container")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getContainer(record.get("m").asNode(), session);
                    }
                    system1 = systems.get(parents.get(record.get("m.structurizr_dsl_identifier").asString()));
                } else if (label.equals("Component")) {
                    if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                        getComponent(record.get("m").asNode(), session);
                    }
                    system1 = systems
                            .get(parents.get(parents.get(record.get("m.structurizr_dsl_identifier").asString())));
                }

                Relationship rel = getRelation(record.get("r").asRelationship(), system1.getId(), system.getId());

                if (rel != null) {
                    system1.getRelationships().add(rel);
                    systems.put(record.get("m.structurizr_dsl_identifier").asString(), system1);
                }
            }

            // Добавление связей для контейнеров
            query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:Container) RETURN m.structurizr_dsl_identifier";
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                Container container = containers.get(record.get("m.structurizr_dsl_identifier").asString());

                // Добавление прямых связей
                query = "MATCH (n:Container{graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN r, m, m.structurizr_dsl_identifier";
                parameters = Values.parameters("val1", container.getProperties().get("structurizr_dsl_identifier"),
                        "cmdb", cmdb);
                Result result1 = session.run(query, parameters);

                while (result1.hasNext()) {
                    record = result1.next();
                    String label = record.get("m").asNode().labels().toString();
                    label = label.substring(1, label.length() - 1);
                    String second_sys_id = null;
                    if (label.equals("SoftwareSystem")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getSystem(record.get("m").asNode());
                        }
                        second_sys_id = systems.get(record.get("m.structurizr_dsl_identifier").asString()).getId();
                    } else if (label.equals("Container")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getContainer(record.get("m").asNode(), session);
                        }
                        second_sys_id = systems.get(parents.get(record.get("m.structurizr_dsl_identifier").asString()))
                                .getId();
                    } else if (label.equals("Component")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getComponent(record.get("m").asNode(), session);
                        }
                        second_sys_id = systems.get(parents.get(parents.get(record.get("m.structurizr_dsl_identifier")
                                .asString()))).getId();
                    }

                    if (!second_sys_id.equals(system.getId())) {
                        Relationship rel = getRelation(record.get("r").asRelationship(), container.getId(),
                                second_sys_id);

                        if (rel != null) {
                            container.getRelationships().add(rel);
                        }

                        rel = getRelation(record.get("r").asRelationship(), system.getId(), second_sys_id);

                        if (rel != null) {
                            system.getRelationships().add(rel);
                        }

                    } else {
                        String second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString();
                        Relationship rel = getRelation(record.get("r").asRelationship(), container.getId(), second_id);

                        if (rel != null) {
                            container.getRelationships().add(rel);
                        }
                    }
                }

                // Добавление обратных связей
                query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:Container {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN r, m, m.structurizr_dsl_identifier";
                result1 = session.run(query, parameters);

                while (result1.hasNext()) {
                    record = result1.next();
                    if (map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString()) && childs
                            .contains(map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString())) {
                        continue;
                    }
                    String label = record.get("m").asNode().labels().toString();
                    label = label.substring(1, label.length() - 1);
                    SoftwareSystem system1 = null;
                    if (label.equals("SoftwareSystem")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getSystem(record.get("m").asNode());
                        }
                        system1 = systems
                                .get(record.get("m.structurizr_dsl_identifier").asString());
                    } else if (label.equals("Container")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getContainer(record.get("m").asNode(), session);
                        }
                        system1 = systems
                                .get(parents.get(record.get("m.structurizr_dsl_identifier").asString()));
                    } else if (label.equals("Component")) {
                        if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                            getComponent(record.get("m").asNode(), session);
                        }
                        system1 = systems
                                .get(parents.get(parents.get(record.get("m.structurizr_dsl_identifier").asString())));
                    }

                    Relationship rel = getRelation(record.get("r").asRelationship(), system1.getId(),
                            container.getId());

                    if (rel != null) {
                        system1.getRelationships().add(rel);
                    }

                    rel = getRelation(record.get("r").asRelationship(), system1.getId(), system.getId());

                    if (rel != null) {
                        system1.getRelationships().add(rel);
                    }

                    systems.put(record.get("m.structurizr_dsl_identifier").asString(), system1);
                }

                containers.put(container.getProperties().get("structurizr_dsl_identifier").toString(), container);

                // Добавление связей для компонентов
                query = "MATCH (n:Container {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:Container) RETURN m.structurizr_dsl_identifier";
                parameters = Values.parameters("val1", container.getProperties().get("structurizr_dsl_identifier"));
                result1 = session.run(query, parameters);

                while (result1.hasNext()) {

                    record = result.next();
                    Component component = components.get(record.get("m.structurizr_dsl_identifier").asString());

                    // Добавление прямых связей
                    query = "MATCH (n:Component {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship {source_workspace: $cmdb}]->(m) RETURN r, m, m.structurizr_dsl_identifier";
                    parameters = Values.parameters("val1", component.getProperties().get("structurizr_dsl_identifier"),
                            "cmdb", cmdb);
                    Result result2 = session.run(query, parameters);

                    while (result2.hasNext()) {
                        record = result2.next();
                        String label = record.get("m").asNode().labels().toString();
                        label = label.substring(1, label.length() - 1);
                        String second_sys_id = null;
                        if (label.equals("SoftwareSystem")) {
                            if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                                getSystem(record.get("m").asNode());
                            }
                            second_sys_id = systems.get(record.get("m.structurizr_dsl_identifier").asString()).getId();
                        } else if (label.equals("Container")) {
                            if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                                getContainer(record.get("m").asNode(), session);
                            }
                            second_sys_id = systems
                                    .get(parents.get(record.get("m.structurizr_dsl_identifier").asString()))
                                    .getId();
                        } else if (label.equals("Component")) {
                            if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                                getComponent(record.get("m").asNode(), session);
                            }
                            second_sys_id = systems
                                    .get(parents
                                            .get(parents.get(record.get("m.structurizr_dsl_identifier").asString())))
                                    .getId();
                        }

                        if (!second_sys_id.equals(system.getId())) {
                            Relationship rel = getRelation(record.get("r").asRelationship(), component.getId(),
                                    second_sys_id);

                            if (rel != null) {
                                component.getRelationships().add(rel);
                            }

                            rel = getRelation(record.get("r").asRelationship(), system.getId(), second_sys_id);

                            if (rel != null) {
                                system.getRelationships().add(rel);
                            }
                        } else {
                            String second_id = map_id.get(record.get("m.structurizr_dsl_identifier").asString())
                                    .toString();
                            Relationship rel = getRelation(record.get("r").asRelationship(), component.getId(),
                                    second_id);

                            if (rel != null) {
                                component.getRelationships().add(rel);
                            }
                        }
                    }

                    // Добавление обратных связей
                    query = "MATCH (m)-[r:Relationship {source_workspace: $cmdb}]->(n:Component {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN r, m, m.structurizr_dsl_identifier";
                    result2 = session.run(query, parameters);

                    while (result2.hasNext()) {
                        record = result2.next();
                        if (map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString()) && childs
                                .contains(
                                        map_id.get(record.get("m.structurizr_dsl_identifier").asString()).toString())) {
                            continue;
                        }
                        String label = record.get("m").asNode().labels().toString();
                        label = label.substring(1, label.length() - 1);
                        SoftwareSystem system1 = null;
                        if (label.equals("SoftwareSystem")) {
                            if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                                getSystem(record.get("m").asNode());
                            }
                            system1 = systems
                                    .get(record.get("m.structurizr_dsl_identifier").asString());
                        } else if (label.equals("Container")) {
                            if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                                getContainer(record.get("m").asNode(), session);
                            }
                            system1 = systems
                                    .get(parents.get(record.get("m.structurizr_dsl_identifier").asString()));
                        } else if (label.equals("Component")) {
                            if (!map_id.containsKey(record.get("m.structurizr_dsl_identifier").asString())) {
                                getComponent(record.get("m").asNode(), session);
                            }
                            system1 = systems
                                    .get(parents
                                            .get(parents.get(record.get("m.structurizr_dsl_identifier").asString())));
                        }

                        Relationship rel = getRelation(record.get("r").asRelationship(), system1.getId(),
                                component.getId());

                        if (rel != null) {
                            system1.getRelationships().add(rel);
                        }

                        rel = getRelation(record.get("r").asRelationship(), system1.getId(), system.getId());

                        if (rel != null) {
                            system1.getRelationships().add(rel);
                        }

                        systems.put(record.get("m.structurizr_dsl_identifier").asString(), system1);
                    }

                    components.put(component.getProperties().get("structurizr_dsl_identifier").toString(), component);
                }
            }
        } else {

            // Добавление контейнеров
            query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:Container) RETURN m";
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                getContainer(record.get("m").asNode(), session);
            }

            // Добавление DeploymentNode
            query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:DeploymentNode) RETURN m";
            parameters = Values.parameters("val1", softwareSystemMnemonic);
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                getDeploymentNode(record.get("m").asNode(), session, environment);
            }

            // Добавление связей DeploymentNode
            query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:DeploymentNode) RETURN m.structurizr_dsl_identifier";
            parameters = Values.parameters("val1", softwareSystemMnemonic);
            result = session.run(query, parameters);

            while (result.hasNext()) {
                record = result.next();
                System.out.println(record.get("m.structurizr_dsl_identifier").asString());
                model.getDeploymentNodes().add(getDeploymentNodeRelations(
                        deploymentNodes.get(record.get("m.structurizr_dsl_identifier").asString()), session, cmdb));
            }
        }

        systems.put(system.getProperties().get("structurizr_dsl_identifier").toString(), system);

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
                containers.put(real_container.getProperties().get("structurizr_dsl_identifier").toString(),
                        real_container);
                containers_list.add(real_container);
            }
            system1.setContainers(containers_list);
            systems.put(system1.getProperties().get("structurizr_dsl_identifier").toString(), system1);
        }

        system = systems.get(softwareSystemMnemonic);

        for (Map.Entry<String, SoftwareSystem> entry : systems.entrySet()) {
            model.getSoftwareSystems().add(entry.getValue());
        }

        workspace.setModel(model);

        if (containerMnemonic == null && environment == null) {

            // Заполнение systemContextViews
            List<SystemContextView> systemContextViews = new ArrayList<>();
            SystemContextView systemContextView = new SystemContextView();
            systemContextView.setElements(new ArrayList<>());
            systemContextView.setRelationships(new ArrayList<>());

            // Проставление параметров
            systemContextView.setSoftwareSystemId(system.getId());
            systemContextView.setEnterpriseBoundaryVisible(true);
            systemContextView.setKey("context");
            systemContextView.setOrder(1);

            // Создание automaticLayout
            AutomaticLayout automaticLayout1 = new AutomaticLayout();
            automaticLayout1.setApplied(false);
            automaticLayout1.setEdgeSeparation(0);
            automaticLayout1.setImplementation(LayoutImplementation.Graphviz);
            automaticLayout1.setNodeSeparation(300);
            automaticLayout1.setRankDirection(RankDirection.TopBottom);
            automaticLayout1.setRankSeparation(300);
            automaticLayout1.setVertices(false);

            systemContextView.setAutomaticLayout(automaticLayout1);

            ElementView sys = new ElementView();
            sys.setId(system.getId());
            sys.setX(0);
            sys.setY(0);
            systemContextView.getElements().add(sys);

            // Проставление Elements и Relationships
            Set<String> objects1 = new HashSet<>();
            objects1.add(system.getId());
            for (Relationship relationship : system.getRelationships()) {
                RelationshipView rel = new RelationshipView();
                rel.setId(relationship.getId());
                systemContextView.getRelationships().add(rel);
                if (!objects1.contains(relationship.getDestinationId())) {
                    ElementView tmp = new ElementView();
                    tmp.setId(relationship.getDestinationId());
                    tmp.setX(0);
                    tmp.setY(0);
                    systemContextView.getElements().add(tmp);
                    objects1.add(relationship.getDestinationId());
                }
            }

            for (Map.Entry<String, SoftwareSystem> entry1 : systems.entrySet()) {
                SoftwareSystem system1 = entry1.getValue();

                if (system1.getId().equals(system.getId())) {
                    continue;
                }

                for (Relationship relationship : system1.getRelationships()) {
                    if (system.getId().equals(relationship.getDestinationId())) {
                        RelationshipView rel = new RelationshipView();
                        rel.setId(relationship.getId());
                        systemContextView.getRelationships().add(rel);
                        if (!objects1.contains(relationship.getSourceId())) {
                            ElementView tmp = new ElementView();
                            tmp.setId(relationship.getSourceId());
                            tmp.setX(0);
                            tmp.setY(0);
                            systemContextView.getElements().add(tmp);
                            objects1.add(relationship.getSourceId());
                        }
                    }
                }
            }

            systemContextViews.add(systemContextView);
            workspace.getViews().setSystemContextViews(systemContextViews);

            // Заполнение containerViews
            List<ContainerView> containerViews = new ArrayList<>();
            ContainerView containerView = new ContainerView();
            containerView.setElements(new ArrayList<>());
            containerView.setRelationships(new ArrayList<>());

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
            Set<String> objects = new HashSet<>();
            Set<String> conts = new HashSet<>();
            objects.add(system.getId());
            for (Container container : system.getContainers()) {
                ElementView cont = new ElementView();
                cont.setId(container.getId());
                cont.setX(0);
                cont.setY(0);
                if (!objects.contains(container.getId())) {
                    containerView.getElements().add(cont);
                    objects.add(container.getId());
                }
                conts.add(container.getId());
                for (Relationship relationship : container.getRelationships()) {
                    RelationshipView rel = new RelationshipView();
                    rel.setId(relationship.getId());
                    containerView.getRelationships().add(rel);
                    if (!objects.contains(relationship.getDestinationId())) {
                        ElementView tmp = new ElementView();
                        tmp.setId(relationship.getDestinationId());
                        tmp.setX(0);
                        tmp.setY(0);
                        containerView.getElements().add(tmp);
                        objects.add(relationship.getDestinationId());
                    }
                }
            }

            for (Map.Entry<String, SoftwareSystem> entry1 : systems.entrySet()) {
                SoftwareSystem system1 = entry1.getValue();
                for (Relationship relationship : system1.getRelationships()) {
                    if (conts.contains(relationship.getDestinationId())) {
                        RelationshipView rel = new RelationshipView();
                        rel.setId(relationship.getId());
                        containerView.getRelationships().add(rel);
                        if (!objects.contains(relationship.getSourceId())) {
                            ElementView cont = new ElementView();
                            cont.setId(relationship.getSourceId());
                            cont.setX(0);
                            cont.setY(0);
                            containerView.getElements().add(cont);
                            objects.add(relationship.getSourceId());
                        }
                    }
                }
            }

            containerViews.add(containerView);
            workspace.getViews().setContainerViews(containerViews);
        } else if (environment == null) {
            // Заполнение componentViews
            List<ComponentView> componentViews = new ArrayList<>();
            ComponentView componentView = new ComponentView();
            componentView.setElements(new ArrayList<>());
            componentView.setRelationships(new ArrayList<>());

            Container container = containers.get(containerMnemonic);

            // Проставление параметров
            componentView.setContainerId(container.getId());
            componentView.setExternalContainerBoundariesVisible(true);
            componentView.setKey("components");
            componentView.setOrder(3);

            // Создание automaticLayout
            AutomaticLayout automaticLayout = new AutomaticLayout();
            automaticLayout.setApplied(false);
            automaticLayout.setImplementation(LayoutImplementation.Graphviz);
            automaticLayout.setEdgeSeparation(0);
            automaticLayout.setNodeSeparation(300);
            automaticLayout.setRankDirection(RankDirection.TopBottom);
            automaticLayout.setRankSeparation(300);
            automaticLayout.setVertices(false);

            componentView.setAutomaticLayout(automaticLayout);

            // Проставление Elements и Relationships
            Set<String> objects = new HashSet<>();
            objects.add(container.getId());
            Set<String> comps = new HashSet<>();
            for (Component component : container.getComponents()) {
                ElementView comp = new ElementView();
                comp.setId(component.getId());
                comp.setX(0);
                comp.setY(0);
                if (!objects.contains(component.getId())) {
                    componentView.getElements().add(comp);
                    objects.add(component.getId());
                }
                comps.add(component.getId());
                for (Relationship relationship : component.getRelationships()) {
                    RelationshipView rel = new RelationshipView();
                    rel.setId(relationship.getId());
                    componentView.getRelationships().add(rel);
                    if (!objects.contains(relationship.getDestinationId())) {
                        ElementView tmp = new ElementView();
                        tmp.setId(relationship.getDestinationId());
                        tmp.setX(0);
                        tmp.setY(0);
                        componentView.getElements().add(tmp);
                        objects.add(relationship.getDestinationId());
                    }
                }
            }

            for (Map.Entry<String, SoftwareSystem> entry1 : systems.entrySet()) {
                SoftwareSystem system1 = entry1.getValue();
                for (Relationship relationship : system1.getRelationships()) {
                    if (comps.contains(relationship.getDestinationId())) {
                        RelationshipView rel = new RelationshipView();
                        rel.setId(relationship.getId());
                        componentView.getRelationships().add(rel);
                        if (!objects.contains(relationship.getSourceId())) {
                            ElementView comp = new ElementView();
                            comp.setId(relationship.getSourceId());
                            comp.setX(0);
                            comp.setY(0);
                            componentView.getElements().add(comp);
                            objects.add(relationship.getSourceId());
                        }
                    }
                }
            }

            componentViews.add(componentView);
            workspace.getViews().setComponentViews(componentViews);
        } else {
            // Заполнение deploymentViews
            List<DeploymentView> deploymentViews = new ArrayList<>();
            DeploymentView deploymentView = new DeploymentView();
            deploymentView.setElements(new ArrayList<>());
            deploymentView.setRelationships(new ArrayList<>());

            // Проставление параметров
            deploymentView.setSoftwareSystemId(system.getId());
            deploymentView.setTitle("Диаграмма развёртывания");
            deploymentView.setEnvironment(environment);
            deploymentView.setKey(environment + "-01");
            deploymentView.setOrder(4);

            // Создание automaticLayout
            AutomaticLayout automaticLayout = new AutomaticLayout();
            automaticLayout.setApplied(false);
            automaticLayout.setEdgeSeparation(0);
            automaticLayout.setImplementation(LayoutImplementation.Graphviz);
            automaticLayout.setNodeSeparation(300);
            automaticLayout.setRankDirection(RankDirection.TopBottom);
            automaticLayout.setRankSeparation(300);
            automaticLayout.setVertices(false);
            deploymentView.setAutomaticLayout(automaticLayout);

            // Проставление Elements и Relationships
            Set<String> objects = new HashSet<>();
            for (DeploymentNode deploymentNode : model.getDeploymentNodes()) {
                getDeploymentElements(deploymentNode, deploymentView, objects);
            }

            deploymentViews.add(deploymentView);
            workspace.getViews().setDeploymentViews(deploymentViews);
        }

        return workspace;
    }
}
