package ru.beeline.architecting_graph.service.createDiagrams;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.config.RestConfig;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.repository.neo4j.BuildGraphQuery;
import ru.beeline.architecting_graph.repository.neo4j.CreateDiagramsQuery;
import ru.beeline.architecting_graph.service.graph.FunctionsForWorkingWithJson;
import ru.beeline.architecting_graph.service.graph.GraphConstruction;

@Service
public class CreateDiagrams {

    @Autowired
    CreateDiagramsQuery createDiagramsQuery;

    @Autowired
    GetObjects getObjects;

    @Autowired
    BuildGraphQuery buildGraphQuery;

    public Boolean checkifContainerExists(Session session, String softwareSystemMnemonic,
                                          String containerMnemonic) {
        Result result = createDiagramsQuery.checkIfContainerExists(session, softwareSystemMnemonic, containerMnemonic);
        return result.hasNext();
    }

    public Boolean checkIfEnvironmentExists(Session session, String environment) {
        Result result = createDiagramsQuery.checkIfEnvironmentExists(session, environment);
        return result.hasNext();
    }

    public  ResponseEntity<String> createDiagramm(RestConfig autorization, String softwareSystemMnemonic,
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
        boolean exists = buildGraphQuery.checkIfObjectExists(session, "Global", systemGraphObject);
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
                                getObjects.GetWorkspace(softwareSystemMnemonic, containerMnemonic, environment,
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
