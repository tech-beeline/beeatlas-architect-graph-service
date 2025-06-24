package ru.beeline.architecting_graph.service.createDiagrams;

import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.model.*;
import ru.beeline.architecting_graph.model.ViewObjects.Views;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GetView {

    @Autowired
    CreateElements createElements;

    @Autowired
    SetElements setElements;

    public Workspace GetContextView(Session session, String softwareSystemMnemonic) {
        DiagramParameters diagramParameters = createElements.createNewDiagramParameters(session, softwareSystemMnemonic);
        createElements.createContextView(session, diagramParameters.getSystem(), softwareSystemMnemonic, diagramParameters);
        updateElements(diagramParameters.getWorkspace(), diagramParameters.getModel(), diagramParameters.getSystem(),
                softwareSystemMnemonic, diagramParameters);
        setElements.setContextView(diagramParameters.getWorkspace(), diagramParameters.getSystem(), diagramParameters);
        setElements.setContainerView(diagramParameters.getWorkspace(), diagramParameters.getSystem(), diagramParameters);
        return diagramParameters.getWorkspace();
    }

    public Workspace GetComponentView(Session session, String softwareSystemMnemonic, String containerMnemonic) {
        DiagramParameters diagramParameters = createElements.createNewDiagramParameters(session, softwareSystemMnemonic);
        createElements.createComponentView(session, softwareSystemMnemonic, containerMnemonic, diagramParameters.getSystem(),
                diagramParameters);
        updateElements(diagramParameters.getWorkspace(), diagramParameters.getModel(), diagramParameters.getSystem(),
                softwareSystemMnemonic, diagramParameters);
        setElements.setComponentView(diagramParameters.getWorkspace(), containerMnemonic, diagramParameters);
        return diagramParameters.getWorkspace();
    }

    public Workspace GetDeploymentView(Session session, String softwareSystemMnemonic, String environment) {
        DiagramParameters diagramParameters = createElements.createNewDiagramParameters(session, softwareSystemMnemonic);
        createElements.createDeploymentView(session, softwareSystemMnemonic, environment, diagramParameters.getModel(), diagramParameters);

        updateElements(diagramParameters.getWorkspace(), diagramParameters.getModel(), diagramParameters.getSystem(),
                softwareSystemMnemonic, diagramParameters);

        setElements.setDeploymentView(diagramParameters.getWorkspace(), diagramParameters.getModel(), diagramParameters.getSystem().getId(),
                environment, diagramParameters);
        return diagramParameters.getWorkspace();
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
}
