package com.github.axet.androidlibrary.widgets;

import android.app.Activity;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.view.Menu;
import android.view.MenuInflater;

public class InvalidateOptionsMenuCompat {
    public static Runnable onCreateOptionsMenu(final Fragment fragment, final Menu menu, final MenuInflater inflater) {
        if (Build.VERSION.SDK_INT < 11) {
            return new Runnable() {
                @Override
                public void run() {
                    fragment.onCreateOptionsMenu(menu, inflater);
                }
            };
        } else {
            return new Runnable() {
                @Override
                public void run() {
                    Activity a = fragment.getActivity();
                    if (a == null || a.isFinishing())
                        return;
                    ActivityCompat.invalidateOptionsMenu(a);
                }
            };
        }
    }
}
