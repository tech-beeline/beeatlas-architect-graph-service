package ru.beeline.architecting_graph.service.graph.container;

import java.util.HashMap;

import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.model.*;
import ru.beeline.architecting_graph.service.graph.relationship.RelationshipUpdateFunctions;
import ru.beeline.architecting_graph.service.graph.component.ComponentRelationFunctions;

public class ContainerRelationshipFunctions {

    public static void updateContainerRelationships(Session session, String graphTag, Model model,
            SoftwareSystem softwareSystem, String cmdb, String curVersion, HashMap<String, GraphObject> objects) {

        if (softwareSystem.getContainers() != null) {
            for (Container container : softwareSystem.getContainers()) {

                if (container.getRelationships() != null) {
                    for (Relationship relationship : container.getRelationships()) {
                        if (relationship.getLinkedRelationshipId() == null) {
                            RelationshipUpdateFunctions.updateDefaultRelationship(session, graphTag, relationship,
                                    model, curVersion, cmdb, "C2", objects);
                        }
                    }
                }

                ComponentRelationFunctions.updateComponentRelationships(session, graphTag, model, container, cmdb,
                        curVersion, objects);
            }
        }
    }
}
