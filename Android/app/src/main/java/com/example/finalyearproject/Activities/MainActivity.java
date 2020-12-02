package com.example.finalyearproject.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.finalyearproject.Adapters.SettingAdapter;
import com.example.finalyearproject.Classes.Constants;
import com.example.finalyearproject.Classes.MySingleton;
import com.example.finalyearproject.Classes.Patient;
import com.example.finalyearproject.Classes.Setting;
import com.example.finalyearproject.Classes.Vital;
import com.example.finalyearproject.R;
import com.example.finalyearproject.databinding.ActivityMainBinding;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import static com.amazonaws.regions.Regions.US_EAST_1;
import static com.example.finalyearproject.Classes.Constants.KEY_COMP_ID;
import static com.example.finalyearproject.Classes.Constants.KEY_COMP_TOKEN;

public class MainActivity extends AppCompatActivity {
    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "";
    // Name of the AWS IoT policy to attach to a newly created certificate
    private static final String AWS_IOT_POLICY_NAME = "";

    // Region of AWS IoT
//    private static final Regions MY_REGION = AP_SOUTH_1;
    private static final Regions MY_REGION = US_EAST_1;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;
    String clientId;
    String keystorePath;
    String keystoreName;
    String keystorePassword;

    KeyStore clientKeyStore = null;
    String certificateId;
    String patientName = null;

    CognitoCachingCredentialsProvider credentialsProvider;
    private WebSocket ws;
    private LineChart chart;
    private final static int INTERVAL = 10000; //10 seconds
    private int y = 1;
    private ArrayList<Entry> entries;
    private TextView volumeTxt;
    private ArrayList<Setting> settings;
    private SettingAdapter settingAdapter;
    private ActivityMainBinding binding;
    private BottomSheetBehavior bottomSheetBehavior;
    boolean doubleBackToExitPressedOnce = false;
    private Button actionButton;
    private Button connectButton;
    private Slider TotalVolumeSlider,RespiratorySlider;
            //,PeakFlowSlider,PeepSlider,, TriggerFlowSlider, InhaleTimeSlider;
    private Context context;
    private BroadcastReceiver receiver;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Handler mHandler;
    private Runnable mHandlerTask;
    private int steps = 650;
    private String status = "OFF";


    public class EchoWebSocketListener extends WebSocketListener {

        private static final int NORMAL_CLOSURE_STATUS = 1000;
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            connectionOpened();
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            output(text);
        }
        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            output("Receiving bytes : " + bytes.hex());
        }
        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            output("Closing : " + code + " / " + reason);
        }
        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            output("Error : " + t.getMessage());
        }

    }

    private void connectionOpened() {

        }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
    }

    private void checkGoogle() {
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
        if (acct != null) {
            String personName = acct.getDisplayName();
            binding.PatientNameTxt.setText(personName);
//            String personGivenName = acct.getGivenName();
//            String personFamilyName = acct.getFamilyName();
//            String personEmail = acct.getEmail();
//            String personId = acct.getId();
//            Uri personPhoto = acct.getPhotoUrl();
         }
    }

    private void init() {
        pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        editor = pref.edit();
        settings = new ArrayList<>();
        entries = new ArrayList<Entry>();
        chart = binding.RealTimeDataLineChart;
        context = getApplicationContext();
        volumeTxt = binding.VolumeTxt;
        actionButton = binding.ActionButton;
        connectButton = binding.BaseButton;
        setSettings();
        Intent intent = getIntent();
        String mode = intent.getStringExtra("Selection");
        binding.ControlModeTxt.setText(mode);
        setupChart();
        actionButton.setEnabled(false);
        initAWS();
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomNavigationContainer);
        binding.PatientNameTxt.setText(pref.getString("Patient",null));
        if(pref.getString(KEY_COMP_ID,null)!=null)
        {
            binding.BaseTxt.setText("Connected to base - " + pref.getString(KEY_COMP_ID,null));
        }
        binding.myToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(actionButton.getText().equals("Start"))
                {
//                    OkHttpClient client = new OkHttpClient();
//                    Request request = new Request.Builder().url("ws://192.168.1.21:81/").build();
//                    EchoWebSocketListener listener = new EchoWebSocketListener();
//                    ws = client.newWebSocket(request, listener);
                    steps = (int)TotalVolumeSlider.getValue();
//                    connectAWS();
                    subscribeAWS();
                    ping();
                    status = "ON";
                    checkIn(status);
                    actionButton.setText("Stop");
                }
                else{
//                    ws.close(1000,"Stopped by user");
                    try {
                        steps = 0;
                        ping();
                        status = "OFF";
                        checkIn(status);
                        stopRepeatingTask();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Disconnect error.", e);
                    }
                    actionButton.setText("Start");
                }
            }
        });

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "New Notification!", Toast.LENGTH_SHORT).show();

            }
        };
        IntentFilter filter = new IntentFilter();
        // specify the action to which receiver will listen
        filter.addAction("com.local.newNotification");
        context.registerReceiver(receiver, filter);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });



         mHandler = new Handler();

         mHandlerTask = new Runnable()
        {
            @Override
            public void run() {
                checkIn(status);
                mHandler.postDelayed(mHandlerTask, INTERVAL);
            }
        };
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("FireBase", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                        System.out.println("Token is " + token);
                        //Toast.makeText(getApplicationContext(), token, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkIn(final String stats) {
        System.out.println("Checked in ");
        final StringRequest request = new StringRequest(Request.Method.POST, Constants.CHECKIN_URL, new com.android.volley.Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                System.out.println("Check in  response -> " + response);


            }
        }, new com.android.volley.Response.ErrorListener() {
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
                String id = pref.getString(Constants.KEY_PATIENT_ID,null);
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                Date date = new Date();
                String time = formatter.format(date);
                params.put(Constants.KEY_ID,id);
                params.put("status",stats);
                params.put("time",time);
                System.out.println(params);
                return params;
            }
        };
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(request);

    }

    private void ping() {
        try {
            mqttManager.publishString(String.valueOf(steps), "inTopic", AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }

    void startRepeatingTask()
    {
        mHandlerTask.run();
    }

    void stopRepeatingTask()
    {
        mHandler.removeCallbacks(mHandlerTask);
    }



    private void setSettings() {
        //DO NOT CHANGE ORDER
        //WILL BREAK FUNCTIONALITY IN SETTING ADAPTER
//        settings.add(new Setting("Total Volume (ml)",240f,1600f)); // 6 mL per kg http://rc.rcjournal.com/content/61/6/774#sec-15
//        settings.add(new Setting("Peak flow",60f,120f)); //Tidal vol / Inhale time  https://www.youtube.com/watch?v=ud0fOZN0o4g
//        settings.add(new Setting("Peep (cm h2o)",20f,200f));
//        settings.add(new Setting("Respiratory rate (BPM)",2f,20f)); //How many breaths per minute the patient should take
//        settings.add(new Setting("Trigger flow (l/min)",2f,20f)); // Speed of air
//        settings.add(new Setting("Inhale time (sec)",2f,20f)); // Time to inhale
//        settingAdapter = new SettingAdapter(settings,getApplicationContext());
//        settingRecycler.setAdapter(settingAdapter);
//        settingRecycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
//        settingAdapter.notifyDataSetChanged();


        TotalVolumeSlider = binding.TotalVolumeSlider;
        RespiratorySlider = binding.RespiratorySlider;

        TotalVolumeSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                float volume =  slider.getValue();
                System.out.println("Volume is " + volume);
                steps = compSteps(volume);
                System.out.println("Steps is " + steps);
            }
        });


        RespiratorySlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                float inhaleTime = 60/slider.getValue();
                float volume =  TotalVolumeSlider.getValue()/1000;
//                float triggerFlow = (volume/inhaleTime) * 60;
//                TriggerFlowSlider.setValue(triggerFlow);
            }
        });

    }

    private int compSteps(float volume) {
        return (int) Math.ceil(1.3*volume);
    }

    private void setupChart() {
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }


    private void newVal(float x) {
        volumeTxt.setText(""+x);
        entries.add(new Entry(y,x));
        updateChart(entries);
        y++;
    }

    private void updateChart(ArrayList<Entry> entries1) {
        LineDataSet dataSet = new LineDataSet(entries1, "Volume");
        chart.setVisibleXRangeMaximum(6);
        dataSet.setDrawIcons(false);
//        dataSet.enableDashedLine(10f, 5f, 0f);
//        dataSet.enableDashedHighlightLine(10f, 5f, 0f);
        dataSet.setColor(Color.DKGRAY);
        dataSet.setCircleColor(Color.DKGRAY);
        dataSet.setLineWidth(1f);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
//        dataSet.setFormLineWidth(1f);
//        dataSet.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
//        dataSet.setFormSize(15.f);
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.notifyDataSetChanged();
        chart.moveViewToX(lineData.getEntryCount());
        }


    private void output(final String txt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                System.out.println(txt);
                try {
                    //So that when an error message comes it doesn't crash
                    newVal(Float.parseFloat(txt));
                }catch (Exception ignored){

                }

            }
        });
    }



    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED)
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                editor.clear();
                editor.apply();
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
    @Override
    protected void onDestroy() {
        //ws.close(EchoWebSocketListener.NORMAL_CLOSURE_STATUS, null);
        try {
            mqttManager.disconnect();
            status = "OFF";
            checkIn(status);
            stopRepeatingTask();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
        }
        super.onDestroy();
    }

    private void initAWS() {
        clientId = UUID.randomUUID().toString();
        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                    binding.ActionButton.setVisibility(View.VISIBLE);
                    connectAWS();
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                binding.ActionButton.setVisibility(View.VISIBLE);
                                connectAWS();
                            }
                        });
                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }
    }

    private void connectAWS() {
        Log.d(LOG_TAG, "clientId = " + clientId);

        try {
            mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == AWSIotMqttClientStatus.Connecting) {
//                                tvStatus.setText("Connecting...");

                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                Toast.makeText(getApplicationContext(),"Connected",Toast.LENGTH_SHORT).show();
                                actionButton.setEnabled(true);
//                                tvStatus.setText("Connected");

                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                if (throwable != null) {
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
//                                tvStatus.setText("Reconnecting");
                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                if (throwable != null) {
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
                                Toast.makeText(getApplicationContext(),"Disconnected",Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(),"Disconnected",Toast.LENGTH_SHORT).show();

                            }
                        }
                    });
                }
            });

        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
//            tvStatus.setText("Error! " + e.getMessage());
        }

    }

    private void subscribeAWS() {
        final String topic ="HeartRate";
        Log.d(LOG_TAG, "topic = " + topic);

        try {
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String message = new String(data, StandardCharsets.UTF_8);
                                    Log.d(LOG_TAG, "Message arrived:");
                                    Log.d(LOG_TAG, "   Topic: " + topic);
                                    Log.d(LOG_TAG, " Message: " + message);
                                    newVal(Float.parseFloat(message));
                                }
                            });
                        }
                    });

//            startRepeatingTask();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }
        try {
            mqttManager.subscribeToTopic("outTopic", AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String message = new String(data, StandardCharsets.UTF_8);
                                    Log.d(LOG_TAG, "Message arrived:");
                                    Log.d(LOG_TAG, "   Topic: " + topic);
                                    Log.d(LOG_TAG, " Message: " + message);
                                    Vital vital = new Gson().fromJson(message, Vital.class);
                                    updateChartVital(vital);
                                }
                            });
                        }
                    });

            startRepeatingTask();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }
    }

    private void updateChartVital(Vital vital) {
            System.out.println(vital.toString());
    }

    private void showDialog() {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = this.getLayoutInflater();
                final View dialogView = inflater.inflate(R.layout.hospital_name_layout, null);
        dialogBuilder.setView(dialogView);

        final TextInputEditText edt = dialogView.findViewById(R.id.HospitalNameEdit);
        edt.setHint("Enter the companion id");
        edt.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        dialogBuilder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
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
                String id = edt.getText().toString().trim();
                if (id.length() > 0 ) {
                    connectToBase(id,b);

                } else {
                    edt.setError("Please enter a name");
                    edt.requestFocus();

                }
            }
        });
    }

    private void connectToBase(final String compId, final AlertDialog b) {
        final StringRequest request = new StringRequest(Request.Method.POST, Constants.BASE_URL, new com.android.volley.Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                System.out.println("Connect to base response -> "+response);
                if(response.contains("token")){
                    String resp[] = response.split(";");
                    editor.putString(KEY_COMP_TOKEN,resp[1]);
                    editor.putString(KEY_COMP_ID,compId);
                    editor.apply();
                    Toast.makeText(getApplicationContext(),"Connected to base",Toast.LENGTH_SHORT).show();
                    b.dismiss();
                    binding.BaseTxt.setText("Connected to base - " +compId);
                }
            }
        }, new com.android.volley.Response.ErrorListener() {
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
                String id = pref.getString(Constants.KEY_PATIENT_ID,null);
                params.put(Constants.KEY_ID,id);
                params.put(Constants.KEY_COMP_ID,compId);
                return params;
            }
        };

        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(request);
    }
}
