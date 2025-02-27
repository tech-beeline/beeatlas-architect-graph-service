package com.example.architecting_graph;

import java.io.IOException;

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

    public Object getJson(Long id) throws IOException {

        // Создаем RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // URL вашего API
        String url = autorization.getUrl() + "/api/v1/documents/" + Long.toString(id);

        // Выполняем GET-запрос без заголовков
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        // Получаем тело ответа как массив байтов
        byte[] responseBody = response.getBody();

        // Преобразуем байты в строку (используем UTF-8)
        String jsonString = new String(responseBody, "UTF-8");

        // Парсим JSON с помощью Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        Object jsonObject = objectMapper.readValue(jsonString, Object.class);

        return jsonObject;
    }

    public static Workspace getWorkspace(Object jsonObject) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.convertValue(jsonObject, Workspace.class);
    }

    @PostMapping("/graph/local/{docId}") // Локальный граф
    public ResponseEntity<String> sayHello1(@PathVariable("docId") Long id) {

        // Проверка подключения к БД
        Driver driver = GraphDatabase.driver(autorization.getUri(),
                AuthTokens.basic(autorization.getUser(), autorization.getPassword()));
        try {
            driver.session();
        } catch (ServiceUnavailableException e) {
            // Возвращаем 400 Bad Request с сообщением
            return ResponseEntity.badRequest().body("400 нет подключения к БД\n" + e.getMessage());
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
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("404 документ не найден\n" + e.getMessage());
            } else if (statusCode.value() == 400) {
                return ResponseEntity.badRequest().body("400 ошибка при получении документа\n" + e.getMessage());
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Ошибка при получении документа\n" + e.getMessage());
            }
        } catch (HttpServerErrorException e) {
            // Обработка ошибок 5xx (серверные ошибки)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("503 сервис документов не доступен\n" + e.getMessage());
        } catch (Exception e) {
            // Обработка других исключений (например, сетевых ошибок)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Неизвестная ошибка\n" + e.getMessage());
        }

        Workspace workspace;

        try {
            // Загрузка workspace
            workspace = getWorkspace(workspaceJson);
        } catch (IOException e) {
            // Обработка исключения
            return ResponseEntity.badRequest().body("400 воркспейс не валиден\n" + e.getMessage());
        }

        try {
            // Построение графа
            MinorGraph.createGraph(workspace, autorization.getUri(), autorization.getUser(),
                    autorization.getPassword());
        } catch (IOException e) {
            // Обработка исключения
            return ResponseEntity.badRequest().body("400 граф не построен\n" + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("Граф построен");
    }

    @PostMapping("/graph/{docId}") // Глобальный граф
    public ResponseEntity<String> sayHello2(@PathVariable("docId") Long id) {

        // Проверка подключения к БД
        Driver driver = GraphDatabase.driver(autorization.getUri(),
                AuthTokens.basic(autorization.getUser(), autorization.getPassword()));
        try {
            driver.session();
        } catch (ServiceUnavailableException e) {
            // Возвращаем 400 Bad Request с сообщением
            return ResponseEntity.badRequest().body("400 нет подключения к БД\n" + e.getMessage());
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
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("404 документ не найден\n" + e.getMessage());
            } else if (statusCode.value() == 400) {
                return ResponseEntity.badRequest().body("400 ошибка при получении документа\n" + e.getMessage());
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Ошибка при получении документа\n" + e.getMessage());
            }
        } catch (HttpServerErrorException e) {
            // Обработка ошибок 5xx (серверные ошибки)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("503 сервис документов не доступен\n" + e.getMessage());
        } catch (Exception e) {
            // Обработка других исключений (например, сетевых ошибок)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Неизвестная ошибка\n" + e.getMessage());
        }

        Workspace workspace;

        try {
            // Загрузка workspace
            workspace = getWorkspace(workspaceJson);
        } catch (IOException e) {
            // Обработка исключения
            return ResponseEntity.badRequest().body("400 воркспейс не валиден\n" + e.getMessage());
        }

        try {
            // Построение графа
            MajorGraph.createGraph(workspace, autorization.getUri(), autorization.getUser(),
                    autorization.getPassword());
        } catch (IOException e) {
            // Обработка исключения
            return ResponseEntity.badRequest().body("400 граф не построен\n" + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("Граф построен");
    }
}
