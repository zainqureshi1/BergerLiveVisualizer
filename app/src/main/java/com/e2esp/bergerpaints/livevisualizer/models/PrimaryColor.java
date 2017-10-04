package com.e2esp.bergerpaints.livevisualizer.models;

import java.util.ArrayList;

/**
 *
 * Created by Zain on 1/31/2017.
 */

public class PrimaryColor extends SecondaryColor {

    private String shade;
    private ArrayList<SecondaryColor> secondaryColors;
    private boolean trayOpen;

    public PrimaryColor(int color, String name, String shade, ArrayList<SecondaryColor> secondaryColors) {
        super(color, name, "");
        this.shade = shade;
        this.secondaryColors = secondaryColors;
        this.trayOpen = false;
    }

    public String getShade() {
        return shade;
    }

    public ArrayList<SecondaryColor> getSecondaryColors() {
        return secondaryColors;
    }

    public boolean isTrayOpen() {
        return trayOpen;
    }

    public void setTrayOpen(boolean trayOpen) {
        this.trayOpen = trayOpen;
    }

    public PrimaryColor clone() {
        return new PrimaryColor(getColor(), getName(), getShade(), new ArrayList<>(secondaryColors));
    }

}
