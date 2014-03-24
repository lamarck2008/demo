package com.md4u.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumMap;

import com.md4u.demo.MDData.MDDataHandler;
import com.md4u.demo.MDData.ServiceBinder;
import com.md4u.demo.MDImageLoader.ImageLoaderHandler;
import com.md4u.demo.MDModel.Post;
import com.md4u.demo.MDModel.Post.Filter;
import com.md4u.demo.MDScrollView.ItemAdapter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class BrowseActivity extends Activity {
    private static class Content {
        public int index;
        public ArrayList<Post> postList;

        Content(int index, ArrayList<Post> postList) {
            this.index = index;
            this.postList = postList;
        }
    }

    private static final Filter[] filters = new Filter[] {
        Filter.ALL_VOTING,
        Filter.NEARBY_VOTING,
        Filter.FANS_VOTING,
        Filter.MY_ALL
    };
    private static final Class<?>[] intents = new Class<?>[] {
        DecisionActivity.class,
        DecisionActivity.class,
        DecisionActivity.class,
        ResultActivity.class
    };
    private View[] tabs;
    private int stripWidth;
    private MDScrollView scroll;
    private EnumMap<Filter, ArrayList<Post>> postLists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_browse);
        getWindow().setFeatureInt(
            Window.FEATURE_CUSTOM_TITLE,
            R.layout.title_browse
        );

        // get resources
        Resources res = getResources();
        String[] tabTitles = res.getStringArray(R.array.browse_tab_titles);
        TypedArray tabDrawables = res.obtainTypedArray(R.array.browse_tab_drawables);
        int dividerWidth = res.getDimensionPixelSize(R.dimen.tab_widget_divider_width);
        stripWidth = res.getDimensionPixelSize(R.dimen.waterfall_strip_width);
        LinearLayout widget = (LinearLayout)findViewById(R.id.browse_widget);
        LayoutInflater layoutInflater = getLayoutInflater();

        // initialization
        tabs = new View[] { null, null, null, null };
        scroll = (MDScrollView)findViewById(R.id.browse_scroll);
        initScrollView(scroll, layoutInflater);
        postLists = new EnumMap<Filter, ArrayList<Post>>(Filter.class);

        // setup tab host
        for (int i = 0; i < tabTitles.length; ++i) {
            // set tab widget text and icon
            tabs[i] = layoutInflater.inflate(R.layout.tab_widget, null);
            widget.addView(tabs[i]);
            LayoutParams params = (LayoutParams)tabs[i].getLayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.weight = 1;
            params.leftMargin = (i == 0) ? 0 : dividerWidth;
            TextView textView = (TextView)tabs[i].findViewById(R.id.tab_widget_text);
            textView.setText(tabTitles[i]);
            ImageView imageView = (ImageView)tabs[i].findViewById(R.id.tab_widget_icon);
            imageView.setImageResource(tabDrawables.getResourceId(i, -1));
            tabs[i].setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    int index = Arrays.asList(tabs).indexOf(view);
                    scroll.setContent(
                        new Content(index, postLists.get(filters[index]))
                    );
                }
            });
        }

        // bind to data service and get the list data
        Intent dataIntent = new Intent(this, MDData.class);
        bindService(dataIntent, serviceConnection, BIND_AUTO_CREATE);

        // recycle resources
        tabDrawables.recycle();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    // class handle connection with MDData
    private MDData dataService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        private ServiceBinder dataBinder;

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            dataBinder = (ServiceBinder)binder;
            dataService = dataBinder.getService();
            for (int i = 0; i < filters.length; ++i) {
                final Filter filter = filters[i];
                dataBinder.getPostList(filter, new MDDataHandler() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public void onDataRecieved(Object... data) {
                        if (data == null) {
                            return;
                        }
                        postLists.put(filter, (ArrayList<Post>)data[0]);
                        if (filter == filters[0]) {
                            scroll.setContent(
                                new Content(0, postLists.get(filter))
                            );
                        }
                    }
                });
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataService = null;
        }
    };

    // initialize scroll view
    private void initScrollView(
        final MDScrollView scroll,
        final LayoutInflater layoutInflater
    ) {
        scroll.initColumns(
            stripWidth, stripWidth, stripWidth, 0, 2, stripWidth
        );

        scroll.setItemAdapter(new ItemAdapter() {
            @Override
            public void onAdd(
                View view, Object content, int index, final int reuseCounter
            ) {
                final Content c = (Content)content;
                if (index >= c.postList.size()) {
                    dataService.updatePostList(filters[c.index]);
                    scroll.onLoaded(null, 0, reuseCounter);
                    return;
                }

                if (view == null) {
                    view = layoutInflater.inflate(
                        R.layout.scroll_item_browse, null
                    );
                }
                Post post = c.postList.get(index);
                view.setTag(R.string.tag_content, content);
                view.setTag(R.string.tag_item, post);
                view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(
                            BrowseActivity.this,
                            intents[c.index]
                        );
                        Post p = (Post)view.getTag(R.string.tag_item);
                        intent.putExtra(
                            getResources().getString(R.string.tag_item),
                            p.toJSONObject().toString()
                        );
                        startActivityForResult(intent, 0);
                    }
                });

                TextView countdown = (TextView)view.findViewById(
                    R.id.waterfall_item_countdown
                );
                TextView bonus = (TextView)view.findViewById(
                    R.id.waterfall_item_bonus
                );
                countdown.setText(formatCountdown(
                    post.dateTime.getTime() +
                    (post.hour * 60 + post.minute) * 60 * 1000 -
                    Calendar.getInstance().getTimeInMillis()
                ));
                bonus.setText(String.valueOf(post.bonus));
                ImageView imageView = (ImageView)view.findViewById(
                    R.id.waterfall_item_image
                );

                MDImageLoader.loadRemoteImage(
                    post.images.get(0).pid,
                    imageView, scroll.getColumnWidth(), 0,
                    new ImageLoaderHandler() {
                        @Override
                        public void onLoaded(ImageView imageView, Bitmap bitmap) {
                            if (bitmap != null) {
                                final int width = bitmap.getWidth();
                                final int height = bitmap.getHeight();
                                ViewGroup.LayoutParams params = imageView.getLayoutParams();
                                params.height = scroll.getColumnWidth() * height / width;
                                scroll.onLoaded(
                                    imageView.getRootView(),
                                    params.height + stripWidth, reuseCounter
                                );
                            } else {
                                scroll.onLoaded(null, 0, reuseCounter);
                            }
                        }
                    }
                );
            }

            @Override
            public void onRemove(View view) {
                // TODO To be implemented
            }

            @Override
            public void onShow(View view) {
                // TODO To be implemented
            }

            @Override
            public void onHide(View view) {
                // TODO To be implemented
            }
        });
    }

    // format countdown string
    private static String formatCountdown(long ms) {
        ms = (ms < 0) ? 0 : ms;
        long minute = ms / 1000 / 60;
        long hour = minute / 60;
        minute %= 60;
        return (hour > 0) ? ">60'" : String.valueOf(minute) + "'";
    }
}
