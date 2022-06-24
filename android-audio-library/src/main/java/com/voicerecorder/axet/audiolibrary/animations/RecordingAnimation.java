package com.voicerecorder.axet.audiolibrary.animations;

//import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Animation;

import androidx.recyclerview.widget.RecyclerView;

import com.github.axet.androidlibrary.animations.ExpandAnimation;
import com.github.axet.androidlibrary.animations.MarginAnimation;
import com.github.axet.audiolibrary.R;

public class RecordingAnimation extends ExpandAnimation {
    public static Animation apply(final RecyclerView list, final View v, final boolean expand, boolean animate) {
        return apply(new LateCreator() {
            @Override
            public MarginAnimation create() {
                return new RecordingAnimation(list, v, expand);
            }
        }, v, expand, animate);
    }

    public RecordingAnimation(RecyclerView list, View v, boolean expand) {
        super(list, v, v.findViewById(R.id.recording_player), null, expand);
    }

    @Override
    public void expandRotate(float e) {
    }
}
