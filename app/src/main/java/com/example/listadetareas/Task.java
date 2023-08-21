package com.example.listadetareas;

import java.io.Serializable;

public class Task implements Serializable {
    private long id;
    private String title;
    private String description;
    private String date;
    private byte[] imagePath;
    private final boolean selected;

    public Task(String title, String description, String date, byte[] imagePath) {
        this.title = title;
        this.description = description;
        this.date = date;
        this.imagePath = imagePath;
        this.selected = false; // Por defecto, la tarea no está seleccionada
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    public void setTitle(String newTitle) {
        this.title = newTitle;
    }

    public void setDescription(String newDescription) {
        this.description = newDescription;
    }

    public void setDate(String newDate) {
        this.date = newDate;
    }

    public byte[] getImagePath() {
        return imagePath;
    }

    public void setImagePath(byte[] image) {
        this.imagePath = image;
    }
}
