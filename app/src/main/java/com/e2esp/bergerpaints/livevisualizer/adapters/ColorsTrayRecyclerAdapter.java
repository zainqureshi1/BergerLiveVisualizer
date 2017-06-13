package com.e2esp.bergerpaints.livevisualizer.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.e2esp.bergerpaints.livevisualizer.R;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnTraysColorClickListener;
import com.e2esp.bergerpaints.livevisualizer.models.PrimaryColor;

import java.util.ArrayList;

/**
 * Created by Zain on 1/31/2017.
 */

public class ColorsTrayRecyclerAdapter extends RecyclerView.Adapter<ColorsTrayRecyclerAdapter.ColorsViewHolder> {

    private Context context;
    private ArrayList<PrimaryColor> colorsList;
    private OnTraysColorClickListener onColorClickListener;

    public ColorsTrayRecyclerAdapter(Context context, ArrayList<PrimaryColor> colorsList, OnTraysColorClickListener onColorClickListener) {
        this.context = context;
        this.colorsList = colorsList;
        this.onColorClickListener = onColorClickListener;
    }

    @Override
    public ColorsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.tray_item_color_layout, parent, false);
        return new ColorsViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return colorsList.size();
    }

    @Override
    public void onBindViewHolder(ColorsViewHolder holder, int position) {
        holder.bindView(colorsList.get(position));
    }

    public class ColorsViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageViewPrimaryColor;

        public ColorsViewHolder(View itemView) {
            super(itemView);
            imageViewPrimaryColor = (ImageView) itemView.findViewById(R.id.imageViewPrimaryColor);
        }

        public void bindView(final PrimaryColor primaryColor) {
            imageViewPrimaryColor.setColorFilter(primaryColor.getColor());
            imageViewPrimaryColor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onColorClickListener.onPrimaryColorClick(primaryColor);
                }
            });
        }

    }

}
