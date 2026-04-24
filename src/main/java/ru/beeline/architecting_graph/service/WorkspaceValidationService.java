/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.client.DocumentClient;
import ru.beeline.architecting_graph.dto.WorkspaceValidationResponseDto;
import ru.beeline.architecting_graph.exception.ValidationException;

@Service
@RequiredArgsConstructor
public class WorkspaceValidationService {

    private final DocumentClient documentClient;
    private final ObjectMapper objectMapper;

    public WorkspaceValidationResponseDto validateByDocId(long docId) {
        String json = documentClient.getDocument(docId);

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new ValidationException("Файл не соответствует json формату");
        }

        JsonNode model = root.get("model");
        if (model == null || model.isNull() || model.isMissingNode()) {
            throw new ValidationException("В файле отсутствует описание Model");
        }

        JsonNode properties = model.get("properties");
        String workspaceCmdb = textOrNull(properties, "workspace_cmdb");
        if (workspaceCmdb == null || workspaceCmdb.isBlank()) {
            throw new ValidationException("Для Model не задано обязательное свойство `workspace_cmdb`");
        }

        JsonNode softwareSystems = model.get("softwareSystems");
        if (softwareSystems == null || !softwareSystems.isArray() || softwareSystems.isEmpty()) {
            throw new ValidationException("В Model не описан ни один SoftwareSystem");
        }

        int matches = 0;
        for (JsonNode ss : softwareSystems) {
            String cmdb = textOrNull(ss.get("properties"), "cmdb");
            if (workspaceCmdb.equals(cmdb)) {
                matches++;
            }
        }
        if (matches == 0) {
            throw new ValidationException("Не описан SoftwareSystem cо свойством `cmdb` равным `workspace_cmdb` из свойства Model");
        }
        if (matches > 1) {
            throw new ValidationException("Описано несколько SoftwareSystem cо свойством `cmdb` равным `workspace_cmdb` из свойства Model");
        }

        for (JsonNode ss : softwareSystems) {
            String ssName = textOrNull(ss, "name");
            if (ssName == null || ssName.isBlank()) {
                throw new ValidationException("Описан SoftwareSystem без заполненного name");
            }

            JsonNode containers = ss.get("containers");
            if (containers != null && containers.isArray() && !containers.isEmpty()) {
                for (JsonNode container : containers) {
                    String containerName = textOrNull(container, "name");
                    if (containerName == null || containerName.isBlank()) {
                        throw new ValidationException("Для контейнера системы " + ssName + " не указан name"
                        );
                    }

                    JsonNode components = container.get("components");
                    if (components != null && components.isArray() && !components.isEmpty()) {
                        for (JsonNode component : components) {
                            String componentName = textOrNull(component, "name");
                            if (componentName == null || componentName.isBlank()) {
                                throw new ValidationException("Для компонента контейнера " + containerName + " в системе " + ssName + " не указан name"
                                );
                            }
                        }
                    }
                }
            }
        }

        JsonNode deploymentNodes = model.get("deploymentNodes");
        if (deploymentNodes != null && deploymentNodes.isArray() && !deploymentNodes.isEmpty()) {
            for (JsonNode dn : deploymentNodes) {
                validateDeploymentNodeRecursive(dn);
            }
        }

        return new WorkspaceValidationResponseDto(true, workspaceCmdb);
    }

    private void validateDeploymentNodeRecursive(JsonNode dn) {
        String name = textOrNull(dn, "name");
        if (name == null || name.isBlank()) {
            throw new ValidationException("Не для всех deploymentNode указан name");
        }

        String env = textOrNull(dn, "environment");
        if (env == null || env.isBlank()) {
            throw new ValidationException("Не для всех deploymentNode указан environment");
        }

        JsonNode children = dn.get("children");
        if (children != null && children.isArray() && !children.isEmpty()) {
            for (JsonNode child : children) {
                validateDeploymentNodeRecursive(child);
            }
        }

        JsonNode containerInstances = dn.get("containerInstances");
        if (containerInstances != null && containerInstances.isArray() && !containerInstances.isEmpty()) {
            for (JsonNode ci : containerInstances) {
                String ciEnv = textOrNull(ci, "environment");
                if (ciEnv == null || ciEnv.isBlank()) {
                    throw new ValidationException("Не для всех containerInstance указан environment");
                }

                JsonNode containerId = ci.get("containerId");
                if (containerId == null || containerId.isNull() || containerId.isMissingNode()) {
                    throw new ValidationException("Не для всех containerInstance указан развернутый Containert");
                }
            }
        }

        JsonNode infrastructureNodes = dn.get("infrastructureNodes");
        if (infrastructureNodes != null && infrastructureNodes.isArray() && !infrastructureNodes.isEmpty()) {
            for (JsonNode ii : infrastructureNodes) {
                String iiEnv = textOrNull(ii, "environment");
                if (iiEnv == null || iiEnv.isBlank()) {
                    throw new ValidationException("Не для всех infrastructureNodes указан environment");
                }

                String iiName = textOrNull(ii, "name");
                if (iiName == null || iiName.isBlank()) {
                    throw new ValidationException("Не для всех infrastructureNodes указан name");
                }
            }
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || v.isMissingNode()) {
            return null;
        }
        return v.isTextual() ? v.asText() : v.asText(null);
    }
}

