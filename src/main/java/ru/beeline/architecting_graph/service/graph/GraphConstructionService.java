package ru.beeline.architecting_graph.service.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.server.ResponseStatusException;
import ru.beeline.architecting_graph.client.DocumentClient;
import ru.beeline.architecting_graph.dto.DeploymentNodeDTO;
import ru.beeline.architecting_graph.dto.TaskCacheDTO;
import ru.beeline.architecting_graph.model.DeploymentNode;
import ru.beeline.architecting_graph.model.Environment;
import ru.beeline.architecting_graph.model.SoftwareSystem;
import ru.beeline.architecting_graph.model.Workspace;
import ru.beeline.architecting_graph.repository.neo4j.DeploymentNodesRepository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GraphConstructionService {

    @Autowired
    private Driver driver;

    @Autowired
    DocumentClient documentClient;

    @Autowired
    ContainerUpdateFunctions containerUpdateFunctions;

    @Autowired
    RedisTemplate<String, TaskCacheDTO> redisTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    DeploymentNodesRepository deploymentNodesRepository;

    public ResponseEntity<String> graphConstruct(Long docId, String graphTag) {
        try (Session session = driver.session()) {
            log.info("graphConstruct is running");
            session.run("RETURN 1");
            String workspaceJson = getWorkspaceJson(docId);
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
                containerUpdateFunctions.createGraph(session, graphTag, workspace);
            } catch (Exception e) {
                log.info("Граф не построен: " + e.getMessage());
                return ResponseEntity.badRequest().body("Граф не построен\n" + e.getMessage());
            }
            log.info("graph constructed");
            return ResponseEntity.status(HttpStatus.CREATED).body("Граф построен");

        } catch (ServiceUnavailableException e) {
            log.info("Нет подключения к БД: " + e.getMessage());
            return ResponseEntity.badRequest().body("Нет подключения к БД");
        }
    }

    private String getWorkspaceJson(Long docId) {
        try {
            return documentClient.getDocument(docId);
        } catch (HttpClientErrorException e) {
            return handleClientError(e);
        } catch (HttpServerErrorException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
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
        Result deploymentNodes = getGlobalDeploymentNodeLikeName();
//        while (deploymentNodes.hasNext()) {
//            Record record = deploymentNodes.next();
//            getContainerInstance(session, record.get("m").asNode(), environment, diagramParameters);
//            SoftwareSystem system = getParentSoftwareSystem();
//            Environment environment = getParentEnvironment();
//            result.add(DeploymentNodeDTO.builder()
//                               .deploymentName(node.getName())
//                               .environmentName(environment.getName())
//                               .cmdb(system.getCmdb()).build())
//        }
//
        return ResponseEntity.ok(result);
    }

    private Result getGlobalDeploymentNodeLikeName() {
        return deploymentNodesRepository.findDeploymentNodesBySearch(null, null);
    }

    private SoftwareSystem getParentSoftwareSystem() {
        //если к найденной DeploymentNode идет прямая связь типа child от SoftwareSystem, она считается родительской
        //если такой нет
        //  найти родительский DeploymentNode от которой есть связь child к найденной, определить для нее родительский SoftwareSystem
        //  если такого нет, найти ее родительский DeploymentNode и повторять рекурсивно пока не будет найден родительский SoftwareSystem
        return null;
    }

    private Environment getParentEnvironment() {
        //если к найденной DeploymentNode идет прямая связь типа child от Environment, она считается родительской
        //если такой нет
        //  найти родительский DeploymentNode от которой есть связь child к найденной, определить для нее родительский Environment
        //  если такого нет, найти ее родительский DeploymentNode и повторять рекурсивно пока не будет найден родительский Environment
        return null;
    }
}
