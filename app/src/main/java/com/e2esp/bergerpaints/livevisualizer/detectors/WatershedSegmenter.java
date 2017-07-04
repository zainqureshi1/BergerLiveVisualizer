package com.e2esp.bergerpaints.livevisualizer.detectors;

import android.graphics.Bitmap;
import android.util.Log;

import com.e2esp.bergerpaints.livevisualizer.utils.Utility;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Zain on 6/29/2017.
 */

public class WatershedSegmenter {
    private final String TAG = "WatershedSegmenter";

    private Point previousPoint;

    private Mat markers;
    private Mat markersMask;
    private Mat wsImage;

    private List<MatOfPoint> contours;
    private MatOfInt4 hierarchy;

    private List<SegmentColor> colorTab;
    private Random random;

    private Mat mask;

    public WatershedSegmenter(Mat image) {
        markers = new Mat();
        markersMask = new Mat();
        wsImage = new Mat();
        contours = new ArrayList<>();
        hierarchy = new MatOfInt4();
        colorTab = new ArrayList<>();
        random = new Random();
        mask = new Mat();

        Imgproc.cvtColor(image, markersMask, Imgproc.COLOR_RGB2GRAY);
        markersMask.setTo(Scalar.all(0));
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
    }

    public void drawLine(Point point) {
        Imgproc.line(markersMask, previousPoint, point, Scalar.all(255), 5, 8, 0);
        previousPoint = point;
    }

    public Bitmap removeLastLine() {
        int colors = colorTab.size();
        Bitmap bitmap = null;
        if (colors > 0) {
            SegmentColor color = colorTab.get(colors - 1);
            markersMask.release();
            markersMask = color.startingMask;
            bitmap = color.startingBitmap;
            colorTab.remove(colors - 1);
        }
        return bitmap;
    }

    public Mat watershed(Mat image) {
        Imgproc.cvtColor(image, wsImage, Imgproc.COLOR_RGB2HSV);
        Imgproc.findContours(markersMask, contours, hierarchy,
                Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.isEmpty()) {
            Log.e(TAG, "Contours is empty");
            return image;
        }

        markers = new Mat(markersMask.size(), CvType.CV_32S);
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
            //Imgproc.drawContours(image, contours, idx, colorTab.get(idx).color, 2);//, 8, hierarchy, Integer.MAX_VALUE, new Point());
        }
        /*int compCount = 0, idx = 0;
        for( ; idx >= 0; idx = (int) hierarchy.get(0, idx)[0], compCount++) {
            Imgproc.drawContours(markers, contours, idx, Scalar.all(compCount + 1), -1);//, 8, hierarchy, Integer.MAX_VALUE, new Point());
        }*/

        if (idx == 0) {
            Log.e(TAG, "Idx is zero");
            return image;
        }

        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2RGB);
        long t = System.currentTimeMillis();
        Imgproc.watershed(image, markers);
        t = System.currentTimeMillis() - t;
        Log.i(TAG, "Imgproc.watershed execution time: " + t + "ms");

        /*Core.inRange(markers, Scalar.all(-1), Scalar.all(-1), mask);
        wsImage.setTo(new Scalar(0, 0, 255), mask);
        Core.inRange(markers, Scalar.all(1), Scalar.all(idx + 1), mask);
        Core.bitwise_not(mask, mask);
        wsImage.setTo(Scalar.all(0), mask);*/

        // Separate H and S channels into separate Mat
        List<Mat> channels = new ArrayList<>();
        Core.split(wsImage, channels);
        List<Mat> hsChannels = new ArrayList<>();
        hsChannels.add(channels.get(0));
        hsChannels.add(channels.get(1));
        Mat hsMat = new Mat();
        Core.merge(hsChannels, hsMat);

        for (int i = 0 ; i < colorTab.size() ; i++) {
            SegmentColor segmentColor = colorTab.get(i);
            if (segmentColor.color != null) {
                Core.inRange(markers, Scalar.all(segmentColor.mark), Scalar.all(segmentColor.mark), mask);
                hsMat.setTo(new Scalar(segmentColor.color.val[0], segmentColor.color.val[1]), mask);
            }
            Log.i(TAG, "Repainting segment " + segmentColor.mark + " with pixels " + Core.countNonZero(mask) + " to color " + segmentColor.color);
        }
        Core.split(hsMat, hsChannels);
        colorTab.clear();

        // Merge new H, S and old V channels into single Mat
        channels.set(0, hsChannels.get(0));
        channels.set(1, hsChannels.get(1));
        Core.merge(channels, wsImage);
        for (Mat channel : channels) {
            channel.release();
        }
        Imgproc.cvtColor(wsImage, image, Imgproc.COLOR_HSV2RGB);

        return image;
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

    }

}