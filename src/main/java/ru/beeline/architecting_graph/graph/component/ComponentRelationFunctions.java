package ru.beeline.architecting_graph.graph.component;

import java.util.HashMap;

import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.graph.container.Container;
import ru.beeline.architecting_graph.graph.graphObject.GraphObject;
import ru.beeline.architecting_graph.graph.model.Model;
import ru.beeline.architecting_graph.graph.relationship.Relationship;
import ru.beeline.architecting_graph.graph.relationship.RelationshipUpdateFunctions;

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
