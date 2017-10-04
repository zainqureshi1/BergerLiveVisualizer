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
 *
 * Created by Zain on 7/3/2017.
 */

public class FloodFillDetector {
    //private final String TAG = "FloodFillDetector";

    private final int FLAGS = 8 + (255 << 8) + Imgproc.FLOODFILL_MASK_ONLY;

    private long mLastProcessTime = -1;

    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = initColorRadius();
    private Scalar mNewValue = new Scalar(255);
    private Scalar mZeroScalar = Scalar.all(0);
    private Rect mOutRect = new Rect();
    private Rect mRoi;

    private Mat mDilateElement;

    private Mat mFloodMask;
    private int mPreviousNonZero = 0;

    private int mDilationIterations = 1;
    private int mStructure = Imgproc.MORPH_RECT;
    private int mDilationSize = 3;

    private Scalar initColorRadius() {
        if (VisualizerActivity.useExteriorTolerance) {
            return new Scalar(VisualizerActivity.EXTERIOR_TOLERANCE[0]*0.5, VisualizerActivity.EXTERIOR_TOLERANCE[1]*0.5, VisualizerActivity.EXTERIOR_TOLERANCE[2]*0.5);
        }
        return new Scalar(VisualizerActivity.INTERIOR_TOLERANCE[0]*0.5, VisualizerActivity.INTERIOR_TOLERANCE[1]*0.5, VisualizerActivity.INTERIOR_TOLERANCE[2]*0.5);
    }

    public void setColorRadius(double ch1, double ch2, double ch3) {
        if (ch1 == -1) {
            ch1 = mColorRadius.val[0]*2.0;
        }
        if (ch2 == -1) {
            ch2 = mColorRadius.val[1]*2.0;
        }
        if (ch3 == -1) {
            ch3 = mColorRadius.val[2]*2.0;
        }
        mColorRadius = new Scalar(ch1*0.5, ch2*0.5, ch3*0.5);
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

    public Mat process(Mat img, Point seedPoint) {
        int cols = img.cols();
        int rows = img.rows();

        // Create mask with border
        if (mFloodMask == null || mFloodMask.rows() != rows + 2 || mFloodMask.cols() != cols + 2) {
            if (mFloodMask != null) {
                mFloodMask.release();
            }
            mFloodMask = Mat.zeros(rows + 2, cols + 2, CvType.CV_8U);
        } else {
            mFloodMask.setTo(mZeroScalar);
        }

        // Apply Flood Fill
        Imgproc.floodFill(img, mFloodMask, seedPoint, mNewValue, mOutRect, mColorRadius, mColorRadius, FLAGS);
        Core.subtract(mFloodMask, mZeroScalar, mFloodMask);

        // Ignore if result too sparse compared to last mask
        int nonZero = Core.countNonZero(mFloodMask);
        if (nonZero < mPreviousNonZero*0.1) {
            //Log.v(TAG, "floodFill low on non zero: " + nonZero + " compared to previous: " + mPreviousNonZero);
            return null;
        }
        mPreviousNonZero = nonZero;

        // Remove border
        if (mRoi == null || mRoi.width != cols || mRoi.height != rows) {
            mRoi = new Rect(1, 1, cols, rows);
        }
        Mat result = mFloodMask.submat(mRoi);

        // Apply Dilate to fill gaps
        if (mDilateElement == null) {
            mDilateElement = Imgproc.getStructuringElement(mStructure, new Size(2 * mDilationSize + 1, 2 * mDilationSize + 1));
        }
        for (int i = 0; i < mDilationIterations; i++) {
            Imgproc.dilate(result, result, mDilateElement);
        }

        Mat mask = result.clone();
        result.release();
        mLastProcessTime = System.currentTimeMillis();

        return mask;
    }

    public int getPreviousNonZero() {
        return mPreviousNonZero;
    }

    public void setPreviousNonZero(int previousNonZero) {
        mPreviousNonZero = previousNonZero;
    }

    public boolean shouldUpdate(int totalMasks) {
        return mLastProcessTime < 0 || (System.currentTimeMillis() - mLastProcessTime >= 200/totalMasks);
    }

}
