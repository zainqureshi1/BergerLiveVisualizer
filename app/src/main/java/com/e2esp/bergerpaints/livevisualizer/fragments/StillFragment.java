package com.e2esp.bergerpaints.livevisualizer.fragments;

import android.Manifest;
import android.app.Dialog;
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
import com.e2esp.bergerpaints.livevisualizer.models.Options;
import com.e2esp.bergerpaints.livevisualizer.utils.PermissionManager;
import com.e2esp.bergerpaints.livevisualizer.utils.Utility;
import com.e2esp.bergerpaints.livevisualizer.views.DrawingView;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zain on 6/15/2017.
 */

public class StillFragment extends Fragment {
    //private final String TAG = "StillFragment";

    public static Mat mRgba;
    public static Mat mFloodMask;

    //private OnFragmentInteractionListener onFragmentInteractionListener;

    private DrawingView drawingView;

    private boolean isWatershedding;

    public StillFragment() {
    }

    public static StillFragment newInstance() {
        return new StillFragment();
    }

    /*@Override
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
    }*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_still, container, false);

        ViewGroup viewContainerDrawing = (ViewGroup) view.findViewById(R.id.viewContainerDrawing);

        if (mRgba != null) {
            Bitmap bitmap = Utility.matToBitmap(mRgba);
            drawingView = new DrawingView(getContext(), mRgba, bitmap);
            ViewGroup.LayoutParams drawingParams = new ViewGroup.LayoutParams(bitmap.getWidth(), bitmap.getHeight());
            viewContainerDrawing.addView(drawingView, drawingParams);
        }

        return view;
    }

    public void setFillColor(int color) {
        if (drawingView != null) {
            drawingView.setFillColor(color);
        }
        if (!isWatershedding && color != -1) {
            applyFloodFill(color);
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
    }

    public void undoWatershedding() {
        drawingView.removeLastLine();
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

    private void applyFloodFill(int color) {
        int[] rgb = Utility.colorIntToRgb(color);
        Scalar colorRgb = new Scalar(rgb[0], rgb[1], rgb[2]);
        Scalar colorHsv = Utility.convertScalarRgb2Hsv(colorRgb);

        // Convert RGBA To HSV
        Mat orgHsv = new Mat();
        Imgproc.cvtColor(mRgba, orgHsv, Imgproc.COLOR_RGB2HSV);

        // Change H and S channels to Fill Color using Flood Mask
        Mat destHsv = changeHS(orgHsv, colorHsv, mFloodMask);

        // Convert HSV back to RGB
        Imgproc.cvtColor(destHsv, mRgba, Imgproc.COLOR_HSV2RGB);
        orgHsv.release();
        destHsv.release();

        Bitmap bitmap = Utility.matToBitmap(mRgba);
        drawingView.changeImage(bitmap);
    }

    private Mat changeHS(Mat srcHsv, Scalar fillHsv, Mat mask) {
        // Split src into HSV channels
        List<Mat> hsvChannels = new ArrayList<>();
        Core.split(srcHsv, hsvChannels);

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
        Mat destHsv = new Mat();
        Core.merge(hsvChannels, destHsv);
        for (Mat channel : hsvChannels) {
            channel.release();
        }

        return destHsv;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionManager.getInstance().onResult(requestCode, permissions, grantResults);
    }

}
