package com.md4u.demo;

import java.util.ArrayList;

import com.md4u.demo.MDData.MDDataHandler;
import com.md4u.demo.MDImageLoader.ImageLoaderHandler;
import com.md4u.demo.MDModel.Fan;
import com.md4u.demo.MDData.ServiceBinder;
import com.md4u.demo.MDScrollView.ItemAdapter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

public class FansActivity extends Activity {
    private int itemHeight;
    private int portraitSize;
    private MDScrollView scroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_fans);
        getWindow().setFeatureInt(
            Window.FEATURE_CUSTOM_TITLE,
            R.layout.title_fans
        );

        // initialization
        itemHeight = getResources().getDimensionPixelSize(
            R.dimen.fans_item_height
        );
        portraitSize = getResources().getDimensionPixelSize(
            R.dimen.fans_item_portrait_size
        );
        scroll = (MDScrollView)findViewById(R.id.fans_list);
        scroll.initColumns(0, 0, 0, 0, 1, 0);
        scroll.setItemAdapter(itemAdapter);

        // bind to data service and get the list data
        Intent dataIntent = new Intent(this, MDData.class);
        bindService(dataIntent, serviceConnection, BIND_AUTO_CREATE);
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
            dataService.getFansList(new MDDataHandler() {
                @Override
                public void onDataRecieved(Object... data) {
                    if (data == null) {
                        return;
                    }
                    scroll.setContent(data[0]);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            dataService = null;
        }
    };

    // item adapter of the list of fans
    private ItemAdapter itemAdapter = new ItemAdapter() {
        @Override
        public void onAdd(
            View view, Object content, int index, final int reuseCounter
        ) {
            @SuppressWarnings("unchecked")
            ArrayList<Fan> fansList = (ArrayList<Fan>)content;
            if (index >= fansList.size()) {
                scroll.onLoaded(null, 0, reuseCounter);
                return;
            }

            if (view == null) {
                view = getLayoutInflater().inflate(
                    R.layout.scroll_item_fans, null
                );
            }
            Fan fan = fansList.get(index);
            view.setTag(R.string.tag_content, content);
            view.setTag(R.string.tag_item, fan);

            // set user name
            TextView name = (TextView)view.findViewById(R.id.fans_name);
            name.setText(fan.user);

            // set user portrait
            ImageView photo = (ImageView)view.findViewById(R.id.fans_portrait);
            MDImageLoader.loadRemoteImage(
                fan.photo.pid, photo, portraitSize, portraitSize,
                new ImageLoaderHandler() {
                    public void onLoaded(
                        ImageView imageView, Bitmap bitmap
                    ) {
                        scroll.onLoaded(
                            imageView.getRootView(),
                            itemHeight, reuseCounter
                        );
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
            Fan fan = (Fan)view.getTag(R.string.tag_item);
            ImageView photo = (ImageView)view.findViewById(R.id.fans_portrait);
            MDImageLoader.loadRemoteImage(
                fan.photo.pid, photo, portraitSize, portraitSize, null
            );
        }

        @Override
        public void onHide(View view) {
            ImageView photo = (ImageView)view.findViewById(R.id.fans_portrait);
            photo.setImageBitmap(null);
        }
    };
}
