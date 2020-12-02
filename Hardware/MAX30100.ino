#include <Wire.h>
#include <ArduinoJson.h>
#include "MAX30100_PulseOximeter.h"
 
#define REPORTING_PERIOD_MS 100
 
PulseOximeter pox;
uint32_t tsLastReport = 0;
 
void onBeatDetected()
{
    digitalWrite(3, HIGH);
    delay(10);
    digitalWrite(3, LOW);
}
 
void setup()
{
    Serial.begin(115200);
    pinMode(3,OUTPUT);

    Serial.print("Initializing pulse oximeter..");
 
    // Initialize the PulseOximeter instance
    // Failures are generally due to an improper I2C wiring, missing power supply
    // or wrong target chip
    if (!pox.begin()) {
        Serial.println("FAILED");
        for(;;);
    } else {
        Serial.println("SUCCESS");
    }
     pox.setIRLedCurrent(MAX30100_LED_CURR_7_6MA);
 
    // Register a callback for the beat detection
    pox.setOnBeatDetectedCallback(onBeatDetected);
}


StaticJsonBuffer<1000> jsonBuffer;
JsonObject& root = jsonBuffer.createObject();
 
void loop()
{
    // Make sure to call update as fast as possible
    pox.update();
    if (millis() - tsLastReport > REPORTING_PERIOD_MS) {
        root["HR"] = pox.getHeartRate();
        root["SP"] = pox.getSpO2();
        root.printTo(Serial);
        tsLastReport = millis();
    }
}
