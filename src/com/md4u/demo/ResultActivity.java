package com.md4u.demo;

import java.util.ArrayList;
import java.util.Calendar;

import com.md4u.demo.MDJSON.MDJSONArray;
import com.md4u.demo.MDJSON.MDJSONObject;
import com.md4u.demo.MDModel.Fan;
import com.md4u.demo.MDModel.Post;
import com.md4u.demo.MDModel.Response;
import com.md4u.demo.MDService.MDServiceHandler;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ResultActivity extends Activity {
    private Post post;
    private TextView[] votes;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_result);
        getWindow().setFeatureInt(
            Window.FEATURE_CUSTOM_TITLE,
            R.layout.title_result
        );

        // get intent data
        String json = getIntent().getExtras().getString(
            getResources().getString(R.string.tag_item)
        );
        post = new Post(MDJSONObject.create(json));

        // get resources
        Resources res = getResources();
        int margin = res.getDimensionPixelSize(R.dimen.result_margin_h4);
        int barSize = res.getDimensionPixelSize(R.dimen.result_bar_container_size);
        Point outSize = new Point();
        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR2) {
            outSize.x = getWindowManager().getDefaultDisplay().getWidth();
        // } else {
        //     getWindowManager().getDefaultDisplay().getSize(outSize);
        // }
        int imageSize = (outSize.x - 2 * (margin + barSize)) / 2;

        // initialization
        TextView countdown = (TextView)findViewById(R.id.result_countdown);
        TextView bonus = (TextView)findViewById(R.id.result_bonus);
        TextView msg = (TextView)findViewById(R.id.result_msg);
        ImageView[] images = new ImageView[] {
            (ImageView)findViewById(R.id.result_image_0),
            (ImageView)findViewById(R.id.result_image_1)
        };
        for (int i = 0; i < images.length; ++i) {
            ViewGroup.LayoutParams params = images[i].getLayoutParams();
            params.width = imageSize;
            params.height = imageSize;
        }
        ImageView[] bars = new ImageView[] {
            (ImageView)findViewById(R.id.result_bar_0),
            (ImageView)findViewById(R.id.result_bar_1)
        };
        int[] colors = new int[] {
            res.getColor(R.color.result_option_0),
            res.getColor(R.color.result_option_1)
        };
        // TODO remove hard code
        for (int i = 0; i < bars.length; ++i) {
            ViewGroup.LayoutParams params = bars[i].getLayoutParams();
            params.height = (i == 0) ? 200 : 96;
            ViewGroup.MarginLayoutParams mparams = (ViewGroup.MarginLayoutParams)params;
            mparams.topMargin = imageSize - params.height;
            ((GradientDrawable)bars[i].getBackground()).setColor(colors[i]);
        }
        votes = new TextView[] {
            (TextView)findViewById(R.id.result_vote_0),
            (TextView)findViewById(R.id.result_vote_1)
        };
        // TODO remove hard code
        MDService.MDServiceInstance.sendResult(post, sendResultHandler);
        votes[0].setText("-1");
        votes[1].setText("-1");

        // set text
        countdown.setText(formatCountdown(
            post.dateTime.getTime() +
            (post.hour * 60 + post.minute) * 60 * 1000 -
            Calendar.getInstance().getTimeInMillis()
        ));
        bonus.setText(String.valueOf(post.bonus));
        msg.setText(post.description);

        // set image
        for (int i = 0; i < post.images.size(); ++i) {
            MDImageLoader.loadRemoteImage(
                post.images.get(i).pid, images[i],
                imageSize, imageSize, null
            );
        }
    }

    // format countdown string
    private static String formatCountdown(long ms) {
        ms = (ms < 0) ? 0 : ms;
        long minute = ms / 1000 / 60;
        long hour = minute / 60;
        minute %= 60;
        return (hour > 0) ? ">60'" : String.valueOf(minute) + "'";
    }
    
    private MDServiceHandler sendResultHandler = new MDServiceHandler() {
        @Override
        public void onSuccess(Response response) {
            boolean flag = (response.code == 0);
            // todo code != 0   
            MDJSONArray ja = response.getJSONArray("result");
            // todo check pic id
            for (int i = 0; i < ja.length() && i<2; ++i) {
            	// todo check 
            	votes[i].setText(ja.getJSONObject(i).getString("support"));
            	
            }
            
        }
    };
}
