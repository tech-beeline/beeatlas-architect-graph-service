package ru.beeline.architecting_graph.graph.model;

import ru.beeline.architecting_graph.graph.softwareSystem.SoftwareSystem;

public class ModelFunctions {

    public static SoftwareSystem getSoftwareSystem(Model model, String cmdb) {
        for (SoftwareSystem softwareSystem : model.getSoftwareSystems()) {
            if (softwareSystem.getProperties() != null && softwareSystem.getProperties().get("cmdb") != null
                    && softwareSystem.getProperties().get("cmdb").equals(cmdb)) {
                return softwareSystem;
            }
        }
        return null;
    }
}
