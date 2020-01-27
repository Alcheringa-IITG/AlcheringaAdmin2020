package com.example.alcheringaadmin2020;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.angmarch.views.NiceSpinner;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class feedActivity extends AppCompatActivity {

    EditText title,description,subtile;
    String ID;
    TextView textViewStatus;
    ProgressBar progressBar;
    StorageReference mStorageReference;
    DatabaseReference mDatabaseReference;
    CollectionReference collectionReference;
    Boolean photoexits;
    Button submit;
    FirebaseFirestore db;
//    FirebaseUser currentFirebaseUser ;
    ProgressDialog dialog;
    String dp_url;
    Button uploadWithPhoto;
    DocumentReference documentReference;
    String chosenTopic ;
    Button uploadWithoutPhoto;
    FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        dialog = new ProgressDialog(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(feedActivity.this,MainActivity.class));
            }
        });
//        final NiceSpinner niceSpinner = (NiceSpinner) findViewById(R.id.nice_spinner);
        
//        mPostLink=findViewById(R.id.postLinkID);  //testing ****

        title=findViewById(R.id.postTitle);
        description=findViewById(R.id.postDesc);
//        subtile=findViewById(R.id.subTitle);
        uploadWithPhoto=findViewById(R.id.buttonPostPhoto);
        uploadWithoutPhoto=findViewById(R.id.buttonPost);

        db = FirebaseFirestore.getInstance();
//        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser() ;

        uploadWithPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getImage();
            }
        });
        uploadWithoutPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postWithoutImage();
            }
        });


        mDatabaseReference = FirebaseDatabase.getInstance().getReference("feed");
        mDatabaseReference.keepSynced(true);

        mStorageReference = FirebaseStorage.getInstance().getReference();
        collectionReference = FirebaseFirestore.getInstance().collection("feed");
        textViewStatus = findViewById(R.id.photoViewStatus);
        progressBar =findViewById(R.id.progressbar);



    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //when the user choses the file
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            //if a file is selected
            if (data.getData() != null) {
                //uploading the file
                uploadFile(data.getData());
            }else{
                Toast.makeText(this, "No file chosen", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void postWithoutImage() {
        Map<String,Object> newComplain = new HashMap<>();
        newComplain.put("title",title.getText().toString());
        newComplain.put("text", description.getText().toString());

        newComplain.put("time", ServerValue.TIMESTAMP);

        //newComplain.put("dp","https://firebasestorage.googleapis.com/v0/b/onestopiitg.appspot.com/o/iitg.jpg?alt=media&token=b9784201-9911-48eb-8f18-bacb80594bbe");
        newComplain.put("image","");

        DatabaseReference dd = mDatabaseReference.push();

        dd.setValue(newComplain, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if(databaseError==null){
                    Toast.makeText(feedActivity.this, "Posted Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                }else{
                    Toast.makeText(feedActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        });

    }


    private void getImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            Toast.makeText(getApplicationContext(),"Give storage permission",Toast.LENGTH_SHORT).show();
            startActivity(intent);
            return;
        }


        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
    }

    private void uploadFile(final Uri data) {

        progressBar.setVisibility(View.VISIBLE);
        Map<String,Object> newComplain = new HashMap<>();
        newComplain.put("title",title.getText().toString());
        newComplain.put("text", description.getText().toString());
        newComplain.put("time",ServerValue.TIMESTAMP);
        newComplain.put("image","");

        DatabaseReference dd = mDatabaseReference.push();
        String ref = dd.toString();

        dd.setValue(newComplain, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                if(databaseError==null){
                    Log.i("Shivam","Complaint added");
//                    ID = documentReference.getId();
                    Uri compressedImageFile;
                    try {
//            compressedImageFile = Uri.fromFile(new Compressor(this).setMaxHeight(1280).setMaxWidth(720).compressToFile(new File(data.getPath())));
                        final StorageReference sRef = mStorageReference.child("PostImages/" +ref+".jpeg");
                        sRef.putFile(data)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @SuppressWarnings("VisibleForTests")
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        progressBar.setVisibility(View.GONE);
                                        textViewStatus.setText("File Uploaded Successfully");


                                        sRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
//                                                Map<String,Object> image = new HashMap<>();
//                                                image.put("image",uri.toString());
//                                                collectionReference.document(ID).set(image, SetOptions.merge());

                                                dd.child("image").setValue(uri.toString(), new DatabaseReference.CompletionListener() {
                                                    @Override
                                                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                                                        if(databaseError==null){
                                                            Toast.makeText(getApplicationContext(),"Post added",Toast.LENGTH_SHORT).show();
                                                            finish();
                                                        }else{
                                                            Toast.makeText(feedActivity.this,"Image not posted: "+ databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                            finish();
                                                        }
                                                    }
                                                });



                                            }
                                        });


                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                })
                                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                    @SuppressWarnings("VisibleForTests")
                                    @Override
                                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                        textViewStatus.setText((int) progress + "% Uploading...");
                                    }
                                });

                    }
                    catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(feedActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Log.i("Shivam","failed");
                    Toast.makeText(feedActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });




    }
}
