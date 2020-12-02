package com.example.finalyearproject.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.example.finalyearproject.databinding.ActivityPatientViewBinding;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.UUID;

import static com.amazonaws.regions.Regions.AP_SOUTH_1;

public class PatientView extends AppCompatActivity {


        ActivityPatientViewBinding patientViewBinding;
        static final String LOG_TAG = PatientView.class.getCanonicalName();

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
        private static final Regions MY_REGION = AP_SOUTH_1;
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

        private LineChart chart;
        private int y = 1;
        private ArrayList<Entry> entries;
        private TextView volumeTxt;

        CognitoCachingCredentialsProvider credentialsProvider;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            patientViewBinding = ActivityPatientViewBinding.inflate(getLayoutInflater());
            setContentView(patientViewBinding.getRoot());
            initActivity();
            setupChart();
            initAWS();
            Intent intent = getIntent();
            patientName = intent.getStringExtra("Patient");
        }

        private void initActivity() {
            entries = new ArrayList<>();
            chart = patientViewBinding.RealTimeDataLineChart;
            volumeTxt = patientViewBinding.VolumeTxt;
        }

        @Override
        protected void onDestroy() {
            try {
                mqttManager.disconnect();
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
                                    subscribeAWS();
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
            } catch (Exception e) {
                Log.e(LOG_TAG, "Subscription error.", e);
            }
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
    }
