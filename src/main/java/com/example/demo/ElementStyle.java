package com.example.demo;

public class ElementStyle {

    private String tag;
    private Integer width;
    private Integer height;
    private String background;
    private String stroke;
    private Integer strokeWidth;
    private String color;
    private Integer fontSize;
    private Shape shape;
    private String icon;
    private Border border;
    private Integer opacity;
    private Boolean metadata;
    private Boolean description;

    // Getters and Setters

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getStroke() {
        return stroke;
    }

    public void setStroke(String stroke) {
        this.stroke = stroke;
    }

    public Integer getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(Integer strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public Shape getShape() {
        return shape;
    }

    public void setShape(Shape shape) {
        this.shape = shape;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Border getBorder() {
        return border;
    }

    public void setBorder(Border border) {
        this.border = border;
    }

    public Integer getOpacity() {
        return opacity;
    }

    public void setOpacity(Integer opacity) {
        this.opacity = opacity;
    }

    public Boolean getMetadata() {
        return metadata;
    }

    public void setMetadata(Boolean metadata) {
        this.metadata = metadata;
    }

    public Boolean getDescription() {
        return description;
    }

    public void setDescription(Boolean description) {
        this.description = description;
    }

    // Enums

    public enum Shape {
        Box,
        RoundedBox,
        Component,
        Circle,
        Ellipse,
        Hexagon,
        Diamond,
        Folder,
        Cylinder,
        Pipe,
        WebBrowser,
        Window,
        MobileDevicePortrait,
        MobileDeviceLandscape,
        Person,
        Robot
    }

    public enum Border {
        Solid,
        Dashed,
        Dotted
    }
}
