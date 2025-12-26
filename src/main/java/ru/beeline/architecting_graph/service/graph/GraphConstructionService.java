/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.service.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import ru.beeline.architecting_graph.client.ProductClient;
import ru.beeline.architecting_graph.dto.*;
import ru.beeline.architecting_graph.model.Workspace;
import ru.beeline.architecting_graph.repository.neo4j.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.beeline.architecting_graph.utils.JwtUtils.isIpAddress;

@Slf4j
@Service
public class GraphConstructionService {

    @Autowired
    EnvironmentRepository environmentRepository;

    @Autowired
    ContainerUpdateFunctions containerUpdateFunctions;

    @Autowired(required = false)
    RedisTemplate<String, TaskCacheDTO> redisTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    DeploymentNodesRepository deploymentNodesRepository;

    @Autowired
    SoftwareSystemRepository softwareSystemRepository;
    @Autowired
    private ContainerRepository containerRepository;
    @Autowired
    private ComponentRepository componentRepository;
    @Autowired
    private ProductClient productClient;

    public ResponseEntity<String> graphConstruct(String workspaceJson, String graphTag) {
        log.info("graphConstruct is running");
        if (workspaceJson == null) {
            log.info("Документ не найден");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Документ не найден");
        }
        Workspace workspace;
        try {
            workspace = objectMapper.readValue(workspaceJson, Workspace.class);
        } catch (Exception e) {
            log.info("Полученный workspace не валиден: " + e.getMessage());
            log.info("workspaceJson is: " + workspaceJson);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Полученный workspace не валиден");
        }
        try {
            containerUpdateFunctions.createGraph(graphTag, workspace);
        } catch (Exception e) {
            log.info("Граф не построен: " + e.getMessage());
            return ResponseEntity.badRequest().body("Граф не построен\n" + e.getMessage());
        }
        log.info("graph constructed");
        return ResponseEntity.status(HttpStatus.CREATED).body("Граф построен");
    }

    private String handleClientError(HttpClientErrorException e) {
        HttpStatus status = e.getStatusCode();
        if (status == HttpStatus.NOT_FOUND) {
            return null;
        } else if (status == HttpStatus.BAD_REQUEST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Полученный workspace не валиден");
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Доступ запрещен");
        }
    }

    public static Workspace getWorkspaceFileForTest(ObjectMapper objectMapper) throws Exception {
        String FilePath = "workspace_RNC.json";
        File file = new File(FilePath);
        Workspace workspace = objectMapper.readValue(file, Workspace.class);
        return workspace;
    }

    public ResponseEntity<TaskCacheDTO> getGraphByTask(String graphType, String taskId) {
        String redisKey = "graph:" + taskId;

        TaskCacheDTO existingDto = redisTemplate.opsForValue().get(redisKey);

        if (existingDto == null) {
            log.warn("No cache entry for taskId: {}", taskId);
            return ResponseEntity.notFound().build();
        }

        if (!graphType.equalsIgnoreCase(existingDto.getType())) {
            log.warn("Cache entry type '{}' does not match requested graphType '{}'", existingDto.getType(), graphType);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(existingDto);
    }

    public ResponseEntity<List<DeploymentNodeDTO>> getDeploymentNode(String search) {
        List<DeploymentNodeDTO> result = new ArrayList<>();
        Result deploymentNodes = deploymentNodesRepository.findDeploymentNodesBySearch(search);
        if (!deploymentNodes.hasNext() && isIpAddress(search)) {
            List<ProductInfraSearchDTO> products = productClient.getProductInfraByVimIp(search);
            if (products.isEmpty()) {
                ResponseEntity.ok(result);
            }
            List<String> originNames = products.stream()
                    .map(ProductInfraSearchDTO::getName)
                    .collect(Collectors.toList());

            deploymentNodesRepository.updateIpForDeploymentNodesByOriginNames(originNames, products.get(0).getValue());

            deploymentNodes = deploymentNodesRepository.findDeploymentNodesBySearch(search);
        }
        while (deploymentNodes.hasNext()) {
            Record deployment = deploymentNodes.next();
            Record system = getParentSoftwareSystem(deployment.get("n").asNode().id());
            Record environment = getParentEnvironment(deployment.get("n").asNode().id());
            result.add(DeploymentNodeDTO.builder()
                               .id(deployment.get("n").asNode().id())
                               .deploymentName(deployment.get("n").asNode().get("name").asString())
                               .environmentName(environment.get("parent.name").asString())
                               .cmdb(system.get("d").asNode().get("cmdb").asString())
                               .ip(deployment.get("n").asNode().containsKey("ip") ? deployment.get("n")
                                       .asNode()
                                       .get("ip")
                                       .asString() : null)
                               .host(deployment.get("n").asNode().containsKey("host") ? deployment.get("n")
                                       .asNode()
                                       .get("host")
                                       .asString() : null)
                               .build());
        }

        return ResponseEntity.ok(result);
    }

    private Record getParentSoftwareSystem(Long id) {
        Result result = softwareSystemRepository.getParentSystemByDeploymentNodeId(id);
        if (result.hasNext()) {
            Record record = result.next();
            Long parentSystemId = record.get("parent").asNode().id();
            return softwareSystemRepository.getSystemById(parentSystemId).next();
        } else {
            Result parentNodeResult = deploymentNodesRepository.getParentDeploymentNodeId(id);
            Record record = parentNodeResult.next();
            Long parentNodeId = record.get("parent").asNode().id();
            return getParentSoftwareSystem(parentNodeId);
        }
    }

    private Record getParentEnvironment(Long nodeId) {
        Result result = environmentRepository.getDeploymentNodeEnvironmentNameByIdChild(nodeId);
        if (result.hasNext()) {
            return result.next();
        } else {
            Result parentNodeResult = deploymentNodesRepository.getParentDeploymentNodeId(nodeId);
            Record record = parentNodeResult.next();
            Long parentNodeId = record.get("parentNodeId").asLong();
            return getParentEnvironment(parentNodeId);
        }
    }

    public ResponseEntity<List<SearchSoftwareSystemDTO>> getSoftwareSystem(String search) {
        List<SearchSoftwareSystemDTO> result = new ArrayList<>();
        Result softwareSystems = softwareSystemRepository.searchSoftwareSystemsByCMDBorName(search);
        while (softwareSystems.hasNext()) {
            Record softwareSystem = softwareSystems.next();
            result.add(SearchSoftwareSystemDTO.builder()
                               .name(softwareSystem.get("n").asNode().get("name").asString())
                               .cmdb(softwareSystem.get("n").asNode().get("cmdb").asString())
                               .build());
        }
        return ResponseEntity.ok(result);
    }

    public ResponseEntity<InfluenceResponseDTO> getContainerInfluence(String cmdb, String name) {
        if (!softwareSystemRepository.productExists(cmdb)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Long containerId = containerRepository.findContainerIdByParentSystemAndName(name, cmdb);
        if (containerId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Set<String> dependentSystems = new HashSet<>(softwareSystemRepository.findDependentSystemsByContainerId(
                containerId));

        Set<String> influencingSystems = new HashSet<>(softwareSystemRepository.findInfluencingSystemsByNodeId(
                containerId));

        List<Long> components = componentRepository.findComponentsByContainer(containerId);
        for (Long componentId : components) {
            dependentSystems.addAll(softwareSystemRepository.findDependentChildSystemsByComponent(componentId));
            influencingSystems.addAll(softwareSystemRepository.findDependentParentSystemsByComponent(componentId));
        }

        return ResponseEntity.ok(InfluenceResponseDTO.builder()
                                         .dependentSystems(new ArrayList<>(dependentSystems))
                                         .influencingSystems(new ArrayList<>(influencingSystems))
                                         .build());
    }
}
