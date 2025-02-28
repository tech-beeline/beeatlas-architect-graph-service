package com.example.architecting_graph;

import java.util.Map;

public class Workspace {

    private Long id;
    private String name;
    private String description;
    private String version;
    private String thumbnail;
    private String lastModifiedDate;
    private String lastModifiedUser;
    private String lastModifiedAgent;
    private Model model;
    private Views views;
    private Documentation documentation;
    private WorkspaceConfiguration configuration;
    private Map<String, Object> properties;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getLastModifiedUser() {
        return lastModifiedUser;
    }

    public void setLastModifiedUser(String lastModifiedUser) {
        this.lastModifiedUser = lastModifiedUser;
    }

    public String getLastModifiedAgent() {
        return lastModifiedAgent;
    }

    public void setLastModifiedAgent(String lastModifiedAgent) {
        this.lastModifiedAgent = lastModifiedAgent;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Views getViews() {
        return views;
    }

    public void setViews(Views views) {
        this.views = views;
    }

    public Documentation getDocumentation() {
        return documentation;
    }

    public void setDocumentation(Documentation documentation) {
        this.documentation = documentation;
    }

    public WorkspaceConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(WorkspaceConfiguration configuration) {
        this.configuration = configuration;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}