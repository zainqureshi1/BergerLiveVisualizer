package com.e2esp.bergerpaints.livevisualizer.models;

import com.e2esp.bergerpaints.livevisualizer.utils.Utility;

/**
 * Created by Zain on 6/19/2017.
 */

public class Options {

    private int hue;
    private int sat;
    private int val;
    private int dilate;
    private int structure;
    private int size;
    private int mode;
    private int method;

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
        if (split.length > 4) {
            try {
                dilate = Integer.parseInt(split[4]);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (split.length > 5) {
            try {
                structure = Integer.parseInt(split[5]);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (split.length > 6) {
            try {
                size = Integer.parseInt(split[6]);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (split.length > 7) {
            try {
                mode = Integer.parseInt(split[7]);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (split.length > 8) {
            try {
                method = Integer.parseInt(split[8]);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public Options(int hue, int sat, int val, int dilate, int structure, int size, int mode, int method) {
        this.hue = hue;
        this.sat = sat;
        this.val = val;
        this.dilate = dilate;
        this.structure = structure;
        this.size = size;
        this.mode = mode;
        this.method = method;
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

    public int getDilate() {
        return dilate;
    }

    public int getStructure() {
        return structure;
    }

    public int getSize() {
        return size;
    }

    public int getMode() {
        return mode;
    }

    public int getMethod() {
        return method;
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
        String dl = "Dilate:" + (dilate == 0 ? "None" : dilate);
        String st = "Structure:" + (structure == 0 ? "RECT" : structure == 1 ? "CROSS":"ELLIPSE");
        String sz = "Size:" + (size + 1);
        String md = "Mode:" + (mode ==0 ? "EXTERNAL" : mode == 1 ? "LIST" : mode == 2 ? "CCOMP" : mode == 3 ? "TREE" : "FLOODFILL");
        String mt = "Method:" + (method == 0 ? "NONE" : method == 1 ? "SIMPLE" : method == 2 ? "TC89_L1" : "TC89_KCOS");

        String textToShow = h + " " + s + " " + v
                + " " + dl + " " + st + " " + sz
                + " " + md + " " + mt;
        return textToShow;
    }

    public String getTextToSave() {
        String textToSave = name+"_"+hue+"_"+sat+"_"+val+"_"+dilate+"_"+structure+"_"+size+"_"+mode+"_"+method;
        return textToSave;
    }

}
