package com.github.axet.androidlibrary.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import org.jsoup.helper.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextMax extends PathMax {
    // xml call
    public TextMax(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    // xml call
    public TextMax(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // created manualu
    public TextMax(Context context, TextView text) {
        super(context, text);
    }

    @Override
    public int formatText(int max) {
        List<String> sss = new ArrayList<>(Arrays.asList(s.split("\n")));

        for (int i = 0; i < sss.size(); i++) {
            String s = sss.get(i);
            List<String> ss = new ArrayList<>(Arrays.asList(s.split("\\s")));
            sss.set(i, dots(max, ss, "", " ", ""));
        }

        String s = StringUtil.join(sss, "\n");

        return measureText(s);
    }
}
