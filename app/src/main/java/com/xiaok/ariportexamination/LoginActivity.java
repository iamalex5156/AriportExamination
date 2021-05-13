package com.xiaok.ariportexamination;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    private EditText et_usename;
    private EditText et_password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initView();

        findViewById(R.id.btn_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String usename = et_usename.getText().toString().trim();
                String password = et_password.getText().toString().trim();
                if (TextUtils.isEmpty(usename) || TextUtils.isEmpty(password)){
                    Toast.makeText(getApplicationContext(),"用户名或密码不能为空", Toast.LENGTH_SHORT).show();

                }else if (usename.equals("admin") && password.equals("123456")){
                    startActivity(new Intent(getApplicationContext(),MainActivity.class));

                }else {
                   Toast.makeText(getApplicationContext(),"用户名或密码错误，注意区分大小写", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initView(){
        et_usename = findViewById(R.id.et_usename);
        et_password = findViewById(R.id.et_password);
    }
}
