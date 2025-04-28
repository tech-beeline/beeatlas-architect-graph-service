package ru.beeline.architecting_graph.createDiagrams;

import java.util.Objects;

public class Edge {

    private String from;
    private String to;
    private String description;

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getDescription() {
        return description;
    }

    public void setParams(String from, String to, String description) {
        this.from = from;
        this.to = to;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Edge edge = (Edge) o;
        return Objects.equals(this.to, edge.to) && Objects.equals(this.from, edge.from)
                && Objects.equals(this.description, edge.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.from, this.to, this.description);
    }

}
