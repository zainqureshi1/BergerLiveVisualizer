package com.e2esp.bergerpaints.livevisualizer.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.e2esp.bergerpaints.livevisualizer.detectors.CannyEdgeDetector;
import com.e2esp.bergerpaints.livevisualizer.detectors.ColorBlobDetector;
import com.e2esp.bergerpaints.livevisualizer.R;
import com.e2esp.bergerpaints.livevisualizer.detectors.FloodFillDetector;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnFragmentInteractionListener;
import com.e2esp.bergerpaints.livevisualizer.utils.Utility;
import com.e2esp.bergerpaints.livevisualizer.views.CameraView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by Zain on 6/15/2017.
 */

public class CameraFragment extends Fragment implements View.OnTouchListener {
    private final String TAG = "CameraFragment";

    private static CameraFragment instance;

    private OnFragmentInteractionListener onFragmentInteractionListener;

    private boolean isBlobColorSelected = false;
    private boolean isFillColorSelected = false;
    private Mat mRgba;
    private Rect mTouchedRect;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private ColorBlobDetector mBlobDetector;
    private CannyEdgeDetector mCannyDetector;
    private FloodFillDetector mFloodDetector;
    private Scalar mContourColor;
    private Scalar mFillColorRgb;
    private Scalar mFillColorHsv;

    public boolean doingColorBlob;
    public boolean doingCanny;
    public boolean doingFloodFill;
    private boolean takePicture;

    private CameraView cameraView;

    public CameraFragment() {
    }

    public static CameraFragment newInstance() {
        instance = new CameraFragment();
        return instance;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            onFragmentInteractionListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onFragmentInteractionListener = null;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getContext()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    cameraView.enableView();
                    cameraView.setOnTouchListener(CameraFragment.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private void init(int cameraWidth, int cameraHeight) {
        mRgba = new Mat(cameraHeight, cameraWidth, CvType.CV_8UC4);
        mTouchedRect = new Rect();
        mBlobDetector = new ColorBlobDetector();
        mCannyDetector = new CannyEdgeDetector();
        mFloodDetector = new FloodFillDetector();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        mContourColor = new Scalar(255, 0, 0, 255);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        cameraView = (CameraView) view.findViewById(R.id.cameraView);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setMaxFrameSize(1280, 720);
        cameraView.setCvCameraViewListener(cameraViewListener);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraView != null)
            cameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, getContext(), mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null)
            cameraView.disableView();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (cameraView.getWidth() - cols) / 2;
        int yOffset = (cameraView.getHeight() - rows) / 2;

        int x = (int) event.getX() - xOffset;
        int y = (int) event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        mTouchedRect.x = (x > 4) ? x - 4 : 0;
        mTouchedRect.y = (y > 4) ? y - 4 : 0;

        mTouchedRect.width = (x + 4 < cols) ? x + 4 - mTouchedRect.x : cols - mTouchedRect.x;
        mTouchedRect.height = (y + 4 < rows) ? y + 4 - mTouchedRect.y : rows - mTouchedRect.y;

        isBlobColorSelected = true;
        //isFillColorSelected = false;

        //onFragmentInteractionListener.onInteraction(OnFragmentInteractionListener.CLEAR_COLOR_SELECTIONS, null);

        return false; // don't need subsequent touch events
    }

    private Scalar calculateBlobColorHsv() {
        Mat touchedRegionRgba = mRgba.submat(mTouchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        Scalar blobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = mTouchedRect.width * mTouchedRect.height;
        for (int i = 0; i < blobColorHsv.val.length; i++)
            blobColorHsv.val[i] /= pointCount;

        Log.i(TAG, "blobColorHsv: "+blobColorHsv);
        mBlobColorRgba = Utility.convertScalarHsv2Rgba(blobColorHsv);

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        //Log.i(TAG, "Touched Rect: "+mTouchedRect);
        //Log.i(TAG, "Touched RGBA: "+rgbaDump);
        //Log.i(TAG, "BloB Color HSV: "+blobColorHsv.toString());
        return blobColorHsv;
    }

    private Point touchPoint() {
        return new Point(mTouchedRect.x + mTouchedRect.width / 2, mTouchedRect.y + mTouchedRect.height / 2);
    }

    private void floodFill1(Mat img, int channel, double newValue, Point seedPoint, double variation) {
        Mat clone = img.clone();

        try {
            int rows = img.rows();
            int cols = img.cols();
            boolean[][] visited = new boolean[rows][cols];

            if (seedPoint.x >= 0 && seedPoint.x < rows &&
                    seedPoint.y >= 0 && seedPoint.y < cols) {
                Queue<Point> queue = new LinkedList<>();
                queue.add(seedPoint);

                double initialValue = img.get((int)seedPoint.x, (int)seedPoint.y)[channel];
                double loLimit = Math.max(0, initialValue - variation);
                double hiLimit = Math.min(180, initialValue + variation);

                int pixelCount = 0;
                while (!queue.isEmpty()) {
                    Point p = queue.remove();
                    if (!visited[(int) p.x][(int) p.y]
                            && setIfInRange(img, channel, newValue, p, loLimit, hiLimit)) {
                        visited[(int) p.y][(int) p.x] = true;
                        pixelCount++;

                        if (p.x + 1 < rows) {
                            queue.add(new Point(p.x + 1, p.y));
                        }
                        if (p.x - 1 >= 0) {
                            queue.add(new Point(p.x - 1, p.y));
                        }
                        if (p.y + 1 < cols) {
                            queue.add(new Point(p.x, p.y + 1));
                        }
                        if (p.y - 1 >= 0) {
                            queue.add(new Point(p.x, p.y - 1));
                        }
                    }
                    if (pixelCount % 10 == 0) {
                        Log.i(TAG, "FloodFill1 :: During Matched Pixels: "+pixelCount);
                    }
                }
                Log.i(TAG, "FloodFill1 :: Matched Pixels: "+pixelCount);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Mat diff = new Mat();
        Core.subtract(img, clone, diff);
        int nz = Core.countNonZero(diff);
        Log.i(TAG, "clonedDiff :: rows:"+diff.rows()+" cols:"+diff.cols()+" nz:"+nz);
        diff.release();
        clone.release();
    }

    private boolean setIfInRange(Mat img, int channel, double newValue, Point point, double loLimit, double hiLimit) {
        double value = img.get((int)point.x, (int)point.y)[channel];
        if (value >= loLimit && value <= hiLimit) {
            img.get((int)point.x, (int)point.y)[channel] = newValue;
            return true;
        }
        return false;
    }

    public void takePicture() {
        takePicture = true;
    }

    public void setFillColor(int color) {
        if (color == -1) {
            isFillColorSelected = false;
        } else {
            int[] rgb = Utility.colorIntToRgb(color);
            mFillColorRgb = new Scalar(rgb[0], rgb[1], rgb[2]);
            mFillColorHsv = Utility.convertScalarRgb2Hsv(mFillColorRgb);
            isFillColorSelected = true;
        }
    }

    public void setToleranceLevel(int channel, double level) {
        switch (channel) {
            case 0:
                Log.i(TAG, "Tolerance Hue: "+level);
                mBlobDetector.setColorRadius(level, -1, -1, -1);
                mFloodDetector.setColorRadius(level, -1, -1, -1);
                break;
            case 1:
                Log.i(TAG, "Tolerance Sat: "+level);
                mBlobDetector.setColorRadius(-1, level, -1, -1);
                mFloodDetector.setColorRadius(-1, level, -1, -1);
                break;
            case 2:
                Log.i(TAG, "Tolerance Val: "+level);
                mBlobDetector.setColorRadius(-1, -1, level, -1);
                mFloodDetector.setColorRadius(-1, -1, level, -1);
                break;
        }
        if (isBlobColorSelected) {
            mBlobDetector.setHsvColor(mBlobColorHsv);
        }
    }

    public void setModes(int dilate, int structure, int dilateSize, int mode, int method) {
        if (mBlobDetector != null) {
            mBlobDetector.setModes(dilate, structure, dilateSize, mode, method);
        }
        if (mFloodDetector != null) {
            mFloodDetector.setModes(dilate, structure, dilateSize);
        }
    }

    public void setCannyControls(int kernelSize, int threshold, double thresholdRatio, int sobelSize, boolean l2Gradient) {
        if (kernelSize != -1) {
            mCannyDetector.mKernelSize = kernelSize;
        }
        if (threshold != -1) {
            mCannyDetector.mThreshold = threshold;
        }
        if (thresholdRatio != -1) {
            mCannyDetector.mRatio = thresholdRatio;
        }
        if (sobelSize != -1) {
            mCannyDetector.mSobel = sobelSize;
        }
        mCannyDetector.mL2 = l2Gradient;
    }

    private Mat changeHS(Mat srcHsv, Scalar fillHsv, Mat mask) {
        // Split src into HSV channels
        List<Mat> hsvChannels = new ArrayList<>();
        Core.split(srcHsv, hsvChannels);

        // Separate H and S channels into separate Mat
        List<Mat> hsChannels = new ArrayList<>();
        hsChannels.add(hsvChannels.get(0));
        hsChannels.add(hsvChannels.get(1));
        Mat hsMat = new Mat();
        Core.merge(hsChannels, hsMat);

        // Set H and S channels to fill color using given mask
        hsMat.setTo(fillHsv, mask);
        Core.split(hsMat, hsChannels);
        hsMat.release();

        // Merge new H, S and old V channels into single Mat
        hsvChannels.set(0, hsChannels.get(0));
        hsvChannels.set(1, hsChannels.get(1));
        Mat destHsv = new Mat();
        Core.merge(hsvChannels, destHsv);
        for (Mat channel : hsvChannels) {
            channel.release();
        }

        return destHsv;
    }

    private Mat processCameraFrame(Mat inputRgba) {
        mRgba = inputRgba;

        if (doingColorBlob) {
            if (isBlobColorSelected && isFillColorSelected) {
                if (mBlobDetector.shouldUpdate() && !takePicture) {
                    Scalar blobColorHsv = calculateBlobColorHsv();
                    if (!mBlobColorHsv.equals(blobColorHsv)) {
                        mBlobColorHsv = blobColorHsv;
                        mBlobDetector.setHsvColor(mBlobColorHsv);
                    }
                    if (mBlobDetector.isUpdateNeeded()) {
                        mBlobDetector.process(inputRgba);
                    }
                }

                // Convert RGBA To HSV
                Mat orgHsv = new Mat();
                Imgproc.cvtColor(inputRgba, orgHsv, Imgproc.COLOR_RGB2HSV);

                // Draw contours
                List<MatOfPoint> contours = mBlobDetector.getContours();
                Imgproc.drawContours(inputRgba, contours, -1, mFillColorRgb, -1);

                // Generate mask from drawn Contours
                Mat conHsv = new Mat();
                Imgproc.cvtColor(inputRgba, conHsv, Imgproc.COLOR_RGB2HSV);
                Mat mask = new Mat();
                Core.inRange(conHsv, mFillColorHsv, mFillColorHsv, mask);
                conHsv.release();

                // Change H and S channels to Fill Color using Contours Mask
                Mat destHsv = changeHS(orgHsv, mFillColorHsv, mask);

                // Convert HSV back to RGB
                Imgproc.cvtColor(destHsv, inputRgba, Imgproc.COLOR_HSV2RGB);
                orgHsv.release();
                mask.release();
                destHsv.release();
            }
        } else if (doingFloodFill) {
            if (isBlobColorSelected && isFillColorSelected) {
                // Convert RGBA To HSV
                Mat orgHsv = new Mat();
                Imgproc.cvtColor(inputRgba, orgHsv, Imgproc.COLOR_RGB2HSV);

                if (mFloodDetector.shouldUpdate() && !takePicture) {
                    // Apply FloodFill to find matching area
                    Point touchPoint = touchPoint();
                    mFloodDetector.process(orgHsv, touchPoint);
                }

                // Change H and S channels to Fill Color using Flood Mask
                Mat destHsv = changeHS(orgHsv, mFillColorHsv, mFloodDetector.getMask());

                // Convert HSV back to RGB
                Imgproc.cvtColor(destHsv, inputRgba, Imgproc.COLOR_HSV2RGB);
                orgHsv.release();
                destHsv.release();
            }
        } else if (doingCanny) {
            inputRgba = mCannyDetector.process(inputRgba);
        }


        if (takePicture) {
            takePicture = false;
            onFragmentInteractionListener.onInteraction(OnFragmentInteractionListener.SHOW_STILL_SCREEN, inputRgba.clone());
        }

        return inputRgba;
    }

    private CameraBridgeViewBase.CvCameraViewListener2 cameraViewListener = new CameraBridgeViewBase.CvCameraViewListener2() {
        @Override
        public void onCameraViewStarted(int width, int height) {
            init(width, height);
        }
        @Override
        public void onCameraViewStopped() {
            mRgba.release();
        }
        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
            return processCameraFrame(inputFrame.rgba());
        }
    };

}
