package com.example.attendancetracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;

public class authenticationActivity extends AppCompatActivity {
    EditText passwordField;
    CharSequence invalidPassword = "Password incorrect.";
    int duration = Toast.LENGTH_SHORT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        // Ask the user for storage reading permissions
        String[] requiredPermissions = { Manifest.permission.READ_EXTERNAL_STORAGE };
        ActivityCompat.requestPermissions(this, requiredPermissions, 0);

        Button loginButton = findViewById(R.id.loginBtn);

        passwordField = findViewById(R.id.editPasswordField);

        passwordField.setOnKeyListener(new View.OnKeyListener() {
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

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("DEBUG", "onClick: " + passwordField.getText());
                checkAuthentication();
            }
        });
    }

    public void checkAuthentication() {
        //TODO can't be hardcoded password
        if (passwordField.getText().toString().equals("yellow")) {
            initMainActivity();
        }
        else {
            passwordIncorrectToast();
        }
    }


    public void initMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void passwordIncorrectToast() {
        Toast.makeText(authenticationActivity.this, invalidPassword, duration).show();
    }
}