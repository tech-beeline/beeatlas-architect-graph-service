package com.example.demo;

import java.util.List;
import java.util.Map;

public class Model {

    private Enterprise enterprise;
    private List<Person> people;
    private List<SoftwareSystem> softwareSystems;
    private List<DeploymentNode> deploymentNodes;
    private Map<String, Object> properties;

    // Getters and Setters

    public Enterprise getEnterprise() {
        return enterprise;
    }

    public void setEnterprise(Enterprise enterprise) {
        this.enterprise = enterprise;
    }

    public List<Person> getPeople() {
        return people;
    }

    public void setPeople(List<Person> people) {
        this.people = people;
    }

    public List<SoftwareSystem> getSoftwareSystems() {
        return softwareSystems;
    }

    public void setSoftwareSystems(List<SoftwareSystem> softwareSystems) {
        this.softwareSystems = softwareSystems;
    }

    public List<DeploymentNode> getDeploymentNodes() {
        return deploymentNodes;
    }

    public void setDeploymentNodes(List<DeploymentNode> deploymentNodes) {
        this.deploymentNodes = deploymentNodes;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    // Вложенный класс Enterprise
    public static class Enterprise {
        private String name;

        // Getters and Setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}