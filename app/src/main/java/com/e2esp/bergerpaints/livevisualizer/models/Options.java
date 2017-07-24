package com.e2esp.bergerpaints.livevisualizer.models;

import com.e2esp.bergerpaints.livevisualizer.utils.Utility;

/**
 * Created by Zain on 6/19/2017.
 */

public class Options {

    private int hue;
    private int sat;
    private int val;

    private String name;

    public Options(String savedText) {
        String[] split = savedText.split("_");
        if (split.length > 0) {
            name = split[0];
        }
        if (split.length > 1) {
            try {
                hue = Integer.parseInt(split[1]);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (split.length > 2) {
            try {
                sat = Integer.parseInt(split[2]);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (split.length > 3) {
            try {
                val = Integer.parseInt(split[3]);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public Options(int hue, int sat, int val) {
        this.hue = hue;
        this.sat = sat;
        this.val = val;
    }

    public int getHue() {
        return hue;
    }

    public int getSat() {
        return sat;
    }

    public int getVal() {
        return val;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTextToShow() {
        String h = "H:" + (int) Utility.percentToColorTolerance(hue, 360);
        String s = "S:" + (int) Utility.percentToColorTolerance(sat, 255);
        String v = "V:" + (int) Utility.percentToColorTolerance(val, 255);

        String textToShow = h + " " + s + " " + v;
        return textToShow;
    }

    public String getTextToSave() {
        String textToSave = name+"_"+hue+"_"+sat+"_"+val;
        return textToSave;
    }

}
