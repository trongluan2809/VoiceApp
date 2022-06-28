package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ads.control.ads.Admod;
import com.ads.control.funtion.AdCallback;
import com.github.axet.audiorecorder.R;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.Common;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.adapter.LanguageStartAdapter;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.callback.IClickItemLanguage;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.model.LanguageModel;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SharePrefUtils;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SystemUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LanguageStartActivity extends AppCompatActivity {
    private FrameLayout fr_ads;
    RecyclerView recyclerView;
    ImageView btn_done;
    List<LanguageModel> listLanguage;
    String codeLang;
    String langDevice = "en";
    FrameLayout frAds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_start_actity);
        Configuration config = new Configuration();
        Locale locale = Locale.getDefault();
         langDevice = locale.getLanguage();
         this.getResources().updateConfiguration(config, this.getResources().getDisplayMetrics());

        Locale.setDefault(locale);
        config.locale = locale;


        frAds = findViewById(R.id.fr_ads);
        recyclerView = findViewById(R.id.recyclerView);
        btn_done = findViewById(R.id.btn_done);
        codeLang = Locale.getDefault().getLanguage();

        //ads native
        Admod.getInstance().loadNativeAd(this,getString(R.string.native_language),new AdCallback(){
            @Override
            public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                NativeAdView adView = (NativeAdView) LayoutInflater.from(LanguageStartActivity.this).inflate(R.layout.layout_native_language, null);
                frAds.removeAllViews();
                frAds.addView(adView);
                Admod.getInstance().populateUnifiedNativeAdView(unifiedNativeAd, adView);
            }
        });

        if (!Common.checkNetWork(this)){
            frAds.removeAllViews();
        }


        initData();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        LanguageStartAdapter languageAdapter = new LanguageStartAdapter(listLanguage, new IClickItemLanguage() {
            @Override
            public void onClickItemLanguage(String code) {
                codeLang = code;
            }
        },this);


        String[] langDefault = { "es", "fr", "pt", "de","hi","it"};
        if(!Arrays.asList(langDefault).contains(langDevice)) langDevice = "en";

        languageAdapter.setCheck(langDevice);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(languageAdapter);

        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharePrefUtils.increaseCountFirstHelp(LanguageStartActivity.this);
                SystemUtil.saveLocale(getBaseContext(),codeLang);
                startActivity(new Intent(LanguageStartActivity.this, AppIntro.class).putExtra("INTRO_FROM_SPLASH", true));
                finish();
            }
        });

    }
    private void initData() {
        int position =0;
        boolean isLangDefault = false;
        listLanguage = new ArrayList<>();
        listLanguage.add(new LanguageModel("English","en"));
        listLanguage.add(new LanguageModel("Spanish","es"));
        listLanguage.add(new LanguageModel("French","fr"));
        listLanguage.add(new LanguageModel("Portuguese","pt"));
        listLanguage.add(new LanguageModel("Hindi","hi"));
        listLanguage.add(new LanguageModel("German","de"));
        listLanguage.add(new LanguageModel("Italian","it"));

        for(LanguageModel languageModel:listLanguage){
            if(languageModel.getCode().equals(langDevice)){
                isLangDefault = true;
               break;
            }
            position++;
        }
        if(position>0&&isLangDefault){
            LanguageModel languageModel = listLanguage.get(position);
            listLanguage.remove(position);
            listLanguage.add(0,languageModel);
        }
    }
}