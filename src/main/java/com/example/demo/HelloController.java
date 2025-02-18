package com.example.demo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.*;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

@RestController // Указывает, что это REST-контроллер
@RequestMapping("/api/v1") // Базовый путь для всех методов в этом контроллере
public class HelloController {

    private final Neo4jConfig autorization;

    public HelloController(Neo4jConfig autorization) {
        this.autorization = autorization;
    }

    public static void saveJsonToFile(Object jsonObject, String fileName) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(fileName);

        // Записываем JSON в файл с красивым форматированием
        try (FileWriter fileWriter = new FileWriter(file)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(fileWriter, jsonObject);
        }
    }

    public static void getFile(Long id) throws IOException {

        // Создаем RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // URL вашего API
        String url = "https://document-service-dev-eafdmmart.apps.yd-m6-kt22.vimpelcom.ru/api/v1/documents/"
                + Long.toString(id);

        // Выполняем GET-запрос без заголовков
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        // Получаем тело ответа как массив байтов
        byte[] responseBody = response.getBody();

        // Преобразуем байты в строку (используем UTF-8)
        String jsonString = new String(responseBody, "UTF-8");

        // Парсим JSON с помощью Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        Object jsonObject = objectMapper.readValue(jsonString, Object.class);

        // Сохраняем JSON в файл
        saveJsonToFile(jsonObject, "output.json");
    }

    public static Workspace getWorkspace(File file) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper.readValue(file, Workspace.class);
    }

    @PostMapping("/graph/local/{docId}") // Локальный граф
    public String sayHello1(@PathVariable("docId") Long id) {

        // Проверка подключения к БД
        Driver driver = GraphDatabase.driver(autorization.getUri(),
                AuthTokens.basic(autorization.getUser(), autorization.getPassword()));
        try (Session session = driver.session()) {

        } catch (ServiceUnavailableException e) {
            // Обработка исключения
            return "400 нет подключения к БД";
        } finally {
            driver.close();
        }

        try {
            // Получаем файл
            getFile(id);
        } catch (HttpClientErrorException e) {
            // Обработка ошибок 4xx (клиентские ошибки)
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.value() == 404) {
                return "404 документ не найден";
            } else if (statusCode.value() == 400) {
                return "400";
            } else {
                return "Client error: " + statusCode;
            }
        } catch (HttpServerErrorException e) {
            // Обработка ошибок 5xx (серверные ошибки)
            return "503";
        } catch (Exception e) {
            // Обработка других исключений (например, сетевых ошибок)
            return "An error occurred: " + e.getMessage();
        }

        // Загрузка workspace
        String FilePath = "output.json";
        File file = new File(FilePath);

        Workspace workspace;

        try {
            // Загрузка workspace
            workspace = getWorkspace(file);
        } catch (IOException e) {
            // Обработка исключения
            return "400 Воркспейс не валиден";
        }

        try {
            // Построение графа
            MinorGraph.createGraph(workspace, autorization.getUri(), autorization.getUser(),
                    autorization.getPassword());
        } catch (IOException e) {
            // Обработка исключения
            return "400 Граф не построен";
        }

        return "201 Граф построен";
    }

    @PostMapping("/graph/{docId}") // Глобальный граф
    public String sayHello2(@PathVariable("docId") Long id) {

        // Проверка подключения к БД
        Driver driver = GraphDatabase.driver(autorization.getUri(),
                AuthTokens.basic(autorization.getUser(), autorization.getPassword()));
        try (Session session = driver.session()) {

        } catch (ServiceUnavailableException e) {
            // Обработка исключения
            return "400 нет подключения к БД";
        } finally {
            driver.close();
        }

        try {
            // Получаем файл
            getFile(id);
        } catch (HttpClientErrorException e) {
            // Обработка ошибок 4xx (клиентские ошибки)
            HttpStatusCode statusCode = e.getStatusCode();
            if (statusCode.value() == 404) {
                return "404 документ не найден";
            } else if (statusCode.value() == 400) {
                return "400";
            } else {
                return "Client error: " + statusCode;
            }
        } catch (HttpServerErrorException e) {
            // Обработка ошибок 5xx (серверные ошибки)
            return "503";
        } catch (Exception e) {
            // Обработка других исключений (например, сетевых ошибок)
            return "An error occurred: " + e.getMessage();
        }

        // Загрузка workspace
        String FilePath = "output.json";
        File file = new File(FilePath);

        Workspace workspace;

        try {
            // Загрузка workspace
            workspace = getWorkspace(file);
        } catch (IOException e) {
            // Обработка исключения
            return "400 Воркспейс не валиден";
        }

        try {
            // Построение графа
            MajorGraph.createGraph(workspace, autorization.getUri(), autorization.getUser(),
                    autorization.getPassword());
        } catch (IOException e) {
            // Обработка исключения
            return "400 Граф не построен";
        }

        return "201 Граф построен";
    }
}