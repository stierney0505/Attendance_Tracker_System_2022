package com.example.attendancetracker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {
    public static final String TAG = "DEBUG";
    TinyDB mTinydb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        this.setTitle("Settings");

        TextView appVerTextView = (TextView) findViewById(R.id.appVersionTextView);
        appVerTextView.setText("App version: " + BuildConfig.VERSION_NAME);
        mTinydb = new TinyDB(this);

        //Set up onClickListener for the purge button
        Button purgeBtn = (Button) findViewById(R.id.purgeBtn);
        purgeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle("DELETE ALL SEMESTERS AND SECTIONS");
                //Set up "OK" and "Cancel"
                builder.setPositiveButton("Initiate Purge", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mTinydb.clear();
                        Log.d(TAG, "onClick: TinyDB PURGED" + mTinydb.getAll());
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });
    }
}