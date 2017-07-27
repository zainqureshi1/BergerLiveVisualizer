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
 * Created by Zain on 7/26/2017.
 */

public class ProductRecyclerAdapter extends RecyclerView.Adapter<ProductRecyclerAdapter.ProductViewHolder> {

    private Context context;
    private ArrayList<Product> productsList;
    private OnProductClickListener onProductClickListener;

    public ProductRecyclerAdapter(Context context, ArrayList<Product> productsList, OnProductClickListener onProductClickListener) {
        this.context = context;
        this.productsList = productsList;
        this.onProductClickListener = onProductClickListener;
    }

    @Override
    public ProductViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.card_product_layout, parent, false);
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
            imageView = (ImageView) itemView.findViewById(R.id.imageViewProduct);
            textViewTitle = (TextView) itemView.findViewById(R.id.textViewProductTitle);
            textViewDescription = (TextView) itemView.findViewById(R.id.textViewProductDescription);
            textViewLearnMore = (TextView) itemView.findViewById(R.id.textViewProductLearnMore);
            textViewVisualizeColors = (TextView) itemView.findViewById(R.id.textViewProductVisualizeColors);
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
