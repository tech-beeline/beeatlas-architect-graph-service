package ru.beeline.architecting_graph.service.createDiagrams;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.client.StructurizrClient;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.model.Workspace;
import ru.beeline.architecting_graph.repository.neo4j.GenericRepository;
import ru.beeline.architecting_graph.repository.neo4j.ContainerRepository;
import ru.beeline.architecting_graph.repository.neo4j.EnvironmentRepository;


@Service
public class CreateDiagrams {

    @Autowired
    GetView getView;

    @Autowired
    GenericRepository genericRepository;

    @Autowired
    ContainerRepository containerRepository;

    @Autowired
    EnvironmentRepository environmentRepository;

    @Autowired
    StructurizrClient structurizrClient;

    @Autowired
    private Driver driver;

    public Boolean checkifContainerExists(Session session, String softwareSystemMnemonic, String containerMnemonic) {
        Result result = containerRepository.checkIfContainerExists(session, softwareSystemMnemonic, containerMnemonic);
        return result.hasNext();
    }

    public Boolean checkIfEnvironmentExists(Session session, String environment) {
        Result result = environmentRepository.checkIfEnvironmentExists(session, environment);
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
        try (Session session = driver.session()) {
            session.run("RETURN 1");
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            GraphObject systemGraphObject = new GraphObject("SoftwareSystem",
                                                            "structurizr_dsl_identifier",
                                                            softwareSystemMnemonic);
            boolean exists = genericRepository.checkIfObjectExists(session, "Global", systemGraphObject);
            if (!exists) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Система не найдена");
            }
            Workspace workspace;
            try {
                if (containerMnemonic == null && environment == null) {
                    workspace = getView.GetContextView(session, softwareSystemMnemonic, rankDirection);
                } else if (containerMnemonic != null) {
                    if (!checkifContainerExists(session, softwareSystemMnemonic, containerMnemonic)) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Контейнер не найден");
                    }
                    workspace = getView.GetComponentView(session,
                                                         softwareSystemMnemonic,
                                                         containerMnemonic,
                                                         rankDirection);
                } else {
                    if (!checkIfEnvironmentExists(session, environment)) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Окружение не найдено");
                    }
                    workspace = getView.GetDeploymentView(session, softwareSystemMnemonic, environment, rankDirection);
                }
                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(workspace);
                json = structurizrClient.changeJson(json);
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ошибка при сериализации\n" + e.getMessage());
            }
        } catch (ServiceUnavailableException e) {
            return ResponseEntity.badRequest().body("Нет подключения к БД");
        }
    }
}
