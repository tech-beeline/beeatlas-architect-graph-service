package ru.beeline.architecting_graph.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class APIResponse {

    private Boolean success;
    private String message;
    private Integer revision;
}
