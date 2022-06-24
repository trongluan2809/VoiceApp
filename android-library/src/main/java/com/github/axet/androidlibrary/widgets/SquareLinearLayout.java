package com.github.axet.androidlibrary.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import com.github.axet.androidlibrary.R;

public class SquareLinearLayout extends LinearLayout {

    int maxChild = -1;

    public SquareLinearLayout(Context context) {
        this(context, null);
    }

    public SquareLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        create(context, attrs, 0);
    }

    @TargetApi(11)
    public SquareLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        create(context, attrs, defStyle);
    }

    void create(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SquareLinearLayout, defStyle, 0);
        maxChild = a.getDimensionPixelSize(R.styleable.SquareLinearLayout_maxChild, -1);
        a.recycle();
    }

    int max(int f) {
        if (this.maxChild == -1)
            return f;
        return Math.max(f, this.maxChild);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = 0;

        for (int i = 0; i < getChildCount(); ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE)
                continue;

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
            lp.gravity = Gravity.CENTER;
            count++;
        }

        int mw = MeasureSpec.getSize(widthMeasureSpec) / count;

        if (maxChild != -1) {
            if (mw > maxChild)
                mw = maxChild;
        }

        for (int i = 0; i < count; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE)
                continue;
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            lp.width = mw - lp.leftMargin - lp.rightMargin;
            lp.height = mw - lp.topMargin - lp.bottomMargin;
        }

        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(mw, MeasureSpec.EXACTLY));
    }
}
