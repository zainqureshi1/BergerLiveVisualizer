package com.e2esp.bergerpaints.livevisualizer.models;

import android.graphics.Bitmap;

import org.opencv.core.Mat;

/**
 * Created by Zain on 7/3/2017.
 */

public class EditState {

    private int type;
    private Mat mat;
    private Bitmap bitmap;

    public EditState(int type, Mat mat, Bitmap bitmap) {
        this.type = type;
        this.mat = mat;
        this.bitmap = bitmap;
    }

    public int getType() {
        return type;
    }

    public Mat getMat() {
        return mat;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

}
