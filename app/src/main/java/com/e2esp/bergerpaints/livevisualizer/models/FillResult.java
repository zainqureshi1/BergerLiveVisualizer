package com.e2esp.bergerpaints.livevisualizer.models;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

/**
 * Created by Zain on 8/7/2017.
 */

public class FillResult {

    private Point touchPoint;
    private Scalar color;
    private int previousNonZero;
    private Mat mask;

    public FillResult(Point touchPoint, Scalar color, int previousNonZero, Mat mask) {
        this.touchPoint = touchPoint;
        this.color = color;
        this.previousNonZero = previousNonZero;
        this.mask = mask;
    }

    public Point getTouchPoint() {
        return touchPoint;
    }

    public void setTouchPoint(Point touchPoint) {
        this.touchPoint = touchPoint;
    }

    public Scalar getColor() {
        return color;
    }

    public void setColor(Scalar color) {
        this.color = color;
    }

    public Mat getMask() {
        return mask;
    }

    public void setMask(Mat mask) {
        if (this.mask != null) {
            this.mask.release();
        }
        this.mask = mask;
    }

    public int getPreviousNonZero() {
        return previousNonZero;
    }

    public void setPreviousNonZero(int previousNonZero) {
        this.previousNonZero = previousNonZero;
    }

}
