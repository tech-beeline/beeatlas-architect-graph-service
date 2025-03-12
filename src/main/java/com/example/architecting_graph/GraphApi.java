package com.example.architecting_graph;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

@RestController // Указывает, что это REST-контроллер
@RequestMapping("/api/v1") // Базовый путь для всех методов в этом контроллере
public class GraphApi {

    private final RestConfig autorization;

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
                if (!result.hasNext()) {
                    driver.close();
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Контейнер не найден");
                }
            }

            driver.close();
            try {

                // Преобразуем объект в JSON-строку
                String json = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(GetObjects.GetWorkspace(softwareSystemMnemonic, containerMnemonic,
                                autorization.getUri(), autorization.getUser(), autorization.getPassword()));
                return ResponseEntity.status(200).body(json);
            } catch (Exception e) {
                driver.close();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Ошибка при сериализации" + '\n' + e.getMessage());
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
