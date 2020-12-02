package com.example.finalyearproject.Classes;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Vital {

    @SerializedName("HR")
    @Expose
    private Integer hR;
    @SerializedName("SP")
    @Expose
    private Integer sP;

    public Integer getHR() {
        return hR;
    }

    public void setHR(Integer hR) {
        this.hR = hR;
    }

    public Integer getSP() {
        return sP;
    }

    public void setSP(Integer sP) {
        this.sP = sP;
    }

    @Override
    public String toString() {
        return "Vital{" +
                "hR=" + hR +
                ", sP=" + sP +
                '}';
    }
}