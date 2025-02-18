package com.example.demo;

import java.util.List;

public class WorkspaceConfiguration {

    private List<User> users;
    private Visibility visibility;
    private Scope scope;

    // Getters and Setters

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    // Enums

    public enum Visibility {
        Public,
        Private
    }

    public enum Scope {
        Landscape,
        SoftwareSystem
    }
}
