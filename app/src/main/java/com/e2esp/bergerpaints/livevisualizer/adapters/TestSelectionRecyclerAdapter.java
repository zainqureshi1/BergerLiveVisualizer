package com.e2esp.bergerpaints.livevisualizer.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.e2esp.bergerpaints.livevisualizer.R;
import com.e2esp.bergerpaints.livevisualizer.models.TestSelection;

import java.util.ArrayList;

/**
 * Created by Zain on 1/31/2017.
 */

public class TestSelectionRecyclerAdapter extends RecyclerView.Adapter<TestSelectionRecyclerAdapter.SelectionsViewHolder> {

    private Context context;
    private ArrayList<TestSelection> selectionsList;

    public TestSelectionRecyclerAdapter(Context context, ArrayList<TestSelection> selectionsList) {
        this.context = context;
        this.selectionsList = selectionsList;
    }

    @Override
    public SelectionsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_test_selection, parent, false);
        return new SelectionsViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return selectionsList.size();
    }

    @Override
    public void onBindViewHolder(SelectionsViewHolder holder, int position) {
        holder.bindView(selectionsList.get(position));
    }

    public class SelectionsViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageViewSelection;
        private TextView textViewSelectionName;

        public SelectionsViewHolder(View itemView) {
            super(itemView);
            imageViewSelection = (ImageView) itemView.findViewById(R.id.imageViewSelection);
            textViewSelectionName = (TextView) itemView.findViewById(R.id.textViewSelectionName);
        }

        public void bindView(TestSelection selection) {
            imageViewSelection.setImageBitmap(selection.getBitmap());
            textViewSelectionName.setText(selection.getName());
        }

    }

}
