package ru.sawim.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.viewpagerindicator.R;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * This widget implements the dynamic action bar tab behavior that can change
 * across different configurations or circumstances.
 */
public class IconTabPageIndicator extends HorizontalScrollView {

    /**
     * Interface for a callback when the selected tab has been reselected.
     */
    public interface OnTabReselectedListener {
        /**
         * Callback when the selected tab has been reselected.
         *
         * @param position Position of the current center item.
         */
        void onTabReselected(int position);
    }

    public interface OnTabSelectedListener {
        /**
         * Callback when the selected tab has been reselected.
         *
         * @param position Position of the current center item.
         */
        void onTabSelected(int position);
    }

    private Runnable mTabSelector;

    private final OnClickListener mIconTabClickListener = new OnClickListener() {
        public void onClick(View view) {
            IconTabView tabView = (IconTabView) view;
            final int oldSelected = getCurrentItem();
            final int newSelected = tabView.getIndex();
            setCurrentItem(newSelected);
            if (mTabSelectedListener != null) {
                mTabSelectedListener.onTabSelected(newSelected);
            }
            if (oldSelected == newSelected && mTabReselectedListener != null) {
                mTabReselectedListener.onTabReselected(newSelected);
            }
        }
    };

    private final IcsLinearLayout mTabLayout;

    private int mMaxTabWidth;
    private int mSelectedTabIndex;

    private OnTabSelectedListener mTabSelectedListener;
    private OnTabReselectedListener mTabReselectedListener;

    public IconTabPageIndicator(Context context) {
        this(context, null);
    }

    public IconTabPageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        setHorizontalScrollBarEnabled(false);

        mTabLayout = new IcsLinearLayout(context,
                R.attr.vpiTabPageIndicatorStyle);
        addView(mTabLayout, new ViewGroup.LayoutParams(WRAP_CONTENT,
                MATCH_PARENT));
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        mTabSelectedListener = listener;
    }

    public void setOnTabReselectedListener(OnTabReselectedListener listener) {
        mTabReselectedListener = listener;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final boolean lockedExpanded = widthMode == MeasureSpec.EXACTLY;
        setFillViewport(lockedExpanded);

        final int childCount = mTabLayout.getChildCount();
        if (childCount > 1
                && (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST)) {
            if (childCount > 2) {
                mMaxTabWidth = (int) (MeasureSpec.getSize(widthMeasureSpec) * 0.4f);
            } else {
                mMaxTabWidth = MeasureSpec.getSize(widthMeasureSpec) / 2;
            }
        } else {
            mMaxTabWidth = -1;
        }
        final int oldWidth = getMeasuredWidth();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int newWidth = getMeasuredWidth();

        if (lockedExpanded && oldWidth != newWidth) {
            // Recenter the tab display if we're at a new (scrollable) size.
            setCurrentItem(mSelectedTabIndex);
        }
    }

    private void animateToTab(final int position) {
        final View tabView = mTabLayout.getChildAt(position);
        if (mTabSelector != null) {
            removeCallbacks(mTabSelector);
        }
        mTabSelector = new Runnable() {
            public void run() {
                final int scrollPos = tabView.getLeft()
                        - (getWidth() - tabView.getWidth()) / 2;
                smoothScrollTo(scrollPos, 0);
                mTabSelector = null;
            }
        };
        post(mTabSelector);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mTabSelector != null) {
            // Re-post the selector we saved
            post(mTabSelector);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mTabSelector != null) {
            removeCallbacks(mTabSelector);
        }
    }

    public void addTab(int index, Drawable icon) {
        final IconTabView icontabView = new IconTabView(getContext());
        icontabView.mIndex = index;
        icontabView.setFocusable(true);
        icontabView.setOnClickListener(mIconTabClickListener);

        icontabView.setImageDrawable(icon);

        mTabLayout.addView(icontabView, new LinearLayout.LayoutParams(0,
                MATCH_PARENT, 1));
    }

    public void removeAllTabs() {
        mTabLayout.removeAllViews();
    }

    public void updateTabIcon(int i, Drawable icon) {
        ((IconTabView) mTabLayout.getChildAt(i)).setImageDrawable(icon);
    }

    public void setCurrentItem(int item) {
        mSelectedTabIndex = item;

        final int tabCount = mTabLayout.getChildCount();
        for (int i = 0; i < tabCount; i++) {
            final View child = mTabLayout.getChildAt(i);
            final boolean isSelected = (i == item);
            child.setSelected(isSelected);
            if (isSelected) {
                animateToTab(item);
            }
        }
    }

    public int getCurrentItem() {
        return mSelectedTabIndex;
    }

    private class IconTabView extends ImageView {
        private int mIndex;

        public IconTabView(Context context) {
            super(context, null, R.attr.vpiTabPageIndicatorStyle);
        }

        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            // Re-measure if we went beyond our maximum size.
            if (mMaxTabWidth > 0 && getMeasuredWidth() > mMaxTabWidth) {
                super.onMeasure(MeasureSpec.makeMeasureSpec(mMaxTabWidth,
                        MeasureSpec.EXACTLY), heightMeasureSpec);
            }
        }

        public int getIndex() {
            return mIndex;
        }
    }
}
