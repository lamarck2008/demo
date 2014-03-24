package com.md4u.demo;

import java.util.ArrayList;

import com.md4u.demo.MDData.MDDataHandler;
import com.md4u.demo.MDData.ServiceBinder;
import com.md4u.demo.MDModel.Bonus;
import com.md4u.demo.MDModel.Bonus.Record;
import com.md4u.demo.MDScrollView.ItemAdapter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class BonusActivity extends Activity {
    private TabHost tabHost;
    private MDScrollView history;
    private int historyHeight;
    private String[] historyContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_bonus);
        getWindow().setFeatureInt(
            Window.FEATURE_CUSTOM_TITLE,
            R.layout.title_bonus
        );

        // get resources
        Resources res = getResources();
        String[] tabTitles = res.getStringArray(R.array.bonus_tab_titles);
        TypedArray tabDrawables = res.obtainTypedArray(R.array.bonus_tab_drawables);
        TypedArray tabContents = res.obtainTypedArray(R.array.bonus_tab_contents);
        int dividerWidth = res.getDimensionPixelSize(R.dimen.tab_widget_divider_width);

        // initialization
        tabHost = (TabHost)findViewById(R.id.bonus_tabhost);
        history = (MDScrollView)findViewById(R.id.bonus_history);
        history.initColumns(0, 0, 0, 0, 1, 0);
        history.setItemAdapter(historyItemAdapter);
        historyContent = res.getStringArray(R.array.bonus_history_contents);
        historyHeight = res.getDimensionPixelSize(R.dimen.bonus_history_height);

        // bind to data service and get the list data
        Intent dataIntent = new Intent(this, MDData.class);
        bindService(dataIntent, serviceConnection, BIND_AUTO_CREATE);

        // setup tab host
        tabHost.setup();
        LayoutInflater layoutInflater = getLayoutInflater();
        for (int i = 0; i < tabTitles.length; ++i) {
            View view = layoutInflater.inflate(R.layout.tab_widget, null);
            TextView textView = (TextView)view.findViewById(R.id.tab_widget_text);
            textView.setText(tabTitles[i]);
            ImageView imageView = (ImageView)view.findViewById(R.id.tab_widget_icon);
            imageView.setImageResource(tabDrawables.getResourceId(i, -1));
            TabSpec tabSpec = tabHost.newTabSpec(tabTitles[i]);
            tabSpec.setIndicator(view).setContent(tabContents.getResourceId(i, -1));
            tabHost.addTab(tabSpec);
            View childTab = tabHost.getTabWidget().getChildTabViewAt(i);
            childTab.setBackgroundResource(R.color.tab_widget_background);
            MarginLayoutParams params = (MarginLayoutParams)childTab.getLayoutParams();
            params.setMargins((i == 0) ? 0 : dividerWidth, 0, 0, 0);
        }

        // recycle resources
        tabDrawables.recycle();
        tabContents.recycle();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    // class handle connection with MDData
    private ServiceConnection serviceConnection = new ServiceConnection() {
        private ServiceBinder dataService;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            dataService = (ServiceBinder)service;
            dataService.getBonusHistory(new MDDataHandler(){
                @Override
                public void onDataRecieved(Object... data) {
                    if (data == null) {
                        return;
                    }
                    TextView totalBonus = (TextView)findViewById(
                        R.id.bonus_mybonus_value
                    );
                    totalBonus.setText(
                        String.valueOf(((Bonus)data[0]).total)
                    );
                    history.setContent(data[1]);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataService = null;
        }
    };

    // item handler of the list of bonus history
    private ItemAdapter historyItemAdapter = new ItemAdapter() {
        @Override
        public void onAdd(
            View view, Object content, int index, final int reuseCounter
        ) {
            @SuppressWarnings("unchecked")
            ArrayList<Record> historyData = (ArrayList<Record>)content;
            if (index >= historyData.size()) {
                history.onLoaded(null, 0, reuseCounter);
                return;
            }

            if (view == null) {
                view = getLayoutInflater().inflate(
                    R.layout.scroll_item_bonus_history, null
                );
            }
            Record item = historyData.get(index);
            view.setTag(R.string.tag_content, content);
            view.setTag(R.string.tag_item, item);

            TextView time = (TextView)view.findViewById(R.id.bonus_history_time);
            ImageView circle = (ImageView)view.findViewById(R.id.bonus_history_circle);
            ImageView icon = (ImageView)view.findViewById(R.id.bonus_history_icon);
            TextView title = (TextView)view.findViewById(R.id.bonus_history_title);
            TextView content_0 = (TextView)view.findViewById(R.id.bonus_history_content_0);
            TextView content_1 = (TextView)view.findViewById(R.id.bonus_history_content_1);

            time.setText(MDDateFormat.formatBonus(item.dateTime));
            title.setText(Integer.toString(item.bonus));
            if (item.type.equals("publish")) {
                content_0.setText(historyContent[4]);
            } else {
                content_0.setText(
                    historyContent[0] + " " +
                    item.forUser + " " +
                    historyContent[1]
                );
            }
            if (item.bonus > 0) {
                content_1.setText(
                    historyContent[2] + " " +
                    item.bonus + " " +
                    historyContent[3]
                );
            } else {
                content_1.setText(
                    historyContent[5] + " " +
                    (-1 * item.bonus) + " " +
                    historyContent[6]
                );
            }

            int color = getResources().getColor(item.type.equals("publish") ?
                R.color.bonus_history_negative : R.color.bonus_history_positive
            );
            ((GradientDrawable)circle.getBackground()).setColor(color);
            title.setTextColor(color);
            content_0.setTextColor(color);
            content_1.setTextColor(color);
            icon.setBackgroundResource(item.type.equals("publish") ?
                R.drawable.bonus_green : R.drawable.bonus_orange
            );

            View line = (View)view.findViewById(R.id.bonus_item_line_up);
            line.setBackgroundColor(getResources().getColor((index == 0) ?
                android.R.color.transparent : R.color.bonus_history_timeline
            ));

            history.onLoaded(view, historyHeight, reuseCounter);
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
    };
}
