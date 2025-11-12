package ru.beeline.architecting_graph.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.architecting_graph.service.createDiagrams.DiagramService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class DiagramController {

    @Autowired
    DiagramService diagramService;


    @GetMapping("/deployment/{environment}/{softwareSystemMnemonic}")
    @Operation(summary = "Генерация json с описанием deploymentView")
    public ResponseEntity<String> getDeploymentDiagramm(@PathVariable String environment,
                                                        @PathVariable String softwareSystemMnemonic,
                                                        @PathVariable(required = false) String rankDirection) {

        return diagramService.createDiagram(softwareSystemMnemonic, null, environment, rankDirection);
    }

    @GetMapping("/context/{softwareSystemMnemonic}")
    @Operation(summary = "Генерация json с описанием contextView")
    public ResponseEntity<String> getContextDiagram(@PathVariable String softwareSystemMnemonic,
                                                    @RequestParam(required = false) String rankDirection) {
        return getC4Diagram(softwareSystemMnemonic, null, rankDirection);
    }

    @GetMapping("/diagram/context")
    @Operation(summary = "Построение context диаграммы V2")
    public ResponseEntity<String> getContextDiagramV2(@RequestParam String cmdb,
                                                      @RequestParam(required = false) String rankDirection,
                                                      @RequestParam String communicationDirection) {
        return diagramService.createContextDiagramV2(cmdb, rankDirection, communicationDirection);
    }

    @GetMapping("/context/{softwareSystemMnemonic}/{containerMnemonic}")
    @Operation(summary = "Генерация json с описанием containerView")
    public ResponseEntity<String> getC4Diagram(@PathVariable String softwareSystemMnemonic,
                                               @PathVariable(required = false) String containerMnemonic,
                                               @RequestParam(required = false) String rankDirection) {

        return diagramService.createDiagram(softwareSystemMnemonic, containerMnemonic, null, rankDirection);
    }

    @GetMapping("/diagram/deployment")
    @Operation(summary = "Построение Deployment диаграммы")
    public ResponseEntity<String> getDiagramDeployment(@RequestParam String cmdb,
                                                       @RequestParam String env,
                                                       @RequestParam(name = "rank-direction", required = false) String rankDirection,
                                                       @RequestParam(name = "deployment-name") String deploymentName) {
        return diagramService.getDiagramDeployment(cmdb, env, rankDirection, deploymentName);
    }

    @GetMapping("/diagram/dot")
    @Operation(summary = "Построение Deployment диаграммы dot")
    public ResponseEntity<String> getDiagramDeploymentDot(@RequestParam Long id) {
        return diagramService.getDiagramDeploymentDot(id);
    }

    @GetMapping("/context/dot")
    @Operation(summary = "Построение Deployment диаграммы с зависимыми систетмами в формате DOT")
    public ResponseEntity<String> getContextDiagramDot(@RequestParam String cmdb) {
        return diagramService.getContextDiagramDot(cmdb);
    }

    @GetMapping("/diagram/elements")
    @Operation(summary = "Построение elements")
    public ResponseEntity<List<Map<String, Object>>> getDiagramDeploymentElements(@RequestParam Long id) {
        return diagramService.getDiagramDeploymentElements(id);
    }
}
