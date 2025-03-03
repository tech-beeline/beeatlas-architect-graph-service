package com.example.architecting_graph;

import org.springframework.web.bind.annotation.PostMapping;
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

    public Object getJson(Long id) throws Exception {

        // Создаем RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // URL вашего API
        String url = autorization.getUrl() + "/api/v1/documents/" + Long.toString(id);

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
    public ResponseEntity<String> sayHello1(@PathVariable("docId") Long id) {

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
            workspaceJson = getJson(id);
        } catch (HttpClientErrorException e) {
            // Обработка ошибок 4xx (клиентские ошибки)
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.value() == 404) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Документ не найден");
            } else if (statusCode.value() == 400) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Ошибка при получении документа");
            }
        } catch (HttpServerErrorException e) {
            // Обработка ошибок 5xx (серверные ошибки)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("");
        } catch (Exception e) {
            // Обработка других исключений (например, сетевых ошибок)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Неизвестная ошибка" + '\n' + e.getMessage());
        }

        Workspace workspace;

        try {
            // Загрузка workspace
            workspace = getWorkspace(workspaceJson);
        } catch (Exception e) {
            // Обработка исключения
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
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
    public ResponseEntity<String> sayHello2(@PathVariable("docId") Long id) {

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
            workspaceJson = getJson(id);
        } catch (HttpClientErrorException e) {
            // Обработка ошибок 4xx (клиентские ошибки)
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.value() == 404) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Документ не найден");
            } else if (statusCode.value() == 400) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Ошибка при получении документа");
            }
        } catch (HttpServerErrorException e) {
            // Обработка ошибок 5xx (серверные ошибки)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("");
        } catch (Exception e) {
            // Обработка других исключений (например, сетевых ошибок)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Неизвестная ошибка" + '\n' + e.getMessage());
        }

        Workspace workspace;

        try {
            // Загрузка workspace
            workspace = getWorkspace(workspaceJson);
        } catch (Exception e) {
            // Обработка исключения
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
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
}
