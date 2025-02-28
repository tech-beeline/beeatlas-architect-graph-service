package com.example.architecting_graph;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestConfig {

    @Value("${spring.neo4j.uri}")
    private String uri;

    @Value("${spring.neo4j.authentication.username}")
    private String user;

    @Value("${spring.neo4j.authentication.password}")
    private String password;

    @Value("${spring.services.documents.url}")
    private String url;

    public String getUri() {
        return uri.strip();
    }

    public String getUser() {
        return user.strip();
    }

    public String getPassword() {
        return password;
    }

    public String getUrl() {
        return url.strip();
    }
}