package com.md4u.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

public class AccountActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_account);
        getWindow().setFeatureInt(
            Window.FEATURE_CUSTOM_TITLE,
            R.layout.title_account
        );
    }
}
