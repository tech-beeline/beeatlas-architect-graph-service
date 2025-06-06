package ru.beeline.architecting_graph.service.getElements;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ru.beeline.architecting_graph.config.RestConfig;
import ru.beeline.architecting_graph.service.graph.GraphConstruction;

public class getElements {

    private static Object convertValue(Value value) {
        switch (value.type().name()) {
            case "NODE":
                Map<String, Object> nodeMap = new HashMap<>();
                nodeMap.put("labels", value.asNode().labels());
                nodeMap.put("properties", value.asNode().asMap());
                return nodeMap;

            case "RELATIONSHIP":
                Map<String, Object> relMap = new HashMap<>();
                relMap.put("type", value.asRelationship().type());
                relMap.put("properties", value.asRelationship().asMap());
                return relMap;

            default:
                return value.asObject();
        }
    }

    public static ResponseEntity<String> processingQuery(String query, RestConfig autorization) {

        Driver driver = GraphDatabase.driver(autorization.getUri(),
                AuthTokens.basic(autorization.getUser(), autorization.getPassword()));

        Session session;
        try {
            session = GraphConstruction.connectToDatabase(driver, autorization);
        } catch (ServiceUnavailableException e) {
            return ResponseEntity.badRequest().body("Нет подключения к БД");
        }

        Result result = session.run(query);
        ObjectMapper mapper = new ObjectMapper();
        String returnElements = "[";
        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Map<String, Object> row = new HashMap<>();

            record.fields().forEach(field -> {
                String key = field.key();
                Value value = field.value();

                Object convertedValue = convertValue(value);
                row.put(key, convertedValue);
            });

            try {
                String jsonRow = mapper.writeValueAsString(row);
                returnElements = returnElements + jsonRow + (result.hasNext() ? "," : "");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        returnElements = returnElements + "]";

        return ResponseEntity.status(200).contentType(MediaType.APPLICATION_JSON).body(returnElements);
    }
}
