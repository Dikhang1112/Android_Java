package com.voggella.android.doan.GiaoDienLogin;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.voggella.android.doan.R;

public class Login extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        //Link button with id
        Button btnSignin = findViewById(R.id.btnSignIn);
        Button btnSignup = findViewById(R.id.btnSignUp);
        Button btnAdmin = findViewById(R.id.btnAdmin);

        //Nhay qua trang sign in

btnSignin.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        Intent intent = new Intent(Login.this, SignIn.class);
        startActivity(intent);
    }
});

btnSignup.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        Intent intent = new Intent(Login.this, SignUp.class);
        startActivity(intent);
    }
});
btnAdmin.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(Login.this, AdminLogin.class);
        startActivity(intent);
    }
});
    }
}