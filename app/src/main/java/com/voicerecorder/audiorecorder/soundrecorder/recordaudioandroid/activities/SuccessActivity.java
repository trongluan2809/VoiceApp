package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.amazic.ads.callback.InterCallback;
import com.amazic.ads.callback.NativeCallback;
import com.amazic.ads.util.Admod;
import com.bumptech.glide.Glide;
import com.github.axet.androidlibrary.activities.AppCompatThemeActivity;
import com.github.axet.audiorecorder.R;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.Common;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.AudioApplication;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.Storage;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SystemUtil;

import java.util.Locale;

public class SuccessActivity extends AppCompatThemeActivity {

    private Storage storage;
    private Toolbar toolbar;
    private ImageView ivLoading;
    private LinearLayout llLoading, llSuccess;
    private Button btnPreview;
    private InterstitialAd mInterPreview;
    private TextView tvFile;

    @Override
    public int getAppTheme() {
        return AudioApplication.getTheme(this, R.style.RecThemeLight_NoActionBar, R.style.RecThemeDark_NoActionBar);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set Language
        SystemUtil.setLocale(getBaseContext());

        Common.changeColor(this);

        setContentView(R.layout.activity_success);

        toolbar = findViewById(R.id.toolbar);
        ivLoading = findViewById(R.id.iv_loading);
        llLoading = findViewById(R.id.ll_loading);
        llSuccess = findViewById(R.id.ll_success);
        btnPreview = findViewById(R.id.iv_preview);
        tvFile = findViewById(R.id.tv_file);

//        loadInterPreview();

        try {
            String name = getIntent().getStringExtra("FILE_NAME");
            tvFile.setText(getResources().getString(R.string.savings)+ name);
        } catch (Exception e) {
            e.printStackTrace();
            tvFile.setText("");
        }

        toolbar.setTitle(getString(R.string.saving));

        Glide.with(this).load(getDrawable(R.drawable.ic_loading_save_file)).into(ivLoading);

        storage = new Storage(this);
        updateHeader();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                toolbar.setTitle(getString(R.string.saved));
                llLoading.setVisibility(View.GONE);
                llSuccess.setVisibility(View.VISIBLE);
            }
        }, 4000);

        btnPreview.setOnClickListener(v -> {
            startActivity(new Intent(SuccessActivity.this, MainActivity.class));
            SuccessActivity.this.finish();
        });

    }



    @Override
    public void onBackPressed() {
//        startActivity(new Intent(SuccessActivity.this, MainActivity.class));
//        SuccessActivity.this.finish();
    }

    private void updateHeader() {
        Uri uri = storage.getStoragePath();
        long free = Storage.getFree(this, uri);
        long sec = Storage.average(this, free);
        TextView text = findViewById(R.id.space_left);
        text.setText(AudioApplication.formatFree(this, free, sec));
    }

    public static void setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

}
