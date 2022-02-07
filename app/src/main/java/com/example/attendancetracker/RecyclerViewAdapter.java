package com.example.attendancetracker;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "RecyclerViewAdapter";
    private ArrayList<String> mList;
    private Context mContext;
    private TinyDB mTinydb;
    private String mSemesterName;
    private DatabaseReference mDatabase;

    public RecyclerViewAdapter(ArrayList<String> semesterList, Context context, TinyDB tinydb, String semester) {
        mList = semesterList;
        mContext = context;
        mTinydb = tinydb;
        mSemesterName = semester;
    }

    public RecyclerViewAdapter(ArrayList<String> semesterList, Context context, TinyDB tinydb) {
        mList = semesterList;
        mContext = context;
        mTinydb = tinydb;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_listitem, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        holder.listText.setText(mList.get(position));
        mDatabase = FirebaseDatabase.getInstance().getReference();
        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked on " + mList.get(position));
                if (mContext instanceof SectionListActivity)
                    launchSectionActivity(mList.get(position), mSemesterName);
                else
                    launchSectionListActivity(mList.get(position));
            }
        });

        /*
         * OnClick listener for the remove 'X' icon for each listed semester
         * Tapping the 'X' launches an AlertDialog for confirmation to remove.
         */
        holder.removeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Remove " + mList.get(position) + "?");

                //Set up the buttons
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String tempListItem = mList.get(position);
                        mList.remove(position);
                        if (mContext instanceof SectionListActivity) {
                            removeSection(tempListItem);
                            removeSectionFromOnlineDB(mSemesterName, tempListItem);
                        }
                        else {
                            removeSemesterFromRealtimeDB(tempListItem);
                            mTinydb.putListString("semesterStorage", mList);
                        }

                        Log.d(TAG, "onClick: " + mTinydb.getListString("semesterStorage"));
                        notifyDataSetChanged();
                    }
                });

                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });
    }

    /*
     *This method will remove the desired class section from TinyDB
     */
    private void removeSection(String sectionName) {
        ArrayList<String> semesterList = mTinydb.getListString("semesterStorage");
        for (int i = 0; i < semesterList.size(); i++) {
            String semesterIndex = semesterList.get(i);
            ArrayList<String> sectionList = mTinydb.getListString(semesterIndex);
            for (int j = 0; j < sectionList.size(); j++) {
                if (sectionList.get(j).equals(sectionName)) {
                    sectionList.remove(sectionName);
                    mTinydb.putListString(semesterIndex, sectionList);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mList == null)
            return 0;
        else
            return mList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView listText;
        RelativeLayout parentLayout;
        ImageView removeIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            listText = itemView.findViewById(R.id.listItemView);
            parentLayout = itemView.findViewById(R.id.parent_layout);
            removeIcon = itemView.findViewById(R.id.removeImageView);
            mContext = itemView.getContext();
        }
    }

    private void launchSectionListActivity(String semesterName) {
        Intent intent = new Intent(mContext, SectionListActivity.class);
        intent.putExtra("semesterKey", semesterName);
        mContext.startActivity(intent);
    }

    private void launchSectionActivity(String sectionName, String semesterName) {
        Intent intent = new Intent(mContext, SectionViewActivity.class);
        intent.putExtra("sectionKey", sectionName);
        intent.putExtra("semesterKey", semesterName);
        mContext.startActivity(intent);
    }

    private void removeSectionFromOnlineDB(String semesterName, String sectionName) {
        mDatabase.child("semesters").child(semesterName).child(sectionName).setValue(null);
    }

    private void removeSemesterFromRealtimeDB(String semesterName) {
        mDatabase.child("semesters").child(semesterName).setValue(null);
    }
}
