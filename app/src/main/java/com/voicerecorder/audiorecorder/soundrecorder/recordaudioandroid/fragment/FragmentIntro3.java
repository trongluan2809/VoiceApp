package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.github.axet.audiorecorder.R;

public class FragmentIntro3 extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        return inflater.inflate(R.layout.fragment_intro3, container, false);
    }
}