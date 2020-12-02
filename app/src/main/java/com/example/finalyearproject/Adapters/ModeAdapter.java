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

public class ModeAdapter  extends RecyclerView.Adapter<ModeAdapter.ModeHolder> {
    private Context context;
    private List<String> modes = new ArrayList<>();

    public ModeAdapter(List<String> modes, Context context) {
        this.modes = modes;
        this.context = context;
    }


    @NonNull
    @Override
    public ModeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.mode_card_layout,parent,false);
        return new ModeHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ModeHolder holder, int position) {
        String mode = modes.get(position);
        holder.setDetails(mode);
    }

    @Override
    public int getItemCount() {
        return modes==null?0 : modes.size();
    }

    public  interface  onClickInterface{
        void clicked(int position);
    }

    public onClickInterface monClickInterface;

    public void setMonClickInterface(onClickInterface monClickInterface) {
        this.monClickInterface = monClickInterface;
    }

    class ModeHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private TextView modeTextView ;
    private CardView modeCard;
    public ModeHolder(View itemview ) {
        super(itemview);
        modeTextView = itemview.findViewById(R.id.ModeName);
        modeCard = itemview.findViewById(R.id.ModeCard);
        modeCard.setOnClickListener(this);
    }

        public void setDetails(String mode) {
            modeTextView.setText(mode);

    }

        @Override
        public void onClick(View v) {
            if (monClickInterface!=null)
                monClickInterface.clicked(getAdapterPosition());
        }
    }
}
