package com.e2esp.bergerpaints.livevisualizer.views;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

import java.util.List;

/**
 * Created by Zain on 6/5/2017.
 */

public class CameraView extends JavaCameraView {

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setResolution(Camera.Size resolution) {
        disconnectCamera();
        mMaxHeight = resolution.height;
        mMaxWidth = resolution.width;
        int width = getWidth();
        int height = getHeight();
        connectCamera(width, height);
    }

    public Camera.Size getResolution(int width, int height) {
        return mCamera.new Size(width, height);
    }

    public Camera.Size getResolution() {
        return mCamera.getParameters().getPreviewSize();
    }

}
