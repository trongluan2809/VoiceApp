/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.axet.androidlibrary.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.WrapperListAdapter;

import com.github.axet.androidlibrary.R;
import com.github.axet.androidlibrary.app.AssetsDexLoader;

import java.util.ArrayList;

/**
 * https://android.googlesource.com/platform/packages/apps/Gallery2/+/idea133/src/com/android/photos/views/HeaderGridView.java
 * <p>
 * A {@link GridView} that supports adding header rows in a
 * very similar way to {@link android.widget.ListView}.
 * See {@link HeaderGridView#addHeaderView(View, Object, boolean)}
 */
@TargetApi(11)
public class HeaderGridView extends GridView {
    public static final String TAG = HeaderGridView.class.getSimpleName();

    Paint divider = new Paint();
    ArrayList<FixedViewInfo> mHeaderViewInfos = new ArrayList<FixedViewInfo>();
    ArrayList<FixedViewInfo> mFooterViewInfos = new ArrayList<FixedViewInfo>();

    public static int getNumColumns(GridView grid) {
        if (Build.VERSION.SDK_INT >= 11)
            return grid.getNumColumns();
        try {
            return AssetsDexLoader.getPrivateField(GridView.class, "mNumColumns").getInt(grid);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A class that represents a fixed view in a list, for example a header at the top
     * or a footer at the bottom.
     */
    public static class FixedViewInfo {
        /**
         * The view to add to the grid
         */
        public View view;
        public ViewGroup viewContainer;
        /**
         * The data backing the view. This is returned from {@link ListAdapter#getItem(int)}.
         */
        public Object data;
        /**
         * <code>true</code> if the fixed view should be selectable in the grid
         */
        public boolean isSelectable;
    }

    public class FullWidthFixedViewLayout extends FrameLayout {
        public FullWidthFixedViewLayout(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int targetWidth = HeaderGridView.this.getMeasuredWidth() - HeaderGridView.this.getPaddingLeft() - HeaderGridView.this.getPaddingRight();
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.getMode(widthMeasureSpec));
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * ListAdapter used when a HeaderGridView has header views. This ListAdapter
     * wraps another one and also keeps track of the header views and their
     * associated data objects.
     * <p>This is intended as a base class; you will probably not need to
     * use this class directly in your own code.
     */
    public static class HeaderViewGridAdapter implements WrapperListAdapter, Filterable {
        // This is used to notify the container of updates relating to number of columns
        // or headers changing, which changes the number of placeholders needed
        private final DataSetObservable mDataSetObservable = new DataSetObservable();
        private final ListAdapter mAdapter;
        private int mNumColumns = 1;
        // This ArrayList is assumed to NOT be null.
        ArrayList<FixedViewInfo> mHeaderViewInfos;
        ArrayList<FixedViewInfo> mFooterViewInfos;
        boolean mAreAllFixedViewsSelectable;
        private final boolean mIsFilterable;

        public HeaderViewGridAdapter(ArrayList<FixedViewInfo> headerViewInfos, ArrayList<FixedViewInfo> footerViewInfos, ListAdapter adapter) {
            mAdapter = adapter;
            mIsFilterable = adapter instanceof Filterable;
            if (headerViewInfos == null) {
                throw new IllegalArgumentException("headerViewInfos cannot be null");
            }
            mHeaderViewInfos = headerViewInfos;
            mFooterViewInfos = footerViewInfos;
            mAreAllFixedViewsSelectable = areAllListInfosSelectable(mHeaderViewInfos);
        }

        public int getHeadersCount() {
            return mHeaderViewInfos.size();
        }

        public int getFooterCount() {
            return mFooterViewInfos.size();
        }

        @Override
        public boolean isEmpty() {
            return (mAdapter == null || mAdapter.isEmpty()) && getHeadersCount() == 0 && getFooterCount() == 0;
        }

        public void setNumColumns(int numColumns) {
            if (numColumns < 1) {
                throw new IllegalArgumentException("Number of columns must be 1 or more");
            }
            if (mNumColumns != numColumns) {
                mNumColumns = numColumns;
                notifyDataSetChanged();
            }
        }

        public int getNumColumns() {
            return mNumColumns;
        }

        private boolean areAllListInfosSelectable(ArrayList<FixedViewInfo> infos) {
            if (infos != null) {
                for (FixedViewInfo info : infos) {
                    if (!info.isSelectable) {
                        return false;
                    }
                }
            }
            return true;
        }

        public boolean removeHeader(View v) {
            for (int i = 0; i < mHeaderViewInfos.size(); i++) {
                FixedViewInfo info = mHeaderViewInfos.get(i);
                if (info.view == v) {
                    mHeaderViewInfos.remove(i);
                    mAreAllFixedViewsSelectable = areAllListInfosSelectable(mHeaderViewInfos);
                    mDataSetObservable.notifyChanged();
                    return true;
                }
            }
            return false;
        }

        public boolean removeFooter(View v) {
            for (int i = 0; i < mFooterViewInfos.size(); i++) {
                FixedViewInfo info = mFooterViewInfos.get(i);
                if (info.view == v) {
                    mFooterViewInfos.remove(i);
                    mAreAllFixedViewsSelectable = areAllListInfosSelectable(mFooterViewInfos);
                    mDataSetObservable.notifyChanged();
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getCount() {
            if (mAdapter != null) {
                int adapterCount = mAdapter.getCount();
                int rem = adapterCount % mNumColumns;
                if (rem > 0)
                    adapterCount += mNumColumns - rem;
                return getHeadersCount() * mNumColumns + getFooterCount() * mNumColumns + adapterCount;
            } else {
                return getHeadersCount() * mNumColumns + getFooterCount() * mNumColumns;
            }
        }

        @Override
        public boolean areAllItemsEnabled() {
            if (mAdapter != null)
                return mAreAllFixedViewsSelectable && mAdapter.areAllItemsEnabled();
            else
                return true;
        }

        @Override
        public boolean isEnabled(int position) {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders)
                return (position % mNumColumns == 0) && mHeaderViewInfos.get(position / mNumColumns).isSelectable;
            // Adapter
            final int adjPosition = position - numHeadersAndPlaceholders;
            int adapterCount = 0;
            if (mAdapter != null) {
                adapterCount = mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return mAdapter.isEnabled(adjPosition);
                }
                int rem = adjPosition % mNumColumns;
                if (rem > 0) {
                    adapterCount += mNumColumns - adapterCount % mNumColumns;
                    if (adjPosition < adapterCount) {
                        return false;
                    }
                }
            }
            final int adjPositionFooter = adjPosition - adapterCount;
            int numFootersAndPlaceholders = getFooterCount() * mNumColumns;
            if (adjPositionFooter >= 0 && adjPositionFooter < numFootersAndPlaceholders)
                return (adjPositionFooter % mNumColumns == 0) && mFooterViewInfos.get(adjPositionFooter / mNumColumns).isSelectable;
            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public Object getItem(int position) {
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                if (position % mNumColumns == 0) {
                    return mHeaderViewInfos.get(position / mNumColumns).data;
                }
                return null;
            }
            // Adapter
            final int adjPosition = position - numHeadersAndPlaceholders;
            int adapterCount = 0;
            if (mAdapter != null) {
                adapterCount = mAdapter.getCount();
                if (adjPosition < adapterCount)
                    return mAdapter.getItem(adjPosition);
                int rem = adjPosition % mNumColumns;
                if (rem > 0) {
                    adapterCount += mNumColumns - adapterCount % mNumColumns;
                    if (adjPosition < adapterCount)
                        return null;
                }
            }
            final int adjPositionFooter = adjPosition - adapterCount;
            int numFootersAndPlaceholders = getFooterCount() * mNumColumns;
            if (adjPositionFooter >= 0 && adjPositionFooter < numFootersAndPlaceholders) {
                if (adjPositionFooter % mNumColumns == 0)
                    return mFooterViewInfos.get(position / mNumColumns).data;
            }
            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public long getItemId(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (mAdapter != null && position >= numHeadersAndPlaceholders) {
                int adjPosition = position - numHeadersAndPlaceholders;
                int adapterCount = mAdapter.getCount();
                if (adjPosition < adapterCount)
                    return mAdapter.getItemId(adjPosition);
            }
            return -1;
        }

        @Override
        public boolean hasStableIds() {
            if (mAdapter != null)
                return mAdapter.hasStableIds();
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView != null)
                convertView.setVisibility(View.VISIBLE);
            // Header (negative positions will throw an ArrayIndexOutOfBoundsException)
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (position < numHeadersAndPlaceholders) {
                View headerViewContainer = mHeaderViewInfos.get(position / mNumColumns).viewContainer;
                if (position % mNumColumns == 0) {
                    return headerViewContainer;
                } else {
                    if (convertView == null) {
                        convertView = new View(parent.getContext());
                    }
                    // We need to do this because GridView uses the height of the last item
                    // in a row to determine the height for the entire row.
                    convertView.setVisibility(View.INVISIBLE);
                    convertView.setMinimumHeight(headerViewContainer.getHeight());
                    return convertView;
                }
            }
            // Adapter
            final int adjPosition = position - numHeadersAndPlaceholders;
            int adapterCount = 0;
            if (mAdapter != null) {
                adapterCount = mAdapter.getCount();
                if (adapterCount > 0) {
                    if (adjPosition < adapterCount) {
                        return mAdapter.getView(adjPosition, convertView, parent);
                    }
                    int last = adapterCount - 1;
                    int rem = adapterCount % mNumColumns;
                    if (rem > 0) {
                        adapterCount += mNumColumns - rem;
                        if (adjPosition < adapterCount) {
                            convertView = mAdapter.getView(last, convertView, parent);
                            // We need to do this because GridView uses the height of the last item
                            // in a row to determine the height for the entire row.
                            convertView.setVisibility(View.INVISIBLE);
                            return convertView;
                        }
                    }
                }
            }
            final int adjPositionFooter = adjPosition - adapterCount;
            int numFootersAndPlaceholders = getFooterCount() * mNumColumns;
            if (adjPositionFooter >= 0 && adjPositionFooter < numFootersAndPlaceholders) {
                View headerViewContainer = mFooterViewInfos.get(adjPositionFooter / mNumColumns).viewContainer;
                if (adjPositionFooter % mNumColumns == 0) {
                    return headerViewContainer;
                } else {
                    if (convertView == null) {
                        convertView = new View(parent.getContext());
                    }
                    // We need to do this because GridView uses the height of the last item
                    // in a row to determine the height for the entire row.
                    convertView.setVisibility(View.INVISIBLE);
                    convertView.setMinimumHeight(headerViewContainer.getHeight());
                    return convertView;
                }
            }
            throw new ArrayIndexOutOfBoundsException(position);
        }

        @Override
        public int getItemViewType(int position) {
            int numHeadersAndPlaceholders = getHeadersCount() * mNumColumns;
            if (mAdapter != null && position >= numHeadersAndPlaceholders) {
                int adjPosition = position - numHeadersAndPlaceholders;
                int adapterCount = mAdapter.getCount();
                if (adjPosition < adapterCount) {
                    return mAdapter.getItemViewType(adjPosition);
                }
                int last = adapterCount - 1;
                int rem = adapterCount % mNumColumns;
                if (rem > 0) {
                    adapterCount += mNumColumns - rem;
                    if (adjPosition < adapterCount) {
                        return mAdapter.getItemViewType(last);
                    }
                }
            }
            return AdapterView.ITEM_VIEW_TYPE_HEADER_OR_FOOTER;
        }

        @Override
        public int getViewTypeCount() {
            if (mAdapter != null)
                return mAdapter.getViewTypeCount() + 1;
            return 2;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
            mDataSetObservable.registerObserver(observer);
            if (mAdapter != null)
                mAdapter.registerDataSetObserver(observer);
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
            mDataSetObservable.unregisterObserver(observer);
            if (mAdapter != null)
                mAdapter.unregisterDataSetObserver(observer);
        }

        @Override
        public Filter getFilter() {
            if (mIsFilterable)
                return ((Filterable) mAdapter).getFilter();
            return null;
        }

        @Override
        public ListAdapter getWrappedAdapter() {
            return mAdapter;
        }

        public void notifyDataSetChanged() {
            mDataSetObservable.notifyChanged();
        }
    }

    public HeaderGridView(Context context) {
        super(context);
        initHeaderGridView();
    }

    public HeaderGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initHeaderGridView();
    }

    public HeaderGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initHeaderGridView();
    }

    private void initHeaderGridView() {
        super.setClipChildren(false);
        divider.setColor(ThemeUtils.getThemeColor(getContext(), R.attr.colorButtonNormal));
        divider.setStrokeWidth(ThemeUtils.dp2px(getContext(), 2));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ListAdapter adapter = getAdapter();
        if (adapter != null && adapter instanceof HeaderViewGridAdapter)
            ((HeaderViewGridAdapter) adapter).setNumColumns(getNumColumns(this));
    }

    @Override
    public void setClipChildren(boolean clipChildren) {
        // Ignore, since the header rows depend on not being clipped
    }

    /**
     * Add a fixed view to appear at the top of the grid. If addHeaderView is
     * called more than once, the views will appear in the order they were
     * added. Views added using this call can take focus if they want.
     * <p>
     * NOTE: Call this before calling setAdapter. This is so HeaderGridView can wrap
     * the supplied cursor with one that will also account for header views.
     *
     * @param v            The view to add.
     * @param data         Data to associate with this view
     * @param isSelectable whether the item is selectable
     */
    public void addHeaderView(View v, Object data, boolean isSelectable) {
        ListAdapter adapter = getAdapter();
        if (adapter != null && !(adapter instanceof HeaderViewGridAdapter)) {
            throw new IllegalStateException("Cannot add header view to grid -- setAdapter has already been called.");
        }
        FixedViewInfo info = new FixedViewInfo();
        FrameLayout fl = new FullWidthFixedViewLayout(getContext());
        fl.addView(v);
        info.view = v;
        info.viewContainer = fl;
        info.data = data;
        info.isSelectable = isSelectable;
        mHeaderViewInfos.add(info);
        // in the case of re-adding a header view, or adding one later on,
        // we need to notify the observer
        if (adapter != null) {
            ((HeaderViewGridAdapter) adapter).notifyDataSetChanged();
        }
    }

    public void addFooterView(View v, Object data, boolean isSelectable) {
        ListAdapter adapter = getAdapter();
        if (adapter != null && !(adapter instanceof HeaderViewGridAdapter)) {
            throw new IllegalStateException("Cannot add header view to grid -- setAdapter has already been called.");
        }
        FixedViewInfo info = new FixedViewInfo();
        FrameLayout fl = new FullWidthFixedViewLayout(getContext());
        fl.addView(v);
        info.view = v;
        info.viewContainer = fl;
        info.data = data;
        info.isSelectable = isSelectable;
        mFooterViewInfos.add(info);
        // in the case of re-adding a header view, or adding one later on,
        // we need to notify the observer
        if (adapter != null) {
            ((HeaderViewGridAdapter) adapter).notifyDataSetChanged();
        }
    }

    /**
     * Add a fixed view to appear at the top of the grid. If addHeaderView is
     * called more than once, the views will appear in the order they were
     * added. Views added using this call can take focus if they want.
     * <p>
     * NOTE: Call this before calling setAdapter. This is so HeaderGridView can wrap
     * the supplied cursor with one that will also account for header views.
     *
     * @param v The view to add.
     */
    public void addHeaderView(View v) {
        addHeaderView(v, null, true);
    }

    public void addFooterView(View v) {
        addFooterView(v, null, true);
    }

    public int getHeaderViewCount() {
        return mHeaderViewInfos.size();
    }

    public boolean removeFooterView(View v) {
        if (mFooterViewInfos.size() > 0) {
            boolean result = false;
            ListAdapter adapter = getAdapter();
            if (adapter != null && ((HeaderViewGridAdapter) adapter).removeFooter(v)) {
                result = true;
            }
            removeFixedViewInfo(v, mFooterViewInfos);
            return result;
        }
        return false;
    }

    /**
     * Removes a previously-added header view.
     *
     * @param v The view to remove
     * @return true if the view was removed, false if the view was not a header
     * view
     */
    public boolean removeHeaderView(View v) {
        if (mHeaderViewInfos.size() > 0) {
            boolean result = false;
            ListAdapter adapter = getAdapter();
            if (adapter != null && ((HeaderViewGridAdapter) adapter).removeHeader(v)) {
                result = true;
            }
            removeFixedViewInfo(v, mHeaderViewInfos);
            return result;
        }
        return false;
    }

    private void removeFixedViewInfo(View v, ArrayList<FixedViewInfo> where) {
        int len = where.size();
        for (int i = 0; i < len; ++i) {
            FixedViewInfo info = where.get(i);
            if (info.view == v) {
                where.remove(i);
                break;
            }
        }
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if (mHeaderViewInfos.size() > 0) {
            HeaderViewGridAdapter hadapter = new HeaderViewGridAdapter(mHeaderViewInfos, mFooterViewInfos, adapter);
            int numColumns = getNumColumns(this);
            if (numColumns > 1)
                hadapter.setNumColumns(numColumns);
            super.setAdapter(hadapter);
        } else {
            super.setAdapter(adapter);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int targetWidth = HeaderGridView.this.getMeasuredWidth() - HeaderGridView.this.getPaddingLeft() - HeaderGridView.this.getPaddingRight();

        int space = 0;
        if (Build.VERSION.SDK_INT >= 16) {
            space = getVerticalSpacing();
        } else {
            if (getChildCount() > 1) {
                space = getChildAt(1).getTop() - getChildAt(0).getBottom();
            }
        }
        float dividerSize = space / 2;

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int bottom = child.getBottom();
            if (child instanceof FullWidthFixedViewLayout) {
                FullWidthFixedViewLayout m = (FullWidthFixedViewLayout) child;
                if (m.getChildCount() > 0) {
                    child = m.getChildAt(0);
                } else {
                    continue;
                }
            }
            if (child.getVisibility() != VISIBLE) {
                continue;
            }
            canvas.drawLine(0, bottom + dividerSize, targetWidth, bottom + dividerSize, divider);
        }
        super.dispatchDraw(canvas);
    }
}