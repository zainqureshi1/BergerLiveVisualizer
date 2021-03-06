package com.e2esp.bergerpaints.livevisualizer.fragments;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.e2esp.bergerpaints.livevisualizer.R;
import com.e2esp.bergerpaints.livevisualizer.activities.VisualizerActivity;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnDrawingTouchListener;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnFragmentInteractionListener;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnWatershedTabChangeListener;
import com.e2esp.bergerpaints.livevisualizer.models.FillResult;
import com.e2esp.bergerpaints.livevisualizer.utils.PermissionManager;
import com.e2esp.bergerpaints.livevisualizer.utils.Utility;
import com.e2esp.bergerpaints.livevisualizer.views.DrawingView;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.io.File;
import java.util.ArrayList;

/**
 *
 * Created by Zain on 6/15/2017.
 */

public class StillFragment extends Fragment {
    //private final String TAG = "StillFragment";

    public static Mat mRgba;
    public static ArrayList<FillResult> mFillResults;
    private FillResult mFillResult;

    private Mat mOverlayMat;
    private Mat mUnderlayMat;
    private Mat mFinalMat;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_still, container, false);

        ViewGroup viewContainerDrawing = view.findViewById(R.id.viewContainerDrawing);

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (drawingView != null) {
            drawingView.destroy();
        }
        if (mOverlayMat != null) {
            mOverlayMat.release();
            mOverlayMat = null;
        }
        if (mUnderlayMat != null) {
            mUnderlayMat.release();
            mUnderlayMat = null;
        }
        if (mFinalMat != null) {
            mFinalMat.release();
            mFinalMat = null;
        }
    }

    public void setFillColor(int color) {
        mFillColor = color;
        if (drawingView != null) {
            drawingView.setFillColor(color);
        }
        if (color != -1) {
            if (!isWatershedding) {
                if (appliedWatershedding) {
                    if (drawingView != null) {
                        drawingView.changeAppliedColor();
                    }
                } else {
                    applyFloodFill();
                }
            }
        }
    }

    private Scalar getFillColorRgba() {
        if (mFillColor == -1) {
            return Scalar.all(0);
        }
        int[] rgb = Utility.colorIntToRgb(mFillColor);
        return new Scalar(rgb[0], rgb[1], rgb[2], VisualizerActivity.OVERLAY_ALPHA);
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
        String fileName = getString(R.string.app_name)+"_"+System.currentTimeMillis()+".jpg";
        saveFile(fileName);
    }

    /*public void saveImage(final Options options) {
        final Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_save);

        final EditText editText = dialog.findViewById(R.id.editTextSave);
        Button button = dialog.findViewById(R.id.buttonSave);

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
    }*/

    private void saveFile(final String fileName) {
        PermissionManager.getInstance().checkPermissionRequest(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE, 121, "App require permission to save image", new PermissionManager.Callback() {
            @Override
            public void onGranted() {
                if (drawingView == null) {
                    return;
                }
                Bitmap bitmap = drawingView.getImage();
                if (bitmap == null) {
                    return;
                }
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
        for (FillResult fillResult: mFillResults) {
            double[] maskAtPoint = fillResult.getMask().get(y, x);
            if (maskAtPoint != null && maskAtPoint.length > 0 && maskAtPoint[0] > 0) {
                mFillResult = fillResult;
                applyFloodFill();
                return;
            }
        }
    }

    private void applyFloodFill() {
        if (mFillResults == null || mFillResults.size() <= 0) {
            return;
        }
        if (mFillResult == null) {
            mFillResult = mFillResults.get(mFillResults.size() - 1);
        }
        if (mFillColor != -1) {
            mFillResult.setColor(getFillColorRgba());
        }

        if (mOverlayMat == null) {
            mOverlayMat = new Mat(mRgba.size(), CvType.CV_8UC4);
        }
        Scalar zero = Scalar.all(0);
        mOverlayMat.setTo(zero).release();
        if (mUnderlayMat == null) {
            mUnderlayMat = new Mat(mRgba.size(), CvType.CV_8UC4);
        }
        mRgba.copyTo(mUnderlayMat);

        for (FillResult fillResult: mFillResults) {
            mOverlayMat.setTo(fillResult.getColor(), fillResult.getMask()).release();
            mUnderlayMat.setTo(zero, fillResult.getMask()).release();
        }

        if (mFinalMat == null) {
            mFinalMat = new Mat(mRgba.size(), CvType.CV_8UC4);
        }
        double overlayAlpha = (double) VisualizerActivity.OVERLAY_ALPHA / 255.0;
        Core.addWeighted(mRgba, 1.0 - overlayAlpha, mOverlayMat, overlayAlpha, 0.0, mFinalMat);
        Core.addWeighted(mFinalMat, 1.0, mUnderlayMat, 1.0, 0.0, mFinalMat);

        Bitmap bitmap = Utility.matToBitmap(mFinalMat);
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
