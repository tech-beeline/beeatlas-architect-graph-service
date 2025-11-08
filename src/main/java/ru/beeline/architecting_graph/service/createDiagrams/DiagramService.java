
package ru.beeline.architecting_graph.service.createDiagrams;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.file.FileSinkDOT;
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
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        Map<String, Map<String, Object>> containersInstances = new HashMap<>();

        ciMap.values().forEach(containerInstance -> {
            processCI(containerInstance, ciMap, relMap, ssMap, cMap, dnMap, containersInstances);
        });

        ciMap.putAll(containersInstances);

        List<Map<String, String>> elements = new ArrayList<>();
         elements.addAll(dnMap.keySet().stream().map(id -> Map.of("id", String.valueOf(id))).collect(Collectors.toList()));
         elements.addAll(ciMap.keySet().stream().map(id -> Map.of("id", String.valueOf(id))).collect(Collectors.toList()));
         elements.addAll(inMap.keySet().stream().map(id -> Map.of("id", String.valueOf(id))).collect(Collectors.toList()));
         elements.addAll(ssMap.keySet().stream().map(id -> Map.of("id", String.valueOf(id))).collect(Collectors.toList()));

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

    private void processCI(Map<String, Object> containerInstance,
                           Map<String, Map<String, Object>> ciMap,
                           Map<String, Map<String, Object>> relMap,
                           Map<String, Map<String, Object>> ssMap,
                           Map<String, Map<String, Object>> cMap,
                           Map<String, Map<String, Object>> dnMap,
                           Map<String, Map<String, Object>> containersInstances) {
        Result resultSet =
                genericRepository.findIncomingRelationshipsByContainerInstance(Long.parseLong(String.valueOf(
                        containerInstance.get("id"))));
        while (resultSet.hasNext()) {
            var rec = resultSet.next();
            var rel = rec.get("r").asRelationship();
            var src = rec.get("src").asNode();
            var dst = rec.get("dst").asNode();
            var container = rec.get("container").asNode();
            var ss = rec.get("ss").asNode();
            var dn = rec.get("dn").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(src.id()));
            relation.put("destinationId", String.valueOf(dst.id()));
            relation.put("description", rel.get("description").asString(null) + String.valueOf(rel.id()));
            relation.put("technology", rel.get("technology").asString(null));
            ((HashSet) ciMap.get(String.valueOf(dst.id())).get("relationships")).add(relation);
            relMap.put(String.valueOf(rel.id()), relation);

            if (ssMap.containsKey(String.valueOf(ss.id()))) {
                if (!cMap.containsKey(String.valueOf(src.id()))) {
                    Map<String, Object> newContainer = new HashMap<>();
                    newContainer.put("id", String.valueOf(container.id()));
                    newContainer.put("name", container.get("name").asString());
                    newContainer.put("relationships", new HashSet<>());
                    cMap.put(String.valueOf(src.id()), newContainer);
                    ((HashSet) ssMap.get(String.valueOf(ss.id())).get("containers")).add(newContainer);
                }
            } else {
                Map<String, Object> newContainer = new HashMap<>();
                newContainer.put("id", String.valueOf(container.id()));
                newContainer.put("name", container.get("name").asString());
                newContainer.put("relationships", new HashSet<>());
                cMap.put(String.valueOf(src.id()), newContainer);

                Map<String, Object> newSoftwareSystem = new HashMap<>();
                newSoftwareSystem.put("id", String.valueOf(ss.id()));
                newSoftwareSystem.put("name", ss.get("cmdb").asString());
                newSoftwareSystem.put("containers", newContainer);
                ssMap.put(String.valueOf(src.id()), newSoftwareSystem);
            }
            if (dnMap.containsKey(String.valueOf(dn.id()))) {
                if (!dnMap.get(String.valueOf(dn.id())).get("containerInstances").toString().contains(String.valueOf(src.id()))) {
                    Map<String, Object> ci = new HashMap<>();
                    ci.put("id", String.valueOf(src.id()));
                    ci.put("environment", "BY BEEATLAS");
                    ci.put("containerId", String.valueOf(container.id()));
                    containersInstances.put(String.valueOf(src.id()), ci);
                    ((HashSet) dnMap.get(String.valueOf(dn.id())).get("containerInstances")).add(ci);
                }
            } else {
                Map<String, Object> ci = new HashMap<>();
                ci.put("id", String.valueOf(src.id()));
                ci.put("environment", "BY BEEATLAS");
                ci.put("containerId", String.valueOf(src.id()));
                containersInstances.put(String.valueOf(src.id()), ci);
                HashSet hashSet = new HashSet();
                hashSet.add(ci);
                Map<String, Object> newDeploymentNode = new HashMap<>();
                newDeploymentNode.put("id", String.valueOf(dn.id()));
                newDeploymentNode.put("name", dn.get("name").asString());
                newDeploymentNode.put("containerInstances", hashSet);
                newDeploymentNode.put("infrastructureNodes", new ArrayList<>());
                newDeploymentNode.put("environment", "BY BEEATLAS");
                newDeploymentNode.put("children", new ArrayList<>());
                newDeploymentNode.put("relationships", new HashSet<>());
                dnMap.put(String.valueOf(dn.id()), newDeploymentNode);
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
            var rel = rec.get("r").asRelationship();
            var src = rec.get("src").asNode();
            var dst = rec.get("dst").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(src.id()));
            relation.put("destinationId", String.valueOf(dst.id()));
            relation.put("description", rel.get("description").asString(null) + String.valueOf(rel.id()));
            relation.put("technology", rel.get("technology").asString(null));
            ((HashSet) inMap.get(String.valueOf(dst.id())).get("relationships")).add(relation);
            relMap.put(String.valueOf(rel.id()), relation);

            if(!inMap.containsKey(String.valueOf(src.id()))) {
                Map<String, Object> InfrastructureNode = new HashMap<>();
                InfrastructureNode.put("id", String.valueOf(src.id()));
                InfrastructureNode.put("name", src.get("name").asString());
                InfrastructureNode.put("environment", "BY BEEATLAS");
                Result dnToIn = genericRepository.findDeploymentNodesByInfrastructureId(Long.parseLong(String.valueOf(src.id())));
                while (dnToIn.hasNext()) {
                    var dnToInRec = dnToIn.next();
                    var subDn = dnToInRec.get("dn").asNode();
                    if (dnMap.containsKey(String.valueOf(subDn.id()))) {
                        ((ArrayList) dnMap.get(String.valueOf(subDn.id())).get("infrastructureNodes")).add(InfrastructureNode);
                    } else {
                        Map<String, Object> newDeploymentNode = new HashMap<>();
                        newDeploymentNode.put("id", String.valueOf(src.id()));
                        newDeploymentNode.put("name", src.get("name").asString());
                        newDeploymentNode.put("environment", "BY BEEATLAS");
                        deploymentNodes.add(newDeploymentNode);
                    }
                }
            }
        }
    }

    private void processDntoIn(Map<String, Object> in, Map<String, Map<String, Object>> dnMap, List<Map<String, Object>> deploymentNodes,
                               Map<String, Map<String, Object>> inMap, Map<String, Map<String, Object>> relMap) {
        Result resultDn = genericRepository.findRelationshipsFromDeploymentNode(Long.parseLong(String.valueOf(in.get("id"))));
        while (resultDn.hasNext()) {
            var rec = resultDn.next();
            var rel = rec.get("r").asRelationship();
            var src = rec.get("src").asNode();
            var dst = rec.get("dst").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(src.id()));
            relation.put("destinationId", String.valueOf(dst.id()));
            relation.put("description", rel.get("description").asString(null) + String.valueOf(rel.id()));
            relation.put("technology", rel.get("technology").asString(null));

            if(!dnMap.containsKey(String.valueOf(src.id()))) {
                Map<String, Object> newDeploymentNode = new HashMap<>();
                newDeploymentNode.put("id", String.valueOf(src.id()));
                newDeploymentNode.put("name", src.get("name").asString());
                newDeploymentNode.put("environment", "BY BEEATLAS");
                deploymentNodes.add(newDeploymentNode);
            }
            ((HashSet) inMap.get(String.valueOf(src.id())).get("relationships")).add(relation);
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
            var rel = rec.get("r").asRelationship();
            var src = rec.get("src").asNode();
            var dst = rec.get("dst").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(src.id()));
            relation.put("destinationId", String.valueOf(dst.id()));
            relation.put("description", rel.get("description").asString(null) + String.valueOf(rel.id()));
            relation.put("technology", rel.get("technology").asString(null));
            ((HashSet) dnMap.get(String.valueOf(dst.id())).get("relationships")).add(relation);
            relMap.put(String.valueOf(rel.id()), relation);

            if(!inMap.containsKey(String.valueOf(src.id()))) {
                Map<String, Object> InfrastructureNode = new HashMap<>();
                InfrastructureNode.put("id", String.valueOf(src.id()));
                InfrastructureNode.put("name", src.get("name").asString());
                InfrastructureNode.put("environment", "BY BEEATLAS");
                Result dnToIn =
                        genericRepository.findDeploymentNodesByInfrastructureId(Long.parseLong(String.valueOf(src.id())));
                while (dnToIn.hasNext()) {
                    var dnToInRec = dnToIn.next();
                    var subDn = dnToInRec.get("dn").asNode();
                    if (dnMap.containsKey(String.valueOf(subDn.id()))) {
                        ((ArrayList) dnMap.get(String.valueOf(subDn.id())).get("infrastructureNodes")).add(InfrastructureNode);
                    } else {
                        Map<String, Object> newDeploymentNode = new HashMap<>();
                        newDeploymentNode.put("id", String.valueOf(src.id()));
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
            var rel = rec.get("r").asRelationship();
            var src = rec.get("src").asNode();
            var dst = rec.get("dst").asNode();

            Map<String, Object> relation = new HashMap<>();
            relation.put("id", String.valueOf(rel.id()));
            relation.put("sourceId", String.valueOf(src.id()));
            relation.put("destinationId", String.valueOf(dst.id()));
            relation.put("description", rel.get("description").asString(null) + String.valueOf(rel.id()));
            relation.put("technology", rel.get("technology").asString(null));

            if(!dnMap.containsKey(src.get("id"))) {
                Map<String, Object> newDeploymentNode = new HashMap<>();
                newDeploymentNode.put("id", String.valueOf(src.id()));
                newDeploymentNode.put("name", src.get("name").asString());
                newDeploymentNode.put("environment", "BY BEEATLAS");
                deploymentNodes.add(newDeploymentNode);
            }
            ((HashSet) dnMap.get(dst.get("id")).get("relationships")).add(relation);
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
        Set<Map<String, Object>> containerInstances = new HashSet<>();
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
            containerInstance.put("relationships", new HashSet<>());
            containerInstances.add(containerInstance);
            ciMap.put(String.valueOf(ciNode.id()), containerInstance);

            Map<String, Object> container = new HashMap<>();
            container.put("id", String.valueOf(cNode.id()));
            container.put("name", cNode.get("name").asString());
            container.put("relationships", new HashSet<>());
            containers.add(container);
            cMap.put(String.valueOf(cNode.id()), container);

            if(ssMap.containsKey(String.valueOf(ssNode.id()))){
                ((HashSet) ssMap.get(String.valueOf(ssNode.id())).get("containers")).add(container);
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
            infrastructure.put("relationships", new HashSet<>());

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
        softwareSystemObj.put("containers", new HashSet<>());
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
        systemContextView.put("environment", "BY-BEEATLAS");
        systemContextView.put("key", "BY-BEEATLAS-01");
        systemContextView.put("paperSize", "A6_Portrait");
        systemContextView.put("relationships", relations);

        Map<String, Object> views = new HashMap<>();
        List<Map<String, Object>> systemContextViews = new ArrayList<>();
        systemContextViews.add(systemContextView);
        views.put("deploymentViews", systemContextViews);

        diagram.put("model", model);
        diagram.put("views", views);

        return diagram;
    }

    public ResponseEntity<String> getDiagramDeploymentDot(Long nodeId) {
        var record = genericRepository.getNodeTypeAndNameById(nodeId);
        org. neo4j. driver. Record rec = record.next();
        String nodeType = rec.get("nodeType").asString("");
        String rootName = rec.get("name").asString("Unnamed");

        if (nodeType.isEmpty())
            throw new IllegalArgumentException("Node with id " + nodeId + " does not exist");

        Graph graph = new SingleGraph("Neo4j Graph");
        org. graphstream. graph. Node centralNode = graph.addNode("central");
        centralNode.setAttribute("ui.label", rootName);
        centralNode.setAttribute("color", "green");

        switch (nodeType) {
            case "DeploymentNode":
                var depResults = genericRepository.getDeploymentNodeDependencies(nodeId);
                if (depResults.hasNext()) {
                    var depRecord = depResults.next();
                    addNodesFromCollection(graph, depRecord, "deploymentSources", "red", "Вызов", "central");
                    addNodesFromCollection(graph, depRecord, "infrastructureSources", "red", "Вызов", "central");
                    addNodesFromCollection(graph, depRecord, "deploymentTargets", "blue", "Deploy", "central");
                    addNodesFromCollection(graph, depRecord, "infrastructureNodes", "blue", "Deploy", "central");
                    addNodesFromCollection(graph, depRecord, "containerInstances", "blue", "Deploy", "central");
                    addNodesFromCollection(graph, depRecord, "containers", "blue", "Deploy", "central");
                }
                break;

            case "Container":
                var containerResults = genericRepository.getContainerDependencies(nodeId);
                if (containerResults.hasNext()) {
                    var containerRecord = containerResults.next();
                    addNodesFromCollection(graph, containerRecord, "containersSources", "red", "Вызов", "central");
                }
                break;

            case "InfrastructureNode":
                var infraResults = genericRepository.getInfrastructureNodeDependencies(nodeId);
                if (infraResults.hasNext()) {
                    var infraRecord = infraResults.next();
                    addNodesFromCollection(graph, infraRecord, "infrastructureSources", "red", "Вызов", "central");
                    addNodesFromCollection(graph, infraRecord, "deploymentSources", "red", "Вызов", "central");
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported node type: " + nodeType);
        }

        return ResponseEntity.ok(convertToDotFormat(graph));
    }

    private void addNodesFromCollection(Graph graph, org.neo4j.driver.Record record,
                                        String key, String color, String edgeLabel, String centralNodeId) {
        List<Object> nodeObjects = record.get(key).asList();
        if (nodeObjects.isEmpty()) return;

        for (Object obj : nodeObjects) {
            if (!(obj instanceof org.neo4j.driver.types.Node)) continue;

            org.neo4j.driver.types.Node node = (org.neo4j.driver.types.Node) obj;
            String nodeId = key + "_" + node.id();
            org.graphstream.graph.Node graphNode;
            if (graph.getNode(nodeId) == null) {
                graphNode = graph.addNode(nodeId);
            } else {
                graphNode = graph.getNode(nodeId);
            }
            String label = node.get("name") != null ? node.get("name").asString() : "Unnamed";
            graphNode.setAttribute("ui.label", label);
            graphNode.setAttribute("color", color);

            String edgeId = nodeId + "_" + centralNodeId;
            if (graph.getEdge(edgeId) == null) {
                graph.addEdge(edgeId, nodeId, centralNodeId, true)
                        .setAttribute("ui.label", edgeLabel);
            }
        }
    }

    private String convertToDotFormat(Graph graph) {
        try {
            FileSinkDOT sink = new FileSinkDOT();
            sink.setDirected(true);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            sink.writeAll(graph, outputStream);
            return outputStream.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error converting graph to DOT format", e);
        }
    }
}
