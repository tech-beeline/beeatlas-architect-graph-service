package com.example.architecting_graph;

public class User {

    private String username;
    private Role role;

    // Getters and Setters

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

    // Enum for Role
    public enum Role {
        ReadWrite,
        ReadOnly
    }
}
