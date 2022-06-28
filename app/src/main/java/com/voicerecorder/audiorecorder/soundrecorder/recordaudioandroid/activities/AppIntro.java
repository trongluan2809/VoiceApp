package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.ads.control.ads.Admod;
import com.ads.control.funtion.AdCallback;
import com.github.axet.audiorecorder.R;
import com.github.axet.audiorecorder.databinding.ActivityAppIntroBinding;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.Common;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.fragment.FragmentIntro1;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.fragment.FragmentIntro2;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.fragment.FragmentIntro3;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SystemUtil;

public class AppIntro extends AppCompatActivity {
    private ActivityAppIntroBinding binding;
    private int position = 0;
    InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAppIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Set Language
        SystemUtil.setLocale(getBaseContext());


        loadAdsInterIntro();

        binding.fabNext.setOnClickListener(v -> {
            if (position < 2) {
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position + 1));
            } else {

                if (Common.checkNetWork(this)){
                    Admod.getInstance().forceShowInterstitial(this,
                            mInterstitialAd,
                            new AdCallback(){
                                @Override
                                public void onAdClosed() {
                                    startMainActivity();
                                }

                                @Override
                                public void onAdFailedToLoad(@Nullable @org.jetbrains.annotations.Nullable LoadAdError i) {
                                    startMainActivity();
                                }
                            });
                }else {
                    startMainActivity();
                }
            }
        });

        binding.fabPrev.setOnClickListener(v -> {
            if (position > 0) {
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position - 1));
            }
        });

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                position = tab.getPosition();
                if (position == 0) binding.fabPrev.setVisibility(View.INVISIBLE);
                else binding.fabPrev.setVisibility(View.VISIBLE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        binding.viewPager2.setAdapter(new ViewPager2Adapter(this));
        new TabLayoutMediator(binding.tabLayout, binding.viewPager2, (tab, position) -> {
            // Disable touch for Tab Layout
            tab.view.setClickable(false);
        }).attach();
    }

    private void startMainActivity() {

        startMain();

    }

    private void startMain() {
        SharedPreferences.Editor editor = getSharedPreferences("MY_PREFS_GUIDE", MODE_PRIVATE).edit();
        editor.putBoolean("guided", true);
        editor.apply();
        startActivity(new Intent(AppIntro.this, MainActivity.class));
        finish();
    }

    public static class ViewPager2Adapter extends FragmentStateAdapter {
        public ViewPager2Adapter(FragmentActivity activity) {
            super(activity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1:
                    return new FragmentIntro2();
                case 2:
                    return new FragmentIntro3();
                default:
                    return new FragmentIntro1();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int readExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return readExternalStoragePermission == PackageManager.PERMISSION_GRANTED && writeExternalStoragePermission == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void loadAdsInterIntro(){
        Admod.getInstance().getInterstitalAds(
                this,
                getResources().getString(R.string.inter_intro),new AdCallback(){
                    @Override
                    public void onInterstitialLoad(@Nullable @org.jetbrains.annotations.Nullable InterstitialAd interstitialAd) {
                        super.onInterstitialLoad(interstitialAd);
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@Nullable @org.jetbrains.annotations.Nullable LoadAdError i) {
                        super.onAdFailedToLoad(i);
                    }
                }
        );

    }
}