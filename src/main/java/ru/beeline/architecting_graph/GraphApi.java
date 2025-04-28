package ru.beeline.architecting_graph;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.ResponseEntity;

import ru.beeline.architecting_graph.otherObjects.RestConfig;
import ru.beeline.architecting_graph.graph.graphConstruction.GraphConstruction;
import ru.beeline.architecting_graph.createDiagrams.CreateDiagrams;
import ru.beeline.architecting_graph.compareVersions.CompareVersions;

@RestController
@RequestMapping("/api/v1")
public class GraphApi {

    private final RestConfig autorization;

    public GraphApi(RestConfig autorization) {
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

    @GetMapping("/diff/{cmdb_code}/{v1}/{v2}")
    public ResponseEntity<String> compareVersions(@PathVariable String cmdb, @PathVariable Integer firstVersion,
            @PathVariable(required = false) Integer secondVersion) {

        return CompareVersions.compareVersion(autorization, cmdb, firstVersion, secondVersion);
    }

    @GetMapping("/diff/{cmdb_code}/{v1}")
    public ResponseEntity<String> compareWithCur(@PathVariable String cmdb, @PathVariable Integer firstVersion) {
        return CompareVersions.compareVersion(autorization, cmdb, firstVersion, null);
    }
}
