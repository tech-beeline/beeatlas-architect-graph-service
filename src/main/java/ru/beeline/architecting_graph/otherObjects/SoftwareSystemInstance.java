package ru.beeline.architecting_graph.otherObjects;

import java.util.List;
import java.util.Map;

import ru.beeline.architecting_graph.graph.relationship.Relationship;

public class SoftwareSystemInstance {

    private String id;
    private String softwareSystemId;
    private Integer instanceId;
    private String environment;
    private String tags;
    private Map<String, Object> properties;
    private List<Perspective> perspectives;
    private List<Relationship> relationships;
    private List<HttpHealthCheck> healthChecks;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSoftwareSystemId() {
        return softwareSystemId;
    }

    public void setSoftwareSystemId(String softwareSystemId) {
        this.softwareSystemId = softwareSystemId;
    }

    public Integer getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Integer instanceId) {
        this.instanceId = instanceId;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
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

    public List<HttpHealthCheck> getHealthChecks() {
        return healthChecks;
    }

    public void setHealthChecks(List<HttpHealthCheck> healthChecks) {
        this.healthChecks = healthChecks;
    }
}
