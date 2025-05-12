package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.Session;
import org.neo4j.driver.Driver;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.beeline.architecting_graph.model.Workspace;
import ru.beeline.architecting_graph.config.RestConfig;

public class GraphConstruction {

    public static Session connectToDatabase(Driver driver, RestConfig autorization) throws ServiceUnavailableException {
        Session session = driver.session();
        String query = "MATCH (n) RETURN n";
        session.run(query);
        return session;
    }

    public static ResponseEntity<String> getWorkspaceJsonExceptions(Exception e) {
        if (e.getClass() == HttpClientErrorException.class) {
            HttpClientErrorException httpException = (HttpClientErrorException) e;
            HttpStatusCode statusCode = httpException.getStatusCode();
            if (statusCode.value() == 404) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Документ не найден");
            } else if (statusCode.value() == 400) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Полученный workspace не валиден");
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Доступ запрещен ");
            }
        } else if (e.getClass() == HttpServerErrorException.class) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Ошибка при загрузке документа");
        } else {
            return ResponseEntity.status(520).body("Неизвестная ошибка" + '\n' + e.getMessage());
        }
    }

    public static ResponseEntity<String> graphConstruct(Long docId, RestConfig autorization, String GraphTag) {

        Driver driver = GraphDatabase.driver(autorization.getUri(),
                AuthTokens.basic(autorization.getUser(), autorization.getPassword()));

        Session session;

        try {
            session = connectToDatabase(driver, autorization);
        } catch (ServiceUnavailableException e) {
            return ResponseEntity.badRequest().body("Нет подключения к БД");
        }

        String workspaceJson = null;

        try {
            workspaceJson = FunctionsForWorkingWithJson.getJson(docId, autorization);
        } catch (Exception e) {
            getWorkspaceJsonExceptions(e);
        }

        Workspace workspace;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            workspace = WorkspaceFunctions.getWorkspace(workspaceJson, objectMapper);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Полученный workspace не валиден");
        }

        try {
            GraphFunctions.createGraph(session, GraphTag, workspace);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Граф не построен");
        }

        driver.close();
        return ResponseEntity.status(HttpStatus.CREATED).body("Граф построен");
    }
}
