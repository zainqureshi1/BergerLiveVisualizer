package com.e2esp.bergerpaints.livevisualizer.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.e2esp.bergerpaints.livevisualizer.interfaces.OnProductClickListener;
import com.e2esp.bergerpaints.livevisualizer.models.Product;
import com.e2esp.bergerpaints.livevisualizer.R;

import java.util.ArrayList;

/**
 *
 * Created by Zain on 7/26/2017.
 */

public class ProductRecyclerAdapter extends RecyclerView.Adapter<ProductRecyclerAdapter.ProductViewHolder> {

    private LayoutInflater inflater;
    private ArrayList<Product> productsList;
    private OnProductClickListener onProductClickListener;

    public ProductRecyclerAdapter(Context context, ArrayList<Product> productsList, OnProductClickListener onProductClickListener) {
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.productsList = productsList;
        this.onProductClickListener = onProductClickListener;
    }

    @Override
    public ProductViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.card_product_layout, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return productsList.size();
    }

    @Override
    public void onBindViewHolder(ProductViewHolder holder, int position) {
        holder.bindView(productsList.get(position));
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;
        private TextView textViewTitle;
        private TextView textViewDescription;
        private TextView textViewLearnMore;
        private TextView textViewVisualizeColors;

        ProductViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewProduct);
            textViewTitle = itemView.findViewById(R.id.textViewProductTitle);
            textViewDescription = itemView.findViewById(R.id.textViewProductDescription);
            textViewLearnMore = itemView.findViewById(R.id.textViewProductLearnMore);
            textViewVisualizeColors = itemView.findViewById(R.id.textViewProductVisualizeColors);
        }

        void bindView(final Product product) {
            imageView.setImageResource(product.getImageRes());
            textViewTitle.setText(product.getName());
            textViewDescription.setText(product.getDescription());

            textViewLearnMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onProductClickListener.onLearnMoreClick(product);
                }
            });
            textViewVisualizeColors.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onProductClickListener.onVisualizeColorsClick(product);
                }
            });
        }

    }

}
