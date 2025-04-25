package ru.beeline.architecting_graph.otherObjects;

import java.util.List;
import java.util.Map;

public class Configuration {

    private Styles styles;
    private String lastSavedView;
    private String defaultView;
    private List<String> themes;
    private Branding branding;
    private Terminology terminology;
    private MetadataSymbols metadataSymbols;
    private Map<String, Object> properties;

    public Styles getStyles() {
        return styles;
    }

    public void setStyles(Styles styles) {
        this.styles = styles;
    }

    public String getLastSavedView() {
        return lastSavedView;
    }

    public void setLastSavedView(String lastSavedView) {
        this.lastSavedView = lastSavedView;
    }

    public String getDefaultView() {
        return defaultView;
    }

    public void setDefaultView(String defaultView) {
        this.defaultView = defaultView;
    }

    public List<String> getThemes() {
        return themes;
    }

    public void setThemes(List<String> themes) {
        this.themes = themes;
    }

    public Branding getBranding() {
        return branding;
    }

    public void setBranding(Branding branding) {
        this.branding = branding;
    }

    public Terminology getTerminology() {
        return terminology;
    }

    public void setTerminology(Terminology terminology) {
        this.terminology = terminology;
    }

    public MetadataSymbols getMetadataSymbols() {
        return metadataSymbols;
    }

    public void setMetadataSymbols(MetadataSymbols metadataSymbols) {
        this.metadataSymbols = metadataSymbols;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public static class Styles {
        private List<ElementStyle> elements;
        private List<RelationshipStyle> relationships;

        public List<ElementStyle> getElements() {
            return elements;
        }

        public void setElements(List<ElementStyle> elements) {
            this.elements = elements;
        }

        public List<RelationshipStyle> getRelationships() {
            return relationships;
        }

        public void setRelationships(List<RelationshipStyle> relationships) {
            this.relationships = relationships;
        }
    }

    public enum MetadataSymbols {
        SquareBrackets,
        RoundBrackets,
        CurlyBrackets,
        AngleBrackets,
        DoubleAngleBrackets,
        None
    }
}
