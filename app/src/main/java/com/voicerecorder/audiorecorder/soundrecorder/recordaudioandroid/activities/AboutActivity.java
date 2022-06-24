package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.github.axet.audiorecorder.R;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SystemUtil;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        // Set Language
        SystemUtil.setLocale(getBaseContext());
//        getWindow().setStatusBarColor(ContextCompat.getColor(this, com.github.axet.audiolibrary.R.color.green_bold));
    }
}