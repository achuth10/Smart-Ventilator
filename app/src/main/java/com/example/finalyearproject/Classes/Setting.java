package com.example.finalyearproject.Classes;

public class Setting {
    private String name;
    private float from,to;

    public Setting(String name, float from, float to) {
        this.name = name;
        this.from = from;
        this.to = to;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getfrom() {
        return from;
    }

    public void setfrom(float from) {
        this.from = from;
    }

    public float getto() {
        return to;
    }

    public void setto(float to) {
        this.to = to;
    }
}
