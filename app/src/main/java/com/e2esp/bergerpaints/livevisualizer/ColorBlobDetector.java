package com.e2esp.bergerpaints.livevisualizer;

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
    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.3;
    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(50, 50, 50, 0);
    private Mat mSpectrum = new Mat();
    private List<MatOfPoint> mContours = new ArrayList<>();

    // Cache
    //private Mat mPyrDownMat = new Mat();
    private Mat mHsvMat = new Mat();
    public Mat mMask = new Mat();
    private Mat mDilatedMask = new Mat();
    //private List<Mat> mDilatedMasks = new ArrayList<>();
    private int mDilationSize = 3;
    //private int maskCount = -1;
    private Mat mHierarchy = new Mat();

    private long mLastProcessTime = -1;
    private boolean mUpdateNeeded = false;

    public void setColorRadius(Scalar radius) {
        mColorRadius = radius;
    }

    public void setHsvColor(Scalar hsvColor) {
        double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0] - mColorRadius.val[0] : 0;
        double maxH = (hsvColor.val[0] + mColorRadius.val[0] <= 255) ? hsvColor.val[0] + mColorRadius.val[0] : 255;

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;

        mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
        mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

        mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
        mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        Mat spectrumHsv = new Mat(1, (int)(maxH-minH), CvType.CV_8UC3);

        for (int j = 0; j < maxH-minH; j++) {
            byte[] tmp = {(byte)(minH+j), (byte)255, (byte)255};
            spectrumHsv.put(0, j, tmp);
        }

        Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);

        mUpdateNeeded = true;
    }

    public Mat getSpectrum() {
        return mSpectrum;
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
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2 * mDilationSize + 1, 2 * mDilationSize + 1));
        Imgproc.dilate(mMask, mDilatedMask, element);
        //Log.i(TAG, "Dilated Mask: "+mDilatedMasks.get(maskCount).dump());
        //Core.hconcat(mDilatedMasks, mDilatedMask);
        //Log.i(TAG, "Combined Mask: "+mDilatedMask.dump());

        //mDilatedMask = openByReconstruction(mMask, 3, mDilationSize);

        List<MatOfPoint> contours = new ArrayList<>();

        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

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
        float result = ((float)matches)/((float)(rows*cols));
        return result > 0.5f;
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }

}
