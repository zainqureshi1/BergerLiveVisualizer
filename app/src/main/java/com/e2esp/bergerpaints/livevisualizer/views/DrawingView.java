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
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnDrawingTouchListener;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnWatershedTabChangeListener;
import com.e2esp.bergerpaints.livevisualizer.utils.Utility;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

/**
 *
 * Created by Zain on 6/15/2017.
 */

public class DrawingView extends View {

    private Mat mMat;
    private Scalar mFillColorRgb;
    private Bitmap mBitmap;
    private Bitmap mInitialBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mPaint;
    private Paint mBitmapPaint;
    private WatershedSegmenter watershedSegmenter;

    private OnDrawingTouchListener onDrawingTouchListener;

    private boolean isWatershedding;

    public DrawingView(Context c) {
        super(c);
    }

    public DrawingView(Context c, Mat mat, Bitmap bitmap, OnWatershedTabChangeListener onWatershedTabChangeListener, OnDrawingTouchListener onDrawingTouchListener) {
        super(c);
        mMat = mat;
        mPath = new Path();
        mBitmap = bitmap;
        mInitialBitmap = bitmap.copy(bitmap.getConfig(), true);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(5f);

        watershedSegmenter = new WatershedSegmenter(mMat, onWatershedTabChangeListener);
        this.onDrawingTouchListener = onDrawingTouchListener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPath, mPaint);
    }

    public void startWatershedding() {
        isWatershedding = true;
    }

    public void stopWatershedding() {
        isWatershedding = false;
        changeImage(mInitialBitmap.copy(mInitialBitmap.getConfig(), true));
        watershedSegmenter.clearColorsTab(false);
    }

    public void changeImage(Bitmap bitmap) {
        if (mBitmap != null) {
            mBitmap.recycle();
        }
        mBitmap = bitmap;
        mCanvas.setBitmap(mBitmap);
        invalidate();
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        mPath.reset();
        if (mFillColorRgb == null) {
            mPaint.setColor(Color.WHITE);
        } else {
            int color = Utility.colorRgbToInt((int)mFillColorRgb.val[0], (int)mFillColorRgb.val[1], (int)mFillColorRgb.val[2]);
            mPaint.setColor(color);
        }
        mPath.moveTo(x, y);
        watershedSegmenter.startLine(new Point(x, y), mFillColorRgb, mBitmap.copy(mBitmap.getConfig(), true));
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
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (!isWatershedding) {
            if (onDrawingTouchListener != null) {
                onDrawingTouchListener.onTouched((int)x, (int)y);
            }
            return false;
        }

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
        changeImage(Utility.matToBitmap(mat));
        mInitialBitmap = mBitmap.copy(mBitmap.getConfig(), true);
    }

    public void changeAppliedColor() {
        Mat mat = watershedSegmenter.coloredWatershed(mMat, Utility.convertScalarRgb2Hsv(mFillColorRgb));
        changeImage(Utility.matToBitmap(mat));
        mInitialBitmap = mBitmap.copy(mBitmap.getConfig(), true);
    }

    public void removeLastLine() {
        Bitmap previousBitmap = watershedSegmenter.removeLastLine();
        if (previousBitmap != null) {
            changeImage(previousBitmap);
        }
    }

}
