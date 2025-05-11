package ru.beeline.architecting_graph.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.architecting_graph.model.GraphObject;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Connection {

    private GraphObject source;
    private GraphObject destination;
    private String relationshipType;
    private String level;
    private String cmdb;
}
