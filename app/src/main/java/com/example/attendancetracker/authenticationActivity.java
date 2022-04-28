package com.example.attendancetracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;

public class authenticationActivity extends AppCompatActivity {

    CharSequence invalidPassword = "Password incorrect.";
    int duration = Toast.LENGTH_SHORT;
    private EditText emailEnter, passwordEnter;
    private Button loginButton;
    private Button registerButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);
        mAuth = FirebaseAuth.getInstance();
        emailEnter = findViewById(R.id.email);
        passwordEnter = findViewById(R.id.editPasswordField);
        loginButton = findViewById(R.id.loginBtn);
        registerButton = findViewById(R.id.RegBtn);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAuthentication();
            }
        });
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

        passwordEnter = findViewById(R.id.editPasswordField);
        passwordEnter.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_ENTER:
                            checkAuthentication();
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });
    }

    private void register() {
        Intent intent = new Intent(authenticationActivity.this, RegistrationActivity.class);
        startActivity(intent);
    }

    private void checkAuthentication() {
        String email, password;
        password = passwordEnter.getText().toString();
        email = emailEnter.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), "Enter your email and/or password", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(
                    @NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    initMainActivity();
                }
                else {
                    Toast.makeText(getApplicationContext(),"Invalid login information", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void initMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

}