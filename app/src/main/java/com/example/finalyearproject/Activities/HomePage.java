package com.example.finalyearproject.Activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import com.example.finalyearproject.R;
import com.example.finalyearproject.Fragments.TempFragment;
import com.hitomi.smlibrary.OnSpinMenuStateChangeListener;
import com.hitomi.smlibrary.SpinMenu;

import java.util.ArrayList;
import java.util.List;

public class HomePage extends AppCompatActivity {

    private SpinMenu spinMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        spinMenu = (SpinMenu) findViewById(R.id.spin_menu);

        List<String> hintStrList = new ArrayList<>();
        hintStrList.add("PC-SIMV");
        hintStrList.add("PC-CMV");
        hintStrList.add("Non Invasive");
        hintStrList.add("Volume Control");
        hintStrList.add("Test");
        hintStrList.add("Test");
        hintStrList.add("Test");
        hintStrList.add("Test");
        hintStrList.add("Test");

        spinMenu.setHintTextStrList(hintStrList);
        spinMenu.setHintTextColor(Color.parseColor("#FFFFFF"));
        spinMenu.setHintTextSize(14);

        spinMenu.setEnableGesture(true);
        final List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(new TempFragment());
        fragmentList.add(new TempFragment());
        fragmentList.add(new TempFragment());
        fragmentList.add(new TempFragment());
        fragmentList.add(new TempFragment());
        FragmentPagerAdapter fragmentPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragmentList.get(position);
            }

            @Override
            public int getCount() {
                return fragmentList.size();
            }
        };
        spinMenu.setFragmentAdapter(fragmentPagerAdapter);

        spinMenu.setOnSpinMenuStateChangeListener(new OnSpinMenuStateChangeListener() {
            @Override
            public void onMenuOpened() {
                Toast.makeText(getApplicationContext(), "SpinMenu opened", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMenuClosed() {
                Toast.makeText(getApplicationContext(), "SpinMenu closed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}