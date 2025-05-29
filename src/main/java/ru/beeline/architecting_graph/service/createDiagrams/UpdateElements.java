package ru.beeline.architecting_graph.service.createDiagrams;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.model.Component;
import ru.beeline.architecting_graph.model.Container;
import ru.beeline.architecting_graph.model.DiagramParameters;
import ru.beeline.architecting_graph.model.Model;
import ru.beeline.architecting_graph.model.SoftwareSystem;
import ru.beeline.architecting_graph.model.Workspace;
import ru.beeline.architecting_graph.service.createDiagrams.ViewObjects.Views;

@Service
public class UpdateElements {

    public List<Component> updateContainerComponents(Container container, DiagramParameters diagramParameters) {
        List<Component> components = new ArrayList<>();

        for (Component component : container.getComponents()) {
            String componentDSLIdentifier = component.getProperties().get("structurizr_dsl_identifier").toString();
            components.add(diagramParameters.getCreatedComponents().get(componentDSLIdentifier));
        }

        return components;
    }

    public List<Container> updateSystemContainers(SoftwareSystem system, DiagramParameters diagramParameters) {
        List<Container> containers = new ArrayList<>();

        for (Container container : system.getContainers()) {
            List<Component> components = updateContainerComponents(container, diagramParameters);
            String containerDSLIdentifier = container.getProperties().get("structurizr_dsl_identifier").toString();
            Container updatedContainer = diagramParameters.getCreatedContainers().get(containerDSLIdentifier);
            updatedContainer.setComponents(components);
            diagramParameters.getCreatedContainers().put(containerDSLIdentifier, updatedContainer);
            containers.add(updatedContainer);
        }

        return containers;
    }

    public void updateElements(Workspace workspace, Model model, SoftwareSystem system, String softwareSystemMnemonic,
            DiagramParameters diagramParameters) {

        diagramParameters.getCreatedSystems().put(softwareSystemMnemonic, system);

        for (Map.Entry<String, SoftwareSystem> entry : diagramParameters.getCreatedSystems().entrySet()) {

            SoftwareSystem currentSystem = entry.getValue();
            String systemDSLIdentifier = currentSystem.getProperties().get("structurizr_dsl_identifier").toString();
            List<Container> containers = updateSystemContainers(currentSystem, diagramParameters);
            currentSystem.setContainers(containers);
            diagramParameters.getCreatedSystems().put(systemDSLIdentifier, currentSystem);
        }

        system = diagramParameters.getCreatedSystems().get(softwareSystemMnemonic);

        workspace.setId(1L);
        workspace.setViews(new Views());

        model.setSoftwareSystems(new ArrayList<>());

        for (Map.Entry<String, SoftwareSystem> entry : diagramParameters.getCreatedSystems().entrySet()) {
            model.getSoftwareSystems().add(entry.getValue());
        }

        workspace.setModel(model);
    }
}