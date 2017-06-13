package com.e2esp.bergerpaints.livevisualizer.models;

import java.util.ArrayList;

/**
 * Created by Zain on 3/6/2017.
 */

public class ProductColor {
    private String name;
    private int imageRes;

    private ArrayList<SecondaryColor> secondaryColors;
    private boolean trayOpen;

    public ProductColor(String name, int imageRes, ArrayList<SecondaryColor> secondaryColors) {
        this.name = name;
        this.imageRes = imageRes;
        this.secondaryColors = secondaryColors;
        this.trayOpen = false;
    }

    public String getName() {
        return name;
    }

    public int getImageRes() {
        return imageRes;
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

    public ProductColor clone() {
        return new ProductColor(getName(), getImageRes(), new ArrayList<>(secondaryColors));
    }

}
