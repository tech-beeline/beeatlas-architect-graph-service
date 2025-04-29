package ru.beeline.architecting_graph.service.graph.workspace;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.beeline.architecting_graph.model.Workspace;

public class WorkspaceFunctions {

    public static Workspace getWorkspaceFileForTest(ObjectMapper objectMapper) throws Exception {
        String FilePath = "workspace_RNC.json";
        File file = new File(FilePath);
        Workspace workspace = objectMapper.readValue(file, Workspace.class);
        return workspace;
    }

    public static Workspace getWorkspace(String json, ObjectMapper objectMapper) throws Exception {
        Workspace workspace;
        try {
            workspace = objectMapper.readValue(json, Workspace.class);
        } catch (Exception e) {
            throw e;
        }
        return workspace;
    }
}
