package ru.beeline.architecting_graph.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.architecting_graph.config.RestConfig;
import ru.beeline.architecting_graph.graph.graphConstruction.GraphConstruction;
import ru.beeline.architecting_graph.service.compareVersions.CompareVersions;
import ru.beeline.architecting_graph.service.createDiagrams.CreateDiagrams;

@RestController
@RequestMapping("/api/v1")
public class GraphController {

    @Autowired
    CompareVersions compareVersions;

    private final RestConfig autorization;

    public GraphController(RestConfig autorization) {
        this.autorization = autorization;
    }

    @PostMapping("/graph/local/{docId}")
    public ResponseEntity<String> LocalGraph(@PathVariable("docId") Long docId) {
        return GraphConstruction.graphConstruct(docId, autorization, "Local");
    }

    @PostMapping("/graph/{docId}")
    public ResponseEntity<String> GlobalGraph(@PathVariable("docId") Long docId) {
        return GraphConstruction.graphConstruct(docId, autorization, "Global");
    }

    @GetMapping("/context/{softwareSystemMnemonic}/{containerMnemonic}")
    public ResponseEntity<String> getC4Diagramm(@PathVariable String softwareSystemMnemonic,
                                                @PathVariable(required = false) String containerMnemonic) {

        return CreateDiagrams.createDiagramm(autorization, softwareSystemMnemonic, containerMnemonic, null);
    }

    @GetMapping("/context/{softwareSystemMnemonic}")
    public ResponseEntity<String> getContextDiagramm(@PathVariable String softwareSystemMnemonic) {
        return getC4Diagramm(softwareSystemMnemonic, null);
    }

    @GetMapping("/deployment/{environment}/{softwareSystemMnemonic}")
    public ResponseEntity<String> getDeploymentDiagramm(@PathVariable String environment,
                                                        @PathVariable String softwareSystemMnemonic) {

        return CreateDiagrams.createDiagramm(autorization, softwareSystemMnemonic, null, environment);
    }

    @GetMapping("/diff/{cmdb}/{firstVersion}/{secondVersion}")
    public ResponseEntity<String> compareVersions(@PathVariable String cmdb,
                                                  @PathVariable Integer firstVersion,
                                                  @PathVariable(required = false) Integer secondVersion) {
        return compareVersions.compareVersion(cmdb, firstVersion, secondVersion);
    }

    @GetMapping("/diff/{cmdb}/{firstVersion}")
    public ResponseEntity<String> compareWithCur(@PathVariable String cmdb,
                                                 @PathVariable Integer firstVersion) {
        return compareVersions.compareVersion(cmdb, firstVersion, null);
    }
}
