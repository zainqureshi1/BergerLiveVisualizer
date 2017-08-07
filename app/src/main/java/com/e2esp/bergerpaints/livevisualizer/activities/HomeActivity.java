package com.e2esp.bergerpaints.livevisualizer.activities;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.e2esp.bergerpaints.livevisualizer.R;
import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_home);

        setupViews();
    }

    private void setupViews() {
        AppCompatTextView textViewProductsDescription = (AppCompatTextView) findViewById(R.id.textViewBergerProductsDescription);
        textViewProductsDescription.setMovementMethod(new ScrollingMovementMethod());
        AppCompatTextView textViewBergerVisualizerDescription = (AppCompatTextView) findViewById(R.id.textViewBergerVisualizerDescription);
        textViewBergerVisualizerDescription.setMovementMethod(new ScrollingMovementMethod());

        findViewById(R.id.buttonViewAllProducts).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewProducts();
            }
        });
        findViewById(R.id.buttonStartVisualizing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVisualizing();
            }
        });
    }

    private void viewProducts() {
        startActivity(new Intent(this, ProductsActivity.class));
        finish();
        overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.slide_out_to_top);
    }

    private void startVisualizing() {
        startActivity(new Intent(this, VisualizerActivity.class));
        finish();
        overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.slide_out_to_top);
    }

    private boolean backPressed = false;
    @Override
    public void onBackPressed() {
        if (backPressed) {
            super.onBackPressed();
            return;
        }

        backPressed = true;
        Toast.makeText(this, getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                backPressed = false;
            }
        }, 2000);
    }

}
