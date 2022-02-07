package com.example.attendancetracker;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class SectionListActivity extends AppCompatActivity {
    private static final String TAG = "DEBUG";
    private String semesterName = "";
    ArrayList<String> sectionList = new ArrayList<String>();
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_section_list);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //Retrieve data from previous activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            semesterName = extras.getString("semesterKey");
        }
        this.setTitle(semesterName);

        TinyDB tinydb = new TinyDB(this);
        initRecyclerView(tinydb);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.addSectionBtn);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addSection(tinydb);
            }
        });
    }

    private void addSection(TinyDB tinydb) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Section");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        //Set up "OK" and "Cancel"
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String section_Text = input.getText().toString();
                sectionList.add(section_Text);
                tinydb.putListString(semesterName, sectionList);
                addSectionToOnlineDB(section_Text);
                Log.d(TAG, "onClick: ADDED " + section_Text +" to " + semesterName);
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
        RecyclerView recyclerView = findViewById(R.id.sectionListRecyclerView);
        ArrayList<String> sectionList = retrieveListFromStorage(tinydb);
        Log.d(TAG, "Section List from storage: " + sectionList);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(sectionList, this, tinydb, semesterName);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public ArrayList<String> retrieveListFromStorage(TinyDB tinydb) {
        if (!tinydb.getListString(semesterName).isEmpty()) {
            Log.d(TAG, "retrieveListFromStorage: SEMESTER IS" + semesterName);
            sectionList = tinydb.getListString(semesterName);
            return sectionList;
        }
        else
            return null;

    }

    private void addSectionToOnlineDB(String sectionName) {
        mDatabase.child("semesters").child(semesterName).child(sectionName).setValue(sectionName);
    }
}