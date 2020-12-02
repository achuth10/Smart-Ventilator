package com.example.finalyearproject.Activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.finalyearproject.Classes.Constants;
import com.example.finalyearproject.Classes.MySingleton;
import com.example.finalyearproject.Classes.Patient;
import com.example.finalyearproject.PatientAdapter;
import com.example.finalyearproject.databinding.ActivityPatientListBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PatientList extends AppCompatActivity {

    private static final long INTERVAL = 15000;
    private Context context;
    ActivityPatientListBinding mainBinding;
    TextView name;
    String  id ;
    ArrayList<Patient> patients;
    private Handler mHandler;
    private Runnable mHandlerTask;
    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         mainBinding = ActivityPatientListBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        context = getApplicationContext();
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "New Notification!", Toast.LENGTH_SHORT).show();

            }
        };
        IntentFilter filter = new IntentFilter();
        // specify the action to which receiver will listen
        filter.addAction("com.local.newNotification");
        context.registerReceiver(receiver, filter);
        init();
        //getConnected();
    }
    private void init() {
        patients = new ArrayList<>();
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        id = pref.getString(Constants.KEY_COMP_ID, null);
        name = mainBinding.hospitalNameTxt;
        if(pref.getString("Companion",null)!=null) {
            name.setText(pref.getString("Companion", null));
            mainBinding.idTxt.setText(id);
        }

        mHandler = new Handler();

        mHandlerTask = new Runnable()
        {
            @Override
            public void run() {
                checkStatus();
                mHandler.postDelayed(mHandlerTask, INTERVAL);
            }
        };
        startRepeatingTask();
    }

    private void checkStatus() {
        final StringRequest request = new StringRequest(Request.Method.POST, Constants.RUNNING_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                System.out.println("check status response -> " + response);
                if(!response.contains("No running ventilators")){
                    if(patients!=null)
                        patients.clear();
                    Gson g = new Gson();
                    JsonObject jsonObject;
                    Patient patient;
                    JsonArray convertedObject = new Gson().fromJson(response, JsonArray.class);
                    for (JsonElement jsonElement : convertedObject) {
                        jsonObject = (JsonObject)jsonElement;
                        patient = g.fromJson(jsonObject, Patient.class);
                        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                        Date date = new Date();
                        String now = formatter.format(date);
                        String[] nows = now.split(" ");
                        String lastSeen = patient.getLastSeen();
                        String [] lastSeens = lastSeen.split(" ");
                        if(nows[0].equals(lastSeens[0])){
                            SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                            Date date1 = null,date2 = null;
                            try {
                                date1 = format.parse(lastSeens[1]);
                                date2 = format.parse(nows[1]);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            long difference = (date2.getTime() - date1.getTime())/1000;
                            System.out.println("Difference is " + difference);
                            if(difference>10)
                            {
                                alert(patient.getName());
                                patient.setAvailable(false);
                            }
                            else{
                                patient.setAvailable(true);
                            }
                        }
                        patients.add(patient);
                    }
                    displayPatients();
                }
                else{
                    //Toast.makeText(getApplicationContext(),"No ventilators connected",Toast.LENGTH_SHORT).show();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Toast.makeText(getContext(),error.toString(),Toast.LENGTH_SHORT).show();
                System.out.println("Error is " + error.toString());
            }
        })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map <String,String> params  = new HashMap<String,String>();
                params.put(Constants.KEY_ID,id);
                return params;
            }
        };
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(request);

    }

    private void alert(String name) {
//        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), notification);
//        mp.start();
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                mMediaPlayer.stop();
//            }
//        }, 5000);//millisec.

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle("Ventilator for " + name + " has malfunctioned");
        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
//        dialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int whichButton) {
//
//                dialog.dismiss();
//            }
//        });

        final AlertDialog b = dialogBuilder.create();
        b.show();
        b.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                b.dismiss();
            }
        });
    }

    void startRepeatingTask()
    {
        mHandlerTask.run();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRepeatingTask();
    }

    void stopRepeatingTask()
    {
        mHandler.removeCallbacks(mHandlerTask);
    }

    private void getConnected() {
        final StringRequest request = new StringRequest(Request.Method.POST, Constants.CONNECTED_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                System.out.println("Connected response -> " + response);
                if(!response.contains("No patients")){
                    if(patients!=null)
                        patients.clear();
                    Gson g = new Gson();
                    JsonObject jsonObject;
                    Patient patient;

                    JsonArray convertedObject = new Gson().fromJson(response, JsonArray.class);
                    for (JsonElement jsonElement : convertedObject) {
                        jsonObject = (JsonObject)jsonElement;
                        patient = g.fromJson(jsonObject, Patient.class);
                        patients.add(patient);
                    }
                    displayPatients();
                }
                else{
                    Toast.makeText(getApplicationContext(),"No ventilators connected",Toast.LENGTH_SHORT).show();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Toast.makeText(getContext(),error.toString(),Toast.LENGTH_SHORT).show();
                System.out.println("Error is " + error.toString());
            }
        })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map <String,String> params  = new HashMap<String,String>();
                params.put(Constants.KEY_ID,id);
                return params;
            }
        };
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(request);
    }

    private void displayPatients() {
        RecyclerView recyclerView = mainBinding.PatientRecycler;
        PatientAdapter patientAdapter = new PatientAdapter(patients, getApplicationContext());
        patientAdapter.setMonClickPatient(new PatientAdapter.onClickPatient() {
            @Override
            public void clicked(int position) {
                Intent intent = new Intent(getApplicationContext(), PatientView.class);
                intent.putExtra("Patient", patients.get(position));
                startActivity(intent);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(patientAdapter);
        patientAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {

            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
//                editor.clear();
//                editor.apply();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }

}
