package com.e2esp.bergerpaints.livevisualizer.detectors;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zain on 8/11/2017.
 */

public class MaskApplier {

    private List<Mat> mHsvChannels = new ArrayList<>();

    private int gcCount = 0;

    public void apply(Mat hsvMat, Scalar fillHsv, Mat mask) {
        // Split src into HSV channels
        Core.split(hsvMat, mHsvChannels);

        // Set H and S channels to fill color using given mask
        if (mHsvChannels.size() > 0) {
            mHsvChannels.get(0).setTo(Scalar.all(fillHsv.val[0]), mask);
        }
        if (mHsvChannels.size() > 1) {
            mHsvChannels.get(1).setTo(Scalar.all(fillHsv.val[1]), mask);
        }

        // Merge channels back into single Mat
        Core.merge(mHsvChannels, hsvMat);
        for (Mat channel : mHsvChannels) {
            channel.release();
        }
        mHsvChannels.clear();
        gcCount++;
        if (gcCount > 10) {
            gcCount = 0;
            applyGC();
        }
    }

    private void applyGC() {
        //Log.e("Garbage Collector", "Start");
        System.gc();
        //Log.e("Garbage Collector", "End");
    }

}
