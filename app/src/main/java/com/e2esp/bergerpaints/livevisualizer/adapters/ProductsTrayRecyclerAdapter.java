package com.e2esp.bergerpaints.livevisualizer.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.e2esp.bergerpaints.livevisualizer.R;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnTraysProductClickListener;
import com.e2esp.bergerpaints.livevisualizer.models.ProductColor;

import java.util.ArrayList;

/**
 * Created by Zain on 3/6/2017.
 */

public class ProductsTrayRecyclerAdapter extends RecyclerView.Adapter<ProductsTrayRecyclerAdapter.ColorsViewHolder> {

    private Context context;
    private ArrayList<ProductColor> colorsList;
    private OnTraysProductClickListener onProductClickListener;

    public ProductsTrayRecyclerAdapter(Context context, ArrayList<ProductColor> colorsList, OnTraysProductClickListener onProductClickListener) {
        this.context = context;
        this.colorsList = colorsList;
        this.onProductClickListener = onProductClickListener;
    }

    @Override
    public ColorsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.tray_item_product_layout, parent, false);
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

    class ColorsViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageViewProductIcon;
        private TextView textViewProductName;

        ColorsViewHolder(View itemView) {
            super(itemView);
            imageViewProductIcon = (ImageView) itemView.findViewById(R.id.imageViewProductIcon);
            textViewProductName = (TextView) itemView.findViewById(R.id.textViewProductName);
        }

        void bindView(final ProductColor productColor) {
            imageViewProductIcon.setImageResource(productColor.getImageRes());
            textViewProductName.setText(productColor.getName());
            imageViewProductIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onProductClickListener.onProductColorClick(productColor);
                }
            });
        }

    }

}
