package ru.beeline.architecting_graph.service.createDiagrams;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.beeline.architecting_graph.config.RestConfig;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.service.graph.CommonFunctions;
import ru.beeline.architecting_graph.service.graph.FunctionsForWorkingWithJson;
import ru.beeline.architecting_graph.service.graph.GraphConstruction;

public class CreateDiagrams {

    public static Boolean checkifContainerExists(Session session, String softwareSystemMnemonic,
                                                 String containerMnemonic) {

        String query = "MATCH (a:SoftwareSystem {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Child]->(b:Container {graphTag: \"Global\", structurizr_dsl_identifier: $val2}) "
                + "WHERE r.graphTag = \"Global\"  RETURN EXISTS((a)-->(b)) AS relationship_exists";
        Value parameters = Values.parameters("val1", softwareSystemMnemonic, "val2", containerMnemonic);
        Result result = session.run(query, parameters);
        if (!result.hasNext()) {
            return false;
        }
        return true;
    }

    public static Boolean checkIfEnvironmentExists(Session session, String environment) {
        String query = "MATCH (n:Environment {name: $val1}) RETURN n";
        Value parameters = Values.parameters("val1", environment);
        Result result = session.run(query, parameters);
        if (!result.hasNext()) {
            return false;
        }
        return true;
    }

    public static ResponseEntity<String> createDiagramm(RestConfig autorization, String softwareSystemMnemonic,
                                                        String containerMnemonic, String environment) {

        Driver driver = GraphDatabase.driver(autorization.getUri(),
                AuthTokens.basic(autorization.getUser(), autorization.getPassword()));

        Session session;

        try {
            session = GraphConstruction.connectToDatabase(driver, autorization);
        } catch (ServiceUnavailableException e) {
            return ResponseEntity.badRequest().body("Нет подключения к БД");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        GraphObject systemGraphObject = new GraphObject("SoftwareSystem", "structurizr_dsl_identifier",
                softwareSystemMnemonic);

        boolean exists = CommonFunctions.checkIfObjectExists(session, "Global", systemGraphObject);

        if (exists) {

            if (containerMnemonic != null
                    && !checkifContainerExists(session, softwareSystemMnemonic, containerMnemonic)) {

                driver.close();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Контейнер не найден");
            }

            if (environment != null
                    && !checkIfEnvironmentExists(session, environment)) {

                driver.close();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Окружение не найдено");
            }

            try {

                String json = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(
                                GetObjects.GetWorkspace(softwareSystemMnemonic, containerMnemonic, environment,
                                        autorization.getUri(), autorization.getUser(), autorization.getPassword()));

                json = FunctionsForWorkingWithJson.changeJson(json, autorization);

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
}
