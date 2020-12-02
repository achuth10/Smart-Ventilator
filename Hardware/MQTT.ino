/*
  AWS Iot Core

  It connects to AWS IoT server then:
  - publishes "hello world" to the topic "outTopic" every two seconds
  - subscribes to the topic "inTopic", printing out any messages
*/

#include "FS.h"
#include <ESP8266WiFi.h>
#include <PubSubClient.h> //https://www.arduinolibraries.info/libraries/pub-sub-client
#include <NTPClient.h> //https://www.arduinolibraries.info/libraries/ntp-client
#include <WiFiUdp.h>
#include <stdlib.h>
#include <string.h>
#include <Wire.h>
#include <ArduinoJson.h>
#include <SoftwareSerial.h>
SoftwareSerial s(D5,D6);  //(Rx, Tx)

int Step = 0; //GPIO0---D3 of Nodemcu--Step of stepper motor driver
int Dir  = 2;

int i = 0;
int recvSteps = 0;

// Update these with values suitable for your network.

const char* ssid = "";
const char* password = "";
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org");

const char* AWS_endpoint = ""; //MQTT broker ip

void callback(char* topic, byte* payload, unsigned int length) {
  String res = "";
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) {
//    Serial.print((char)payload[i]);
    res+=(char)payload[i];
  }
  Serial.println("Response is " + res);
  int n = res.length();
  char response[n + 1];
  strcpy(response, res.c_str());
  recvSteps = atoi(response);
  if (recvSteps>1500)
  {
    recvSteps = 1500;
  }
  Serial.print("Received Steps: ");
  Serial.println(recvSteps);
}
WiFiClientSecure espClient;
PubSubClient client(AWS_endpoint, 8883, callback, espClient); //set MQTT port number to 8883 as per //standard
//============================================================================
#define BUFFER_LEN 256
long lastMsg = 0;
char msg[BUFFER_LEN];
int value = 0;
byte mac[6];
char mac_Id[18];
//============================================================================

void setup_wifi() {

  delay(10);
  // We start by connecting to a WiFi network
  espClient.setBufferSizes(512, 512);
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());

  timeClient.begin();
  while (!timeClient.update()) {
    timeClient.forceUpdate();
  }

  espClient.setX509Time(timeClient.getEpochTime());

}

void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Attempt to connect
    if (client.connect("ESPthing")) {
      Serial.println("connected");
      // Once connected, publish an announcement...
      client.publish("outTopic", "hello world");
      // ... and resubscribe
      client.subscribe("inTopic");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");

      char buf[256];
      espClient.getLastSSLError(buf, 256);
      Serial.print("WiFiClientSecure SSL error: ");
      Serial.println(buf);

      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

void setup() {
 pinMode(Step, OUTPUT); //Step pin as output
 pinMode(Dir,  OUTPUT); //Direction pin as output
 Serial.begin(115200);
 s.begin(115200);
  
  Serial.setDebugOutput(true);
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(LED_BUILTIN, OUTPUT);
  setup_wifi();
  delay(1000);
  if (!SPIFFS.begin()) {
    Serial.println("Failed to mount file system");
    return;
  }

  Serial.print("Heap: "); Serial.println(ESP.getFreeHeap());

  // Load certificate file
  File cert = SPIFFS.open("/cert.der", "r"); //replace cert.crt eith your uploaded file name
  if (!cert) {
    Serial.println("Failed to open cert file");
  }
  else
    Serial.println("Success to open cert file");

  delay(1000);

  if (espClient.loadCertificate(cert))
    Serial.println("cert loaded");
  else
    Serial.println("cert not loaded");

  // Load private key file
  File private_key = SPIFFS.open("/private.der", "r"); //replace private eith your uploaded file name
  if (!private_key) {
    Serial.println("Failed to open private cert file");
  }
  else
    Serial.println("Success to open private cert file");

  delay(1000);

  if (espClient.loadPrivateKey(private_key))
    Serial.println("private key loaded");
  else
    Serial.println("private key not loaded");

  // Load CA file
  File ca = SPIFFS.open("/ca.der", "r"); //replace ca eith your uploaded file name
  if (!ca) {
    Serial.println("Failed to open ca ");
  }
  else
    Serial.println("Success to open ca");

  delay(1000);

  if (espClient.loadCACert(ca))
    Serial.println("ca loaded");
  else
    Serial.println("ca failed");

}

int sp = 0;
int hr = 0;

void loop() {

  if (!client.connected()) {
    reconnect();
  }
  
  client.loop();

  StaticJsonBuffer<1000> jsonBuffer;
  JsonObject& root = jsonBuffer.parseObject(s);
  if (root == JsonObject::invalid())
    return;

  int data1 = root["HR"];
  int data2 = root["SP"];
  String data = "{HR: " + String(data1) + ", SP: " + String(data2) + "}";
  int l1 = data.length();
  char char_array[l1 + 1];
  strcpy(char_array, data.c_str());
    
  long now = millis();
  if (now - lastMsg > 1000) {
    lastMsg = now;
     
    Serial.print("Publish message: ");
    Serial.println(char_array);
    client.publish("outTopic", char_array);

  }

    digitalWrite(Dir, HIGH); //Rotate stepper motor in clock wise direction
    for(i=1;i<=recvSteps;i++)
    {
      digitalWrite(Step, HIGH);
      delay(1);
      digitalWrite(Step, LOW);
      delay(1);
    }
    digitalWrite(LED_BUILTIN, HIGH);
    digitalWrite(Dir, LOW); //Rotate stepper motor in anti clock wise direction
    for(i=1;i<=recvSteps;i++)
    {
      digitalWrite(Step, HIGH);
      delay(1);
      digitalWrite(Step, LOW);
      delay(1);
    }
    digitalWrite(LED_BUILTIN, LOW);
}
