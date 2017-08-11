package com.e2esp.bergerpaints.livevisualizer.detectors;

import com.e2esp.bergerpaints.livevisualizer.activities.VisualizerActivity;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Zain on 7/3/2017.
 */

public class FloodFillDetector {
    //private final String TAG = "FloodFillDetector";

    private long mLastProcessTime = -1;

    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(VisualizerActivity.DEFAULT_TOLERANCE[0], VisualizerActivity.DEFAULT_TOLERANCE[1], VisualizerActivity.DEFAULT_TOLERANCE[2]);

    private Mat mDilateElement;

    private Mat mMask = new Mat();
    private int mPreviousNonZero = 0;

    private int mDilationIterations = 1;
    private int mStructure = Imgproc.MORPH_RECT;
    private int mDilationSize = 3;

    public void setColorRadius(double ch1, double ch2, double ch3, double ch4) {
        if (ch1 == -1) {
            ch1 = mColorRadius.val[0];
        }
        if (ch2 == -1) {
            ch2 = mColorRadius.val[1];
        }
        if (ch3 == -1) {
            ch3 = mColorRadius.val[2];
        }
        if (ch4 == -1) {
            ch4 = mColorRadius.val[3];
        }
        mColorRadius = new Scalar(ch1, ch2, ch3, ch4);
    }

    public void setModes(int dilate, int structure, int dilateSize) {
        if (dilate != -1) {
            mDilationIterations = dilate;
        }
        if (structure != -1) {
            mStructure = structure;
        }
        if (dilateSize != -1) {
            mDilationSize = dilateSize;
        }

        if (mDilateElement != null) {
            mDilateElement.release();
        }
        mDilateElement = null;
    }

    public boolean process(Mat img, Point seedPoint) {
        // Create mask with border
        Mat floodFilled = Mat.zeros(img.rows() + 2, img.cols() + 2, CvType.CV_8U);
        Scalar diff = new Scalar(mColorRadius.val[0]*0.5, mColorRadius.val[1]*0.5, mColorRadius.val[2]*0.5);

        // Apply Flood Fill
        Imgproc.floodFill(img, floodFilled, seedPoint, new Scalar(255), new Rect(), diff, diff, 8 + (255 << 8) + Imgproc.FLOODFILL_MASK_ONLY);
        Core.subtract(floodFilled, Scalar.all(0), floodFilled);

        // Remove border
        Rect roi = new Rect(1, 1, img.cols(), img.rows());
        Mat result = floodFilled.submat(roi);
        floodFilled.release();

        // Ignore if result too sparse compared to last mask
        int nonZero = Core.countNonZero(result);
        if (nonZero < mPreviousNonZero*0.1) {
            //Log.v(TAG, "floodFill low on non zero: " + nonZero + " compared to previous: " + mPreviousNonZero);
            result.release();
            return false;
        }

        // Apply Dilate to fill gaps
        if (mDilateElement == null) {
            mDilateElement = Imgproc.getStructuringElement(mStructure, new Size(2 * mDilationSize + 1, 2 * mDilationSize + 1));
        }
        for (int i = 0; i < mDilationIterations; i++) {
            Imgproc.dilate(result, result, mDilateElement);
        }

        if (mMask != null) {
            mMask.release();
        }
        mMask = result;
        mPreviousNonZero = nonZero;

        mLastProcessTime = System.currentTimeMillis();
        return true;
    }

    public Mat getMask() {
        return mMask;
    }

    public int getPreviousNonZero() {
        return mPreviousNonZero;
    }

    public void setPreviousNonZero(int previousNonZero) {
        mPreviousNonZero = previousNonZero;
    }

    public boolean shouldUpdate() {
        return mLastProcessTime < 0 || (System.currentTimeMillis() - mLastProcessTime >= 200);
    }

}
