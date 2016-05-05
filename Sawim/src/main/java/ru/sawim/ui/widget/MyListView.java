package ru.sawim.ui.widget;

import android.content.Context;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 29.08.13
 * Time: 14:54
 * To change this template use File | Settings | File Templates.
 */
public class MyListView extends RecyclerView {

    RecyclerContextMenuInfo contextMenuInfo;
    OnItemClickListener onItemClickListener;
    OnItemLongClickListener onItemLongClickListener;
    OnInterceptTouchListener onInterceptTouchListener;
    View emptyView;
    Runnable selectChildRunnable;

    GestureDetector gestureDetector;
    View currentChildView;
    int currentChildPosition;
    boolean interceptedByChild;
    boolean disallowInterceptTouchEvents;
    boolean instantClick;
    Runnable clickRunnable;

    public MyListView(Context context) {
        super(context);
        init();
    }

    public MyListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MyListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setScrollbarFadingEnabled(true);
        setVerticalScrollBarEnabled(true);
        setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setSmoothScrollbarEnabled(true);
        linearLayoutManager.setStackFromEnd(false);
        setLayoutManager(linearLayoutManager);
        /*DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setSupportsChangeAnimations(true);
        itemAnimator.setMoveDuration(200);
        itemAnimator.setAddDuration(250);
        itemAnimator.setRemoveDuration(200);*/
        setItemAnimator(null);

        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState != SCROLL_STATE_IDLE && currentChildView != null) {
                    if (selectChildRunnable != null) {
                        removeCallbacks(selectChildRunnable);
                        selectChildRunnable = null;
                    }
                    MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0);
                    try {
                        gestureDetector.onTouchEvent(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    currentChildView.onTouchEvent(event);
                    event.recycle();
                    currentChildView.setPressed(false);
                    currentChildView = null;
                    interceptedByChild = false;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        addOnItemTouchListener(new RecyclerListViewItemClickListener(getContext()));
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongPress(View view, int position);
    }

    public interface OnInterceptTouchListener {
        boolean onInterceptTouchEvent(MotionEvent event);
    }

    private class RecyclerListViewItemClickListener implements RecyclerView.OnItemTouchListener {

        public RecyclerListViewItemClickListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    if (currentChildView != null && onItemClickListener != null) {
                        currentChildView.setPressed(true);
                        final View view = currentChildView;
                        if (instantClick) {
                            view.playSoundEffect(SoundEffectConstants.CLICK);
                            onItemClickListener.onItemClick(view, currentChildPosition);
                        }
                        postDelayed(clickRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (this == clickRunnable) {
                                    clickRunnable = null;
                                }
                                if (view != null) {
                                    view.setPressed(false);
                                    if (!instantClick) {
                                        view.playSoundEffect(SoundEffectConstants.CLICK);
                                        if (onItemClickListener != null) {
                                            onItemClickListener.onItemClick(view, currentChildPosition);
                                        }
                                    }
                                }
                            }
                        }, ViewConfiguration.getPressedStateDuration());

                        if (selectChildRunnable != null) {
                            removeCallbacks(selectChildRunnable);
                            selectChildRunnable = null;
                            currentChildView = null;
                            interceptedByChild = false;
                        }
                    }
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent event) {
                    if (currentChildView != null) {
                        if (onItemLongClickListener != null) {
                            if (onItemLongClickListener.onItemLongPress(currentChildView, currentChildPosition)) {
                                currentChildView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                            }
                        } else {
                            showContextMenuForChild(currentChildView);
                        }
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent event) {
            int action = event.getActionMasked();
            boolean isScrollIdle = getScrollState() == RecyclerView.SCROLL_STATE_IDLE;
            if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) && currentChildView == null && isScrollIdle) {
                currentChildView = view.findChildViewUnder(event.getX(), event.getY());
                currentChildPosition = -1;
                if (currentChildView != null) {
                    currentChildPosition = view.getChildAdapterPosition(currentChildView);
                    MotionEvent childEvent = MotionEvent.obtain(0, 0, event.getActionMasked(), event.getX() - currentChildView.getLeft(), event.getY() - currentChildView.getTop(), 0);
                    if (currentChildView.onTouchEvent(childEvent)) {
                        //interceptedByChild = true;
                    }
                    childEvent.recycle();
                }
            }
            if (currentChildView != null && !interceptedByChild) {
                try {
                    gestureDetector.onTouchEvent(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                if (!interceptedByChild && currentChildView != null) {
                    selectChildRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (selectChildRunnable != null && currentChildView != null) {
                                currentChildView.setPressed(true);
                                selectChildRunnable = null;
                            }
                        }
                    };
                    postDelayed(selectChildRunnable, ViewConfiguration.getTapTimeout());
                }
            } else if (currentChildView != null && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL || !isScrollIdle)) {
                if (selectChildRunnable != null) {
                    removeCallbacks(selectChildRunnable);
                    selectChildRunnable = null;
                }
                currentChildView.setPressed(false);
                currentChildView = null;
                interceptedByChild = false;
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView view, MotionEvent event) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            cancelClickRunnables(true);
        }
    }

    public void cancelClickRunnables(boolean uncheck) {
        if (selectChildRunnable != null) {
            removeCallbacks(selectChildRunnable);
            selectChildRunnable = null;
        }
        if (currentChildView != null) {
            if (uncheck) {
                currentChildView.setPressed(false);
            }
            currentChildView = null;
        }
        if (clickRunnable != null) {
            removeCallbacks(clickRunnable);
            clickRunnable = null;
        }
        interceptedByChild = false;
    }

    private AdapterDataObserver observer = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            checkIfEmpty();
        }
    };

    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        onItemLongClickListener = listener;
    }

    public void setEmptyView(View view) {
        if (emptyView == view) {
            return;
        }
        emptyView = view;
        checkIfEmpty();
    }

    public View getEmptyView() {
        return emptyView;
    }

    public void invalidateViews() {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            getChildAt(a).invalidate();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        if (disallowInterceptTouchEvents) {
            requestDisallowInterceptTouchEvent(true);
        }
        return onInterceptTouchListener != null && onInterceptTouchListener.onInterceptTouchEvent(e) || super.onInterceptTouchEvent(e);
    }

    private void checkIfEmpty() {
        if (emptyView == null || getAdapter() == null) {
            return;
        }
        boolean emptyViewVisible = getAdapter().getItemCount() == 0;
        emptyView.setVisibility(emptyViewVisible ? VISIBLE : GONE);
        setVisibility(emptyViewVisible ? INVISIBLE : VISIBLE);
    }

    public void setOnInterceptTouchListener(OnInterceptTouchListener listener) {
        onInterceptTouchListener = listener;
    }

    public void setInstantClick(boolean value) {
        instantClick = value;
    }

    public void setDisallowInterceptTouchEvents(boolean value) {
        disallowInterceptTouchEvents = value;
    }

    @Override
    public void setAdapter(Adapter adapter) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(observer);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer);
        }
        checkIfEmpty();
    }

    @Override
    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return contextMenuInfo;
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        final int longPressPosition = getChildAdapterPosition(originalView);
        if (longPressPosition >= 0) {
            final long longPressId = getAdapter().getItemId(longPressPosition);
            contextMenuInfo = new RecyclerContextMenuInfo(longPressPosition, longPressId);
            return super.showContextMenuForChild(originalView);
        }
        return false;
    }

    public static class RecyclerContextMenuInfo implements ContextMenu.ContextMenuInfo {

        public RecyclerContextMenuInfo(int position, long id) {
            this.position = position;
            this.id = id;
        }

        final public int position;
        final public long id;
    }

    /**
     * Check if this view can be scrolled vertically in a certain direction.
     *
     * @param direction Negative to check scrolling up, positive to check scrolling down.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */
    @Override
    public boolean canScrollVertically(int direction) {
        final int offset = computeVerticalScrollOffset();
        final int range = computeVerticalScrollRange() - computeVerticalScrollExtent();
        if (range == 0) return false;
        if (direction < 0) {
            return offset > 0;
        } else {
            return offset < range - 1;
        }
    }

    @Override
    public void stopScroll() {
        try {
            super.stopScroll();
        } catch (NullPointerException exception) {
            /**
             *  The mLayout has been disposed of before the
             *  RecyclerView and this stops the application
             *  from crashing.
             */
        }
    }
}
