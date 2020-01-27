package com.example.alcheringaadmin2020;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.angmarch.views.NiceSpinner;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
//import okhttp3.Request;
import okhttp3.RequestBody;

import static com.google.firebase.firestore.FieldValue.serverTimestamp;
//import okhttp3.Response;

public class MainActivity extends AppCompatActivity {


    FirebaseFirestore firestore;
    DocumentReference documentReference;
    String chosenTopic ;
    NiceSpinner niceSpinner;
    EditText title,message;
    Button submit;
    Button feedBtn;
    String chosentopic;
//    ArrayAdapter aa;
    ProgressDialog progressDialog;
    private String serverKey = "AAAAyClnqTE:APA91bHNYLgZZdL91IOdFWB23oV7YJ9SPhiRu7DsNBLqUuf1UwUdXIHgA7argtzWB_DHi5vWB4q7nBvpIMIc9S0oZDIdGXFPLegaSh-oEhdBSeqhyMZJwNTTrQ4TyBXwEyX0qPL5RQUo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        title = findViewById(R.id.title);
        message = findViewById(R.id.message);
        submit = findViewById(R.id.sendButton);
        progressDialog = new ProgressDialog(this);
        feedBtn = findViewById(R.id.feedBtn);
        niceSpinner = findViewById(R.id.spinner);
        firestore = FirebaseFirestore.getInstance();
//        niceSpinner.setOnItemSelectedListener(this);
        documentReference = firestore.collection("topics").document("topics");
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task)
            {
                Toast.makeText(MainActivity.this, "task", Toast.LENGTH_SHORT).show();
                if (task.isSuccessful())
                {
                    DocumentSnapshot document = task.getResult() ;
                    Log.d("DATA", document.getId() + " => " + document.getData()) ;
                    Map<String,Object > m = document.getData() ;

                    final ArrayList<String> topics = (ArrayList<String>) m.get("topics") ;

                    Toast.makeText(MainActivity.this, "topic received", Toast.LENGTH_SHORT).show();

                    if(topics.size() == 0)
                    {
                        Toast.makeText(MainActivity.this, "Add a topic First", Toast.LENGTH_SHORT).show();
                        //startActivity(new Intent(sendNotif.this,MainActivity.class)) ;
                        // NO TOPICS ADDED SENT TO THE MAIN ACTIVITY //
                        //finishActivity(1);
                    }

                    niceSpinner.attachDataSource(topics);


                    niceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                        {
                            chosenTopic = topics.get(position) ;

                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent)
                        {
                            chosenTopic = topics.get(0) ;

                        }
                    });



                } else {
                    Log.w("Failed", "Error getting documents.", task.getException());
                    Toast.makeText(MainActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendRequestByOk(title.getText().toString().trim(), message.getText().toString().trim(), "") ;
            }
        });

        feedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,feedActivity.class));
            }
        });

    }

    @SuppressLint("StaticFieldLeak")
    private void sendRequestByOk(final String title, final String body, final String imgUrl) {
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                OkHttpClient client = new OkHttpClient();
                JSONObject json = new JSONObject();
                JSONObject jsonData = new JSONObject();
                String topic =  "/topics/" + chosenTopic ;
                try
                {
                    jsonData.put("title",title);
                    jsonData.put("body",body);
                    jsonData.put("icon",imgUrl) ;
                    json.put("data",jsonData);
                    json.put("to",topic);

                    RequestBody body = RequestBody.create(JSON, json.toString());
                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .header("Authorization", "key=" + serverKey)
                            .url("https://fcm.googleapis.com/fcm/send")
                            .post(body)
                            .build();

                    okhttp3.Response response = client.newCall(request).execute();
                    String finalResponse = response.body().string();
                    Toast.makeText(MainActivity.this, finalResponse, Toast.LENGTH_LONG).show();

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                return  null;
            }
        }.execute();

        progressDialog.setMessage("Sending Notifications...");
        progressDialog.show();
        HashMap<String,Object> data = new HashMap<>();
        data.put("title",title);
        data.put("body",body);
        data.put("timestamp", serverTimestamp());

        firestore.collection("notificationLog").document("id").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                String x= documentSnapshot.getString("id");
                int y = (Integer.valueOf(x)+1);
                x = String.valueOf(y);
                final String finalX = x;
                data.put("id",finalX);
                firestore.collection("notificationLog").document("allNotifications").collection("notifications").document(x).set(data).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Map<String,String> nid = new HashMap<>();
                        nid.put("id", finalX);
                        firestore.collection("notificationLog").document("id").update("id", finalX).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(MainActivity.this, "Successfully sent the notification", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();

                            }
                        });

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(MainActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
            }
        });


    }
}
