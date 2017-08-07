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
import com.e2esp.bergerpaints.livevisualizer.detectors.FloodFillDetector;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnFragmentInteractionListener;
import com.e2esp.bergerpaints.livevisualizer.models.FillResult;
import com.e2esp.bergerpaints.livevisualizer.utils.Utility;
import com.e2esp.bergerpaints.livevisualizer.views.CameraView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
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
import java.util.List;

/**
 * Created by Zain on 6/15/2017.
 */

public class CameraFragment extends Fragment implements View.OnTouchListener {
    private final String TAG = "CameraFragment";

    private OnFragmentInteractionListener onFragmentInteractionListener;

    private boolean isBlobColorSelected = false;
    private boolean isFillColorSelected = false;
    private Mat mRgba;
    private Rect mTouchedRect;
    private Scalar mBlobColorHsv;
    private FloodFillDetector mFloodDetector;
    private Scalar mFillColorRgb;
    private Scalar mFillColorHsv;

    private ArrayList<FillResult> mFillResults;

    private boolean takePicture;

    private CameraView cameraView;

    public CameraFragment() {
    }

    public static CameraFragment newInstance() {
        CameraFragment instance = new CameraFragment();
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
        mFloodDetector = new FloodFillDetector();
        mBlobColorHsv = new Scalar(255);
        mFillResults = new ArrayList<>();
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

    public void takePicture() {
        takePicture = true;
    }

    public void setFillColor(int color) {
        if (color == -1) {
            isFillColorSelected = false;
            if (mFillResults != null) {
                mFillResults.clear();
            }
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
                mFloodDetector.setColorRadius(level, -1, -1, -1);
                break;
            case 1:
                Log.i(TAG, "Tolerance Sat: "+level);
                mFloodDetector.setColorRadius(-1, level, -1, -1);
                break;
            case 2:
                Log.i(TAG, "Tolerance Val: "+level);
                mFloodDetector.setColorRadius(-1, -1, level, -1);
                break;
        }
    }

    public void setModes(int dilate, int structure, int dilateSize, int mode, int method) {
        if (mFloodDetector != null) {
            mFloodDetector.setModes(dilate, structure, dilateSize);
        }
    }

    private void changeHS(Mat hsvMat, Scalar fillHsv, Mat mask) {
        // Split src into HSV channels
        List<Mat> hsvChannels = new ArrayList<>();
        Core.split(hsvMat, hsvChannels);

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
        hsvMat.setTo(Scalar.all(0));
        Core.merge(hsvChannels, hsvMat);
        for (Mat channel : hsvChannels) {
            channel.release();
        }
    }

    private Mat processCameraFrame(Mat inputRgba) {
        mRgba = inputRgba;
        Mat hsvMat = null;
        boolean handledTouch = false;
        Point touchPoint = null;
        if (isBlobColorSelected && isFillColorSelected) {
            touchPoint = touchPoint();
        }

        if (mFillResults.size() > 0) {
            // Convert RGBA To HSV
            hsvMat = new Mat();
            Imgproc.cvtColor(inputRgba, hsvMat, Imgproc.COLOR_RGB2HSV);
            boolean updateMasks = mFloodDetector.shouldUpdate() && !takePicture;

            // Apply any previous selections first
            for (int i = 0; i < mFillResults.size(); i++) {
                FillResult fillResult = mFillResults.get(i);

                // Check if new selection part of previous mask
                if (!handledTouch && isBlobColorSelected && isFillColorSelected) {
                    double[] maskAtPoint = fillResult.getMask().get((int)touchPoint.y, (int)touchPoint.x);
                    if (maskAtPoint != null && maskAtPoint.length > 0 && maskAtPoint[0] > 0) {
                        fillResult.setTouchPoint(touchPoint);
                        fillResult.setColor(mFillColorHsv);
                        handledTouch = true;
                    }
                }

                // Update mask if required
                if (updateMasks) {
                    // Apply FloodFill to find matching area
                    mFloodDetector.setPreviousNonZero(fillResult.getPreviousNonZero());
                    if (mFloodDetector.process(hsvMat, fillResult.getTouchPoint())) {
                        fillResult.setMask(mFloodDetector.getMask().clone());
                        fillResult.setPreviousNonZero(mFloodDetector.getPreviousNonZero());
                    }
                }

                // Change H and S channels to Fill Color using Flood Mask
                changeHS(hsvMat, fillResult.getColor(), fillResult.getMask());
            }
        }

        if (!handledTouch && !takePicture && isBlobColorSelected && isFillColorSelected) {
            if (hsvMat == null) {
                // Convert RGBA To HSV
                hsvMat = new Mat();
                Imgproc.cvtColor(inputRgba, hsvMat, Imgproc.COLOR_RGB2HSV);
            }

            // Apply FloodFill to find matching area
            mFloodDetector.setPreviousNonZero(0);
            if (mFloodDetector.process(hsvMat, touchPoint)) {
                FillResult fillResult = new FillResult(touchPoint, mFillColorHsv, mFloodDetector.getPreviousNonZero(), mFloodDetector.getMask().clone());
                mFillResults.add(fillResult);

                // Change H and S channels to Fill Color using Flood Mask
                changeHS(hsvMat, fillResult.getColor(), fillResult.getMask());
            }
        }
        if (hsvMat != null) {
            // Convert HSV back to RGB
            Imgproc.cvtColor(hsvMat, inputRgba, Imgproc.COLOR_HSV2RGB);
            hsvMat.release();
        }

        if (takePicture) {
            takePicture = false;
            ArrayList<Mat> mFillMasks = new ArrayList<>();
            for (FillResult fillResult: mFillResults) {
                mFillMasks.add(fillResult.getMask());
            }
            onFragmentInteractionListener.onInteraction(OnFragmentInteractionListener.SHOW_STILL_SCREEN, inputRgba.clone(), mFillMasks);
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
