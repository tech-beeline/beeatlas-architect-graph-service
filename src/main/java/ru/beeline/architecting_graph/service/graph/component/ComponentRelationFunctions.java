package ru.beeline.architecting_graph.service.graph.component;

import java.util.HashMap;

import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.model.Component;
import ru.beeline.architecting_graph.model.Container;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.model.Model;
import ru.beeline.architecting_graph.model.Relationship;
import ru.beeline.architecting_graph.service.graph.relationship.RelationshipUpdateFunctions;

public class ComponentRelationFunctions {

    public static void updateComponentRelationships(Session session, String graphTag, Model model, Container container,
                                                    String cmdb, String curVersion, HashMap<String, GraphObject> objects) {

        if (container.getComponents() != null) {
            for (Component component : container.getComponents()) {

                if (component.getRelationships() != null) {
                    for (Relationship relationship : component.getRelationships()) {
                        if (relationship.getLinkedRelationshipId() == null) {
                            RelationshipUpdateFunctions.updateDefaultRelationship(session, graphTag, relationship,
                                    model, curVersion, cmdb, "C3", objects);
                        }
                    }
                }
            }
        }
    }
}
