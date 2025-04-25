package ru.beeline.architecting_graph.otherObjects;

import java.util.List;

public class Views {

    private List<SystemLandscapeView> systemLandscapeViews;
    private List<SystemContextView> systemContextViews;
    private List<ContainerView> containerViews;
    private List<ComponentView> componentViews;
    private List<DynamicView> dynamicViews;
    private List<DeploymentView> deploymentViews;
    private List<FilteredView> filteredViews;
    private List<ImageView> imageViews;
    private Configuration configuration;

    public List<SystemLandscapeView> getSystemLandscapeViews() {
        return systemLandscapeViews;
    }

    public void setSystemLandscapeViews(List<SystemLandscapeView> systemLandscapeViews) {
        this.systemLandscapeViews = systemLandscapeViews;
    }

    public List<SystemContextView> getSystemContextViews() {
        return systemContextViews;
    }

    public void setSystemContextViews(List<SystemContextView> systemContextViews) {
        this.systemContextViews = systemContextViews;
    }

    public List<ContainerView> getContainerViews() {
        return containerViews;
    }

    public void setContainerViews(List<ContainerView> containerViews) {
        this.containerViews = containerViews;
    }

    public List<ComponentView> getComponentViews() {
        return componentViews;
    }

    public void setComponentViews(List<ComponentView> componentViews) {
        this.componentViews = componentViews;
    }

    public List<DynamicView> getDynamicViews() {
        return dynamicViews;
    }

    public void setDynamicViews(List<DynamicView> dynamicViews) {
        this.dynamicViews = dynamicViews;
    }

    public List<DeploymentView> getDeploymentViews() {
        return deploymentViews;
    }

    public void setDeploymentViews(List<DeploymentView> deploymentViews) {
        this.deploymentViews = deploymentViews;
    }

    public List<FilteredView> getFilteredViews() {
        return filteredViews;
    }

    public void setFilteredViews(List<FilteredView> filteredViews) {
        this.filteredViews = filteredViews;
    }

    public List<ImageView> getImageViews() {
        return imageViews;
    }

    public void setImageViews(List<ImageView> imageViews) {
        this.imageViews = imageViews;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
