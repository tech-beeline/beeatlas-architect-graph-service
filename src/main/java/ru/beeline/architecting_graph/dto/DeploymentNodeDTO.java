package ru.beeline.architecting_graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentNodeDTO {
    private String deploymentName;
    private String cmdb;
    private String environmentName;
}