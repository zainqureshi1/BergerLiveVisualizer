package com.e2esp.bergerpaints.livevisualizer.fragments;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.e2esp.bergerpaints.livevisualizer.R;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnFragmentInteractionListener;
import com.e2esp.bergerpaints.livevisualizer.models.Options;
import com.e2esp.bergerpaints.livevisualizer.utils.PermissionManager;
import com.e2esp.bergerpaints.livevisualizer.views.DrawingView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Zain on 6/15/2017.
 */

public class StillFragment extends Fragment {
    private final String TAG = this.getClass().getName();

    private static StillFragment instance;

    private OnFragmentInteractionListener onFragmentInteractionListener;

    public static Bitmap mBitmap;

    private ViewGroup viewContainerDrawing;
    private DrawingView drawingView;

    public StillFragment() {
    }

    public static StillFragment newInstance() {
        instance = new StillFragment();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_still, container, false);

        viewContainerDrawing = (ViewGroup) view.findViewById(R.id.viewContainerDrawing);

        if (mBitmap != null) {
            drawingView = new DrawingView(getContext(), mBitmap);
            ViewGroup.LayoutParams drawingParams = new ViewGroup.LayoutParams(mBitmap.getWidth(), mBitmap.getHeight());
            viewContainerDrawing.addView(drawingView, drawingParams);
        }

        return view;
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
                save(fileName);
            }
            @Override
            public void onDenied() {
                Toast.makeText(getContext(), "Write permission denied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void save(String fileName) {
        try {
            File file = new File(getAppFolder(), fileName);
            file.createNewFile();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(byteArray);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private File getAppFolder() {
        try {
            File appFolder = new File(Environment.getExternalStorageDirectory(), getString(R.string.app_name));
            if (!appFolder.exists() || !appFolder.isDirectory()) {
                if (!appFolder.mkdir()) {
                    return null;
                }
            }
            return appFolder;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

}
