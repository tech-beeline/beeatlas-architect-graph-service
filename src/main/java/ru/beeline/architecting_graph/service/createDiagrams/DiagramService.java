package ru.beeline.architecting_graph.service.createDiagrams;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Result;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.client.StructurizrClient;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.model.Workspace;
import ru.beeline.architecting_graph.repository.neo4j.*;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class DiagramService {

    @Autowired
    ViewService viewService;

    @Autowired
    GenericRepository genericRepository;

    @Autowired
    SoftwareSystemRepository softwareSystemRepository;

    @Autowired
    ContainerRepository containerRepository;

    @Autowired
    EnvironmentRepository environmentRepository;

    @Autowired
    RelationshipRepository relationshipRepository;

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

    public ResponseEntity<String> createDiagram(String softwareSystemMnemonic,
                                                String containerMnemonic,
                                                String environment,
                                                String rankDirection) {
        if (rankDirection == null) {
            rankDirection = "LeftRight";
        }
        if (!rankDirection.equals("TopBottom") && !rankDirection.equals("LeftRight")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Недопустимая ориентация диаграммы");
        }
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
                workspace = viewService.GetContextView(softwareSystemMnemonic, rankDirection);
            } else if (containerMnemonic != null) {
                if (!checkifContainerExists(softwareSystemMnemonic, containerMnemonic)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Контейнер не найден");
                }
                workspace = viewService.GetComponentView(softwareSystemMnemonic, containerMnemonic, rankDirection);
            } else {
                if (!checkIfEnvironmentExists(environment)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Окружение не найдено");
                }
                workspace = viewService.GetDeploymentView(softwareSystemMnemonic, environment, rankDirection);
            }
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(workspace);
            json = structurizrClient.changeJson(json);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ошибка при сериализации\n" + e.getMessage());
        }
    }

    public ResponseEntity<String> createContextDiagramV2(String cmdb,
                                                         String rankDirection,
                                                         String communicationDirection) {
        if (rankDirection == null) {
            rankDirection = "LeftRight";
        }
        if (!rankDirection.equals("TopBottom") && !rankDirection.equals("LeftRight")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Недопустимая ориентация диаграммы");
        }
        if (!communicationDirection.equals("in") && !communicationDirection.equals("out")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Недопустимая ориентация диаграммы");
        }
        Result ssResult = softwareSystemRepository.getSoftwareSystem(cmdb);

        if (!ssResult.hasNext()) {
            throw new RuntimeException("SoftwareSystem не найден");
        }

        Node softwareSystemNode = ssResult.next().get("softwareSystem").asNode();
        Long softwareSystemId = softwareSystemNode.id();
        String softwareSystemName = softwareSystemNode.get("cmdb").asString();

        List<Long> allNodeIds = softwareSystemRepository.getContainerAndComponentChildIds(cmdb);
        allNodeIds.add(softwareSystemId);

        List<Map<String, Object>> relationships = "in".equalsIgnoreCase(communicationDirection) ?
                relationshipRepository.getIncomingRelationships(allNodeIds,softwareSystemId) :
                relationshipRepository.getOutgoingRelationships(allNodeIds, softwareSystemId);
        Set<Long> relIds = relationships.stream()
                .map(rel -> Long.parseLong((String) rel.get("in".equalsIgnoreCase(communicationDirection) ?
                                                                    "sourceId" : "destinationId")))
                .collect(Collectors.toSet());

        Set<Map<String, Object>> softwareSystems = softwareSystemRepository.getSoftwareSystemsFromRelationships(new ArrayList<>(relIds));

        Map<Long, Map<String, String>> containerToParentSS = softwareSystemRepository.findParentSoftwareSystemsByContainers(relIds);
        Map<Long, Map<String, String>> componentToParentSS = softwareSystemRepository.getParentSoftwareSystemsForComponents(relIds);

        Set<String> existingNames = softwareSystems.stream().map(ss -> ss.get("name").toString()).collect(Collectors.toSet());

        for (Map<String, Object> rel : relationships) {
            Long relId = Long.parseLong((String) rel.get("in".equalsIgnoreCase(communicationDirection) ?
                                                                       "sourceId" : "destinationId"));

            Map<String, String> parentSS = null;
            if (containerToParentSS.containsKey(relId)) {
                parentSS = containerToParentSS.get(relId);
            }
            if (componentToParentSS.containsKey(relId)) {
                parentSS = componentToParentSS.get(relId);
            }
            if (parentSS != null) {
                rel.put("sourceId", parentSS.get("id"));
                if (!existingNames.contains(parentSS.get("name"))) {
                    softwareSystems.add(Map.of("id", parentSS.get("id"), "name", parentSS.get("name")));
                    existingNames.add(parentSS.get("name"));
                }
            }
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(createDiagram(rankDirection,
                                                                  softwareSystemId,
                                                                  softwareSystemName,
                                                                  relationships,
                                                                  softwareSystems));
            json = structurizrClient.changeJson(json);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            log.error(e.getMessage(), e.getStackTrace());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка сериализации JSON" + e.getMessage());
        }
    }

    private static Map<String, Object> createDiagram(String rankDirection,
                                                          Long softwareSystemId,
                                                          String softwareSystemName,
                                                          List<Map<String, Object>> relationships,
                                                          Set<Map<String, Object>> softwareSystems) {
        Map<String, Object> diagram = new HashMap<>();

        Map<String, Object> model = new HashMap<>();
        Map<String, Object> ssMap = new HashMap<>();
        ssMap.put("id", softwareSystemId);
        ssMap.put("name", softwareSystemName);
        ssMap.put("relationships", relationships);
        softwareSystems.add(ssMap);
        model.put("softwareSystems", softwareSystems);

        Map<String, Object> automaticLayout = new HashMap<>();
        automaticLayout.put("applied", false);
        automaticLayout.put("edgeSeparation", 0);
        automaticLayout.put("implementation", "Graphviz");
        automaticLayout.put("nodeSeparation", 300);
        automaticLayout.put("rankDirection", rankDirection);
        automaticLayout.put("rankSeparation", 300);
        automaticLayout.put("vertices", false);

        Map<String, Object> systemContextView = new HashMap<>();
        systemContextView.put("automaticLayout", automaticLayout);
        systemContextView.put("elements", softwareSystems);
        systemContextView.put("key", "context");
        systemContextView.put("relationships", relationships);
        systemContextView.put("softwareSystemId", softwareSystemId);

        Map<String, Object> views = new HashMap<>();
        List<Map<String, Object>> systemContextViews = new ArrayList<>();
        systemContextViews.add(systemContextView);
        views.put("systemContextViews", systemContextViews);

        diagram.put("model", model);
        diagram.put("views", views);
        return diagram;
    }
}
