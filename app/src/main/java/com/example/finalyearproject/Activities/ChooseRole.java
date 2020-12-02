package com.example.finalyearproject.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.finalyearproject.Classes.Constants;
import com.example.finalyearproject.Classes.MySingleton;
import com.example.finalyearproject.R;
import com.example.finalyearproject.databinding.ActivityChooseRoleBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;
import java.util.Map;

import pl.droidsonroids.gif.GifImageView;

public class ChooseRole extends AppCompatActivity {
    ActivityChooseRoleBinding mainBinding;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private String token;
    private TextInputLayout layout;
    private GifImageView loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = ActivityChooseRoleBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
        pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        editor = pref.edit();
        String user = pref.getString("User",null);
        if(user!=null)
        {
            if(user.equals("Companion")) {

                startActivity(new Intent(getApplicationContext(), PatientList.class));
            }
            else
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
        }

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("FireBase", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                         token = task.getResult().getToken();
                        System.out.println("Token is " + token);
                        //Toast.makeText(getApplicationContext(), token, Toast.LENGTH_SHORT).show();
                    }
                });


        mainBinding.CompanionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog("Companion");
            }
        });
        mainBinding.CompanionCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog("Companion");
            }
        });

        mainBinding.VentilatorCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog("Ventilator");
            }
        });

        mainBinding.VentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog("Ventilator");
            }
        });
    }
    private void showDialog(final String type) {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.hospital_name_layout, null);
        dialogBuilder.setView(dialogView);
         layout = dialogView.findViewById(R.id.HospitalNameLayout);
        final TextInputEditText edt = dialogView.findViewById(R.id.HospitalNameEdit);

         loader = dialogView.findViewById(R.id.hospitalNameLoader);


        if(type.equals("Companion"))
        {
//            dialogBuilder.setTitle("Enter Hospital Name");
            edt.setHint("Hospital Name");
        }
        else{
//            dialogBuilder.setTitle("Enter Patient Name");
            edt.setHint("Patient Name");
        }

        dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        final AlertDialog b = dialogBuilder.create();
        b.show();
        b.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nametxt = edt.getText().toString().trim();
                if (nametxt.length() > 0 ) {
                    if(token==null)
                    {
                        Toast.makeText(getApplicationContext(),"Please try again",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        register(nametxt,b,type);
                    }

                } else {
                    edt.setError("Please enter a name");
                    edt.requestFocus();

                }
            }
        });
    }
    public void register(final String name, final AlertDialog b, final String type){
        loader.setVisibility(View.VISIBLE);
        layout.setVisibility(View.GONE);
        final StringRequest request = new StringRequest(Request.Method.POST, Constants.REG_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                loader.setVisibility(View.GONE);
                if(response.contains("Registered")){
                    String[] resp = response.split(";");
                    if(type.equals("Companion")) {
                        editor.putString("User", "Companion");
                        editor.putString("Companion", name);
                        editor.putString(Constants.KEY_COMP_ID, resp[1]);
                        editor.apply();
                        b.dismiss();
                        startActivity(new Intent(getApplicationContext(), PatientList.class));
                    }
                    else{
                        editor.putString("User","Patient");
                        editor.putString("Patient", name);
                        editor.putString(Constants.KEY_PATIENT_ID, resp[1]);
                        editor.apply();
                        b.dismiss();
                        startActivity(new Intent(getApplicationContext(),MainActivity.class));
                    }
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Toast.makeText(getContext(),error.toString(),Toast.LENGTH_SHORT).show();
                loader.setVisibility(View.GONE);
                System.out.println("Error is " + error.toString());
            }
        })
        {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map <String,String> params  = new HashMap<String,String>();
                params.put(Constants.KEY_NAME,name);
                params.put(Constants.KEY_TOKEN,token);
                return params;
            }
        };

        //Prevent multi request when slow connection observed
        request.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(request);
    }
}