package ru.beeline.architecting_graph.graph.softwareSystem;

import java.util.HashMap;

import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.graph.graphObject.GraphObject;
import ru.beeline.architecting_graph.graph.model.Model;
import ru.beeline.architecting_graph.graph.relationship.Relationship;
import ru.beeline.architecting_graph.graph.relationship.RelationshipUpdateFunctions;
import ru.beeline.architecting_graph.graph.container.ContainerRelationshipFunctions;

public class SoftwareSystemRelationshipFunctions {

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

            ContainerRelationshipFunctions.updateContainerRelationships(session, graphTag, model, softwareSystem, cmdb,
                    curVersion, objects);
        }
    }
}
