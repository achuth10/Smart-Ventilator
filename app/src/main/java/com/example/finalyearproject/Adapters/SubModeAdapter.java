package com.example.finalyearproject.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalyearproject.R;

import java.util.ArrayList;
import java.util.List;

public class SubModeAdapter extends RecyclerView.Adapter<SubModeAdapter.SubModeHolder> {
    private Context context;
    private List<String> modes = new ArrayList<>();

    public SubModeAdapter(List<String> modes, Context context) {
        this.modes = modes;
        this.context = context;
    }

    @NonNull
    @Override
    public SubModeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.mode_card_layout,parent,false);
        return new SubModeHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SubModeHolder holder, int position) {
        String mode = modes.get(position);
        holder.setDetails(mode);
    }

    @Override
    public int getItemCount() {
        return modes==null?0 : modes.size();
    }

    static class SubModeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView modeTextView ;
        private CardView modeCard;

    public SubModeHolder(@NonNull View itemView) {
        super(itemView);
        modeTextView = itemView.findViewById(R.id.ModeName);
        modeCard = itemView.findViewById(R.id.ModeCard);
        modeCard.setOnClickListener(this);
    }

        public void setDetails(String mode) {
        }

        @Override
        public void onClick(View v) {

        }
    }
}
