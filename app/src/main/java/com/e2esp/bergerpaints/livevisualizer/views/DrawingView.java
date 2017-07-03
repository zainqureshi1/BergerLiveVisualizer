package com.e2esp.bergerpaints.livevisualizer.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import com.e2esp.bergerpaints.livevisualizer.detectors.WatershedSegmenter;
import com.e2esp.bergerpaints.livevisualizer.utils.Utility;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Zain on 6/15/2017.
 */

public class DrawingView extends View {

    public int width;
    public int height;
    private Mat mMat;
    private Scalar mFillColorRgb;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mPaint;
    private Paint mBitmapPaint;
    private WatershedSegmenter watershedSegmenter;
    Context context;

    public DrawingView(Context c, Mat mat, Bitmap bitmap) {
        super(c);
        context = c;
        mMat = mat;
        mPath = new Path();
        mBitmap = bitmap;
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(5f);

        watershedSegmenter = new WatershedSegmenter(mMat);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        //mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        watershedSegmenter.startLine(new Point(x, y), mFillColorRgb);
        mX = x;
        mY = y;
    }

    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            watershedSegmenter.drawLine(new Point((x + mX) / 2, (y + mY) / 2));
            mX = x;
            mY = y;
        }
    }

    private void touch_up() {
        mPath.lineTo(mX, mY);
        watershedSegmenter.drawLine(new Point(mX, mY));
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        // kill this so we don't double draw
        mPath.reset();

        //watershed();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }

    public void setFillColor(int color) {
        if (color == -1) {
            mFillColorRgb = null;
        } else {
            int[] rgb = Utility.colorIntToRgb(color);
            mFillColorRgb = new Scalar(rgb[0], rgb[1], rgb[2]);
        }
    }

    public void watershed() {
        Mat mat = watershedSegmenter.watershed(mMat);
        mBitmap = Utility.matToBitmap(mat);
        mCanvas.setBitmap(mBitmap);
        invalidate();
    }

    /*private Mat watershed(Mat rgba) {
        Mat threeChannel = new Mat();
        Imgproc.cvtColor(rgba, threeChannel, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(threeChannel, threeChannel, 100, 255, Imgproc.THRESH_BINARY);

        Mat fg = new Mat(rgba.size(), CvType.CV_8U);
        Imgproc.erode(threeChannel,fg,new Mat(),new Point(-1,-1),2);

        Mat bg = new Mat(rgba.size(),CvType.CV_8U);
        Imgproc.dilate(threeChannel,bg,new Mat(),new Point(-1,-1),3);
        Imgproc.threshold(bg,bg,1, 128,Imgproc.THRESH_BINARY_INV);

        Mat markers = new Mat(rgba.size(),CvType.CV_8U, new Scalar(0));
        Core.add(fg, bg, markers);

        WatershedSegmenter segmenter = new WatershedSegmenter();
        segmenter.setMarkers(markers);
        Mat result = segmenter.process(rgba);

        return result;
    }*/

}
