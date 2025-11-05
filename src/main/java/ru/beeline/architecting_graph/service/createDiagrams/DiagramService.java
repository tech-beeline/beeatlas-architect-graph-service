
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

    @Autowired
    DeploymentNodesRepository deploymentNodesRepository;

    @Autowired
    InfrastructureNodesRepository infrastructureNodesRepository;
    @Autowired
    private ContainerInstanceRepository containerInstanceRepository;

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

        Set<Long> allNodeIds = softwareSystemRepository.getContainerAndComponentChildIds(cmdb);
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
                rel.put("in".equalsIgnoreCase(communicationDirection) ?
                                "sourceId" : "destinationId", parentSS.get("id"));
                if (!existingNames.contains(parentSS.get("name"))) {
                    softwareSystems.add(Map.of("id", parentSS.get("id").toString(), "name", parentSS.get("name")));
                    existingNames.add(parentSS.get("name"));
                }
            }
        }
        relationships =
                relationships.stream().filter(rel -> !rel.get("sourceId").equals(rel.get("destinationId"))).collect(Collectors.toList());
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
        ssMap.put("id", softwareSystemId.toString());
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
        systemContextView.put("softwareSystemId", softwareSystemId.toString());

        Map<String, Object> views = new HashMap<>();
        List<Map<String, Object>> systemContextViews = new ArrayList<>();
        systemContextViews.add(systemContextView);
        views.put("systemContextViews", systemContextViews);

        diagram.put("model", model);
        diagram.put("views", views);
        return diagram;
    }

    public ResponseEntity<String> getDiagramDeployment(String cmdb, String env, String rankDirection, String deploymentName) {
        if (rankDirection == null) {
            rankDirection = "LeftRight";
        }
        if (!rankDirection.equals("TopBottom") && !rankDirection.equals("LeftRight")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Недопустимая ориентация диаграммы");
        }
        Result deploymentNodeAndSoftwareSystem =
                deploymentNodesRepository.findDeploymentNodeByNameEnvCmdb(deploymentName, env, cmdb);
        if (!deploymentNodeAndSoftwareSystem.hasNext()) {
            throw new RuntimeException("SoftwareSystem не найден");
        }
        var record = deploymentNodeAndSoftwareSystem.next();
        Node dnNode = record.get("dn").asNode();
        Map<String, Object> mainSoftwareSystem = getSoftwareSystem(record);
        Map<String, Map<String, Object>> ssMap = new HashMap<>();
        Map<String, Map<String, Object>> dnMap = new HashMap<>();
        Map<String, Map<String, Object>> ciMap = new HashMap<>();
        Map<String, Map<String, Object>> inMap = new HashMap<>();
        Map<String, Map<String, Object>> cMap = new HashMap<>();
        Map<String, Map<String, Object>> relMap = new HashMap<>();
        ssMap.put(mainSoftwareSystem.get("id").toString(), mainSoftwareSystem);

        Map<String, Object> deploymentNode = recursiveConstructDnCiIn(dnNode, ssMap, dnMap, inMap, ciMap, cMap);
        List<Map<String, Object>> deploymentNodes = Arrays.asList(deploymentNode);

        dnMap.values().forEach(dn->{
            processDnToDn(dn, dnMap, deploymentNodes, relMap);
            processInToDn(dn, dnMap, inMap, deploymentNodes, relMap);
        });

        inMap.values().forEach(in->{
            processDntoIn(in, dnMap, deploymentNodes, inMap, relMap);
            processIntoIn(in, dnMap, deploymentNodes, inMap, relMap);
        });

        cMap.values().forEach(container -> {
            processCCtoCC(cmdb, container, cMap, relMap, ssMap, dnMap, ciMap);
            processComToCC(cmdb, container, cMap, relMap, ssMap, dnMap, ciMap);
            processCCtoChildComp(cmdb, container, cMap, relMap, ssMap, dnMap, ciMap);
            processCCtoAll(cmdb, container, cMap, relMap, ssMap, dnMap, ciMap);
        });

        List<Map<String, String>> elements = new ArrayList<>();
         elements.addAll(dnMap.keySet().stream().map(id -> Map.of("id", String.valueOf(id))).collect(Collectors.toList()));
         elements.addAll(ciMap.keySet().stream().map(id -> Map.of("id", String.valueOf(id))).collect(Collectors.toList()));
         elements.addAll(inMap.keySet().stream().map(id -> Map.of("id", String.valueOf(id))).collect(Collectors.toList()));
         elements.addAll(ssMap.keySet().stream().map(id -> Map.of("id", String.valueOf(id))).collect(Collectors.toList()));
         elements.addAll(cMap.keySet().stream().map(id -> Map.of("id", String.valueOf(id))).collect(Collectors.toList()));

        List<Map<String, String>> relations = relMap.keySet().stream().map(id -> Map.of("id", String.valueOf(id))).collect(Collectors.toList());

        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(createDiagram(rankDirection, deploymentNodes, ssMap, elements, relations));
            json = structurizrClient.changeJson(json);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            log.error(e.getMessage(), e.getStackTrace());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка сериализации JSON" + e.getMessage());
        }
    }

    private void processCCtoAll(String cmdb,
                                Map<String, Object> container,
                                Map<String, Map<String, Object>> cMap,
                                Map<String, Map<String, Object>> relMap,
                                Map<String, Map<String, Object>> ssMap,
                                Map<String, Map<String, Object>> dnMap,
                                Map<String, Map<String, Object>> ciMap) {
        Result resultSet = genericRepository.findIncomingComponentRelationshipsExtended(Long.parseLong(String.valueOf(
                container.get("id"))), cmdb);
        while (resultSet.hasNext()) {
            var rec = resultSet.next();
            var rel = rec.get("r").asNode();
            var src = rec.get("src").asNode();
            var srcComp = rec.get("srcComp").asNode();
            var dstComp = rec.get("dstComp").asNode();
            var ci = rec.get("ci").asNode();
            var parentSoftwareSystem = rec.get("parentSoftwareSystem").asNode();
            var parentDeploymentNode = rec.get("parentDeploymentNode").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(srcComp.id()));
            relation.put("destinationId", String.valueOf(dstComp.id()));
            relation.put("description", rel.get("description").asString(null));
            relation.put("technology", rel.get("technology").asString(null));
            ((ArrayList) cMap.get(src.get("id").asString()).get("relationships")).add(relation);
            relMap.put(String.valueOf(rel.id()), relation);

            if (ssMap.containsKey(parentSoftwareSystem.get("id").asString())) {
                if (!cMap.containsKey(src.get("id").asString())) {
                    Map<String, Object> newContainer = new HashMap<>();
                    newContainer.put("id", src.get("id").asString());
                    newContainer.put("name", src.get("name").asString());
                    newContainer.put("relatioships", new ArrayList<>());
                    cMap.put(src.get("id").asString(), newContainer);
                    ((ArrayList) ssMap.get(parentSoftwareSystem.get("id")).get("containers")).add(newContainer);

                }
            } else {
                Map<String, Object> newContainer = new HashMap<>();
                newContainer.put("id", src.get("id").asString());
                newContainer.put("name", src.get("name").asString());
                newContainer.put("relationships", new ArrayList<>());
                cMap.put(src.get("id").asString(), newContainer);

                Map<String, Object> newSoftwareSystem = new HashMap<>();
                newSoftwareSystem.put("id", parentSoftwareSystem.get("id").asString());
                newSoftwareSystem.put("name", parentSoftwareSystem.get("cmdb").asString());
                newSoftwareSystem.put("containers", newContainer);
                ssMap.put(src.get("id").asString(), newSoftwareSystem);
            }
            if (dnMap.containsKey(parentDeploymentNode.get("id").asString())) {
                if (!ciMap.containsKey(src.get("id").asString())) {
                    Map<String, Object> containerInstance = new HashMap<>();
                    containerInstance.put("id", ci.get("id").asString());
                    containerInstance.put("environment","BY BEEATLAS");
                    containerInstance.put("containerId", src.get("id").asString());
                    ciMap.put(ci.get("id").asString(), containerInstance);
                    ((ArrayList) dnMap.get(parentDeploymentNode.get("id")).get("containerInstances")).add(containerInstance);
                }
            } else {
                Map<String, Object> containerInstance = new HashMap<>();
                containerInstance.put("id", ci.get("id").asString());
                containerInstance.put("environment","BY BEEATLAS");
                containerInstance.put("containerId", src.get("id").asString());
                ciMap.put(ci.get("id").asString(), containerInstance);

                Map<String, Object> newDeploymentNode = new HashMap<>();
                newDeploymentNode.put("id", parentDeploymentNode.get("id").asString());
                newDeploymentNode.put("name", parentDeploymentNode.get("name").asString());
                newDeploymentNode.put("containerInstances", containerInstance);
                newDeploymentNode.put("infrastructureNodes", new ArrayList<>());
                newDeploymentNode.put("environment", "BY BEEATLAS");
                newDeploymentNode.put("children", new ArrayList<>());
                newDeploymentNode.put("relationships", new ArrayList<>());
                dnMap.put(parentDeploymentNode.get("id").asString(), newDeploymentNode);
            }
        }
    }

    private void processCCtoChildComp(String cmdb, Map<String, Object> container, Map<String, Map<String, Object>> cMap, Map<String, Map<String, Object>> relMap, Map<String, Map<String, Object>> ssMap, Map<String, Map<String, Object>> dnMap, Map<String, Map<String, Object>> ciMap) {
        Result resultSet = genericRepository.findIncomingContainerRelationshipsExtendedChildComponent(Long.parseLong(String.valueOf(container.get("id"))), cmdb);
        while (resultSet.hasNext()) {
            var rec = resultSet.next();
            var rel = rec.get("r").asNode();
            var src = rec.get("src").asNode();
            var dstComp = rec.get("dstComp").asNode();
            var ci = rec.get("ci").asNode();
            var parentSoftwareSystem = rec.get("parentSoftwareSystem").asNode();
            var parentDeploymentNode = rec.get("parentDeploymentNode").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(src.id()));
            relation.put("destinationId", String.valueOf(dstComp.id()));
            relation.put("description", rel.get("description").asString(null));
            relation.put("technology", rel.get("technology").asString(null));
            ((ArrayList) cMap.get(src.get("id").asString()).get("relationships")).add(relation);
            relMap.put(String.valueOf(rel.id()), relation);

            if (ssMap.containsKey(parentSoftwareSystem.get("id").asString())) {
                if (!cMap.containsKey(src.get("id").asString())) {
                    Map<String, Object> newContainer = new HashMap<>();
                    newContainer.put("id", src.get("id").asString());
                    newContainer.put("name", src.get("name").asString());
                    newContainer.put("relatioships", new ArrayList<>());
                    cMap.put(src.get("id").asString(), newContainer);
                    ((ArrayList) ssMap.get(parentSoftwareSystem.get("id")).get("containers")).add(newContainer);

                }
            } else {
                Map<String, Object> newContainer = new HashMap<>();
                newContainer.put("id", src.get("id").asString());
                newContainer.put("name", src.get("name").asString());
                newContainer.put("relationships", new ArrayList<>());
                cMap.put(src.get("id").asString(), newContainer);

                Map<String, Object> newSoftwareSystem = new HashMap<>();
                newSoftwareSystem.put("id", parentSoftwareSystem.get("id").asString());
                newSoftwareSystem.put("name", parentSoftwareSystem.get("cmdb").asString());
                newSoftwareSystem.put("containers", newContainer);
                ssMap.put(src.get("id").asString(), newSoftwareSystem);
            }
            if (dnMap.containsKey(parentDeploymentNode.get("id").asString())) {
                if (!ciMap.containsKey(src.get("id").asString())) {
                    Map<String, Object> containerInstance = new HashMap<>();
                    containerInstance.put("id", ci.get("id").asString());
                    containerInstance.put("environment","BY BEEATLAS");
                    containerInstance.put("containerId", src.get("id").asString());
                    ciMap.put(ci.get("id").asString(), containerInstance);
                    ((ArrayList) dnMap.get(parentDeploymentNode.get("id")).get("containerInstances")).add(containerInstance);
                }
            } else {
                Map<String, Object> containerInstance = new HashMap<>();
                containerInstance.put("id", ci.get("id").asString());
                containerInstance.put("environment","BY BEEATLAS");
                containerInstance.put("containerId", src.get("id").asString());
                ciMap.put(ci.get("id").asString(), containerInstance);

                Map<String, Object> newDeploymentNode = new HashMap<>();
                newDeploymentNode.put("id", parentDeploymentNode.get("id").asString());
                newDeploymentNode.put("name", parentDeploymentNode.get("name").asString());
                newDeploymentNode.put("containerInstances", containerInstance);
                newDeploymentNode.put("infrastructureNodes", new ArrayList<>());
                newDeploymentNode.put("environment", "BY BEEATLAS");
                newDeploymentNode.put("children", new ArrayList<>());
                newDeploymentNode.put("relationships", new ArrayList<>());
                dnMap.put(parentDeploymentNode.get("id").asString(), newDeploymentNode);
            }
        }
    }

    private void processComToCC(String cmdb, Map<String, Object> container, Map<String, Map<String, Object>> cMap, Map<String, Map<String, Object>> relMap, Map<String, Map<String, Object>> ssMap, Map<String, Map<String, Object>> dnMap, Map<String, Map<String, Object>> ciMap) {
        Result resultSet = genericRepository.findIncomingContainerRelationshipsExtendedComponent(Long.parseLong(String.valueOf(container.get("id"))), cmdb);
        while (resultSet.hasNext()) {
            var rec = resultSet.next();
            var rel = rec.get("r").asNode();
            var src = rec.get("src").asNode();
            var srcComponent = rec.get("srcComponent").asNode();
            var dst = rec.get("dst").asNode();
            var ci = rec.get("ci").asNode();
            var parentSoftwareSystem = rec.get("parentSoftwareSystem").asNode();
            var parentDeploymentNode = rec.get("parentDeploymentNode").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(srcComponent.id()));
            relation.put("destinationId", String.valueOf(dst.id()));
            relation.put("description", rel.get("description").asString(null));
            relation.put("technology", rel.get("technology").asString(null));
            ((ArrayList) cMap.get(src.get("id").asString()).get("relationships")).add(relation);
            relMap.put(String.valueOf(rel.id()), relation);

            if (ssMap.containsKey(parentSoftwareSystem.get("id").asString())) {
                if (!cMap.containsKey(src.get("id").asString())) {
                    Map<String, Object> newContainer = new HashMap<>();
                    newContainer.put("id", src.get("id").asString());
                    newContainer.put("name", src.get("name").asString());
                    newContainer.put("relatioships", new ArrayList<>());
                    cMap.put(src.get("id").asString(), newContainer);
                    ((ArrayList) ssMap.get(parentSoftwareSystem.get("id")).get("containers")).add(newContainer);

                }
            } else {
                Map<String, Object> newContainer = new HashMap<>();
                newContainer.put("id", src.get("id").asString());
                newContainer.put("name", src.get("name").asString());
                newContainer.put("relationships", new ArrayList<>());
                cMap.put(src.get("id").asString(), newContainer);

                Map<String, Object> newSoftwareSystem = new HashMap<>();
                newSoftwareSystem.put("id", parentSoftwareSystem.get("id").asString());
                newSoftwareSystem.put("name", parentSoftwareSystem.get("cmdb").asString());
                newSoftwareSystem.put("containers", newContainer);
                ssMap.put(src.get("id").asString(), newSoftwareSystem);
            }
            if (dnMap.containsKey(parentDeploymentNode.get("id").asString())) {
                if (!ciMap.containsKey(src.get("id").asString())) {
                    Map<String, Object> containerInstance = new HashMap<>();
                    containerInstance.put("id", ci.get("id").asString());
                    containerInstance.put("environment","BY BEEATLAS");
                    containerInstance.put("containerId", src.get("id").asString());
                    ciMap.put(ci.get("id").asString(), containerInstance);
                    ((ArrayList) dnMap.get(parentDeploymentNode.get("id")).get("containerInstances")).add(containerInstance);
                }
            } else {
                Map<String, Object> containerInstance = new HashMap<>();
                containerInstance.put("id", ci.get("id").asString());
                containerInstance.put("environment","BY BEEATLAS");
                containerInstance.put("containerId", src.get("id").asString());
                ciMap.put(ci.get("id").asString(), containerInstance);

                Map<String, Object> newDeploymentNode = new HashMap<>();
                newDeploymentNode.put("id", parentDeploymentNode.get("id").asString());
                newDeploymentNode.put("name", parentDeploymentNode.get("name").asString());
                newDeploymentNode.put("containerInstances", containerInstance);
                newDeploymentNode.put("infrastructureNodes", new ArrayList<>());
                newDeploymentNode.put("environment", "BY BEEATLAS");
                newDeploymentNode.put("children", new ArrayList<>());
                newDeploymentNode.put("relationships", new ArrayList<>());
                dnMap.put(parentDeploymentNode.get("id").asString(), newDeploymentNode);
            }
        }
    }

    private void processCCtoCC(String cmdb, Map<String, Object> container, Map<String, Map<String, Object>> cMap, Map<String, Map<String, Object>> relMap, Map<String, Map<String, Object>> ssMap, Map<String, Map<String, Object>> dnMap, Map<String, Map<String, Object>> ciMap) {
        Result resultSet = genericRepository.findIncomingContainerRelationshipsExtendedContainer(Long.parseLong(String.valueOf(container.get("id"))), cmdb);
        while (resultSet.hasNext()) {
            var rec = resultSet.next();
            var rel = rec.get("r").asNode();
            var src = rec.get("src").asNode();
            var dst = rec.get("dst").asNode();
            var ci = rec.get("ci").asNode();
            var parentSoftwareSystem = rec.get("parentSoftwareSystem").asNode();
            var parentDeploymentNode = rec.get("parentDeploymentNode").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(src.id()));
            relation.put("destinationId", String.valueOf(dst.id()));
            relation.put("description", rel.get("description").asString(null));
            relation.put("technology", rel.get("technology").asString(null));
            ((ArrayList) cMap.get(src.get("id").asString()).get("relationships")).add(relation);
            relMap.put(String.valueOf(rel.id()), relation);

            if (ssMap.containsKey(parentSoftwareSystem.get("id").asString())) {
                if (!cMap.containsKey(src.get("id").asString())) {
                    Map<String, Object> newContainer = new HashMap<>();
                    newContainer.put("id", src.get("id").asString());
                    newContainer.put("name", src.get("name").asString());
                    newContainer.put("relatioships", new ArrayList<>());
                    cMap.put(src.get("id").asString(), newContainer);
                    ((ArrayList) ssMap.get(parentSoftwareSystem.get("id")).get("containers")).add(newContainer);

                }
            } else {
                Map<String, Object> newContainer = new HashMap<>();
                newContainer.put("id", src.get("id").asString());
                newContainer.put("name", src.get("name").asString());
                newContainer.put("relationships", new ArrayList<>());
                cMap.put(src.get("id").asString(), newContainer);

                Map<String, Object> newSoftwareSystem = new HashMap<>();
                newSoftwareSystem.put("id", parentSoftwareSystem.get("id").asString());
                newSoftwareSystem.put("name", parentSoftwareSystem.get("cmdb").asString());
                newSoftwareSystem.put("containers", newContainer);
                ssMap.put(src.get("id").asString(), newSoftwareSystem);
            }
            if (dnMap.containsKey(parentDeploymentNode.get("id").asString())) {
                if (!ciMap.containsKey(src.get("id").asString())) {
                    Map<String, Object> containerInstance = new HashMap<>();
                    containerInstance.put("id", ci.get("id").asString());
                    containerInstance.put("environment","BY BEEATLAS");
                    containerInstance.put("containerId", src.get("id").asString());
                    ciMap.put(ci.get("id").asString(), containerInstance);
                    ((ArrayList) dnMap.get(parentDeploymentNode.get("id")).get("containerInstances")).add(containerInstance);
                }
            } else {
                Map<String, Object> containerInstance = new HashMap<>();
                containerInstance.put("id", ci.get("id").asString());
                containerInstance.put("environment","BY BEEATLAS");
                containerInstance.put("containerId", src.get("id").asString());
                ciMap.put(ci.get("id").asString(), containerInstance);

                Map<String, Object> newDeploymentNode = new HashMap<>();
                newDeploymentNode.put("id", parentDeploymentNode.get("id").asString());
                newDeploymentNode.put("name", parentDeploymentNode.get("name").asString());
                newDeploymentNode.put("containerInstances", containerInstance);
                newDeploymentNode.put("infrastructureNodes", new ArrayList<>());
                newDeploymentNode.put("environment", "BY BEEATLAS");
                newDeploymentNode.put("children", new ArrayList<>());
                newDeploymentNode.put("relationships", new ArrayList<>());
                dnMap.put(parentDeploymentNode.get("id").asString(), newDeploymentNode);
            }
        }
    }

    private void processIntoIn(Map<String, Object> in,
                               Map<String, Map<String, Object>> dnMap,
                               List<Map<String, Object>> deploymentNodes,
                               Map<String, Map<String, Object>> inMap, Map<String, Map<String, Object>> relMap) {
        Result resultIn = genericRepository.findIncomingDeploymentNodeRelationshipsFromIncomingDeploymentNode(Long.parseLong(String.valueOf(in.get("id"))));
        while (resultIn.hasNext()) {
            var rec = resultIn.next();
            var rel = rec.get("r").asNode();
            var src = rec.get("src").asNode();
            var dst = rec.get("dst").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(src.id()));
            relation.put("destinationId", String.valueOf(dst.id()));
            relation.put("description", rel.get("description").asString(null));
            relation.put("technology", rel.get("technology").asString(null));
            ((ArrayList) inMap.get(dst.get("id")).get("relationships")).add(relation);
            relMap.put(String.valueOf(rel.id()), relation);

            if(!inMap.containsKey(src.get("id"))) {
                Map<String, Object> InfrastructureNode = new HashMap<>();
                InfrastructureNode.put("id", src.get("id").asString());
                InfrastructureNode.put("name", src.get("name").asString());
                InfrastructureNode.put("environment", "BY BEEATLAS");
                Result dnToIn = genericRepository.findDeploymentNodesByInfrastructureId(Long.parseLong(String.valueOf(src.get("id"))));
                while (dnToIn.hasNext()) {
                    var dnToInRec = dnToIn.next();
                    var subDn = dnToInRec.get("dn").asNode();
                    if (dnMap.containsKey(subDn.get("id"))) {
                        ((ArrayList) dnMap.get(subDn.get("id")).get("infrastructureNodes")).add(InfrastructureNode);
                    } else {
                        Map<String, Object> newDeploymentNode = new HashMap<>();
                        newDeploymentNode.put("id", src.get("id").asString());
                        newDeploymentNode.put("name", src.get("name").asString());
                        newDeploymentNode.put("environment", "BY BEEATLAS");
                        deploymentNodes.add(newDeploymentNode);
                    }
                }
            }
        }
    }

    private void processDntoIn(Map<String, Object> in, Map<String, Map<String, Object>> dnMap, List<Map<String, Object>> deploymentNodes, Map<String, Map<String, Object>> inMap, Map<String, Map<String, Object>> relMap) {
        Result resultDn = genericRepository.findRelationshipsFromDeploymentNode(Long.parseLong(String.valueOf(in.get("id"))));
        while (resultDn.hasNext()) {
            var rec = resultDn.next();
            var rel = rec.get("r").asNode();
            var src = rec.get("src").asNode();
            var dst = rec.get("dst").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(src.id()));
            relation.put("destinationId", String.valueOf(dst.id()));
            relation.put("description", rel.get("description").asString(null));
            relation.put("technology", rel.get("technology").asString(null));

            if(!dnMap.containsKey(src.get("id"))) {
                Map<String, Object> newDeploymentNode = new HashMap<>();
                newDeploymentNode.put("id", src.get("id").asString());
                newDeploymentNode.put("name", src.get("name").asString());
                newDeploymentNode.put("environment", "BY BEEATLAS");
                deploymentNodes.add(newDeploymentNode);
            }
            ((ArrayList) inMap.get(src.get("id")).get("relationships")).add(relation);
            relMap.put(String.valueOf(rel.id()), relation);

        }
    }

    private void processInToDn(Map<String, Object> dn,
                               Map<String, Map<String, Object>> dnMap,
                               Map<String, Map<String, Object>> inMap,
                               List<Map<String, Object>> deploymentNodes, Map<String, Map<String, Object>> relMap) {
        Result resultIn = genericRepository.findIncomingDeploymentNodeRelationshipsFromInfrastructureNode(Long.parseLong(String.valueOf(dn.get("id"))));
        while (resultIn.hasNext()) {
            var rec = resultIn.next();
            var rel = rec.get("r").asNode();
            var src = rec.get("src").asNode();
            var dst = rec.get("dst").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(src.id()));
            relation.put("destinationId", String.valueOf(dst.id()));
            relation.put("description", rel.get("description").asString(null));
            relation.put("technology", rel.get("technology").asString(null));
            ((ArrayList) dnMap.get(dst.get("id")).get("relationships")).add(relation);
            relMap.put(String.valueOf(rel.id()), relation);

            if(!inMap.containsKey(src.get("id"))) {
                Map<String, Object> InfrastructureNode = new HashMap<>();
                InfrastructureNode.put("id", src.get("id").asString());
                InfrastructureNode.put("name", src.get("name").asString());
                InfrastructureNode.put("environment", "BY BEEATLAS");
                Result dnToIn = genericRepository.findDeploymentNodesByInfrastructureId(Long.parseLong(String.valueOf(src.get("id"))));
                while (dnToIn.hasNext()) {
                    var dnToInRec = dnToIn.next();
                    var subDn = dnToInRec.get("dn").asNode();
                    if (dnMap.containsKey(subDn.get("id"))) {
                        ((ArrayList) dnMap.get(subDn.get("id")).get("infrastructureNodes")).add(InfrastructureNode);
                    } else {
                        Map<String, Object> newDeploymentNode = new HashMap<>();
                        newDeploymentNode.put("id", src.get("id").asString());
                        newDeploymentNode.put("name", src.get("name").asString());
                        newDeploymentNode.put("environment", "BY BEEATLAS");
                        deploymentNodes.add(newDeploymentNode);
                    }
                }
            }
        }
    }

    private void processDnToDn(Map<String, Object> dn, Map<String, Map<String, Object>> dnMap, List<Map<String, Object>> deploymentNodes, Map<String, Map<String, Object>> relMap) {
        Result resultDn = genericRepository.findIncomingDeploymentNodeRelationships(Long.parseLong(String.valueOf(dn.get("id"))));
        while (resultDn.hasNext()) {
            var rec = resultDn.next();
            var rel = rec.get("r").asNode();
            var src = rec.get("src").asNode();
            var dst = rec.get("dst").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(src.id()));
            relation.put("destinationId", String.valueOf(dst.id()));
            relation.put("description", rel.get("description").asString(null));
            relation.put("technology", rel.get("technology").asString(null));

            if(!dnMap.containsKey(src.get("id"))) {
                Map<String, Object> newDeploymentNode = new HashMap<>();
                newDeploymentNode.put("id", src.get("id").asString());
                newDeploymentNode.put("name", src.get("name").asString());
                newDeploymentNode.put("environment", "BY BEEATLAS");
                deploymentNodes.add(newDeploymentNode);
            }
            ((ArrayList) dnMap.get(dst.get("id")).get("relationships")).add(relation);
            relMap.put(String.valueOf(rel.id()), relation);

        }
    }

    public Map<String, Object> recursiveConstructDnCiIn(Node dnNode,
                                                        Map<String, Map<String, Object>> ssMap,
                                                        Map<String, Map<String, Object>> dnMap,
                                                        Map<String, Map<String, Object>> inMap,
                                                        Map<String, Map<String, Object>> ciMap,
                                                        Map<String, Map<String, Object>> cMap){
        String dnName = dnNode.get("name").asString();
        String dnId = String.valueOf(dnNode.id());
        Map<String, Object> deploymentNode = new HashMap<>();
        deploymentNode.put("id", dnId);
        deploymentNode.put("name", dnName);
        deploymentNode.put("environment", "BY BEEATLAS");

        Result containerInstancesWithContainers = genericRepository.getContainerInstancesWithContainersAndSoftwareSystems(dnNode.id());
        List<Map<String, Object>> containerInstances = new ArrayList<>();
        List<Map<String, Object>> containers = new ArrayList<>();
        List<Map<String, Object>> softwareSystems = new ArrayList<>();
        while (containerInstancesWithContainers.hasNext()) {
            var containerInstancesWithContainersRecord = containerInstancesWithContainers.next();
            var ciNode = containerInstancesWithContainersRecord.get("ci").asNode();
            var ssNode = containerInstancesWithContainersRecord.get("ss").asNode();
            var cNode = containerInstancesWithContainersRecord.get("c").asNode();

            Map<String, Object> containerInstance = new HashMap();
            containerInstance.put("id", String.valueOf(ciNode.id()));
            containerInstance.put("environment", "BY BEEATLAS");
            containerInstance.put("containerId", String.valueOf(cNode != null ? cNode.id() : ciNode.id()));
            containerInstances.add(containerInstance);
            ciMap.put(String.valueOf(ciNode.id()), containerInstance);

            Map<String, Object> container = new HashMap<>();
            container.put("id", String.valueOf(cNode.id()));
            container.put("name", cNode.get("name").asString());
            container.put("relationships", new ArrayList<>());
            containers.add(container);
            cMap.put(String.valueOf(cNode.id()), container);

            if(ssMap.containsKey(String.valueOf(ssNode.id()))){
                ((ArrayList) ssMap.get(String.valueOf(ssNode.id())).get(containers)).add(container);
            }else {
                Map<String, Object> softwareSystem = new HashMap<>();
                softwareSystem.put("id", String.valueOf(ssNode.id()));
                softwareSystem.put("name", ssNode.get("cmdb").asString());
                softwareSystem.put("containers", new ArrayList<>(Arrays.asList(container)));
                softwareSystems.add(softwareSystem);
            }

        }
        deploymentNode.put("containerInstances", containerInstances);
        deploymentNode.put("infrastructureNodes", mapInfrastructureNodes(infrastructureNodesRepository.getInfrastructureNodesByDeploymentNodeId(dnNode.id()), inMap));

        List<Map<String, Object>> children = new ArrayList<>();
        Result kids = deploymentNodesRepository.getChildDeploymentNodesById(dnNode.id());
        while (kids.hasNext()) {
            var childNode = kids.next().get("child").asNode();
            Map<String, Object> child = recursiveConstructDnCiIn(childNode, ssMap, dnMap, inMap, ciMap, cMap);
            children.add(child);
            dnMap.put(child.get("id").toString(), child);
        }
        deploymentNode.put("children", children);

        return deploymentNode;
        }

    public List<Map<String, Object>> mapInfrastructureNodes(Result result, Map<String, Map<String, Object>> inMap) {
        List<Map<String, Object>> infrastructureNodes = new ArrayList<>();

        while (result.hasNext()) {
            var record = result.next();
            var infNode = record.get("inf").asNode();

            Map<String, Object> infrastructure = new HashMap<>();
            infrastructure.put("id", String.valueOf(infNode.id()));
            infrastructure.put("name", infNode.get("name").asString());
            infrastructure.put("environment", "BY BEEATLAS");
            infrastructure.put("relationships", new ArrayList<>());

            infrastructureNodes.add(infrastructure);
            inMap.put(String.valueOf(infNode.id()), infrastructure);
        }
        return infrastructureNodes;
    }

    private static Map<String, Object> getSoftwareSystem(org.neo4j.driver.Record record) {
        var ssNode = record.get("ss").asNode();

        Map<String, Object> softwareSystemObj = new HashMap<>();
        softwareSystemObj.put("id", String.valueOf(ssNode.id()));
        softwareSystemObj.put("name", ssNode.get("cmdb").asString());
        softwareSystemObj.put("containers", new ArrayList<>());
        return softwareSystemObj;
    }


    private static Map<String, Object> createDiagram(String rankDirection,
                                                     List<Map<String, Object>> deploymentNodes,
                                                     Map<String,Map<String, Object>> softwareSystem,
                                                     List<Map<String, String>> elements,
                                                     List<Map<String, String>> relations) {
        Map<String, Object> diagram = new HashMap<>();

        Map<String, Object> model = new HashMap<>();
        model.put("deploymentNodes", deploymentNodes);
        model.put("softwareSystems", new ArrayList<>(softwareSystem.values()));

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
        systemContextView.put("elements", elements);
        systemContextView.put("key", "context");
        systemContextView.put("relationships", relations);

        Map<String, Object> views = new HashMap<>();
        List<Map<String, Object>> systemContextViews = new ArrayList<>();
        systemContextViews.add(systemContextView);
        views.put("systemContextViews", systemContextViews);

        diagram.put("model", model);
        diagram.put("views", views);

        return diagram;
    }

}
