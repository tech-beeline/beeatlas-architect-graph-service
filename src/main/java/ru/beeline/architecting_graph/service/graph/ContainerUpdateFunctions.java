package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.model.*;
import ru.beeline.architecting_graph.repository.neo4j.BuildGraphQuery;

import java.util.HashMap;
import java.util.Map;

@Service
public class ContainerUpdateFunctions {

    @Autowired
    DeploymentNodeUpdateFunctions deploymentNodeUpdateFunctions;

    @Autowired
    ComponentUpdateService componentUpdateService;

    @Autowired
    BuildGraphQuery buildGraphQuery;

    @Autowired
    CreateExternalObjects createExternalObjects;

    public void createGraph(Session session, String graphTag, Workspace workspace) throws Exception {
        Model model = workspace.getModel();
        String cmdb = model.getProperties().get("workspace_cmdb").toString();
        SoftwareSystem softwareSystem = getSoftwareSystem(model, cmdb);
        HashMap<String, GraphObject> objects = new HashMap<>();
        String curVersion = null;
        if (graphTag.equals("Global")) {
            curVersion = updateSystem(session, graphTag, softwareSystem, cmdb, objects);
            Integer prevVersion = Integer.parseInt(curVersion) - 1;
            setEndVersion(session, graphTag, cmdb, prevVersion.toString());
        } else {
            graphTag = "Local " + cmdb;
            buildGraphQuery.deleteGraph(session, graphTag);
            updateSystem(session, graphTag, softwareSystem, cmdb, objects);
        }
        updateContainers(session, graphTag, model, softwareSystem, cmdb, curVersion, objects);
        updateSystemRelationships(session, graphTag, model, cmdb, curVersion,
                objects);
        updateDeploymentNodes(session, graphTag, model, softwareSystem.getId(),
                cmdb, curVersion, objects);
        if (model.getDeploymentNodes() != null) {
            for (DeploymentNode deploymentNode : model.getDeploymentNodes()) {
                deploymentNodeUpdateFunctions.updateDeploymentNodeRelationships(session, graphTag, deploymentNode,
                        curVersion, cmdb, model, objects);
            }
        }
    }

    public SoftwareSystem getSoftwareSystem(Model model, String cmdb) {
        for (SoftwareSystem softwareSystem : model.getSoftwareSystems()) {
            if (softwareSystem.getProperties() != null && softwareSystem.getProperties().get("cmdb") != null
                    && softwareSystem.getProperties().get("cmdb").equals(cmdb)) {
                return softwareSystem;
            }
        }
        return null;
    }

    public String updateSystem(Session session, String graphTag, SoftwareSystem softwareSystem, String cmdb,
                               HashMap<String, GraphObject> objects) {
        GraphObject systemGraphObject = new GraphObject("SoftwareSystem", "cmdb", cmdb);
        boolean exists = buildGraphQuery.checkIfObjectExists(session, graphTag, systemGraphObject);
        if (!exists) {
            buildGraphQuery.createObject(session, graphTag, systemGraphObject);
        }
        objects.put(softwareSystem.getId(), systemGraphObject);
        return setParametersForSystem(session, graphTag, softwareSystem, cmdb, systemGraphObject);
    }

    public String setParametersForSystem(Session session, String graphTag, SoftwareSystem softwareSystem,
                                         String cmdb, GraphObject systemGraphObject) {
        String version = null;
        if (graphTag.equals("Global")) {
            version = getSystemVersion(session, graphTag, systemGraphObject).toString();
        }
        buildGraphQuery.setSystemParameters(session, graphTag, cmdb, softwareSystem, version);
        setSystemProperties(session, graphTag, softwareSystem, cmdb);
        return version;
    }

    public void setParametersForContainer(Session session, String graphTag, Container container,
                                          GraphObject containerGraphObject, String curVersion) {
        if (graphTag.equals("Global")
                && buildGraphQuery.getObjectParameter(session, graphTag, containerGraphObject, "startVersion")
                .toString().equals("NULL")) {
            buildGraphQuery.setObjectParameter(session, graphTag, containerGraphObject, "startVersion", curVersion);
        }
        buildGraphQuery.setContainerParameters(session, graphTag, container, containerGraphObject, curVersion);
        setContainerProperties(session, graphTag, container);
    }

    public void setContainerProperties(Session session, String graphTag, Container container) {
        if (container.getProperties() != null) {
            for (Map.Entry<String, Object> entry : container.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replaceAll("[^a-zA-Z0-9]", "_");
                buildGraphQuery.executeSetContainerProperties(session, graphTag, container.getName(), key,
                        entry.getValue());
            }
        }
    }

    public Integer getContainerNumberOfConnects(Session session, String graphTag, String containerExternalName) {
        Result result = buildGraphQuery.getContainerRelationships(session, graphTag, containerExternalName);
        String numberOfRelationships = result.next().get("numberOfRelationships").toString();
        if (numberOfRelationships.equals("NULL")) {
            return 0;
        }
        return Integer.parseInt(numberOfRelationships);
    }

    public Boolean needContainerReplace(Session session, String graphTag, Container container,
                                        GraphObject externalContainerGraphObject) {
        Integer numberOfRelationshipsFirst = getContainerNumberOfConnects(session, graphTag,
                externalContainerGraphObject.getValue());
        String source = buildGraphQuery.getObjectParameter(session, graphTag, externalContainerGraphObject, "source")
                .toString();
        Integer numberOfRelationshipsSecond = 0;
        if (container.getRelationships() != null) {
            numberOfRelationshipsSecond = container.getRelationships().size();
        }
        if (container.getComponents() != null) {
            numberOfRelationshipsSecond = numberOfRelationshipsSecond + container.getComponents().size();
        }
        if (numberOfRelationshipsFirst >= numberOfRelationshipsSecond || source.equals("\"landscape\"")) {
            return false;
        }
        return true;
    }

    public Boolean changeContainer(Session session, String graphTag, Container container,
                                   GraphObject externalContainerGraphObject, String curVersion, HashMap<String, GraphObject> objects) {
        String startVersion = buildGraphQuery
                .getObjectParameter(session, graphTag, externalContainerGraphObject, "startVersion").toString();
        if (startVersion.equals("NULL")) {
            buildGraphQuery.setObjectParameter(session, graphTag, externalContainerGraphObject, "name",
                    container.getName());
            return true;
        }
        String endVersion = buildGraphQuery
                .getObjectParameter(session, graphTag, externalContainerGraphObject, "endVersion").toString();
        if (endVersion.equals("NULL")) {
            return needContainerReplace(session, graphTag, container, externalContainerGraphObject);
        }
        return true;
    }

    public void updateContainer(Session session, String graphTag, Container container, String curVersion,
                                HashMap<String, GraphObject> objects) {
        boolean externalExists = false;
        GraphObject externalContainerGraphObject = null;
        if (container.getProperties() != null && container.getProperties().containsKey("external_name")
                && container.getProperties().get("external_name") != null) {
            String containerExternalName = container.getProperties().get("external_name").toString();
            externalContainerGraphObject = new GraphObject("Container", "external_name",
                    containerExternalName);
            externalExists = buildGraphQuery.checkIfObjectExists(session, graphTag, externalContainerGraphObject);
        }
        if (externalExists) {
            if (changeContainer(session, graphTag, container, externalContainerGraphObject, curVersion, objects)) {
                buildGraphQuery.setObjectParameter(session, graphTag, externalContainerGraphObject, "external_name",
                        null);
            } else {
                container.getProperties().put("external_name", null);
            }
        }
        GraphObject containerGraphObject = new GraphObject("Container", "name", container.getName());
        boolean exists = buildGraphQuery.checkIfObjectExists(session, graphTag, containerGraphObject);
        if (!exists) {
            buildGraphQuery.createObject(session, graphTag, containerGraphObject);
        }
        objects.put(container.getId(), containerGraphObject);
        setParametersForContainer(session, graphTag, container, containerGraphObject, curVersion);
    }

    public void updateContainers(Session session, String graphTag, Model model, SoftwareSystem softwareSystem,
                                 String cmdb, String curVersion, HashMap<String, GraphObject> objects) {
        if (softwareSystem.getContainers() != null) {
            for (Container container : softwareSystem.getContainers()) {
                container.setName(container.getName() + "." + cmdb.toString());
                String containerExternalName = cmdb;
                if (container.getProperties() != null && container.getProperties().containsKey("external_name")
                        && container.getProperties().get("external_name") != null) {
                    containerExternalName = container.getProperties().get("external_name").toString() + "."
                            + cmdb;
                    container.getProperties().put("external_name", containerExternalName);
                }
                updateContainer(session, graphTag, container, curVersion, objects);
                createExternalObjects.updateChildRelationship(session, graphTag, model, curVersion,
                        softwareSystem.getId(), container.getId(), cmdb, objects);
                componentUpdateService.updateComponents(session, graphTag, model, container, cmdb, curVersion,
                        containerExternalName, objects);
            }
        }
    }

    public void setContainerEndVersion(Session session, String graphTag, String cmdb, String curVersion) {
        Result result = buildGraphQuery.getContainers(session, graphTag, cmdb);
        while (result.hasNext()) {
            String containerName = result.next().get("containerName").toString();
            containerName = containerName.substring(1, containerName.length() - 1);

            GraphObject containerGraphObject = new GraphObject("Container", "name", containerName);
            buildGraphQuery.setObjectParameter(session, graphTag, containerGraphObject, "endVersion", curVersion);
            setComponentEndVersion(session, graphTag, containerName, cmdb, curVersion);
        }
    }

    public void setComponentEndVersion(Session session, String graphTag, String containerName, String cmdb, String curVersion) {
        Result result = buildGraphQuery.findComponentNamesWithNullEndVersion(session, graphTag, containerName);
        while (result.hasNext()) {
            String rawName = result.next().get("componentName").toString();
            String cleanedName = rawName.substring(1, rawName.length() - 1);
            GraphObject componentGraphObject = new GraphObject("Component", "name", cleanedName);
            buildGraphQuery.setObjectParameter(session, graphTag, componentGraphObject, "endVersion", curVersion);
        }
    }

    public void updateContainerRelationships(Session session, String graphTag, Model model,
                                             SoftwareSystem softwareSystem, String cmdb, String curVersion, HashMap<String, GraphObject> objects) {
        if (softwareSystem.getContainers() != null) {
            for (Container container : softwareSystem.getContainers()) {
                if (container.getRelationships() != null) {
                    for (RelationshipEntity relationship : container.getRelationships()) {
                        if (relationship.getLinkedRelationshipId() == null) {
                            createExternalObjects.updateDefaultRelationship(session, graphTag, relationship,
                                    model, curVersion, cmdb, "C2", objects);
                        }
                    }
                }
                componentUpdateService.updateComponentRelationships(session, graphTag, model, container, cmdb,
                        curVersion, objects);
            }
        }
    }

    public void setEndVersion(Session session, String graphTag, String cmdb, String curVersion) {
        setContainerEndVersion(session, graphTag, cmdb, curVersion);
        Result result = buildGraphQuery.getDeploymentNodeNames(session, graphTag, cmdb);
        while (result.hasNext()) {
            String deploymentNodeName = result.next().get("deploymentNodeName").toString();
            deploymentNodeName = deploymentNodeName.substring(1, deploymentNodeName.length() - 1);
            deploymentNodeUpdateFunctions.setDeploymentNodeEndVersion(session, graphTag, deploymentNodeName,
                    curVersion, cmdb);
        }
        setRelationshipsEndVersion(session, graphTag, curVersion, cmdb);
    }

    public GraphObject getGraphObject(Value graphNode) {
        String label = graphNode.asNode().labels().toString();
        String type = label.substring(1, label.length() - 1);
        String key = "name";
        if (type.equals("SoftwareSystem")) {
            key = "cmdb";
        }
        Object value = graphNode.asNode().asMap().get(key);
        if (value == null) {
            value = graphNode.asNode().asMap().get("external_name");
        }
        return new GraphObject(type, key, value.toString());
    }

    public void setRelationshipsEndVersion(Session session, String graphTag, String curVersion, String cmdb) {
        Result result = buildGraphQuery.getRelationships(session, graphTag, cmdb);
        while (result.hasNext()) {
            Record record = result.next();
            Connection connection = new Connection();
            connection.setSource(getGraphObject(record.get("n")));
            connection.setDestination(getGraphObject(record.get("m")));
            connection.setCmdb(cmdb);
            Value connectValue = record.get("r");
            connection.setRelationshipType(connectValue.asRelationship().type().toString());
            String relationshipDescription = connectValue.asRelationship().get("description").toString();
            relationshipDescription = relationshipDescription.substring(1, relationshipDescription.length() - 1);
            buildGraphQuery.setRelationshipParameter(session, graphTag, relationshipDescription, connection,
                    "endVersion", curVersion);
        }
    }

    public void updateDeploymentNodes(Session session, String graphTag, Model model, String softwareSystemId,
                                      String cmdb, String curVersion, HashMap<String, GraphObject> objects) {
        if (model.getDeploymentNodes() != null) {
            for (DeploymentNode deploymentNode : model.getDeploymentNodes()) {
                deploymentNode.setName(deploymentNode.getName() + "." + cmdb.toString());
                deploymentNodeUpdateFunctions.updateDeploymentNode(session, graphTag, deploymentNode, curVersion, cmdb,
                        model,
                        objects);
                createExternalObjects.updateChildRelationship(session, graphTag, model,
                        curVersion, softwareSystemId, deploymentNode.getId(), cmdb, objects);
            }
        }
    }

    public Integer getSystemVersion(Session session, String graphTag, GraphObject systemGraphObject) {
        String version = buildGraphQuery.getObjectParameter(session, graphTag, systemGraphObject, "version").toString();
        if (version.equals("NULL")) {
            version = "0";
            buildGraphQuery.setObjectParameter(session, graphTag, systemGraphObject, "version", "0");
        } else {
            version = version.substring(1, version.length() - 1);
        }
        return (Integer.parseInt(version) + 1);
    }

    public void setSystemProperties(Session session, String graphTag, SoftwareSystem softwareSystem, String cmdb) {
        if (softwareSystem.getProperties() != null) {
            for (Map.Entry<String, Object> entry : softwareSystem.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replaceAll("[^a-zA-Z0-9]", "_");
                buildGraphQuery.updateSystemProperty(session, graphTag, cmdb, key, entry.getValue());
            }
        }
    }

    public void updateSystemRelationships(Session session, String graphTag, Model model, String cmdb,
                                          String curVersion, HashMap<String, GraphObject> objects) {
        for (SoftwareSystem softwareSystem : model.getSoftwareSystems()) {
            if (softwareSystem.getRelationships() != null) {
                for (RelationshipEntity relationship : softwareSystem.getRelationships()) {
                    if (relationship.getLinkedRelationshipId() == null) {
                        createExternalObjects.updateDefaultRelationship(session, graphTag, relationship, model,
                                curVersion, cmdb, "C1", objects);
                    }
                }
            }
            updateContainerRelationships(session, graphTag, model, softwareSystem, cmdb,
                    curVersion, objects);
        }
    }
}
