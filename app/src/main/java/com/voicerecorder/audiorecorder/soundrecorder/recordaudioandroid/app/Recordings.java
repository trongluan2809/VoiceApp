package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.voicerecorder.axet.audiolibrary.app.RecordingsAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Recordings extends RecordingsAdapter {
    public Recordings(Context context, RecyclerView list) {
        super(context, list);
    }

    public void setEmptyView(View empty) {
        this.empty.setEmptyView(empty);
    }


    @Override
    public void load(Uri mount, boolean clean, Runnable done) {
        if (!Storage.exists(context, mount)) {
            items.clear();
            if (done != null)
                done.run();
            return;
        }
        try {
            super.load(mount, clean, done);
        } catch (RuntimeException e) {
            Log.e(TAG, "load", e);
            items.clear();
            if (done != null)
                done.run();
        }
    }

    @Override
    public void scan(List<Storage.Node> nn, boolean clean, Runnable done) {
        EncodingStorage encodings = ((AudioApplication) context.getApplicationContext()).encodings;
        for (Storage.Node n : new ArrayList<>(nn)) {
            for (File key : encodings.keySet()) {
                EncodingStorage.Info info = encodings.get(key);
                if (n.uri.equals(info.targetUri))
                    nn.remove(n);
            }
        }
        super.scan(nn, clean, done);
    }
}
