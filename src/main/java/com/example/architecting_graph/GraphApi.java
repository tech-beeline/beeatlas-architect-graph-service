package com.example.architecting_graph;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController // Указывает, что это REST-контроллер
@RequestMapping("/api/v1") // Базовый путь для всех методов в этом контроллере
public class GraphApi {

    private final RestConfig autorization;
    private static Long id_obj;
    private static Map<String, Long> map_id = new HashMap<>();
    private static Map<String, SoftwareSystem> systems = new HashMap<>();
    private static Map<String, Container> containers = new HashMap<>();
    private static Map<String, Component> components = new HashMap<>();
    private static Map<String, DeploymentNode> deploymentNodes = new HashMap<>();
    private static Map<String, InfrastructureNode> infrastructureNodes = new HashMap<>();

    public GraphApi(RestConfig autorization) {
        this.autorization = autorization;
    }

    public Object getJson(Long id_file) throws Exception {

        // Создаем RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // URL вашего API
        String url = autorization.getUrl() + "/api/v1/documents/" + Long.toString(id_file);

        // Выполняем GET-запрос без заголовков
        ResponseEntity<byte[]> response;
        try {
            // Выполняем GET-запрос без заголовков
            response = restTemplate.getForEntity(url, byte[].class);
        } catch (Exception e) {
            throw e;
        }

        try {
            // Получаем тело ответа как массив байтов
            byte[] responseBody = response.getBody();

            // Преобразуем байты в строку (используем UTF-8)
            String jsonString = new String(responseBody, "UTF-8");

            // Парсим JSON с помощью Jackson
            ObjectMapper objectMapper = new ObjectMapper();
            Object jsonObject = objectMapper.readValue(jsonString, Object.class);

            return jsonObject;
        } catch (Exception e) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        }
    }

    public static Workspace getWorkspace(Object jsonObject) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Workspace workspace;
        try {
            workspace = objectMapper.convertValue(jsonObject, Workspace.class);
        } catch (Exception e) {
            throw e;
        }
        return workspace;
    }

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

    public static void getContainer(Node node, Session session) {
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

    @PostMapping("/graph/local/{docId}") // Локальный граф
    public ResponseEntity<String> LocalGraph(@PathVariable("docId") Long id_file) {

        // Проверка подключения к БД
        Driver driver = GraphDatabase.driver(autorization.getUri(),
                AuthTokens.basic(autorization.getUser(), autorization.getPassword()));
        try {
            Session session = driver.session();
            String query = "MATCH (n) RETURN n";
            session.run(query);
        } catch (ServiceUnavailableException e) {
            // Возвращаем 400 Bad Request с сообщением
            return ResponseEntity.badRequest().body("Нет подключения к БД");
        } finally {
            driver.close();
        }

        Object workspaceJson = null;

        try {
            // Получаем файл
            workspaceJson = getJson(id_file);
        } catch (HttpClientErrorException e) {
            // Обработка ошибок 4xx (клиентские ошибки)
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.value() == 404) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Документ не найден");
            } else if (statusCode.value() == 400) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Полученный workspace не валиден");
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Доступ запрещен ");
            }
        } catch (HttpServerErrorException e) {
            // Обработка ошибок 5xx (серверные ошибки)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Ошибка при загрузке документа");
        } catch (Exception e) {
            // Обработка других исключений (например, сетевых ошибок)
            return ResponseEntity.status(520)
                    .body("Неизвестная ошибка" + '\n' + e.getMessage());
        }

        Workspace workspace;

        try {
            // Загрузка workspace
            workspace = getWorkspace(workspaceJson);
        } catch (Exception e) {
            // Обработка исключения
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Полученный workspace не валиден");
        }

        try {
            // Построение графа
            MinorGraph.createGraph(workspace, autorization.getUri(), autorization.getUser(),
                    autorization.getPassword());
        } catch (Exception e) {
            // Обработка исключения
            return ResponseEntity.badRequest().body("Граф не построен");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("Граф построен");
    }

    @PostMapping("/graph/{docId}") // Глобальный граф
    public ResponseEntity<String> GlobalGraph(@PathVariable("docId") Long id_file) {

        // Проверка подключения к БД
        Driver driver = GraphDatabase.driver(autorization.getUri(),
                AuthTokens.basic(autorization.getUser(), autorization.getPassword()));
        try {
            Session session = driver.session();
            String query = "MATCH (n) RETURN n";
            session.run(query);
        } catch (ServiceUnavailableException e) {
            // Возвращаем 400 Bad Request с сообщением
            return ResponseEntity.badRequest().body("Нет подключения к БД");
        } finally {
            driver.close();
        }

        Object workspaceJson = null;

        try {
            // Получаем файл
            workspaceJson = getJson(id_file);
        } catch (HttpClientErrorException e) {
            // Обработка ошибок 4xx (клиентские ошибки)
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.value() == 404) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Документ не найден");
            } else if (statusCode.value() == 400) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Полученный workspace не валиден");
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Доступ запрещен ");
            }
        } catch (HttpServerErrorException e) {
            // Обработка ошибок 5xx (серверные ошибки)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Ошибка при загрузке документа");
        } catch (Exception e) {
            // Обработка других исключений (например, сетевых ошибок)
            return ResponseEntity.status(520)
                    .body("Неизвестная ошибка" + '\n' + e.getMessage());
        }

        Workspace workspace;

        try {
            // Загрузка workspace
            workspace = getWorkspace(workspaceJson);
        } catch (Exception e) {
            // Обработка исключения
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Полученный workspace не валиден");
        }

        try {
            // Построение графа
            MajorGraph.createGraph(workspace, autorization.getUri(), autorization.getUser(),
                    autorization.getPassword());
        } catch (Exception e) {
            // Обработка исключения
            return ResponseEntity.badRequest().body("Граф не построен");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("Граф построен");
    }

    @GetMapping("/context/{softwareSystemMnemonic}/{containerMnemonic}")
    public ResponseEntity<String> getObject(@PathVariable String softwareSystemMnemonic,
            @PathVariable(required = false) String containerMnemonic) {

        // Проверка подключения к БД
        Driver driver = GraphDatabase.driver(autorization.getUri(),
                AuthTokens.basic(autorization.getUser(), autorization.getPassword()));

        Session session;

        try {
            session = driver.session();
            String query = "MATCH (n) RETURN n";
            session.run(query);
        } catch (ServiceUnavailableException e) {
            // Возвращаем 400 Bad Request с сообщением
            return ResponseEntity.badRequest().body("Нет подключения к БД");
        }

        // Возвращаем воркспейс
        id_obj = 2L;
        Workspace workspace = new Workspace();
        Model model = new Model();
        model.setSoftwareSystems(new ArrayList<>());
        model.setDeploymentNodes(new ArrayList<>());
        workspace.setId(1L);

        ObjectMapper objectMapper = new ObjectMapper();
        // objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Проверка на существование системы
        if (MajorGraph.checkIfObjectExists(session, "SoftwareSystem", "structurizr_dsl_identifier",
                softwareSystemMnemonic)) {

            if (containerMnemonic != null) {

                // Проверка на существование контейнера
                String query = "MATCH (a:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(b:Container {graph: \"Global\", structurizr_dsl_identifier: $val2}) WHERE r.graph = \"Global\"  RETURN EXISTS((a)-->(b)) AS relationship_exists";
                Value parameters = Values.parameters("val1", softwareSystemMnemonic, "val2", containerMnemonic);
                Result result = session.run(query, parameters);
                if (result.hasNext()) {

                    return ResponseEntity.status(200).body("Контейнер");
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Контейнер не найден");
                }
            } else {

                String query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n";
                Value parameters = Values.parameters("val1", softwareSystemMnemonic);
                Result result = session.run(query, parameters);
                org.neo4j.driver.Record record = result.next();

                // Добавление системы
                SoftwareSystem system = getSystem(record.get("n").asNode());

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
                query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:DeploymentNode) RETURN m";
                parameters = Values.parameters("val1", softwareSystemMnemonic);
                result = session.run(query, parameters);

                while (result.hasNext()) {
                    record = result.next();
                    getDeploymentNode(record.get("m").asNode(), session);
                }

                // Добавление прямых связей
                query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Relationship]->(m) RETURN r, m, m.structurizr_dsl_identifier";
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
                query = "MATCH (n:SoftwareSystem {graph: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m:DeploymentNode) RETURN m.structurizr_dsl_identifier";
                parameters = Values.parameters("val1", softwareSystemMnemonic);
                result = session.run(query, parameters);

                while (result.hasNext()) {
                    record = result.next();
                    model.getDeploymentNodes().add(getDeploymentNodeRelations(
                            deploymentNodes.get(record.get("m.structurizr_dsl_identifier").asString()), session));
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

                try {
                    // Преобразуем объект в JSON-строку
                    String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(workspace);
                    return ResponseEntity.status(200).body(json);

                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Ошибка при сериализации" + '\n' + e.getMessage());
                }
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Система не найдена");
        }
    }

    @GetMapping("/context/{softwareSystemMnemonic}")
    public ResponseEntity<String> getSystem(@PathVariable String softwareSystemMnemonic) {

        return getObject(softwareSystemMnemonic, null);
    }
}
