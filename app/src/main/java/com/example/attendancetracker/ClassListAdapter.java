package com.example.attendancetracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class ClassListAdapter extends RecyclerView.Adapter<ClassListAdapter.ViewHolder> {
    private static final String TAG = "ClassListAdapter";
    private ArrayList<String> mNameList, mEmailList;
    private String mTinyDBStudentName, mTinyDBStudentEmail;
    private String mSemesterName, mSectionName;
    private Context mContext;
    private TinyDB mTinydb;
    private DatabaseReference mDatabase;

    public ClassListAdapter(ArrayList<String> nameList, ArrayList<String> emailList, TinyDB tinydb, String tinyDBStudentName, String tinyDBStudentEmail, String semesterName, String sectionName, Context context) {
        mNameList = nameList;
        mEmailList = emailList;
        mContext = context;
        mTinydb = tinydb;
        mTinyDBStudentName = tinyDBStudentName;
        mTinyDBStudentEmail = tinyDBStudentEmail;
        mSemesterName = semesterName;
        mSectionName = sectionName;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.student_list_layout, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        Log.d(TAG, "onBindViewHolder: called");
        holder.listText.setText(mNameList.get(position));
        mDatabase = FirebaseDatabase.getInstance().getReference();
        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked on " + mNameList.get(position));
                launchStudentActivity(mNameList.get(position), mEmailList.get(position));
            }
        });

        holder.removeStudentIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: I clicked removeStudentIcon. Where is my dialog?");
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Remove " + mNameList.get(position) + "?");

                //Set up the buttons
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String tempNameItem = mNameList.get(position);
                        String tempEmailItem = mEmailList.get(position);
                        removeStudent(tempNameItem, tempEmailItem);
                        notifyDataSetChanged();
                    }
                });

                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
    }

    private void removeStudent(String name, String email) {
        for (int i = 0; i < mNameList.size(); i++) {
            if (mNameList.get(i).equals(name)) {
                mNameList.remove(name);
                mEmailList.remove(email);
                removeStudentFromRealtimeDB(name, email, mSemesterName, mSectionName);
                mTinydb.putListString(mTinyDBStudentName, mNameList);
                mTinydb.putListString(mTinyDBStudentEmail, mEmailList);
                Log.d(TAG, "onClick: name DB after removal: " + mTinydb.getListString(mTinyDBStudentName));
                Log.d(TAG, "onClick: email DB after removal: " + mTinydb.getListString(mTinyDBStudentEmail));
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mNameList == null)
            return 0;
        else
            return mNameList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView listText;
        RelativeLayout parentLayout;
        ImageView removeStudentIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            listText = itemView.findViewById(R.id.studentNameView);
            parentLayout = itemView.findViewById(R.id.parent_layout1);
            removeStudentIcon = itemView.findViewById(R.id.removeStudentImageView);
            mContext = itemView.getContext();
        }
    }

    private void launchStudentActivity(String studentName, String studentEmail) {
        Intent intent = new Intent(mContext, StudentDetailsActivity.class);
        intent.putExtra("name", studentName);
        intent.putExtra("email", studentEmail);
        mContext.startActivity(intent);
    }

    private void removeStudentFromRealtimeDB(String studentName, String studentEmail, String semesterName, String sectionName) {
        mDatabase.child("semesters").child(semesterName).child(sectionName).child(studentName).setValue(null);
        mDatabase.child("semesters").child(semesterName).child(sectionName).child(studentName).child("email").setValue(null);
    }
}
