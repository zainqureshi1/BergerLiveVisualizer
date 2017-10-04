package com.e2esp.bergerpaints.livevisualizer.models;

/**
 *
 * Created by Zain on 1/31/2017.
 */

public class SecondaryColor {

    private int color;
    private String name;
    private String colorCode;

    public SecondaryColor(int color, String name, String colorCode) {
        this.color = color;
        this.name = name;
        this.colorCode = colorCode;
    }

    public int getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public String getColorCode() {
        return colorCode;
    }

}
