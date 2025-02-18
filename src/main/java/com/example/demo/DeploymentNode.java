package com.example.demo;

import java.util.List;
import java.util.Map;

public class DeploymentNode {

    private String id;
    private String name;
    private String description;
    private String technology;
    private String environment;
    private String instances;
    private String tags;
    private String url;
    private List<DeploymentNode> children;
    private List<InfrastructureNode> infrastructureNodes;
    private List<SoftwareSystemInstance> softwareSystemInstances;
    private List<ContainerInstance> containerInstances;
    private Map<String, Object> properties;
    private List<Perspective> perspectives;
    private List<Relationship> relationships;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTechnology() {
        return technology;
    }

    public void setTechnology(String technology) {
        this.technology = technology;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getInstances() {
        return instances;
    }

    public void setInstances(String instances) {
        this.instances = instances;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<DeploymentNode> getChildren() {
        return children;
    }

    public void setChildren(List<DeploymentNode> children) {
        this.children = children;
    }

    public List<InfrastructureNode> getInfrastructureNodes() {
        return infrastructureNodes;
    }

    public void setInfrastructureNodes(List<InfrastructureNode> infrastructureNodes) {
        this.infrastructureNodes = infrastructureNodes;
    }

    public List<SoftwareSystemInstance> getSoftwareSystemInstances() {
        return softwareSystemInstances;
    }

    public void setSoftwareSystemInstances(List<SoftwareSystemInstance> softwareSystemInstances) {
        this.softwareSystemInstances = softwareSystemInstances;
    }

    public List<ContainerInstance> getContainerInstances() {
        return containerInstances;
    }

    public void setContainerInstances(List<ContainerInstance> containerInstances) {
        this.containerInstances = containerInstances;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public List<Perspective> getPerspectives() {
        return perspectives;
    }

    public void setPerspectives(List<Perspective> perspectives) {
        this.perspectives = perspectives;
    }

    public List<Relationship> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<Relationship> relationships) {
        this.relationships = relationships;
    }
}
