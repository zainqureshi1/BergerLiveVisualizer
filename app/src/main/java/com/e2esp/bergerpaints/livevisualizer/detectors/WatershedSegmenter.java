package com.e2esp.bergerpaints.livevisualizer.detectors;

import android.graphics.Bitmap;
import android.util.Log;

import com.e2esp.bergerpaints.livevisualizer.interfaces.OnWatershedTabChangeListener;
import com.e2esp.bergerpaints.livevisualizer.utils.Utility;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zain on 6/29/2017.
 */

public class WatershedSegmenter {
    private final String TAG = "WatershedSegmenter";

    private Point previousPoint;

    private Mat markersMask;
    private Mat appliedMarkersMask;

    private List<SegmentColor> colorTab;
    private List<SegmentColor> appliedColorTab;

    private OnWatershedTabChangeListener onTabChangeListener;

    public WatershedSegmenter(Mat image, OnWatershedTabChangeListener onTabChangeListener) {
        markersMask = new Mat();
        appliedMarkersMask = new Mat();
        colorTab = new ArrayList<>();
        appliedColorTab = new ArrayList<>();

        Imgproc.cvtColor(image, markersMask, Imgproc.COLOR_RGB2GRAY);
        markersMask.setTo(Scalar.all(0));

        this.onTabChangeListener = onTabChangeListener;
    }

    public void startLine(Point point, Scalar color, Bitmap bitmap) {
        previousPoint = point;
        if (color != null) {
            Log.i(TAG, "Adding color in tab: r:" + color.val[0] + " g:" + color.val[1] + " b:" + color.val[2]);
            colorTab.add(new SegmentColor(point, Utility.convertScalarRgb2Hsv(color), markersMask.clone(), bitmap));
        } else {
            Log.i(TAG, "Adding null color in tab");
            colorTab.add(new SegmentColor(point, null, markersMask.clone(), bitmap));
        }
        publishTabSizeChange();
    }

    public void drawLine(Point point) {
        Imgproc.line(markersMask, previousPoint, point, Scalar.all(255), 5, 8, 0);
        previousPoint = point;
    }

    public Bitmap removeLastLine() {
        int colors = colorTab.size();
        Bitmap bitmap = null;
        if (colors > 0) {
            SegmentColor color = colorTab.remove(colors - 1);
            markersMask.release();
            markersMask = color.startingMask;
            bitmap = color.startingBitmap;
        }
        publishTabSizeChange();
        return bitmap;
    }

    private void publishTabSizeChange() {
        int colored = 0;
        int white = 0;
        for (SegmentColor segmentColor: colorTab) {
            if (segmentColor.color != null) {
                colored++;
            } else {
                white++;
            }
        }
        onTabChangeListener.onTabSizeChange(colored, white);
    }

    public Mat watershed(Mat image) {
        Mat hsvImage = new Mat();
        Imgproc.cvtColor(image, hsvImage, Imgproc.COLOR_RGB2HSV);

        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(markersMask, contours, new Mat(),
                Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.isEmpty()) {
            Log.e(TAG, "Contours is empty");
            return image;
        }

        Mat markers = new Mat(markersMask.size(), CvType.CV_32S);
        markers.setTo(Scalar.all(0));

        if (contours.size() != colorTab.size()) {
            Log.e(TAG, "Contours Size: " + contours.size() + " != ColorTab Size: " + colorTab.size());
            //return image;
        }

        int idx;
        for (idx = 0; idx < contours.size(); idx++) {
            MatOfPoint2f contour = new MatOfPoint2f(contours.get(idx).toArray());
            int mark = idx + 1;
            for (SegmentColor color: colorTab) {
                if (Imgproc.pointPolygonTest(contour, color.startPoint, false) >= 0) {
                    color.mark = mark;
                    break;
                }
            }
            Imgproc.drawContours(markers, contours, idx, Scalar.all(mark), -1);//, 8, hierarchy, Integer.MAX_VALUE, new Point());
        }

        for (MatOfPoint contour: contours) {
            contour.release();
        }
        contours.clear();

        if (idx == 0) {
            Log.e(TAG, "Idx is zero");
            return image;
        }

        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2RGB);
        long t = System.currentTimeMillis();
        Imgproc.watershed(image, markers);
        t = System.currentTimeMillis() - t;
        Log.i(TAG, "Imgproc.watershed execution time: " + t + "ms");

        // Separate H and S channels into separate Mat
        List<Mat> channels = new ArrayList<>();
        Core.split(hsvImage, channels);
        List<Mat> hsChannels = new ArrayList<>();
        hsChannels.add(channels.get(0));
        hsChannels.add(channels.get(1));
        Mat hsMat = new Mat();
        Core.merge(hsChannels, hsMat);

        Mat mask = new Mat();
        for (int i = 0 ; i < colorTab.size() ; i++) {
            SegmentColor segmentColor = colorTab.get(i);
            if (segmentColor.color != null) {
                Core.inRange(markers, Scalar.all(segmentColor.mark), Scalar.all(segmentColor.mark), mask);
                hsMat.setTo(new Scalar(segmentColor.color.val[0], segmentColor.color.val[1]), mask);
            }
            Log.i(TAG, "Repainting segment " + segmentColor.mark + " with pixels " + Core.countNonZero(mask) + " to color " + segmentColor.color);
        }
        Core.split(hsMat, hsChannels);

        markers.release();
        mask.release();

        clearColorsTab(true);
        publishTabSizeChange();

        // Merge new H, S and old V channels into single Mat
        channels.set(0, hsChannels.get(0));
        channels.set(1, hsChannels.get(1));
        Core.merge(channels, hsvImage);
        for (Mat channel : channels) {
            channel.release();
        }
        Imgproc.cvtColor(hsvImage, image, Imgproc.COLOR_HSV2RGB);
        hsvImage.release();

        return image;
    }

    public Mat coloredWatershed(Mat image, Scalar color) {
        if (appliedColorTab.size() < 2) {
            return watershed(image);
        }
        colorTab.clear();
        for (SegmentColor segmentColor: appliedColorTab) {
            if (segmentColor.color != null) {
                segmentColor.color = color;
            }
            colorTab.add(new SegmentColor(segmentColor));
        }
        markersMask.release();
        markersMask = appliedMarkersMask.clone();
        return watershed(image);
    }

    public void clearColorsTab(boolean applied) {
        if (applied) {
            for (SegmentColor segmentColor: appliedColorTab) {
                segmentColor.release();
            }
            appliedColorTab.clear();
            appliedMarkersMask.release();
            appliedMarkersMask = markersMask.clone();
        }
        for (SegmentColor segmentColor: colorTab) {
            if (applied) {
                appliedColorTab.add(segmentColor);
            } else {
                segmentColor.release();
            }
        }
        colorTab.clear();
        markersMask.setTo(Scalar.all(0));
    }

    private class SegmentColor {

        private Point startPoint;
        private Scalar color;
        private int mark;
        private Mat startingMask;
        private Bitmap startingBitmap;

        SegmentColor(Point startPoint, Scalar color, Mat startingMask, Bitmap startingBitmap) {
            this.startPoint = startPoint;
            this.color = color;
            this.startingMask = startingMask;
            this.startingBitmap = startingBitmap;
        }

        SegmentColor(SegmentColor other) {
            this.startPoint = other.startPoint;
            this.color = other.color;
            this.startingMask = other.startingMask;
            this.startingBitmap = other.startingBitmap;
        }

        private void release() {
            if (startingMask != null) {
                startingMask.release();
            }
            if (startingBitmap != null) {
                startingBitmap.recycle();
            }
        }

    }

}