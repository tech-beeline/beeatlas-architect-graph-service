package ru.beeline.architecting_graph.service.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
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
import ru.beeline.architecting_graph.dto.TaskCacheDTO;
import ru.beeline.architecting_graph.model.Workspace;

import java.io.File;

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

    public ResponseEntity<String> graphConstruct(Long docId, String graphTag) {
        try (Session session = driver.session()) {
            session.run("RETURN 1");
            String workspaceJson = getWorkspaceJson(docId);
            if (workspaceJson == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Документ не найден");
            }
            Workspace workspace;
            try {
                workspace = objectMapper.readValue(workspaceJson, Workspace.class);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Полученный workspace не валиден");
            }
            try {
                containerUpdateFunctions.createGraph(session, graphTag, workspace);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Граф не построен\n" + e.getMessage());
            }
            return ResponseEntity.status(HttpStatus.CREATED).body("Граф построен");

        } catch (ServiceUnavailableException e) {
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
        String redisKey = "graph:taskKey:" + taskId;

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
}
