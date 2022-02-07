package com.example.attendancetracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class StudentDetailsActivity extends AppCompatActivity {
    private static final String TAG = "DEBUG";
    private DatabaseReference mDatabase;
    String mStudentName, mStudentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_details);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        //Retrieve data from previous activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mStudentName = extras.getString("name");
            mStudentEmail = extras.getString("email");
            this.setTitle(mStudentName);
            Bitmap myQRCode = generateQRBitmap(mStudentName, mStudentEmail);
            ImageView qrCodeView = findViewById(R.id.qrCodeView);
            qrCodeView.setImageBitmap(myQRCode);
        }
    }

    private Bitmap generateQRBitmap(String studentName, String studentEmail) {
        Bitmap bitmap = null;
        int width = 256;
        int height = 256;

        Log.d(TAG, "generateQRBitmap: student name: " + studentName);

        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(getContents(studentName, studentEmail), BarcodeFormat.QR_CODE, width, height);
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private String getContents(String studentName, String studentEmail) {
        return studentName + ", " + studentEmail;
    }
}