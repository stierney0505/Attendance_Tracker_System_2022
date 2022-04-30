package com.example.attendancetracker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class SectionViewActivity extends AppCompatActivity {
    private static final int NAME_QUERY = 0;
    private static final int EMAIL_QUERY = 1;
    private static final String TAG = "DEBUG";
    private String sectionName, semesterName, studentName, studentEmail;
    private ArrayList<String> nameList = new ArrayList<>();
    private ArrayList<String> emailList = new ArrayList<>();
    private static final int READ_REQUEST_CODE = 42;
    private static final int PICKFILE_RESULT_CODE = 8778;
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

        Button buttonQr = findViewById(R.id.QRButton);

        buttonQr.setText("Scan QR Code");

        buttonQr.setOnClickListener(this::QRScan);


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

        switch (item.getItemId()) {
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
            case R.id.createCSV:
                try {
                    ExportCSV();
                } catch (IOException e) {

                }
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
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                Toast.makeText(getBaseContext(), "QR Scanning cancelled", Toast.LENGTH_SHORT).show();
            } else {
                checkIn(intentResult.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
            } catch (Exception e) {
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
        int nightmode = getBaseContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        switch (nightmode) { //it seems as though the text of a recyclerview doesnt change in nightmode so this is a programmed solution
            case Configuration.UI_MODE_NIGHT_YES:
                recyclerView.setBackgroundColor(Color.parseColor("#6f727b"));
        }
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
                    Toast.makeText(SectionViewActivity.this, "Please enter a Student's Name", Toast.LENGTH_SHORT).show();
                } else
                    nameValid = true;
                if (studentEmail.equals("")) {
                    Toast.makeText(SectionViewActivity.this, "Please enter a Student's Email", Toast.LENGTH_SHORT).show();
                } else
                    emailValid = true;

                if (nameValid & emailValid) {
                    Log.d(TAG, "onClick: we are valid");
                    nameList.add(studentName);
                    emailList.add(studentEmail);
                    mTinydb.putListString(tinyDBStudentName, nameList);
                    mTinydb.putListString(tinyDBStudentEmail, emailList);
                    initClassListView(mTinydb);

                } else {
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
            } else {
                Log.d(TAG, "retrieveFromStorage: nameList " + tinyDBStudentName + " is empty.");
                return null;
            }
        } else if (query == EMAIL_QUERY) {
            if (!mTinydb.getListString(tinyDBStudentEmail).isEmpty()) {
                emailList = mTinydb.getListString(tinyDBStudentEmail);
                Log.d(TAG, "retrieveFromStorage: emailList from " + tinyDBStudentEmail + " is " + emailList);
                return emailList;
            } else {
                Log.d(TAG, "retrieveFromStorage: emailList from " + tinyDBStudentEmail + " is empty.");
                return null;
            }
        } else {
            return null;
        }
    }

    private void addStudentToRealtimeDB(String studentName, String studentEmail, String semesterName, String sectionName) {
        mDatabase.child("semesters").child(semesterName).child(sectionName).child(studentName).setValue(studentName);
        mDatabase.child("semesters").child(semesterName).child(sectionName).child(studentName).child("email").setValue(studentEmail);
        Log.d(TAG, "addStudentToRealtimeDB: added " + studentName + " to Realtime DB");
        Log.d(TAG, "addStudentToRealtimeDB: added " + studentEmail + " to Realtime DB");
    }


    public void QRScan(View view) { //QR code scanning method
        IntentIntegrator intentIntegrator = new IntentIntegrator(this);
        intentIntegrator.setPrompt("Scan a barcode or QR Code");
        intentIntegrator.setOrientationLocked(true);
        intentIntegrator.initiateScan();
    }

    public String separateName(String codeValue) { //Helper method to separate the name from the email from a QR scan
        String temp;
        int comma = codeValue.indexOf(",");
        if (comma == -1)
            comma = codeValue.length() - 1;
        temp = codeValue.substring(0, comma);
        return temp;
    }

    public void checkIn(String unfilteredName) { //Method that takes the raw String from a QR scan and checks the student
        String name = separateName(unfilteredName);
        ArrayList<String> listNames = mTinydb.getListString(tinyDBStudentName);
        for (int i = 0; i < listNames.size(); i++) {
            if (name.equals(listNames.get(i))) {
                if (mTinydb.getListString(name) != null) {
                    ArrayList<String> temp = mTinydb.getListString(name);
                    Date today = new Date();
                    temp.add(today.toString());
                    mTinydb.putListString(name, temp);
                } else {
                    ArrayList<String> temp = new ArrayList<>();
                    Date today = new Date();
                    temp.add(today.toString());
                    mTinydb.putListString(name, temp);
                }
            }

        }
    }

    public String replaceSpaces(String fileName) {//Helper method to replace the spaces in a file name should they occur
        String newFileName = fileName;
        char illegalChar[] = {' ', '@', ':', '#', '<', '>', '$', '+', '%', '!', '`', '&', '{', '}', '=', '|', '\\', '/', '"', '*'};
        for (int i = 0; i < fileName.length(); i++) {
            for (int j = 0; j < illegalChar.length; j++) {
                if (fileName.charAt(i) == illegalChar[j]) {
                    newFileName = newFileName.substring(0, i) + '-' + newFileName.substring(i + 1);
                }
            }

        }
        return newFileName;
    }

    public void ExportCSV() throws IOException { //exports the student attendace to a csv for the semester-section to the downloads folder

        if (!Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(intent);
            return;
        }
        File root = new File(Environment.getExternalStorageDirectory(),"Download");
        String fileName = replaceSpaces("Attendance-" + semesterName + "-" + sectionName + ".csv");
        File csvFile = new File(root, fileName);

        try {
            if (!csvFile.exists()) {
                csvFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        CSVWriter writer = null;
        try {
            FileWriter outputfile = new FileWriter(csvFile);

            writer = new CSVWriter(outputfile);


            ArrayList<String> names = mTinydb.getListString(tinyDBStudentName);
            ArrayList<String> emails = mTinydb.getListString(tinyDBStudentEmail);

            for (int i = 0; i < names.size(); i++) {
                String data[] = listToArray(mTinydb.getListString(names.get(i)), names.get(i), emails.get(i));
                writer.writeNext(data);
            }

            writer.flush();
            writer.close();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String[] listToArray(ArrayList<String> list, String name, String email) { //helper method to turn an Arraylist into an Array
        String[] temp = new String[list.size() + 2];
        temp[0] = name;
        temp[1] = email;
        for (int i = 2; i < list.size() + 1; i++) {
            temp[i] = list.get(i - 1);
        }
        return temp;
    }


}