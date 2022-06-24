package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.callback;

import android.os.SystemClock;
import android.view.View;

public abstract class CustomClickListener implements View.OnClickListener {

    protected int defaultInterval;
    private long lastTimeClicked = 0;

    public CustomClickListener() {
        this(1000);
    }

    public CustomClickListener(int minInterval) {
        this.defaultInterval = minInterval;
    }

    @Override
    public void onClick(View v) {
        if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
            return;
        }
        lastTimeClicked = SystemClock.elapsedRealtime();
        performClick(v);
    }

    public abstract void performClick(View v);

}
