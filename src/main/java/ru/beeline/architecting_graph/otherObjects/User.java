package ru.beeline.architecting_graph.otherObjects;

public class User {

    private String username;
    private Role role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public enum Role {
        ReadWrite,
        ReadOnly
    }
}
