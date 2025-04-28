package ru.beeline.architecting_graph.createDiagrams.ViewObjects;

public class AutomaticLayout {

    private LayoutImplementation implementation;
    private RankDirection rankDirection;
    private Integer rankSeparation;
    private Integer nodeSeparation;
    private Integer edgeSeparation;
    private Boolean vertices;
    private Boolean applied;

    public LayoutImplementation getImplementation() {
        return implementation;
    }

    public void setImplementation(LayoutImplementation implementation) {
        this.implementation = implementation;
    }

    public RankDirection getRankDirection() {
        return rankDirection;
    }

    public void setRankDirection(RankDirection rankDirection) {
        this.rankDirection = rankDirection;
    }

    public Integer getRankSeparation() {
        return rankSeparation;
    }

    public void setRankSeparation(Integer rankSeparation) {
        this.rankSeparation = rankSeparation;
    }

    public Integer getNodeSeparation() {
        return nodeSeparation;
    }

    public void setNodeSeparation(Integer nodeSeparation) {
        this.nodeSeparation = nodeSeparation;
    }

    public Integer getEdgeSeparation() {
        return edgeSeparation;
    }

    public void setEdgeSeparation(Integer edgeSeparation) {
        this.edgeSeparation = edgeSeparation;
    }

    public Boolean getVertices() {
        return vertices;
    }

    public void setVertices(Boolean vertices) {
        this.vertices = vertices;
    }

    public Boolean getApplied() {
        return applied;
    }

    public void setApplied(Boolean applied) {
        this.applied = applied;
    }

    public enum LayoutImplementation {
        Graphviz,
        Dagre
    }

    public enum RankDirection {
        TopBottom,
        BottomTop,
        LeftRight,
        RightLeft
    }
}