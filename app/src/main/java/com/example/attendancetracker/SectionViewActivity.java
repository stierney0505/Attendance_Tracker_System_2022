package com.example.attendancetracker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class SectionViewActivity extends AppCompatActivity {
    private static final int NAME_QUERY = 0;
    private static final int EMAIL_QUERY = 1;
    private static final String TAG = "DEBUG";
    private String sectionName, semesterName, studentName, studentEmail;
    private ArrayList<String> nameList = new ArrayList<>();
    private ArrayList<String> emailList = new ArrayList<>();
    private static final int READ_REQUEST_CODE = 42;
    private String tinyDBStudentName, tinyDBStudentEmail;
    private DatabaseReference mDatabase;
    TinyDB mTinydb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_section_view);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //Retrieve data from previous activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            sectionName = extras.getString("sectionKey");
            semesterName = extras.getString("semesterKey");
        }
        tinyDBStudentName = semesterName + "_" + sectionName + "_Names";
        tinyDBStudentEmail = semesterName + "_" + sectionName + "_Emails";
        this.setTitle(sectionName);
        mTinydb = new TinyDB(SectionViewActivity.this);
        initClassListView(mTinydb);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.section_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Log.d("DEBUG", "onOptionsItemSelected: " + item);

        switch(item.getItemId()) {
            case R.id.importClassList:
                try {
                    importClassList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.addStudent:
                addStudent();
                return true;
        }
        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    /*
     * Allow the user to select a CSV file from external storage
     */
    private void importClassList() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                String path = uri.getPath();
                path = path.substring(path.indexOf(":") + 1);
                readCSVandDisplay(path);
                Toast.makeText(this, "" + path, Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Crawl through the  and print the list
    private void readCSVandDisplay(String path) {
        try {
            File csvfile = new File(Environment.getExternalStorageDirectory() + "/" + path);
            CSVReader reader = new CSVReader(new FileReader(csvfile.getAbsolutePath()));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                nameList.add(nextLine[0]);
                emailList.add(nextLine[1]);
                addStudentToRealtimeDB(nextLine[0], nextLine[1], semesterName, sectionName);
                System.out.println(nextLine[0] + " " +  nextLine[1]);
            }
            mTinydb.putListString(tinyDBStudentName, nameList);
            mTinydb.putListString(tinyDBStudentEmail, emailList);
            Log.d(TAG, "readCSVandDisplay: " + mTinydb.getAll());
            initClassListView(mTinydb);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initClassListView(TinyDB mTinydb) {
        RecyclerView recyclerView = findViewById(R.id.studentListRecyclerView);
        ArrayList<String> classList = retrieveFromStorage(mTinydb, NAME_QUERY);
        ArrayList<String> emailList = retrieveFromStorage(mTinydb, EMAIL_QUERY);
        ClassListAdapter adapter = new ClassListAdapter(classList, emailList, mTinydb, tinyDBStudentName, tinyDBStudentEmail, semesterName, sectionName, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    /*
     * Method to manually add a student to the section.
     * In order to accomplish this, the AlertDialog will
     * need a LinearLayout to show to EditText fields
     */
    private void addStudent() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog alertDialog = builder.create();
        builder.setTitle("Add Student");
        final EditText nameInput = new EditText(this);
        nameInput.setHint("Name");
        final EditText emailInput = new EditText(this);
        emailInput.setHint("Email");

        nameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.addView(nameInput);
        lay.addView(emailInput);
        builder.setView(lay);

        //Set up the buttons
        builder.setPositiveButton("Add Student", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean nameValid = false;
                boolean emailValid = false;
                studentName = nameInput.getText().toString();
                studentEmail = emailInput.getText().toString();
                addStudentToRealtimeDB(studentName, studentEmail, semesterName, sectionName);
                if (studentName.equals("")) {
                    Toast.makeText(SectionViewActivity.this,"Please enter a Student's Name", Toast.LENGTH_SHORT).show();
                }
                else
                    nameValid = true;
                if (studentEmail.equals("")) {
                    Toast.makeText(SectionViewActivity.this,"Please enter a Student's Email", Toast.LENGTH_SHORT).show();
                }
                else
                    emailValid = true;

                if (nameValid & emailValid) {
                    Log.d(TAG, "onClick: we are valid");
                    nameList.add(studentName);
                    emailList.add(studentEmail);
                    mTinydb.putListString(tinyDBStudentName, nameList);
                    mTinydb.putListString(tinyDBStudentEmail, emailList);
                    initClassListView(mTinydb);
                }
                else {
                    Log.d(TAG, "onClick: we are NOT valid");
                    addStudent();
                }
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

    private ArrayList<String> retrieveFromStorage(TinyDB mTinydb, int query) {
        if (query == NAME_QUERY) {
            if (!mTinydb.getListString(tinyDBStudentName).isEmpty()) {
                nameList = mTinydb.getListString(tinyDBStudentName);
                Log.d(TAG, "retrieveFromStorage: nameList from " + tinyDBStudentName + " is " + nameList);
                return nameList;
            }
            else {
                Log.d(TAG, "retrieveFromStorage: nameList " + tinyDBStudentName + " is empty.");
                return null;
            }
        }
        else if (query == EMAIL_QUERY) {
            if (!mTinydb.getListString(tinyDBStudentEmail).isEmpty()) {
                emailList = mTinydb.getListString(tinyDBStudentEmail);
                Log.d(TAG, "retrieveFromStorage: emailList from " + tinyDBStudentEmail + " is " + emailList);
                return emailList;
            }
            else {
                Log.d(TAG, "retrieveFromStorage: emailList from " + tinyDBStudentEmail + " is empty.");
                return null;
            }
        }
        else {
            return null;
        }
    }

    private void addStudentToRealtimeDB(String studentName, String studentEmail, String semesterName, String sectionName) {
        mDatabase.child("semesters").child(semesterName).child(sectionName).child(studentName).setValue(studentName);
        mDatabase.child("semesters").child(semesterName).child(sectionName).child(studentName).child("email").setValue(studentEmail);
        Log.d(TAG, "addStudentToRealtimeDB: added " + studentName + " to Realtime DB");
        Log.d(TAG, "addStudentToRealtimeDB: added " + studentEmail + " to Realtime DB");
    }
}