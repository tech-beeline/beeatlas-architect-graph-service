package ru.beeline.architecting_graph.service.graph.softwareSystem;

import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.model.Model;
import ru.beeline.architecting_graph.model.Relationship;
import ru.beeline.architecting_graph.model.SoftwareSystem;
import ru.beeline.architecting_graph.service.graph.CommonFunctions;
import ru.beeline.architecting_graph.service.graph.container.ContainerUpdateFunctions;
import ru.beeline.architecting_graph.service.graph.relationship.RelationshipUpdateFunctions;

import java.util.HashMap;
import java.util.Map;

public class SoftwareSystemUpdateFunctions {

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

    public static String updateSystem(Session session, String graphTag, SoftwareSystem softwareSystem, String cmdb,
                                      HashMap<String, GraphObject> objects) {

        GraphObject systemGraphObject = new GraphObject("SoftwareSystem", "cmdb", cmdb);

        boolean exists = CommonFunctions.checkIfObjectExists(session, graphTag, systemGraphObject);

        if (!exists) {
            CommonFunctions.createObject(session, graphTag, systemGraphObject);
        }

        objects.put(softwareSystem.getId(), systemGraphObject);

        return setParametersForSystem(session, graphTag, softwareSystem, cmdb, systemGraphObject);
    }

    public static void updateSystemRelationships(Session session, String graphTag, Model model, String cmdb,
                                                 String curVersion, HashMap<String, GraphObject> objects) {

        for (SoftwareSystem softwareSystem : model.getSoftwareSystems()) {

            if (softwareSystem.getRelationships() != null) {
                for (Relationship relationship : softwareSystem.getRelationships()) {
                    if (relationship.getLinkedRelationshipId() == null) {
                        RelationshipUpdateFunctions.updateDefaultRelationship(session, graphTag, relationship, model,
                                curVersion, cmdb, "C1", objects);
                    }
                }
            }

            ContainerUpdateFunctions.updateContainerRelationships(session, graphTag, model, softwareSystem, cmdb,
                    curVersion, objects);
        }
    }
}
