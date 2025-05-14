package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
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
    ComponentUpdateFunctions componentUpdateFunctions;

    @Autowired
    BuildGraphQuery buildGraphQuery;

    @Autowired
    CreateExternalObjects createExternalObjects;

    public void setContainerProperties(Session session, String graphTag, Container container) {
        if (container.getProperties() != null) {
            for (Map.Entry<String, Object> entry : container.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replaceAll("[^a-zA-Z0-9]", "_");
                String setProperties = "MATCH (n:Container {graphTag: $graphTag1, name: $name1}) SET n." + key
                        + " = $value";
                Value parameters = Values.parameters("graphTag1", graphTag, "name1", container.getName(),
                        "value", entry.getValue());
                session.run(setProperties, parameters);
            }
        }
    }

    public void setParametersForContainer(Session session, String graphTag, Container container,
                                          GraphObject containerGraphObject, String curVersion) {

        if (graphTag.equals("Global")
                && CommonFunctions.getObjectParameter(session, graphTag, containerGraphObject, "startVersion")
                .toString().equals("NULL")) {
            CommonFunctions.setObjectParameter(session, graphTag, containerGraphObject, "startVersion", curVersion);
        }
        String setParameters = "MATCH (n:Container {graphTag: $graphTag1, " + containerGraphObject.getKey()
                + ": $value}) " + "SET n.description = $description1, n.technology = $technology1, n.tags = $tags1, "
                + "n.url = $url1, n.group = $group1, n.endVersion = $endVersion1";
        Value parameters = Values.parameters("graphTag1", graphTag, "value", containerGraphObject.getValue(),
                "description1", container.getDescription(), "technology1", container.getTechnology(), "tags1",
                container.getTags(), "url1", container.getUrl(), "group1", container.getGroup(), "endVersion1", null);
        session.run(setParameters, parameters);
        setContainerProperties(session, graphTag, container);
    }

    public Integer getContainerNumberOfConnects(Session session, String graphTag, String containerExternalName) {
        String findConnects = "MATCH (n:Container {graphTag: $graphTag1, external_name: $external_name1})-[r]-() "
                + "RETURN count(r) AS numberOfRelationships";
        Value parameters = Values.parameters("graphTag1", graphTag, "external_name1", containerExternalName);
        Result result = session.run(findConnects, parameters);
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
        String source = CommonFunctions.getObjectParameter(session, graphTag, externalContainerGraphObject, "source")
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
        String startVersion = CommonFunctions
                .getObjectParameter(session, graphTag, externalContainerGraphObject, "startVersion").toString();
        if (startVersion.equals("NULL")) {
            CommonFunctions.setObjectParameter(session, graphTag, externalContainerGraphObject, "name",
                    container.getName());
            return true;
        }
        String endVersion = CommonFunctions
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
                CommonFunctions.setObjectParameter(session, graphTag, externalContainerGraphObject, "external_name",
                        null);
            } else {
                container.getProperties().put("external_name", null);
            }
        }
        GraphObject containerGraphObject = new GraphObject("Container", "name", container.getName());
        boolean exists = buildGraphQuery.checkIfObjectExists(session, graphTag, containerGraphObject);
        if (!exists) {
            CommonFunctions.createObject(session, graphTag, containerGraphObject);
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
                componentUpdateFunctions.updateComponents(session, graphTag, model, container, cmdb, curVersion,
                        containerExternalName, objects);
            }
        }
    }

    public void setContainerEndVersion(Session session, String graphTag, String cmdb, String curVersion) {

        String getContainers = "MATCH (n:SoftwareSystem {cmdb: $cmdb1, graphTag: $graphTag1})-[r:Child]->(m:Container) "
                + "WHERE m.endVersion IS NULL RETURN m.name AS containerName";
        Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb);
        Result result = session.run(getContainers, parameters);
        while (result.hasNext()) {
            String containerName = result.next().get("containerName").toString();
            containerName = containerName.substring(1, containerName.length() - 1);
            GraphObject containerGraphObject = new GraphObject("Container", "name", containerName);
            CommonFunctions.setObjectParameter(session, graphTag, containerGraphObject, "endVersion", curVersion);
            componentUpdateFunctions.setComponentEndVersion(session, graphTag, containerName, cmdb, curVersion);
        }
    }

    public void updateContainerRelationships(Session session, String graphTag, Model model,
                                             SoftwareSystem softwareSystem, String cmdb, String curVersion, HashMap<String, GraphObject> objects) {
        if (softwareSystem.getContainers() != null) {
            for (Container container : softwareSystem.getContainers()) {
                if (container.getRelationships() != null) {
                    for (Relationship relationship : container.getRelationships()) {
                        if (relationship.getLinkedRelationshipId() == null) {
                            createExternalObjects.updateDefaultRelationship(session, graphTag, relationship,
                                    model, curVersion, cmdb, "C2", objects);
                        }
                    }
                }
                componentUpdateFunctions.updateComponentRelationships(session, graphTag, model, container, cmdb,
                        curVersion, objects);
            }
        }
    }

    public void setEndVersion(Session session, String graphTag, String cmdb, String curVersion) {
        setContainerEndVersion(session, graphTag, cmdb, curVersion);
        String getDeploymentNode = "MATCH (n:SoftwareSystem {cmdb: $cmdb1, graphTag: $graphTag1})-[r:Child]->(m:DeploymentNode) "
                + "WHERE m.endVersion IS NULL RETURN m.name as deploymentNodeName";
        Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb);
        Result result = session.run(getDeploymentNode, parameters);
        while (result.hasNext()) {
            String deploymentNodeName = result.next().get("deploymentNodeName").toString();
            deploymentNodeName = deploymentNodeName.substring(1, deploymentNodeName.length() - 1);
            DeploymentNodeUpdateFunctions.setDeploymentNodeEndVersion(session, graphTag, deploymentNodeName,
                    curVersion, cmdb);
        }
        RelationshipEndVersionFunctions.setRelationshipsEndVersion(session, graphTag, curVersion, cmdb);
    }

    public void deleteLocalGraph(Session session) {
        String deleteLocalGraph = "MATCH (n) WHERE n.graphTag = \"Local\" DETACH DELETE n";
        session.run(deleteLocalGraph);
    }

    public void createGraph(Session session, String graphTag, Workspace workspace) throws Exception {
        Model model = workspace.getModel();
        String cmdb = model.getProperties().get("workspace_cmdb").toString();
        SoftwareSystem softwareSystem = ModelFunctions.getSoftwareSystem(model, cmdb);
        HashMap<String, GraphObject> objects = new HashMap<>();
        String curVersion = null;
        if (graphTag.equals("Global")) {
            curVersion = updateSystem(session, graphTag, softwareSystem, cmdb, objects);
            Integer prevVersion = Integer.parseInt(curVersion) - 1;
            setEndVersion(session, graphTag, cmdb, prevVersion.toString());
        } else {
            deleteLocalGraph(session);
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

    public void updateDeploymentNodes(Session session, String graphTag, Model model, String softwareSystemId,
                                      String cmdb, String curVersion, HashMap<String, GraphObject> objects) {

        if (model.getDeploymentNodes() != null) {
            for (DeploymentNode deploymentNode : model.getDeploymentNodes()) {
                deploymentNode.setName(deploymentNode.getName() + "." + cmdb.toString());
                deploymentNodeUpdateFunctions.updateDeploymentNode(session, graphTag, deploymentNode, curVersion, cmdb, model,
                        objects);

                createExternalObjects.updateChildRelationship(session, graphTag, model,
                        curVersion, softwareSystemId, deploymentNode.getId(), cmdb, objects);
            }
        }
    }

    public static Integer getSystemVersion(Session session, String graphTag, GraphObject systemGraphObject) {
        String version = CommonFunctions.getObjectParameter(session, graphTag, systemGraphObject, "version").toString();
        if (version.equals("NULL")) {
            version = "0";
            CommonFunctions.setObjectParameter(session, graphTag, systemGraphObject, "version", "0");
        } else {
            version = version.substring(1, version.length() - 1);
        }
        return (Integer.parseInt(version) + 1);
    }

    public static void setSystemProperties(Session session, String graphTag, SoftwareSystem softwareSystem,
                                           String cmdb) {
        if (softwareSystem.getProperties() != null) {
            for (Map.Entry<String, Object> entry : softwareSystem.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replaceAll("[^a-zA-Z0-9]", "_");
                String setProperties = "MATCH (n:SoftwareSystem {graphTag: $graphTag1, cmdb: $cmdb1}) SET n." + key
                        + " = $value";
                Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb,
                        "value", entry.getValue());
                session.run(setProperties, parameters);
            }
        }
    }

    public static String setParametersForSystem(Session session, String graphTag, SoftwareSystem softwareSystem,
                                                String cmdb, GraphObject systemGraphObject) {

        String version = null;

        if (graphTag.equals("Global")) {
            version = getSystemVersion(session, graphTag, systemGraphObject).toString();
        }

        String setParameters = "MATCH (n:SoftwareSystem {graphTag: $graphTag1, cmdb: $cmdb1}) "
                + "SET n.name = $name1, n.description = $description1, n.tags = $tags1, n.url = $url1, "
                + "n.group = $group1, n.version = $version1";
        Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb, "name1",
                softwareSystem.getName(), "description1", softwareSystem.getDescription(), "tags1",
                softwareSystem.getTags(), "url1", softwareSystem.getUrl(), "group1", softwareSystem.getGroup(),
                "version1", version);
        session.run(setParameters, parameters);

        setSystemProperties(session, graphTag, softwareSystem, cmdb);
        return version;
    }

    public String updateSystem(Session session, String graphTag, SoftwareSystem softwareSystem, String cmdb,
                               HashMap<String, GraphObject> objects) {

        GraphObject systemGraphObject = new GraphObject("SoftwareSystem", "cmdb", cmdb);

        boolean exists = buildGraphQuery.checkIfObjectExists(session, graphTag, systemGraphObject);

        if (!exists) {
            CommonFunctions.createObject(session, graphTag, systemGraphObject);
        }

        objects.put(softwareSystem.getId(), systemGraphObject);

        return setParametersForSystem(session, graphTag, softwareSystem, cmdb, systemGraphObject);
    }

    public void updateSystemRelationships(Session session, String graphTag, Model model, String cmdb,
                                          String curVersion, HashMap<String, GraphObject> objects) {

        for (SoftwareSystem softwareSystem : model.getSoftwareSystems()) {

            if (softwareSystem.getRelationships() != null) {
                for (Relationship relationship : softwareSystem.getRelationships()) {
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
