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

import com.e2esp.bergerpaints.livevisualizer.R;
import com.e2esp.bergerpaints.livevisualizer.activities.VisualizerActivity;
import com.e2esp.bergerpaints.livevisualizer.detectors.FloodFillDetector;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnFragmentInteractionListener;
import com.e2esp.bergerpaints.livevisualizer.models.FillResult;
import com.e2esp.bergerpaints.livevisualizer.utils.Utility;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

/**
 *
 * Created by Zain on 6/15/2017.
 */

public class CameraFragment extends Fragment implements View.OnTouchListener {
    private final String TAG = "CameraFragment";

    private OnFragmentInteractionListener onFragmentInteractionListener;

    private boolean isBlobColorSelected = false;
    private boolean isFillColorSelected = false;
    private boolean isTouchHandled = false;

    private int mMaskToUpdate = -1;
    private int cols = -1;
    private int rows = -1;
    private Mat mHsvMat;
    private Mat mOverlayMat;
    private Mat mUnderlayMat;

    private Rect mTouchedRect;
    private FloodFillDetector mFloodDetector;
    private Scalar mFillColorRgba;

    private ArrayList<FillResult> mFillResults;
    private FillResult mLatestFillResult;

    private boolean takePicture;

    private JavaCameraView cameraView;

    public CameraFragment() {
    }

    public static CameraFragment newInstance() {
        return new CameraFragment();
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

    private void init() {
        mHsvMat = new Mat();
        mTouchedRect = new Rect();
        mFloodDetector = new FloodFillDetector();
        mFillResults = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        cameraView = view.findViewById(R.id.cameraView);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        //cameraView.setMaxFrameSize(1280, 720);
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

        if (mHsvMat != null) {
            mHsvMat.release();
        }
        if (mOverlayMat != null) {
            mOverlayMat.release();
            mOverlayMat = null;
        }
        if (mUnderlayMat != null) {
            mUnderlayMat.release();
            mUnderlayMat = null;
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (cols <= 0 || rows <= 0) {
            return false;
        }

        float camWidth = cameraView.getWidth();
        float camHeight = cameraView.getHeight();
        float xFactor = cols / camWidth;
        float yFactor = rows / camHeight;

        int touchX = (int)(event.getX() * xFactor);
        int touchY = (int)(event.getY() * yFactor);

        if ((touchX < 0) || (touchY < 0) || (touchX > cols) || (touchY > rows)) {
            Log.e(TAG, "Touch: (" + touchX + ", " + touchY + ") outside image: (0, 0, " + cols + ", " + rows + ")");
            return false;
        }

        mTouchedRect.x = (touchX > 4) ? touchX - 4 : 0;
        mTouchedRect.y = (touchY > 4) ? touchY - 4 : 0;

        mTouchedRect.width = (touchX + 4 < cols) ? touchX + 4 - mTouchedRect.x : cols - mTouchedRect.x;
        mTouchedRect.height = (touchY + 4 < rows) ? touchY + 4 - mTouchedRect.y : rows - mTouchedRect.y;

        isTouchHandled = false;
        isBlobColorSelected = true;

        return v.performClick(); // don't need subsequent touch events
    }

    private Point touchPoint() {
        return new Point(mTouchedRect.x + mTouchedRect.width / 2, mTouchedRect.y + mTouchedRect.height / 2);
    }

    public void takePicture() {
        takePicture = true;
    }

    public void setFillColor(int color) {
        if (color == -1) {
            isFillColorSelected = false;
            if (mFillResults != null) {
                for (FillResult fillResult: mFillResults) {
                    fillResult.release();
                }
                mFillResults.clear();
            }
        } else {
            int[] rgb = Utility.colorIntToRgb(color);
            mFillColorRgba = new Scalar(rgb[0], rgb[1], rgb[2], VisualizerActivity.OVERLAY_ALPHA);
            isFillColorSelected = true;
            if (mLatestFillResult != null) {
                mLatestFillResult.setColor(mFillColorRgba);
            }
        }
    }

    public void setTolerance(int hue, int sat, int val) {
        if (mFloodDetector != null) {
            mFloodDetector.setColorRadius(hue, sat, val);
        }
    }

    public void setToleranceLevel(int channel, double level) {
        switch (channel) {
            case 0:
                Log.i(TAG, "Tolerance Hue: "+level);
                mFloodDetector.setColorRadius(level, -1, -1);
                break;
            case 1:
                Log.i(TAG, "Tolerance Sat: "+level);
                mFloodDetector.setColorRadius(-1, level, -1);
                break;
            case 2:
                Log.i(TAG, "Tolerance Val: "+level);
                mFloodDetector.setColorRadius(-1, -1, level);
                break;
        }
    }

    private Mat processCameraFrame(Mat inputRgba) {
        cols = inputRgba.cols();
        rows = inputRgba.rows();
        Point touchPoint = null;

        if (mOverlayMat == null) {
            mOverlayMat = new Mat(inputRgba.size(), CvType.CV_8UC4);
        }
        Scalar zero = Scalar.all(0);
        mOverlayMat.setTo(zero).release();
        if (mUnderlayMat == null) {
            mUnderlayMat = new Mat(inputRgba.size(), CvType.CV_8UC4);
        }
        inputRgba.copyTo(mUnderlayMat);

        boolean matConverted = false;
        if (mFillResults.size() > 0) {
            // Convert RGBA To HSV
            Imgproc.cvtColor(inputRgba, mHsvMat, Imgproc.COLOR_RGB2HSV);
            matConverted = true;
            boolean updateMask = mFloodDetector.shouldUpdate(mFillResults.size()) && !takePicture;
            if (updateMask) {
                mMaskToUpdate++;
                if (mMaskToUpdate >= mFillResults.size()) {
                    mMaskToUpdate = 0;
                }
            }

            // Apply any previous selections first
            for (int i = 0; i < mFillResults.size(); i++) {
                FillResult fillResult = mFillResults.get(i);

                // Check if new selection part of previous mask
                if (!isTouchHandled && isBlobColorSelected && isFillColorSelected) {
                    if (touchPoint == null) {
                        touchPoint = touchPoint();
                    }
                    double[] maskAtPoint = fillResult.getMask().get((int)touchPoint.y, (int)touchPoint.x);
                    if (maskAtPoint != null && maskAtPoint.length > 0 && maskAtPoint[0] > 0) {
                        fillResult.release();
                        mFillResults.remove(i);
                        i--;
                        isTouchHandled = true;
                        Log.i(TAG, "Removed previous Fill Result");
                        continue;
                        /*mLatestFillResult = fillResult;
                        fillResult.setColor(mFillColorRgba);
                        isTouchHandled = true;
                        Log.i(TAG, "Updated existing Fill Result");*/
                    }
                }

                // Update mask if required
                if (updateMask && i == mMaskToUpdate) {
                    // Apply FloodFill to find matching area
                    mFloodDetector.setPreviousNonZero(fillResult.getPreviousNonZero());
                    Mat floodMask = mFloodDetector.process(mHsvMat, fillResult.getTouchPoint());
                    if (floodMask != null) {
                        fillResult.setMask(floodMask);
                        fillResult.setPreviousNonZero(mFloodDetector.getPreviousNonZero());
                        //Log.i(TAG, "Updated mask "+i);
                    } else {
                        //Log.e(TAG, "Failed to update mask "+i);
                        // Must update this mask next time
                        fillResult.setPreviousNonZero(0);
                    }
                }

                mOverlayMat.setTo(fillResult.getColor(), fillResult.getMask()).release();
                mUnderlayMat.setTo(zero, fillResult.getMask()).release();
            }
        }

        if (!isTouchHandled && !takePicture && isBlobColorSelected && isFillColorSelected) {
            if (!matConverted) {
                // Convert RGBA To HSV
                Imgproc.cvtColor(inputRgba, mHsvMat, Imgproc.COLOR_RGB2HSV);
            }

            // Apply FloodFill to find matching area
            mFloodDetector.setPreviousNonZero(0);
            if (touchPoint == null) {
                touchPoint = touchPoint();
            }
            Mat floodMask = mFloodDetector.process(mHsvMat, touchPoint);
            if (floodMask != null) {
                mLatestFillResult = new FillResult(touchPoint, mFillColorRgba, mFloodDetector.getPreviousNonZero(), floodMask);
                mFillResults.add(mLatestFillResult);
                isTouchHandled = true;
                Log.i(TAG, "Added new Fill Result :: Total:"+mFillResults.size());

                mOverlayMat.setTo(mLatestFillResult.getColor(), mLatestFillResult.getMask()).release();
                mUnderlayMat.setTo(zero, mLatestFillResult.getMask()).release();
            }
        }

        if (takePicture) {
            takePicture = false;
            onFragmentInteractionListener.onInteraction(OnFragmentInteractionListener.SHOW_STILL_SCREEN, inputRgba.clone(), mFillResults.clone());
        }

        double overlayAlpha = (double) VisualizerActivity.OVERLAY_ALPHA / 255.0;
        Core.addWeighted(inputRgba, 1.0 - overlayAlpha, mOverlayMat, overlayAlpha, 0.0, inputRgba);
        Core.addWeighted(inputRgba, 1.0, mUnderlayMat, 1.0, 0.0, inputRgba);

        return inputRgba;
    }

    private CameraBridgeViewBase.CvCameraViewListener2 cameraViewListener = new CameraBridgeViewBase.CvCameraViewListener2() {
        @Override
        public void onCameraViewStarted(int width, int height) {
            init();
        }
        @Override
        public void onCameraViewStopped() {
        }
        @Override
        public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
            return processCameraFrame(inputFrame.rgba());
        }
    };

}
