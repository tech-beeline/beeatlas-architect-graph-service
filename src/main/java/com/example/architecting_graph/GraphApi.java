package com.example.architecting_graph;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.neo4j.driver.exceptions.ServiceUnavailableException;

import java.util.Set;
import java.io.File;

import org.neo4j.driver.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;

@RestController // Указывает, что это REST-контроллер
@RequestMapping("/api/v1") // Базовый путь для всех методов в этом контроллере
public class GraphApi {

    private final RestConfig autorization;

    public GraphApi(RestConfig autorization) {
        this.autorization = autorization;
    }

    public static Workspace getWorkspaceFileForTest(Object jsonObject) throws Exception {
        String FilePath = "workspace_RNC.json";
        File file = new File(FilePath);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Workspace workspace = objectMapper.readValue(file, Workspace.class);
        return workspace;
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
            // workspace = getWorkspaceFileForTest(workspaceJson);
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
            return ResponseEntity.badRequest().body("Граф не построен" + '\n' + e);
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
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

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
                        .writeValueAsString(GetObjects.GetWorkspace(softwareSystemMnemonic, containerMnemonic, null,
                                autorization.getUri(), autorization.getUser(), autorization.getPassword()));
                return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(json);
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

    @GetMapping("/deployment/{environment}/{softwareSystemMnemonic}")
    public ResponseEntity<String> getDeployment(@PathVariable String environment,
            @PathVariable String softwareSystemMnemonic) {

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
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Проверка на существование системы
        if (MajorGraph.checkIfObjectExists(session, "SoftwareSystem", "structurizr_dsl_identifier",
                softwareSystemMnemonic)) {

            // Проверка на существование окружения
            String query = "MATCH (n:Environment {name: $val1}) RETURN n";
            Value parameters = Values.parameters("val1", environment);
            Result result = session.run(query, parameters);
            if (!result.hasNext()) {
                driver.close();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Окружение не найдено");
            }

            driver.close();
            try {
                // Преобразуем объект в JSON-строку
                String json = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(GetObjects.GetWorkspace(softwareSystemMnemonic, null, environment,
                                autorization.getUri(), autorization.getUser(), autorization.getPassword()));
                return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(json);
            } catch (Exception e) {
                driver.close();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Ошибка при сериализации" + '\n' + e.getMessage());
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Система не найдена");
        }
    }

    @GetMapping("/diff/{cmdb_code}/{v1}/{v2}")
    public ResponseEntity<String> compareVersions(@PathVariable String cmdb_code, @PathVariable Integer v1,
            @PathVariable(required = false) Integer v2) {

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

        // Проверка на наличие системы
        if (!MajorGraph.checkIfObjectExists(session, "SoftwareSystem", "cmdb", cmdb_code)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Система не найдена");
        }

        // Вычисление текущей версии
        String findVersion = "MATCH (n:SoftwareSystem {graph: \"Global\", cmdb: $cmdb1}) RETURN n.version AS version";
        Value parameters = Values.parameters("cmdb1", cmdb_code);
        Result result = session.run(findVersion, parameters);

        Integer cur_version = -1;

        try {
            cur_version = result.next().get("version").asInt();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("У данной системы отсутствует версионность");
        }

        if (v1 == v2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Версии не могут быть равны");
        } else if (Math.min(v1, v2) < 1 || Math.max(v1, v2) > cur_version) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Неверное значение версий");
        }

        if (v1 > v2) {
            v1 = v1 + v2;
            v2 = v1 - v2;
            v1 = v1 - v2;
        }

        Set<Pair> ans2 = FindChanges.EarlierChanges(v1, v2, cur_version, cmdb_code, session);
        Set<Pair> ans1 = FindChanges.LaterChanges(v1, v2, cur_version, cmdb_code, session);

        String out = "{\n\t\"addElements\": [\n";

        Integer cnt = 0;
        for (Pair el : ans1) {
            if (cnt != ans1.size() - 1) {
                if (el.getSecond().equals("Relationship") || el.getSecond().equals("ContainerInstance")
                        || el.getSecond().equals("SoftwareSystemInstance")) {
                    out = out + el.getFirst() + ",\n";
                } else {
                    out = out + "\t\t{\n" + "\t\t\t\"name\": \"" + el.getFirst() + "\",\n\t\t\t\"type\": \""
                            + el.getSecond() + "\"\n\t\t},\n";
                }
            } else {
                if (el.getSecond().equals("Relationship") || el.getSecond().equals("ContainerInstance")
                        || el.getSecond().equals("SoftwareSystemInstance")) {
                    out = out + el.getFirst() + "\n";
                } else {
                    out = out + "\t\t{\n" + "\t\t\t\"name\": \"" + el.getFirst() + "\",\n\t\t\t\"type\": \""
                            + el.getSecond() + "\"\n\t\t}\n";
                }
            }
            cnt = cnt + 1;
        }

        out = out + "\t],\n\t\"removeElements\": [\n";

        cnt = 0;
        for (Pair el : ans2) {
            if (cnt != ans2.size() - 1) {
                if (el.getSecond().equals("Relationship") || el.getSecond().equals("ContainerInstance")
                        || el.getSecond().equals("SoftwareSystemInstance")) {
                    out = out + el.getFirst() + ",\n";
                } else {
                    out = out + "\t\t{\n" + "\t\t\t\"name\": \"" + el.getFirst() + "\",\n\t\t\t\"type\": \""
                            + el.getSecond() + "\"\n\t\t},\n";
                }
            } else {
                if (el.getSecond().equals("Relationship") || el.getSecond().equals("ContainerInstance")
                        || el.getSecond().equals("SoftwareSystemInstance")) {
                    out = out + el.getFirst() + "\n";
                } else {
                    out = out + "\t\t{\n" + "\t\t\t\"name\": \"" + el.getFirst() + "\",\n\t\t\t\"type\": \""
                            + el.getSecond() + "\"\n\t\t}\n";
                }
            }
            cnt = cnt + 1;
        }

        out = out + "\t]\n}";

        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(out);
    }

    @GetMapping("/diff/{cmdb_code}/{v1}")
    public ResponseEntity<String> compareWithCur(@PathVariable String cmdb_code, @PathVariable Integer v1) {

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

        // Проверка на наличие системы
        if (!MajorGraph.checkIfObjectExists(session, "SoftwareSystem", "cmdb", cmdb_code)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Система не найдена");
        }

        // Вычисление текущей версии
        String findVersion = "MATCH (n:SoftwareSystem {graph: \"Global\", cmdb: $cmdb1}) RETURN n.version AS version";
        Value parameters = Values.parameters("cmdb1", cmdb_code);
        Result result = session.run(findVersion, parameters);

        Integer v2 = -1;

        try {
            v2 = result.next().get("version").asInt();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("У данной системы отсутствует версионность");
        }

        if (v2 <= v1 || v1 < 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Неверное значение версии");
        }

        driver.close();
        return compareVersions(cmdb_code, v1, v2);
    }
}
