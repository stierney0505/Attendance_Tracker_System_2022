package com.example.attendancetracker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int EXTERNAL_STORAGE_PERMISSION_CODE = 23;
    private static final String TAG = "DEBUG";
    ArrayList<String> semesterList = new ArrayList<String>();
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //Show the app version number for beta purposes
        //TODO remove this for release
        TextView appVer = (TextView) findViewById(R.id.appVerTextView);
        appVer.setText("Version " + BuildConfig.VERSION_NAME);

        //Request external storage permission if not already allowed
        requestStoragePermission();

        TinyDB tinydb = new TinyDB(this);
        initRecyclerView(tinydb);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.addSemesterBtn);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addSemester(tinydb);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Log.d("DEBUG", "onOptionsItemSelected: " + item);

        switch(item.getItemId()) {
            case R.id.menuEdit:
                //Launch Settings Activity
                openSettingsActivity(semesterList);
                return true;
        }
        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    private void addSemester(TinyDB tinydb) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Semester");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        //Set up "OK" and "Cancel"
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String semester_Text = input.getText().toString();
                semesterList.add(semester_Text);
                tinydb.putListString("semesterStorage", semesterList);
                addSemesterToOnlineDB(semester_Text);
                initRecyclerView(tinydb);
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

    private void initRecyclerView(TinyDB tinydb) {
        RecyclerView recyclerView = findViewById(R.id.myRecyclerView);
        ArrayList<String> semesterList = retrieveListFromStorage(tinydb);
        Log.d("DEBUG", "Semester List from storage: " + semesterList);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(semesterList, this, tinydb);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void openSettingsActivity(ArrayList<String> semesterList) {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("semesterList", semesterList);
        startActivity(intent);
    }

    public ArrayList<String> retrieveListFromStorage(TinyDB tinydb) {
        if (!tinydb.getListString("semesterStorage").isEmpty()) {
            semesterList = tinydb.getListString("semesterStorage");
            Log.d(TAG, "retrieveListFromStorage: semester list is" + semesterList);
            return semesterList;
        }
        else
            Log.d(TAG, "retrieveListFromStorage: why is it empty?");
            return null;
    }

    /*
     * This method checks if external storage permissions were granted.
     * If not, prompt the user to allow for storage permissions.
     */
    private void requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            return;

        ActivityCompat.requestPermissions(this, new String[]
                {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, EXTERNAL_STORAGE_PERMISSION_CODE);
    }

    /*
     * This function adds the semester to the onlineDB
     */
    private void addSemesterToOnlineDB(String semester) {
        mDatabase.child("semesters").child(semester).setValue(semester);
    }
}