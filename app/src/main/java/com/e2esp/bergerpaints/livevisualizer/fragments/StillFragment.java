package com.e2esp.bergerpaints.livevisualizer.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.e2esp.bergerpaints.livevisualizer.R;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnFragmentInteractionListener;
import com.e2esp.bergerpaints.livevisualizer.views.DrawingView;

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

}
