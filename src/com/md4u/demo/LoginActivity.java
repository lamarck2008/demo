package com.md4u.demo;

import com.md4u.demo.MDModel.Response;
import com.md4u.demo.MDService.MDServiceHandler;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
    private EditText userName;
    private EditText userPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // find views
        userName = (EditText)findViewById(R.id.login_user_name);
        userPwd = (EditText)findViewById(R.id.login_user_pwd);

        // set login click handler
        Button btnLogin = (Button)findViewById(R.id.login_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // user name and password
                final String name = userName.getText().toString();
                final String pwd = userPwd.getText().toString();
                if (name.equals("") || pwd.equals("")) {
                    // either user name or password is empty
                    Toast.makeText(
                        getApplicationContext(),
                        R.string.login_error_empty,
                        Toast.LENGTH_SHORT
                    ).show();
                } else {
                    // check user name and password
                    MDService.MDServiceInstance.login(
                        name, pwd, loginHandler
                    );
                }
            }
        });

        // initialize MDCacheUtils
        MDCacheUtils.setContext(getApplicationContext());
    }

    // handler of network service for login
    private MDServiceHandler loginHandler = new MDServiceHandler() {
        @Override
        public void onSuccess(Response response) {
            if (response.code != 0) {
                // wrong user name or password
                Toast.makeText(
                    getApplicationContext(),
                    R.string.login_error_invalid,
                    Toast.LENGTH_SHORT
                ).show();
            } else {
                // correct user name and password
                Intent intent = new Intent(
                    LoginActivity.this,
                    MainActivity.class
                );
                startActivity(intent);
                finish();
            }
        }
    };
}
