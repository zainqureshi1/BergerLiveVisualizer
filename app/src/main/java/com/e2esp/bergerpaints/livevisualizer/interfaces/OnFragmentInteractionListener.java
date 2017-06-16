package com.e2esp.bergerpaints.livevisualizer.interfaces;

/**
 * Created by Zain on 6/15/2017.
 */

public interface OnFragmentInteractionListener {

    int CLEAR_COLOR_SELECTIONS = 1000;
    int SHOW_STILL_SCREEN = 1001;

    void onInteraction(int type, Object obj);

}
