package com.md4u.demo;

import java.util.ArrayList;
import java.util.Calendar;

import com.md4u.demo.MDJSON.MDJSONObject;
import com.md4u.demo.MDModel.Image;
import com.md4u.demo.MDModel.Post;
import com.md4u.demo.MDModel.Response;
import com.md4u.demo.MDService.MDServiceHandler;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DecisionActivity extends Activity {
    private static String[] TIME_HINT = null;
    private Post post;
    private ImageButton[] btnLike;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_decision);
        getWindow().setFeatureInt(
            Window.FEATURE_CUSTOM_TITLE,
            R.layout.title_decision
        );

        // get intent data
        String json = getIntent().getExtras().getString(
            getResources().getString(R.string.tag_item)
        );
        post = new Post(MDJSONObject.create(json));

        // get resources
        Resources res = getResources();
        if (TIME_HINT == null) {
            TIME_HINT = getResources().getStringArray(
                R.array.decision_time_hint
            );
        }
        int portraitSize = res.getDimensionPixelSize(
            R.dimen.decision_portrait_size
        );
        int margin = res.getDimensionPixelSize(R.dimen.decision_margin_h4);
        Point outSize = new Point();
        // TODO should avoid calling deprecated functions
        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
            outSize.x = getWindowManager().getDefaultDisplay().getWidth();
        // } else {
        //     getWindowManager().getDefaultDisplay().getSize(outSize);
        // }
        int imageSize = (outSize.x - 3 * margin) / 2;

        // initialization
        ImageView portrait = (ImageView)findViewById(R.id.decision_portrait);
        TextView name = (TextView)findViewById(R.id.decision_name);
        TextView time = (TextView)findViewById(R.id.decision_time);
        TextView countdown = (TextView)findViewById(R.id.decision_countdown);
        TextView bonus = (TextView)findViewById(R.id.decision_bonus);
        ImageView[] images = new ImageView[] {
            (ImageView)findViewById(R.id.decision_image_0),
            (ImageView)findViewById(R.id.decision_image_1)
        };
        for (int i = 0; i < images.length; ++i) {
            ViewGroup.LayoutParams params = images[i].getLayoutParams();
            params.width = imageSize;
            params.height = imageSize;
        }
        btnLike = new ImageButton[] {
            (ImageButton)findViewById(R.id.decision_mark_0),
            (ImageButton)findViewById(R.id.decision_mark_1)
        };
        TextView msg = (TextView)findViewById(R.id.decision_msg);
        Button finish = (Button)findViewById(R.id.title_decision_finish);

        // set text
        name.setText(post.senderId);
        time.setText(formatTime(
            Calendar.getInstance().getTimeInMillis() -
            post.dateTime.getTime()
        ));
        countdown.setText(formatCountdown(
            post.dateTime.getTime() +
            (post.hour * 60 + post.minute) * 60 * 1000 -
            Calendar.getInstance().getTimeInMillis()
        ));
        bonus.setText(String.valueOf(post.bonus));
        msg.setText(post.description);

        // set image
        MDImageLoader.loadRemoteImage(
            post.senderPhoto.pid, portrait,
            portraitSize, portraitSize, null
        );
        for (int i = 0; i < post.images.size(); ++i) {
            MDImageLoader.loadRemoteImage(
                post.images.get(i).pid, images[i],
                imageSize, imageSize, null
            );
        }

        // set click listener
        for (int i = 0; i < btnLike.length; ++i) {
            btnLike[i].setOnClickListener(likeClickListener);
            btnLike[i].setTag(R.string.tag_selected, false);
        }
        finish.setOnClickListener(finishClickListener);
    }

    // format time string
    private static String formatTime(long ms) {
        ms = (ms < 0) ? 0 : ms;
        long minute = ms / 1000 / 60;
        long hour = minute / 60;
        long day = hour / 24;
        minute %= 60;
        hour %= 24;

        String string = "";
        if (day > 0) {
            string += String.valueOf(day) + TIME_HINT[0];
        } else {
            if (hour > 0) {
                string += String.valueOf(hour) + TIME_HINT[1];
            } else {
                if (minute > 0) {
                    string += String.valueOf(minute) + TIME_HINT[2];
                }
            }
        }
        return string.equals("") ? TIME_HINT[4] : (string + TIME_HINT[3]);
    }

    // format countdown string
    private static String formatCountdown(long ms) {
        ms = (ms < 0) ? 0 : ms;
        long minute = ms / 1000 / 60;
        long hour = minute / 60;
        minute %= 60;
        return (hour > 0) ? ">60'" : String.valueOf(minute) + "'";
    }

    // listener for clicking like button
    private OnClickListener likeClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            final int[] resids = {
                R.drawable.like_red,
                R.drawable.like_red_hollow
            };
            for (int i = 0; i < btnLike.length; ++i) {
                boolean isSelected = (btnLike[i] == view);
                btnLike[i].setBackgroundResource(resids[isSelected ? 0 : 1]);
                btnLike[i].setTag(R.string.tag_selected, isSelected);
            }
        }
    };

    // listener for clicking finish button
    private OnClickListener finishClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            ArrayList<Image> images = new ArrayList<Image>();
            for (int i = 0; i < btnLike.length; ++i) {
                if ((Boolean)btnLike[i].getTag(R.string.tag_selected)) {
                    images.add(post.images.get(i));
                }
            }
            if (images.size() == 0) {
                // none image is selected
                Toast.makeText(
                    getApplicationContext(),
                    R.string.decision_error_none_image,
                    Toast.LENGTH_SHORT
                ).show();
                return;
            }
            MDService.MDServiceInstance.sendVote(
                post, images, sendVoteHandler
            );
        }
    };

    // handler for sending vote
    private MDServiceHandler sendVoteHandler = new MDServiceHandler() {
        @Override
        public void onSuccess(Response response) {
            boolean flag = (response.code == 0);
            Toast.makeText(
                getApplicationContext(),
                flag ? R.string.decision_success : R.string.decision_failed,
                Toast.LENGTH_SHORT
            ).show();
            if (flag) {
                finish();
            }
        }
    };
}
