package ru.beeline.architecting_graph.service.createDiagrams;

import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.model.DiagramParameters;
import ru.beeline.architecting_graph.model.Workspace;

@Service
public class GetView {

        @Autowired
        CreateElements createElements;

        @Autowired
        UpdateElements updateElements;

        @Autowired
        SetElements setElements;

        public Workspace GetContextView(Session session, String softwareSystemMnemonic) {

                DiagramParameters diagramParameters = createElements.createNewDiagramParameters(session,
                                softwareSystemMnemonic);

                createElements.createContextView(session, diagramParameters.getSystem(), softwareSystemMnemonic,
                                diagramParameters);

                updateElements.updateElements(diagramParameters.getWorkspace(), diagramParameters.getModel(),
                                diagramParameters.getSystem(), softwareSystemMnemonic, diagramParameters);

                setElements.setContextView(diagramParameters.getWorkspace(), diagramParameters.getSystem(),
                                diagramParameters);
                setElements.setContainerView(diagramParameters.getWorkspace(), diagramParameters.getSystem(),
                                diagramParameters);

                return diagramParameters.getWorkspace();
        }

        public Workspace GetComponentView(Session session, String softwareSystemMnemonic, String containerMnemonic) {

                DiagramParameters diagramParameters = createElements.createNewDiagramParameters(session,
                                softwareSystemMnemonic);

                createElements.createComponentView(session, softwareSystemMnemonic, containerMnemonic,
                                diagramParameters.getSystem(), diagramParameters);

                updateElements.updateElements(diagramParameters.getWorkspace(), diagramParameters.getModel(),
                                diagramParameters.getSystem(), softwareSystemMnemonic, diagramParameters);

                setElements.setComponentView(diagramParameters.getWorkspace(), containerMnemonic, diagramParameters);

                return diagramParameters.getWorkspace();
        }

        public Workspace GetDeploymentView(Session session, String softwareSystemMnemonic, String environment) {

                DiagramParameters diagramParameters = createElements.createNewDiagramParameters(session,
                                softwareSystemMnemonic);

                createElements.createDeploymentView(session, softwareSystemMnemonic, environment,
                                diagramParameters.getModel(), diagramParameters);

                updateElements.updateElements(diagramParameters.getWorkspace(), diagramParameters.getModel(),
                                diagramParameters.getSystem(), softwareSystemMnemonic, diagramParameters);

                setElements.setDeploymentView(diagramParameters.getWorkspace(), diagramParameters.getModel(),
                                diagramParameters.getSystem().getId(), environment, diagramParameters);

                return diagramParameters.getWorkspace();
        }
}
