package com.example.finalyearproject.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalyearproject.Classes.Setting;
import com.example.finalyearproject.R;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;

public class SettingAdapter extends RecyclerView.Adapter<SettingAdapter.SettingHolder> {
private Context context;
private ArrayList<Setting>settings;


public SettingAdapter(ArrayList<Setting> settings, Context context)
{
    this.settings = settings;
    this.context = context;
}
    @NonNull
    @Override
    public SettingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.setting_layout,parent,false);
        return new SettingHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingHolder holder, int position) {
        Setting setting = settings.get(position);
        holder.setDetails(setting);
    }

    @Override
    public int getItemCount() {
        return settings==null ? 0:settings.size();
    }

    class SettingHolder extends RecyclerView.ViewHolder{
        private TextView settingName;
        private Slider settingSlider;
        public SettingHolder(@NonNull View itemView) {
            super(itemView);
            settingName  = itemView.findViewById(R.id.SettingName);
            settingSlider  = itemView.findViewById(R.id.SettingSlider);
        }

        public void setDetails(final Setting setting) {
            settingName.setText(setting.getName());
            settingSlider.setValueFrom(setting.getfrom());
            settingSlider.setValueTo(setting.getto());
            settingSlider.setValue(setting.getfrom());
            settingSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(@NonNull Slider slider) {

                }

                @Override
                public void onStopTrackingTouch(@NonNull Slider slider) {

                    if(getAdapterPosition()==0) // Total Volume (ml)
                    {

                    }

                }
            });
        }
    }
}
