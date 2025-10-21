package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.model.*;
import ru.beeline.architecting_graph.repository.neo4j.GenericRepository;
import ru.beeline.architecting_graph.repository.neo4j.ComponentRepository;

import java.util.HashMap;
import java.util.Map;

@Service
public class ComponentUpdateService {

    @Autowired
    GenericRepository genericRepository;

    @Autowired
    CreateExternalObjects createExternalObjects;

    @Autowired
    ComponentRepository componentRepository;

    public void setComponentProperties(Session session, String graphTag, Component component) {
        if (component.getProperties() != null) {
            for (Map.Entry<String, Object> entry : component.getProperties().entrySet()) {
                String sanitizedKey = sanitizeKey(entry.getKey());
                componentRepository.setComponentProperty(session, graphTag, component.getName(), sanitizedKey, entry.getValue());
            }
        }
    }

    private String sanitizeKey(String key) {
        return key.replaceAll("[^a-zA-Z0-9]", "_");
    }

    public void updateComponent(Session session, String graphTag, Component component, String curVersion,
                                HashMap<String, GraphObject> objects) {
        boolean externalExists = false;
        GraphObject externalComponentGraphObject = null;
        if (component.getProperties() != null && component.getProperties().containsKey("external_name")
                && component.getProperties().get("external_name") != null) {
            String componentExternalName = component.getProperties().get("external_name").toString();
            externalComponentGraphObject = new GraphObject("Component", "external_name",
                    componentExternalName);
            externalExists = genericRepository.checkIfObjectExists(session, graphTag, externalComponentGraphObject);
        }
        if (externalExists) {
            if (changeComponent(session, graphTag, component, externalComponentGraphObject, curVersion, objects)) {
                genericRepository.setObjectParameter(session, graphTag, externalComponentGraphObject, "external_name",
                                                     null);
            } else {
                component.getProperties().put("external_name", null);
            }
        }
        GraphObject componentGraphObject = new GraphObject("Component", "name", component.getName());
        boolean exists = genericRepository.checkIfObjectExists(session, graphTag, componentGraphObject);

        if (!exists) {
            genericRepository.createObject(session, graphTag, componentGraphObject);
        }
        objects.put(component.getId(), componentGraphObject);
        setParametersForComponent(session, graphTag, component, componentGraphObject, curVersion);
    }


    public void setParametersForComponent(Session session, String graphTag, Component component,
                                          GraphObject componentGraphObject, String curVersion) {
        Value value = genericRepository.getObjectParameter(session, graphTag, componentGraphObject, "startVersion");
        if ("Global".equals(graphTag) && "NULL".equals(String.valueOf(value))) {
            genericRepository.setObjectParameter(session, graphTag, componentGraphObject, "startVersion", curVersion);
        }
        componentRepository.setMainComponentFields(session, graphTag, componentGraphObject, component);
        setComponentProperties(session, graphTag, component);
    }

    public Boolean needComponentReplace(Session session, String graphTag, Component component,
                                        GraphObject externalComponentGraphObject) {
        Integer numberOfRelationshipsFirst = getComponentNumberOfConnects(session, graphTag,
                externalComponentGraphObject.getValue());
        String source = genericRepository.getObjectParameter(session, graphTag, externalComponentGraphObject, "source")
                .toString();
        Integer numberOfRelationshipsSecond = 0;
        if (component.getRelationships() != null) {
            numberOfRelationshipsSecond = component.getRelationships().size();
        }
        if (numberOfRelationshipsFirst >= numberOfRelationshipsSecond || source.equals("\"landscape\"")) {
            return false;
        }
        return true;
    }

    public Integer getComponentNumberOfConnects(Session session, String graphTag, String componentExternalName) {
        return componentRepository.fetchNumberOfConnections(session, graphTag, componentExternalName);
    }

    public Boolean changeComponent(Session session, String graphTag, Component component,
                                   GraphObject externalComponentGraphObject, String curVersion, HashMap<String, GraphObject> objects) {
        String startVersion = genericRepository
                .getObjectParameter(session, graphTag, externalComponentGraphObject, "startVersion").toString();
        if (startVersion.equals("NULL")) {
            genericRepository.setObjectParameter(session, graphTag, externalComponentGraphObject, "name",
                                                 component.getName());
            return true;
        }
        String endVersion = genericRepository
                .getObjectParameter(session, graphTag, externalComponentGraphObject, "endVersion").toString();
        if (endVersion.equals("NULL")) {
            return needComponentReplace(session, graphTag, component, externalComponentGraphObject);
        }
        return true;
    }


    public void updateComponents(Session session, String graphTag, Model model, Container container, String cmdb,
                                 String curVersion, String containerExternalName, HashMap<String, GraphObject> objects) {
        if (container.getComponents() != null) {
            for (Component component : container.getComponents()) {
                component.setName(component.getName() + "." + container.getName());
                if (component.getProperties() != null && component.getProperties().containsKey("external_name")
                        && component.getProperties().get("external_name") != null) {
                    String componentExternalName = component.getProperties().get("external_name").toString() + "."
                            + containerExternalName;
                    component.getProperties().put("external_name", componentExternalName);
                }
                updateComponent(session, graphTag, component, curVersion, objects);
                createExternalObjects.updateChildRelationship(session, graphTag, model, curVersion,
                        container.getId(), component.getId(), cmdb, objects);
            }
        }
    }

    public void updateComponentRelationships(Session session, String graphTag, Model model, Container container,
                                             String cmdb, String curVersion, HashMap<String, GraphObject> objects) {
        if (container.getComponents() != null) {
            for (Component component : container.getComponents()) {
                if (component.getRelationships() != null) {
                    for (RelationshipEntity relationship : component.getRelationships()) {
                        if (relationship.getLinkedRelationshipId() == null) {
                            createExternalObjects.updateDefaultRelationship(session, graphTag, relationship,
                                    model, curVersion, cmdb, "C3", objects);
                        }
                    }
                }
            }
        }
    }
}
