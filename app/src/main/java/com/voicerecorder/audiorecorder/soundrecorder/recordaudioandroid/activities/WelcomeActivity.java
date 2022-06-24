package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.LinearLayout;

import com.amazic.ads.callback.InterCallback;
import com.amazic.ads.util.Admod;
import com.github.axet.audiorecorder.R;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SystemUtil;

public class WelcomeActivity extends AppCompatActivity {
    LinearLayout btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set Language
        SystemUtil.setLocale(getBaseContext());

        setContentView(R.layout.activity_welcome);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkPermission()) {
                Admod.getInstance().setOpenActivityAfterShowInterAds(true);
            } else {
                Admod.getInstance().setOpenActivityAfterShowInterAds(false);
            }
        } else {
            Admod.getInstance().setOpenActivityAfterShowInterAds(true);
        }

        btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            finish();
        });
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
}