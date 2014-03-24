package com.md4u.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent getDataIntent = new Intent(this, MDData.class);
        startService(getDataIntent);
        setContentView(R.layout.activity_main);
        // set click handlers
        setClickHandler(PostActivity.class, R.id.main_post);
        setClickHandler(BrowseActivity.class, R.id.main_browse);
        setClickHandler(ShareActivity.class, R.id.main_share);
        setClickHandler(BonusActivity.class, R.id.main_bonus);
        setClickHandler(AccountActivity.class, R.id.main_account);
        setClickHandler(FansActivity.class, R.id.main_fans);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    // helper to set click handler
    private void setClickHandler(final Class<?> klass, int buttonId) {
        Button button = (Button)findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, klass);
                startActivityForResult(intent, 0);
            }
        });
    }
}
