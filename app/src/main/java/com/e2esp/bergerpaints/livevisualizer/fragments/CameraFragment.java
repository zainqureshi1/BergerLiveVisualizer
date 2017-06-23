package com.e2esp.bergerpaints.livevisualizer.fragments;

import android.content.Context;
import android.graphics.Bitmap;
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
    private final String TAG = this.getClass().getName();

    private static CameraFragment instance;

    private OnFragmentInteractionListener onFragmentInteractionListener;

    private boolean isBlobColorSelected = false;
    private boolean isFillColorSelected = false;
    private Mat mRgba;
    private Rect mTouchedRect;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private ColorBlobDetector mDetector;
    private CannyEdgeDetector mCannyDetector;
    private Scalar mContourColor;
    private Scalar mFillColorRgb;
    private Scalar mFillColorHsv;

    public boolean doingCanny;
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
        mDetector = new ColorBlobDetector();
        mCannyDetector = new CannyEdgeDetector();
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
        isFillColorSelected = false;

        onFragmentInteractionListener.onInteraction(OnFragmentInteractionListener.CLEAR_COLOR_SELECTIONS, null);

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

    private Mat floodFill(Mat img, Point seedPoint, Scalar newVal)
    {
        Mat clone = img.clone();

        Mat mask = Mat.zeros(img.rows() + 2, img.cols() + 2, CvType.CV_8U);
        double newHue = newVal.val[0];
        Imgproc.floodFill(img, mask, seedPoint, new Scalar(newHue), new Rect(), new Scalar(10), new Scalar(10), 8 + Imgproc.FLOODFILL_FIXED_RANGE);//, 4 + (255 << 8) + Imgproc.FLOODFILL_MASK_ONLY);

        //Core.subtract(floodfilled, Scalar.all(0), floodfilled);

        //Rect roi = new Rect(1, 1, img.cols(), img.rows());
        //Mat result = new Mat();
        //floodfilled.submat(roi).copyTo(result);

        Mat diff = new Mat();
        Core.subtract(img, clone, diff);
        int nz = Core.countNonZero(diff);
        Log.i(TAG, "clonedDiff :: rows:"+diff.rows()+" cols:"+diff.cols()+" nz:"+nz);

        return img;
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
        int[] rgb = Utility.colorIntToRgb(color);
        mFillColorRgb = new Scalar(rgb[0], rgb[1], rgb[2]);
        mFillColorHsv = Utility.convertScalarRgb2Hsv(mFillColorRgb);

        isFillColorSelected = true;
    }

    public void setToleranceLevel(int channel, double level) {
        switch (channel) {
            case 0:
                Log.i(TAG, "Tolerance Hue: "+level);
                mDetector.setColorRadius(level, -1, -1, -1);
                break;
            case 1:
                Log.i(TAG, "Tolerance Sat: "+level);
                mDetector.setColorRadius(-1, level, -1, -1);
                break;
            case 2:
                Log.i(TAG, "Tolerance Val: "+level);
                mDetector.setColorRadius(-1, -1, level, -1);
                break;
        }
        if (isBlobColorSelected) {
            mDetector.setHsvColor(mBlobColorHsv);
        }
    }

    public void setModes(int dilate, int structure, int dilateSize, int mode, int method) {
        if (dilate != -1) {
            mDetector.mDilationIterations = dilate;
        }
        if (structure != -1) {
            mDetector.mStructure = structure;
        }
        if (dilateSize != -1) {
            mDetector.mDilationSize = dilateSize;
        }
        if (mode != -1) {
            mDetector.mContourMode = mode;
        }
        if (method != -1) {
            mDetector.mContourMethod = method;
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

    private Mat processCameraFrame(Mat inputRgba) {
        mRgba = inputRgba;

        if (doingCanny) {
            return mCannyDetector.process(inputRgba);
        }

        if (isBlobColorSelected) {
            if (mDetector.shouldUpdate() && !takePicture) {
                Scalar blobColorHsv = calculateBlobColorHsv();
                if (!mBlobColorHsv.equals(blobColorHsv)) {
                    mBlobColorHsv = blobColorHsv;
                    mDetector.setHsvColor(mBlobColorHsv);
                }
                if (mDetector.isUpdateNeeded()) {
                    mDetector.process(inputRgba);
                }
            }

            List<MatOfPoint> contours = mDetector.getContours();
            //Log.i(TAG, "Contours count: " + contours.size());
            if (isFillColorSelected) {
                Log.i(TAG, "mFillColorHsv: "+mFillColorHsv);

                Mat orgHsv = new Mat();
                Imgproc.cvtColor(inputRgba, orgHsv, Imgproc.COLOR_RGB2HSV);

                List<Mat> channels = new ArrayList<>();
                Core.split(orgHsv, channels);
                orgHsv.release();

                Imgproc.drawContours(inputRgba, contours, -1, mFillColorRgb, -1);
                Mat conHsv = new Mat();
                Imgproc.cvtColor(inputRgba, conHsv, Imgproc.COLOR_RGB2HSV);

                Mat mask = new Mat();
                Core.inRange(conHsv, mFillColorHsv, mFillColorHsv, mask);
                conHsv.release();

                List<Mat> hsChannels = new ArrayList<>();
                hsChannels.add(channels.get(0));
                hsChannels.add(channels.get(1));
                Mat newHsv = new Mat();
                Core.merge(hsChannels, newHsv);

                newHsv.setTo(new Scalar(mFillColorHsv.val[0], mFillColorHsv.val[1]), mask);
                Core.split(newHsv, hsChannels);

                channels.set(0, hsChannels.get(0));
                channels.set(1, hsChannels.get(1));

                Mat destHsv = new Mat();
                Core.merge(channels, destHsv);
                for (Mat channel : channels) {
                    channel.release();
                }

                Imgproc.cvtColor(destHsv, inputRgba, Imgproc.COLOR_HSV2RGB);
                destHsv.release();

                /*Imgproc.drawContours(hsv, contours, -1, new Scalar(mFillColorHsv.val[0]), -1);

                Point touchPoint = touchPoint();*/

                /*List<Mat> channels = new ArrayList<>();
                Core.split(hsv, channels);

                Log.i(TAG, "FloodFill: before: Src: "+channels.get(0));
                Mat channel = floodFill(channels.get(0), touchPoint, mFillColorHsv);
                Log.i(TAG, "FloodFill: after: Src: "+channels.get(0));
                Log.i(TAG, "FloodFill: after: Dst: "+channel);
                channels.set(0, channel);

                double newHue = mFillColorHsv.val[0];
                Mat hueMat = new Mat(channel.rows(), channel.cols(), channel.type(), new Scalar(newHue));
                Mat hueDiff = new Mat();
                Core.subtract(channel, hueMat, hueDiff);
                int nz = Core.countNonZero(hueDiff);
                Log.i(TAG, "Hue Diff:: rows:"+hueDiff.rows()+" cols:"+hueDiff.cols()+" nz:"+nz);

                Mat newHsv = new Mat();
                Core.merge(channels, newHsv);
                Mat diff = new Mat();
                Core.subtract(hsv, newHsv, diff);
                List<Mat> diffChs = new ArrayList<>();
                Core.split(diff, diffChs);
                Mat hueCh = diffChs.get(0);
                //Imgproc.pyrDown(diff, diff);
                //Imgproc.pyrDown(diff, diff);
                int rows = hueCh.rows();
                int cols = hueCh.cols();
                Mat q1 = hueCh.submat(0, rows/2, 0, cols/2);
                Mat q2 = hueCh.submat(rows/2+1, rows-1, 0, cols/2);
                Mat q3 = hueCh.submat(0, rows/2, cols/2+1, cols-1);
                Mat q4 = hueCh.submat(rows/2+1, rows-1, cols/2+1, cols-1);
                int perQ = (rows * cols) / 4;
                int q1z = perQ - Core.countNonZero(q1);
                int q2z = perQ - Core.countNonZero(q2);
                int q3z = perQ - Core.countNonZero(q3);
                int q4z = perQ - Core.countNonZero(q4);
                Log.i(TAG, "Difference Hue rows: "+rows+" cols: "+cols+" perQ: "+perQ);
                Log.i(TAG, "Difference Hue q1z: "+q1z+" q2z: "+q2z+" q3z: "+q3z+" q4z: "+q4z);*/

                //Imgproc.cvtColor(hsv, inputRgba, Imgproc.COLOR_HSV2RGB);
            } else if (!takePicture) {
                Imgproc.drawContours(inputRgba, contours, -1, mContourColor, 1);
            }
        }

        if (takePicture) {
            takePicture = false;
            Bitmap bitmap = Utility.matToBitmap(inputRgba);
            onFragmentInteractionListener.onInteraction(OnFragmentInteractionListener.SHOW_STILL_SCREEN, bitmap);
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
