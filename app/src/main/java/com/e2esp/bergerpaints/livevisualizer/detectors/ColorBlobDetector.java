package com.e2esp.bergerpaints.livevisualizer.detectors;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ColorBlobDetector {
    private final String TAG = "ColorBlobDetector";

    private final int mUpdateDelay = 200;
    //private final int mMaxDilatedMasks = 5;

    // Lower and Upper bounds for range checking in HSV color space
    private Scalar mLowerBound = new Scalar(0);
    private Scalar mUpperBound = new Scalar(0);
    // Color radius for range checking in HSV color space
    public Scalar mColorRadius = new Scalar(50, 50, 50, 0);
    private List<MatOfPoint> mContours = new ArrayList<>();

    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.3;

    // Cache
    //private Mat mPyrDownMat = new Mat();
    private Mat mHsvMat = new Mat();
    public Mat mMask = new Mat();
    //private Mat mDilatedMask = new Mat();
    //private List<Mat> mDilatedMasks = new ArrayList<>();
    //private int maskCount = -1;
    private Mat mHierarchy = new Mat();

    public int mDilationIterations = 1;
    public int mStructure = Imgproc.MORPH_RECT;
    public int mDilationSize = 3;
    public int mContourMode = Imgproc.RETR_EXTERNAL;
    public int mContourMethod = Imgproc.CHAIN_APPROX_SIMPLE;

    private long mLastProcessTime = -1;
    private boolean mUpdateNeeded = false;

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

    public void setHsvColor(Scalar hsvColor) {
        mLowerBound.val[0] = Math.max(hsvColor.val[0] - mColorRadius.val[0], 0);
        mUpperBound.val[0] = Math.min(hsvColor.val[0] + mColorRadius.val[0], 360);

        mLowerBound.val[1] = Math.max(hsvColor.val[1] - mColorRadius.val[1], 0);
        mUpperBound.val[1] = Math.min(hsvColor.val[1] + mColorRadius.val[1], 255);

        mLowerBound.val[2] = Math.max(hsvColor.val[2] - mColorRadius.val[2], 0);
        mUpperBound.val[2] = Math.min(hsvColor.val[2] + mColorRadius.val[2], 255);

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        mUpdateNeeded = true;
    }

    public void setMinContourArea(double area) {
        mMinContourArea = area;
    }

    public void process(Mat rgbaImage) {
        //Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        //Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);

        Imgproc.cvtColor(rgbaImage, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);

        Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);

        //Log.i(TAG, "Original Mask: "+mMask.dump());
        Mat element = Imgproc.getStructuringElement(mStructure, new Size(2 * mDilationSize + 1, 2 * mDilationSize + 1));
        for (int i = 0; i < mDilationIterations; i++) {
            Imgproc.dilate(mMask, mMask, element);
        }
        element.release();
        //Log.i(TAG, "Dilated Mask: "+mDilatedMasks.get(maskCount).dump());
        //Core.hconcat(mDilatedMasks, mDilatedMask);
        //Log.i(TAG, "Combined Mask: "+mDilatedMask.dump());

        //mDilatedMask = openByReconstruction(mMask, 3, mDilationSize);

        List<MatOfPoint> contours = new ArrayList<>();

        Imgproc.findContours(mMask, contours, mHierarchy, mContourMode, mContourMethod);

        // Find max contour area
        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            double area = Imgproc.contourArea(contour);
            if (area > mMinContourArea * maxArea) {
                //Core.multiply(contour, new Scalar(4, 4), contour);
                mContours.add(contour);
            }
        }

        if (mContours.size() > 1) {
            Log.i(TAG, "Contours count: " + mContours.size());
        }

        mLastProcessTime = System.currentTimeMillis();
        mUpdateNeeded = false;
    }

    public Mat openByReconstruction(Mat src, int iteration, int ksize) {
        // first erode the source image
        Mat kernel = Mat.ones(new Size(ksize, ksize), CvType.CV_8U);
        Mat eroded = new Mat();
        while (iteration > 0) {
            Imgproc.erode(src, eroded, kernel);
            iteration--;
        }

        // Now we are going to iteratively regrow the eroded mask.
        // The key difference between just a simple opening is that we
        // mask the regrown everytime with the original src.
        // Thus, the dilated mask never extends beyond where it does in the original.
        Mat thisIteration = eroded.clone();
        Mat lastIteration = eroded.clone();
        while (true) {
            Imgproc.dilate(lastIteration, thisIteration, kernel);
            Core.bitwise_and(thisIteration, src, thisIteration);
            if (matIsEqual(lastIteration, thisIteration)) {
                // convergence !
                break;
            }
            lastIteration = thisIteration.clone();
        }
        kernel.release();
        eroded.release();
        lastIteration.release();

        return thisIteration;
    }

    private boolean matIsEqual(Mat mat1, Mat mat2){
        // treat two empty mat as identical as well
        if (mat1.empty() && mat2.empty()) {
            return true;
        }
        // if dimensionality of two mat is not identical, these two mat is not identical
        if (mat1.cols() != mat2.cols() || mat1.rows() != mat2.rows() || mat1.dims() != mat2.dims()) {
            return false;
        }
        Mat diff = new Mat();
        Core.compare(mat1, mat2, diff, Core.CMP_NE);
        int nz = Core.countNonZero(diff);
        diff.release();
        return nz == 0;
    }

    /*private Mat nextDilatedMask() {
        maskCount++;
        if (maskCount >= mMaxDilatedMasks) {
            maskCount = 0;
        }
        while (mDilatedMasks.size() <= maskCount) {
            mDilatedMasks.add(new Mat());
        }
        return mDilatedMasks.get(maskCount);
    }*/

    public boolean shouldUpdate() {
        if (mLastProcessTime < 0) return true;
        return System.currentTimeMillis() - mLastProcessTime >= mUpdateDelay;
    }

    public boolean isUpdateNeeded() {
        return mUpdateNeeded;// || mDilatedMasks.size() < mMaxDilatedMasks;
    }

    public boolean isInRange(Mat mat) {
        Mat mask = new Mat();
        Core.inRange(mat, mLowerBound, mUpperBound, mask);
        int rows = mask.rows();
        int cols = mask.cols();
        int matches = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double[] cell = mask.get(i, j);
                if (cell[0] == 255) {
                    matches++;
                }
            }
        }
        mask.release();
        float result = ((float)matches)/((float)(rows*cols));
        return result > 0.5f;
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }

}
