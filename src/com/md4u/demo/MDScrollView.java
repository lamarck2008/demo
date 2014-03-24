package com.md4u.demo;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class MDScrollView extends ScrollView implements OnTouchListener {
    private boolean isColumnInitialized = false;
    private boolean isLoaded = false;
    private LinearLayout columns[];
    private int columnWidth;
    private int[] columnHeights;
    private int lastScrollY = -1;

    public MDScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && isColumnInitialized && !isLoaded) {
            columnWidth = columns[0].getWidth();
            setOnTouchListener(this);
            isLoaded = true;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            Message msg = new Message();
            msg.obj = this;
            scrollHandler.sendMessage(msg);
        }
        return false;
    }

    // message handler used to check if scrolling is stopped
    private static Handler scrollHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MDScrollView scroll = (MDScrollView)msg.obj;
            scroll.checkVisibility();
            final int scrollY = scroll.getScrollY();
            if (scrollY == scroll.lastScrollY) {
                scroll.addAnotherItem();
            } else {
                scroll.lastScrollY = scrollY;
                Message nextMsg = new Message();
                nextMsg.obj = scroll;
                scrollHandler.sendMessageDelayed(nextMsg, 40);
            }
        }
    };

    // initialize column layout
    public void initColumns(
        int left, int top, int right, int bottom,
        int columnCount, int columnMargin
    ) {
        if (isColumnInitialized) {
            return;
        }

        // create the container
        LinearLayout container = new LinearLayout(getContext());
        container.setLayoutParams(new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        ));
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setPadding(left, top, right, bottom);
        // create the columns
        columns = new LinearLayout[columnCount];
        columnHeights = new int[columnCount];
        for (int i = 0; i < columnCount; ++i) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
            );
            params.setMargins((i == 0) ? 0 : columnMargin, 0, 0, 0);
            columns[i] = new LinearLayout(getContext());
            columns[i].setLayoutParams(params);
            columns[i].setOrientation(LinearLayout.VERTICAL);
            columnHeights[i] = 0;
            container.addView(columns[i]);
        }
        addView(container);
        isColumnInitialized = true;
    }

    public int getColumnWidth() {
        return isLoaded ? columnWidth : 0;
    }

    private int getShortestColumnIndex() {
        int index = 0;
        int height = Integer.MAX_VALUE;
        for (int i = 0; i < columnHeights.length; ++i) {
            if (columnHeights[i] < height) {
                index = i;
                height = columnHeights[i];
            }
        }
        return index;
    }

    // item adapter for scroll view
    public interface ItemAdapter {
        public void onAdd(
            View view, Object content, int index, final int reuseCounter
        );
        public void onRemove(View view);
        public void onShow(View view);
        public void onHide(View view);
    }

    private ItemAdapter itemAdapter = null;
    private ArrayList<View> itemList = null;

    public void setItemAdapter(ItemAdapter itemAdapter) {
        this.itemAdapter = itemAdapter;
        this.itemList = new ArrayList<View>();
    }

    private Object content = null;
    private int reuseCounter = -1;
    private Semaphore addItemPermit = new Semaphore(1);

    public void setContent(Object content) {
        if (content == null || this.content == content) {
            return;
        }
        this.content = content;
        ++reuseCounter;
        addItemPermit = new Semaphore(1);

        if (isLoaded) {
            for (int i = 0; i < columns.length; ++i) {
                columns[i].removeAllViews();
                columnHeights[i] = 0;
            }
        }
        if (itemAdapter != null) {
            itemList.clear();
        }
        addAnotherItem();
    }

    public void onLoaded(View view, int height, int reuseCounter) {
        if (reuseCounter != this.reuseCounter) {
            return;
        }
        if (view != null && height != 0) {
            int index = getShortestColumnIndex();
            LinearLayout column = columns[index];
            view.setTag(R.string.tag_top, columnHeights[index]);
            column.addView(view);
            columnHeights[index] += height;
            view.setTag(R.string.tag_bottom, columnHeights[index]);
            view.setTag(R.string.tag_visibility, true);
            itemList.add(view);
            addItemPermit.release();
            addAnotherItem();
        } else {
            addItemPermit.release();
        }
    }

    private void addAnotherItem() {
        if (!isLoaded || itemAdapter == null ||
            !addItemPermit.tryAcquire()
        ) {
            return;
        }

        final int scrollY = getScrollY();
        final int visibleHeight = getHeight();
        final int totalHeight = columnHeights[getShortestColumnIndex()];
        if (scrollY + 2 * visibleHeight < totalHeight) {
            addItemPermit.release();
            return;
        }

        itemAdapter.onAdd(
            null, content, itemList.size(), reuseCounter
        );
    }

    private void checkVisibility() {
        final int scrollY = getScrollY();
        final int visibleHeight = getHeight();
        for (int i = 0; i < itemList.size(); ++i) {
            View view = itemList.get(i);
            int t = (Integer)view.getTag(R.string.tag_top);
            int b = (Integer)view.getTag(R.string.tag_bottom);
            boolean v = (Boolean)view.getTag(R.string.tag_visibility);
            if (b <= scrollY - visibleHeight ||
                t >= scrollY + 2 * visibleHeight
            ) {
                if (v) {
                    view.setTag(R.string.tag_visibility, false);
                    itemAdapter.onHide(view);
                }
            } else {
                if (!v) {
                    view.setTag(R.string.tag_visibility, true);
                    itemAdapter.onShow(view);
                }
            }
        }
    }
}
