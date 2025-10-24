package ru.beeline.architecting_graph.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.architecting_graph.dto.ContainerNodeDTO;
import ru.beeline.architecting_graph.dto.DeploymentNodeDTO;
import ru.beeline.architecting_graph.dto.SearchSoftwareSystemDTO;
import ru.beeline.architecting_graph.dto.TaskCacheDTO;
import ru.beeline.architecting_graph.service.compareVersions.CompareVersionsService;
import ru.beeline.architecting_graph.service.createDiagrams.ContainerComponentBuilder;
import ru.beeline.architecting_graph.service.createDiagrams.CreateDiagrams;
import ru.beeline.architecting_graph.service.getElements.GetElements;
import ru.beeline.architecting_graph.service.graph.GraphConstructionService;
import ru.beeline.architecting_graph.service.graph.ProductInfluenceService;
import ru.beeline.fdmlib.dto.graph.ProductInfluenceDTO;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1")
public class GraphController {

    @Autowired
    CompareVersionsService compareVersionService;

    @Autowired
    CreateDiagrams createDiagrams;

    @Autowired
    GraphConstructionService graphConstructionService;

    @Autowired
    ProductInfluenceService productInfluenceService;

    @Autowired
    ContainerComponentBuilder containerComponentBuilder;

    @Autowired
    GetElements getElements;

    @GetMapping("/search/deployment-node")
    @Operation(summary = "Поиск deploymentNode")
    public ResponseEntity<List<DeploymentNodeDTO>> getDeploymentNode(@RequestParam String search) {
        return graphConstructionService.getDeploymentNode(search);
    }

    @GetMapping("/search/container")
    @Operation(summary = "Поиск containerNode")
    public ResponseEntity<List<ContainerNodeDTO>> getContainerNode(@RequestParam(required = false) String search) {
        return ResponseEntity.status(HttpStatus.OK).body(containerComponentBuilder.findContainersWithParentCmdb(search));
    }

    @GetMapping("/search/software-system")
    @Operation(summary = "Поиск deploymentNode")
    public ResponseEntity<List<SearchSoftwareSystemDTO>> getSoftwareSystem(@RequestParam String search) {
        return graphConstructionService.getSoftwareSystem(search);
    }

    @GetMapping("/graph/{graph-type}/task/{task-id}")
    @Operation(summary = "Получение статуса графа по taskKey и типу графа")
    public ResponseEntity<TaskCacheDTO> getGraphByTask(@PathVariable("graph-type") String graphType,
                                                       @PathVariable("task-id") String taskId) {
        return graphConstructionService.getGraphByTask(graphType, taskId);
    }

    @PostMapping("/graph/local/{docId}")
    @Operation(summary = "Пересоздание локального графа, используя документ, в котором описывается система (все вершины и связи помечаются graphTag: Local)")
    public ResponseEntity<String> LocalGraph(@PathVariable("docId") Long docId) {
        return graphConstructionService.graphConstruct(docId, "Local");
    }

    @PostMapping("/graph/{docId}")
    @Operation(summary = "Добавление системы из указанного документа в глобальный граф (все вершины и связи помечаются graphTag: Global)")
    public ResponseEntity<String> GlobalGraph(@PathVariable("docId") Long docId) {
        return graphConstructionService.graphConstruct(docId, "Global");
    }

    @GetMapping("/context/{softwareSystemMnemonic}/{containerMnemonic}")
    @Operation(summary = "Генерация json с описанием containerView")
    public ResponseEntity<String> getC4Diagramm(@PathVariable String softwareSystemMnemonic,
                                                @PathVariable(required = false) String containerMnemonic,
                                                @PathVariable(required = false) String rankDirection) {

        return createDiagrams.createDiagramm(softwareSystemMnemonic, containerMnemonic, null, rankDirection);
    }

    @GetMapping("/context/{softwareSystemMnemonic}")
    @Operation(summary = "Генерация json с описанием contextView")
    public ResponseEntity<String> getContextDiagramm(@PathVariable String softwareSystemMnemonic,
                                                     @PathVariable(required = false, value = "LeftRight") String rankDirection) {
        return getC4Diagramm(softwareSystemMnemonic, null, rankDirection);
    }

    @GetMapping("/graph/product/{cmdb}/influence")
    @Operation(summary = "Метод для получения связанных систем")
    public ResponseEntity<ProductInfluenceDTO> getInfluence(@PathVariable String cmdb) {
        try {
            return ResponseEntity.ok(productInfluenceService.getRelatedSystems(cmdb));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/deployment/{environment}/{softwareSystemMnemonic}")
    @Operation(summary = "Генерация json с описанием deploymentView")
    public ResponseEntity<String> getDeploymentDiagramm(@PathVariable String environment,
                                                        @PathVariable String softwareSystemMnemonic,
                                                        @PathVariable(required = false, value = "LeftRight") String rankDirection) {

        return createDiagrams.createDiagramm(softwareSystemMnemonic, null, environment, rankDirection);
    }

    @GetMapping("/diff/{cmdb}/{firstVersion}/{secondVersion}")
    @Operation(summary = "Сравнение двух версий указанной системы")
    public ResponseEntity<String> compareVersions(@PathVariable String cmdb,
                                                  @PathVariable Integer firstVersion,
                                                  @PathVariable(required = false) Integer secondVersion) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(compareVersionService.compareVersion(cmdb, firstVersion, secondVersion));
    }

    @GetMapping("/diff/{cmdb}/{firstVersion}")
    @Operation(summary = "Сравнение указанной версии системы с текущей (последней/актуальной)")
    public ResponseEntity<String> compareWithCur(@PathVariable String cmdb, @PathVariable Integer firstVersion) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(compareVersionService.compareVersion(cmdb, firstVersion, null));
    }

    @GetMapping("/elements")
    public ResponseEntity<String> getElements(@RequestHeader(value = "CYPHER-QUERY") String query) {
        return getElements.processingQuery(query);
    }
}
