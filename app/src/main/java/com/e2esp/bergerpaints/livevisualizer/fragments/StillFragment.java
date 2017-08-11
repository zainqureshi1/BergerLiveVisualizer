package com.e2esp.bergerpaints.livevisualizer.fragments;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.e2esp.bergerpaints.livevisualizer.R;
import com.e2esp.bergerpaints.livevisualizer.detectors.MaskApplier;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnDrawingTouchListener;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnFragmentInteractionListener;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnWatershedTabChangeListener;
import com.e2esp.bergerpaints.livevisualizer.models.Options;
import com.e2esp.bergerpaints.livevisualizer.utils.PermissionManager;
import com.e2esp.bergerpaints.livevisualizer.utils.Utility;
import com.e2esp.bergerpaints.livevisualizer.views.DrawingView;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Zain on 6/15/2017.
 */

public class StillFragment extends Fragment {
    //private final String TAG = "StillFragment";

    public static Mat mRgba;
    public static ArrayList<Mat> mFloodMasks;
    private Mat mFloodMask;
    private MaskApplier mMaskApplier;

    private OnFragmentInteractionListener onFragmentInteractionListener;

    private DrawingView drawingView;

    private boolean isWatershedding;
    private boolean appliedWatershedding;

    private int mFillColor = -1;

    public StillFragment() {
    }

    public static StillFragment newInstance() {
        return new StillFragment();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_still, container, false);

        ViewGroup viewContainerDrawing = (ViewGroup) view.findViewById(R.id.viewContainerDrawing);

        if (mRgba != null) {
            Bitmap bitmap = Utility.matToBitmap(mRgba);
            drawingView = new DrawingView(getContext(), mRgba, bitmap, new OnWatershedTabChangeListener() {
                @Override
                public void onTabSizeChange(int coloredLines, int whiteLines) {
                    handleCroppingChange(coloredLines, whiteLines);
                }
            }, new OnDrawingTouchListener() {
                @Override
                public void onTouched(int x, int y) {
                    selectFloodMask(x, y);
                }
            });
            ViewGroup.LayoutParams drawingParams = new ViewGroup.LayoutParams(bitmap.getWidth(), bitmap.getHeight());
            viewContainerDrawing.addView(drawingView, drawingParams);
        }

        return view;
    }

    public void setFillColor(int color) {
        mFillColor = color;
        if (drawingView != null) {
            drawingView.setFillColor(color);
        }
        if (color != -1) {
            if (!isWatershedding) {
                if (appliedWatershedding) {
                    drawingView.changeAppliedColor();
                } else {
                    applyFloodFill();
                }
            }
        }
    }

    public void stopWatershedding() {
        isWatershedding = false;
        if (drawingView != null) {
            drawingView.stopWatershedding();
        }
    }

    public void startWatershedding() {
        isWatershedding = true;
        if (drawingView != null) {
            drawingView.startWatershedding();
        }
    }

    public void applyWatershedding() {
        drawingView.watershed();
        appliedWatershedding = true;
    }

    public void undoWatershedding() {
        drawingView.removeLastLine();
    }

    public void saveImage() {
        String fileName = getString(R.string.app_name)+"_"+System.currentTimeMillis()+".png";
        saveFile(fileName);
    }

    public void saveImage(final Options options) {
        final Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_save);

        final EditText editText = (EditText) dialog.findViewById(R.id.editTextSave);
        Button button = (Button) dialog.findViewById(R.id.buttonSave);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editText.getText().toString();
                if (name.isEmpty()) {
                    Toast.makeText(getContext(), "Enter name first", Toast.LENGTH_SHORT).show();
                    return;
                }
                name = name.replaceAll("_", " ");
                String fileName = System.currentTimeMillis()+"_"+name+"_"+options.getTextToSave()+".png";
                dialog.dismiss();
                saveFile(fileName);
            }
        });

        dialog.show();
    }

    private void saveFile(final String fileName) {
        PermissionManager.getInstance().checkPermissionRequest(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE, 121, "App require permission to save image", new PermissionManager.Callback() {
            @Override
            public void onGranted() {
                Bitmap bitmap = Utility.matToBitmap(mRgba);
                File file = Utility.saveToStorage(getContext(), bitmap, fileName);
                if (file != null) {
                    Utility.showToast(getContext(), "Image saved successfully in storage.");
                    Utility.broadcastGalleryUpdate(getContext(), file);
                } else {
                    Utility.showToast(getContext(), "Unable to save image. Try again.");
                }
            }
            @Override
            public void onDenied() {
                Toast.makeText(getContext(), "Write permission denied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectFloodMask(int x, int y) {
        for (Mat mask: mFloodMasks) {
            double[] maskAtPoint = mask.get(y, x);
            if (maskAtPoint != null && maskAtPoint.length > 0 && maskAtPoint[0] > 0) {
                mFloodMask = mask;
                applyFloodFill();
                return;
            }
        }
    }

    private void applyFloodFill() {
        if (mFillColor == -1) {
            return;
        }
        if (mFloodMask == null && mFloodMasks != null && mFloodMasks.size() > 0) {
            mFloodMask = mFloodMasks.get(mFloodMasks.size() - 1);
        }
        if (mFloodMask == null || mFloodMask.cols() <= 0 || mFloodMask.rows() <= 0) {
            return;
        }

        int[] rgb = Utility.colorIntToRgb(mFillColor);
        Scalar colorRgb = new Scalar(rgb[0], rgb[1], rgb[2]);
        Scalar colorHsv = Utility.convertScalarRgb2Hsv(colorRgb);

        // Convert RGBA To HSV
        Mat orgHsv = new Mat();
        Imgproc.cvtColor(mRgba, orgHsv, Imgproc.COLOR_RGB2HSV);

        // Change H and S channels to Fill Color using Flood Mask
        if (mMaskApplier == null) {
            mMaskApplier = new MaskApplier();
        }
        mMaskApplier.apply(orgHsv, colorHsv, mFloodMask);

        // Convert HSV back to RGB
        Imgproc.cvtColor(orgHsv, mRgba, Imgproc.COLOR_HSV2RGB);
        orgHsv.release();

        Bitmap bitmap = Utility.matToBitmap(mRgba);
        drawingView.changeImage(bitmap);
    }

    private void handleCroppingChange(int coloredLines, int whiteLines) {
        if (onFragmentInteractionListener != null) {
            boolean canUndo = coloredLines > 0 || whiteLines > 0;
            boolean canApply = coloredLines > 0 && whiteLines > 0;
            onFragmentInteractionListener.onInteraction(OnFragmentInteractionListener.TOGGLE_CROP_ACTIONS, isWatershedding, canUndo, canApply);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.getInstance().onResult(requestCode, permissions, grantResults);
    }

}
