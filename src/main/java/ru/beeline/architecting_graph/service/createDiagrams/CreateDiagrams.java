package ru.beeline.architecting_graph.service.createDiagrams;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.client.StructurizrClient;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.model.Workspace;
import ru.beeline.architecting_graph.repository.neo4j.ContainerRepository;
import ru.beeline.architecting_graph.repository.neo4j.EnvironmentRepository;
import ru.beeline.architecting_graph.repository.neo4j.GenericRepository;
import ru.beeline.architecting_graph.service.graph.Neo4jSessionManager;


@Service
public class CreateDiagrams {

    @Autowired
    GetView getView;

    @Autowired
    Neo4jSessionManager neo4jSessionManager;

    @Autowired
    GenericRepository genericRepository;

    @Autowired
    ContainerRepository containerRepository;

    @Autowired
    EnvironmentRepository environmentRepository;

    @Autowired
    StructurizrClient structurizrClient;

    public Boolean checkifContainerExists(String softwareSystemMnemonic, String containerMnemonic) {
        Result result = containerRepository.checkIfContainerExists(softwareSystemMnemonic, containerMnemonic);
        return result.hasNext();
    }

    public Boolean checkIfEnvironmentExists(String environment) {
        Result result = environmentRepository.checkIfEnvironmentExists(environment);
        return result.hasNext();
    }

    public ResponseEntity<String> createDiagramm(String softwareSystemMnemonic,
                                                 String containerMnemonic,
                                                 String environment,
                                                 String rankDirection) {
        if (rankDirection == null) {
            rankDirection = "LeftRight";
        }
        if (!rankDirection.equals("TopBottom") && !rankDirection.equals("LeftRight")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Недопустимая ориентация диаграммы");
        }
        neo4jSessionManager.getSession().run("RETURN 1");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        GraphObject systemGraphObject = new GraphObject("SoftwareSystem",
                                                        "structurizr_dsl_identifier",
                                                        softwareSystemMnemonic);
        boolean exists = genericRepository.checkIfObjectExists("Global", systemGraphObject);
        if (!exists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Система не найдена");
        }
        Workspace workspace;
        try {
            if (containerMnemonic == null && environment == null) {
                workspace = getView.GetContextView(softwareSystemMnemonic, rankDirection);
            } else if (containerMnemonic != null) {
                if (!checkifContainerExists(softwareSystemMnemonic, containerMnemonic)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Контейнер не найден");
                }
                workspace = getView.GetComponentView(softwareSystemMnemonic, containerMnemonic, rankDirection);
            } else {
                if (!checkIfEnvironmentExists(environment)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Окружение не найдено");
                }
                workspace = getView.GetDeploymentView(softwareSystemMnemonic, environment, rankDirection);
            }
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(workspace);
            json = structurizrClient.changeJson(json);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ошибка при сериализации\n" + e.getMessage());
        }
    }
}
