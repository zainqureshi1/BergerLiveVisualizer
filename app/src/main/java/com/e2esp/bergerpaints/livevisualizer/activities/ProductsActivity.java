package com.e2esp.bergerpaints.livevisualizer.activities;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.e2esp.bergerpaints.livevisualizer.R;
import com.e2esp.bergerpaints.livevisualizer.adapters.ProductRecyclerAdapter;
import com.e2esp.bergerpaints.livevisualizer.interfaces.OnProductClickListener;
import com.e2esp.bergerpaints.livevisualizer.models.Product;
import com.e2esp.bergerpaints.livevisualizer.utils.GridSpacingItemDecoration;
import com.e2esp.bergerpaints.livevisualizer.utils.Utility;

import java.util.ArrayList;

public class ProductsActivity extends AppCompatActivity {

    private ArrayList<Product> arrayListProducts;
    private ProductRecyclerAdapter productRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_products);

        setupView();
        loadProducts();
    }

    private void setupView() {
        RecyclerView recyclerViewProducts = (RecyclerView) findViewById(R.id.recyclerViewProducts);
        arrayListProducts = new ArrayList<>();
        productRecyclerAdapter = new ProductRecyclerAdapter(this, arrayListProducts, new OnProductClickListener() {
            @Override
            public void onLearnMoreClick(Product product) {
                learnMoreClicked(product);
            }
            @Override
            public void onVisualizeColorsClick(Product product) {
                visualizeColorsClicked(product);
            }
        });
        recyclerViewProducts.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        recyclerViewProducts.addItemDecoration(new GridSpacingItemDecoration(2, Utility.dpToPx(this, 10), true));
        recyclerViewProducts.setItemAnimator(new DefaultItemAnimator());
        recyclerViewProducts.setAdapter(productRecyclerAdapter);
    }

    private void loadProducts() {
        arrayListProducts.clear();

        arrayListProducts.add(new Product("Silk Emulsion", "Berger Silk Emulsion is a high quality acrylic emulsion. It has hiding power and color retention properties. It is a low odor emulsion with high stain resistance, outstanding water resistance, anti-fungal properties, excellent coverage and outstanding washability. It is a premium quality decorative paint which gives an attractive silk luster.", "http://berger.com.pk/business-lines/decorative-business/interiors/silk-emulsion/", R.drawable.berger_silk_emulsion));
        arrayListProducts.add(new Product("Elegance Matt Emulsion", "Berger Robbialac Matt Emulsion is top quality acrylic-based emulsion paint, ideal for interior surfaces. It dries out to a smooth finish which is durable and easily washable.", "http://berger.com.pk/business-lines/decorative-business/interiors/elegance-matt-emulsion/", R.drawable.berger_elegance_matt_emulsion));
        arrayListProducts.add(new Product("VIP Super Gloss Enamel", "VIP Super Gloss Enamel is a top quality synthetic enamel coating for interior and exterior use. It may be applied on prepared primed surfaces such as wood, metal, cement plaster, asbestos, cement sheets and hard board, etc.", "http://berger.com.pk/business-lines/decorative-business/wood-metal/vip-super-gloss-enamel/", R.drawable.berger_vip_super_gloss_enamel));
        arrayListProducts.add(new Product("Easy Clean Emulsion", "Eazy Clean Emulsion is a good quality washable vinyl emulsion paint which can be wiped clean by damp cloth.", "http://berger.com.pk/business-lines/decorative-business/interiors/eazy-clean-emulsion/", R.drawable.berger_easy_clean_emulsion));
        arrayListProducts.add(new Product("New SPD Smooth Emulsion", "SPD is a top quality smooth emulsion. It can be used on walls, ceilings, old and new cement, concrete, plasters, chipboard and hardboard.", "http://berger.com.pk/business-lines/decorative-business/interiors/spd-smooth-emulsion/", R.drawable.berger_new_spd_smooth_emulsion));
        arrayListProducts.add(new Product("All Rounder Matt Enamel", "Allrounder is a high quality matt finish enamel for walls, ceilings, old and new cement plasters, woodwork, metals, chipboard and hardboard. It is tough and long-lasting and is specially recommended for kitchens, bathrooms, corridors and staircase walls, etc.", "http://berger.com.pk/business-lines/decorative-business/wood-metal/allrounder-matt-enamel/", R.drawable.berger_allrounder_matt_enamel));
        arrayListProducts.add(new Product("Weathercoat Acrylic Exterior Finish", "Weathercoat is a smooth water based masonry exterior paint. It contains tough flexible resin pigmented with titanium dioxide and light fast pigments. Its smooth finish has the highest degree of durability and is resistant to all types of weather conditions.", "http://berger.com.pk/business-lines/decorative-business/exteriors/weathercoat-acrylic-exterior-finish/", R.drawable.berger_weathercoat_acrylic_exterior_finish));

        productRecyclerAdapter.notifyDataSetChanged();
    }

    private void learnMoreClicked(Product product) {
        openWebPage(product.getLink());
    }

    private void visualizeColorsClicked(Product product) {
        Intent intent = new Intent(this, VisualizerActivity.class);
        intent.putExtra(VisualizerActivity.EXTRA_SELECTED_PRODUCT, product);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.slide_out_to_top);
    }

    private void openWebPage (String link) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        startActivity(browserIntent);
        overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.slide_out_to_top);
    }

    private void gotoHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
        overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom);
    }

    @Override
    public void onBackPressed() {
        gotoHome();
    }

}
