package ru.beeline.architecting_graph.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.beeline.architecting_graph.model.Decision;
import ru.beeline.architecting_graph.model.DocumentationSection;
import ru.beeline.architecting_graph.model.Image;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Documentation {

    private List<DocumentationSection> sections;
    private List<Decision> decisions;
    private List<Image> images;
}