package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.github.axet.audiorecorder.R;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.adapter.LanguageAdapter;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.callback.IClickItemLanguage;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.model.LanguageModel;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SystemUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LanguageActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    ImageView btn_back;
    List<LanguageModel> listLanguage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language);
        // Set Language
        SystemUtil.setLocale(getBaseContext());

        recyclerView = findViewById(R.id.recyclerView);
        btn_back = findViewById(R.id.btn_back);
        initData();
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        LanguageAdapter languageAdapter = new LanguageAdapter(listLanguage, new IClickItemLanguage() {
            @Override
            public void onClickItemLanguage(String code) {
                SystemUtil.saveLocale(getBaseContext(),code);
                back();
            }
        });

        languageAdapter.setCheck(SystemUtil.getPreLanguage(getBaseContext()));


        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(languageAdapter);
     //   recyclerView.addItemDecoration(itemDecoration);
    }
    private void initData() {
        listLanguage = new ArrayList<>();
        listLanguage.add(new LanguageModel("English","en"));
        listLanguage.add(new LanguageModel("Korean","ko"));
        listLanguage.add(new LanguageModel("Japanese","ja"));
        listLanguage.add(new LanguageModel("Italian","it"));
        listLanguage.add(new LanguageModel("French","fr"));
        listLanguage.add(new LanguageModel("Hindi","hi"));
        listLanguage.add(new LanguageModel("Portuguese","pt"));
        listLanguage.add(new LanguageModel("Spanish","es"));
        listLanguage.add(new LanguageModel("Indonesian","in"));
        listLanguage.add(new LanguageModel("Malaysia","ms"));
        listLanguage.add(new LanguageModel("Philippines","phi"));
        listLanguage.add(new LanguageModel("Chinese","zh"));
        listLanguage.add(new LanguageModel("German","de"));


    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void back(){
            finishAffinity();
            Intent intent = new Intent(LanguageActivity.this,MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(new Intent(LanguageActivity.this, MainActivity.class));
    }

}