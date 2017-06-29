package com.e2esp.bergerpaints.livevisualizer.detectors;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Zain on 6/23/2017.
 */

public class CannyEdgeDetector {

    private Mat mGrayMat = new Mat();
    private Mat mEdgesMat = new Mat();

    public int mKernelSize = 3;
    public double mThreshold = 50;
    public double mRatio = 3;
    public int mSobel = 3;
    public boolean mL2 = false;

    public Mat process(Mat rgbaImage) {
        Imgproc.cvtColor(rgbaImage, mGrayMat, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.blur(mGrayMat, mEdgesMat, new Size(mKernelSize, mKernelSize));

        Imgproc.Canny(mEdgesMat, mEdgesMat, mThreshold, mThreshold * mRatio, mSobel, mL2);

        /*Mat dest = new Mat();
        Core.add(dest, Scalar.all(0), dest);

        rgbaImage.copyTo(dest, mEdgesMat);*/
        return mEdgesMat;
    }

}
