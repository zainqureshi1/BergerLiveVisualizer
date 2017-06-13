package com.e2esp.bergerpaints.livevisualizer.models;

import android.graphics.Bitmap;

/**
 * Created by Zain on 6/9/2017.
 */

public class TestSelection {

    private Bitmap bitmap;
    private String name;

    public TestSelection(Bitmap bitmap, String name) {
        this.bitmap = bitmap;
        this.name = name;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public String getName() {
        return name;
    }

}
