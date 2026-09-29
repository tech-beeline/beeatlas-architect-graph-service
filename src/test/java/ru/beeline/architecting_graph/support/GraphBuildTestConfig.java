/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import ru.beeline.architecting_graph.repository.neo4j.ComponentRepository;
import ru.beeline.architecting_graph.repository.neo4j.ContainerInstanceRepository;
import ru.beeline.architecting_graph.repository.neo4j.ContainerRepository;
import ru.beeline.architecting_graph.repository.neo4j.DeploymentNodesRepository;
import ru.beeline.architecting_graph.repository.neo4j.EnvironmentRepository;
import ru.beeline.architecting_graph.repository.neo4j.GenericRepository;
import ru.beeline.architecting_graph.repository.neo4j.InfrastructureNodesRepository;
import ru.beeline.architecting_graph.repository.neo4j.RelationshipRepository;
import ru.beeline.architecting_graph.repository.neo4j.SoftwareSystemRepository;
import ru.beeline.architecting_graph.service.graph.ComponentUpdateService;
import ru.beeline.architecting_graph.service.graph.ContainerInstanceService;
import ru.beeline.architecting_graph.service.graph.CreateExternalObjects;
import ru.beeline.architecting_graph.service.graph.DeploymentNodeUpdateFunctions;
import ru.beeline.architecting_graph.service.graph.GraphConstructionService;
import ru.beeline.architecting_graph.service.graph.GraphUpdateFunctions;
import ru.beeline.architecting_graph.service.graph.InfrastructureNodeUpdateFunctions;
import ru.beeline.architecting_graph.service.graph.Neo4jSessionManager;

@Configuration
@Import({
        GraphConstructionService.class,
        GraphUpdateFunctions.class,
        DeploymentNodeUpdateFunctions.class,
        ComponentUpdateService.class,
        CreateExternalObjects.class,
        ContainerInstanceService.class,
        InfrastructureNodeUpdateFunctions.class,
        Neo4jSessionManager.class,
        GenericRepository.class,
        ContainerRepository.class,
        ComponentRepository.class,
        ContainerInstanceRepository.class,
        DeploymentNodesRepository.class,
        EnvironmentRepository.class,
        InfrastructureNodesRepository.class,
        RelationshipRepository.class,
        SoftwareSystemRepository.class
})
public class GraphBuildTestConfig {

    @Bean(destroyMethod = "")
    public Driver neo4jDriver() {
        return QueryCounter.wrap(EmbeddedNeo4j.driver());
    }

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json().build();
    }
}
