package com.example.finalyearproject.Classes;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Patient implements Serializable {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("token")
    @Expose
    private String token;
    @SerializedName("compId")
    @Expose
    private String compId;


    @SerializedName("status")
    @Expose
    private String status;

    @SerializedName("lastSeen")
    @Expose
    private String lastSeen;


    private boolean available;

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }


    public String getCompId() {
        return compId;
    }


    public void setToken(String token) {
        this.token = token;
    }

    public void setCompId(String compId) {
        this.compId = compId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(String lastSeen) {
        this.lastSeen = lastSeen;
    }
}
