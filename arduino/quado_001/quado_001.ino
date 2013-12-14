#include <Usb.h>
#include <AndroidAccessory.h>

#define MSG_SIZE  8
#define LED0          13

#define MOTOR_A       8
#define MOTOR_B       9
#define MOTOR_C       10
#define MOTOR_D       11

#define HEARTBEAT  0x1
#define MOTOR      0x2

#define STATUS     0xA
#define SENSOR     0xB

#define NOT_READY  0x0
#define READY      0x1


AndroidAccessory acc("GDG Suwon",            // Manufacturer
                     "clavier_aoa",                 // Model
                     "MiniHack project Arduino Board",   // Description
                     "1.0",                     // Version
                     "http://gdg-suwon.blogspot.kr/2012/06/gdg-suwon-hack-time-for-android-adk.html",  // URI
                     "0000000012345678");       // Serial

void setup()
{
  Serial.begin(115200);
  Serial.print("\r\nADK Started\r\n");

  pinMode(LED0, OUTPUT);
  pinMode(MOTOR_A, OUTPUT);
  pinMode(MOTOR_B, OUTPUT);

  // Power On Android Accessory interface and init USB controller
  acc.powerOn();
}

void sendStatus(byte status)
{
  byte buffer[MSG_SIZE];
  buffer[0] = STATUS;
  buffer[1] = status;
  acc.write(buffer, MSG_SIZE);
}

void sendSensor(byte sensor)
{
  byte buffer[MSG_SIZE];
  buffer[0] = SENSOR;
  buffer[1] = sensor;
  acc.write(buffer, MSG_SIZE);
}

void loop()
{
  byte data[MSG_SIZE];
  
  digitalWrite(MOTOR_A, LOW);
  digitalWrite(MOTOR_B, LOW);
  
  if (acc.isConnected()) 
  {  
    digitalWrite(LED0, HIGH);
    
    // Read data from android.
    int len = acc.read(data, sizeof(data), 1);
    if (len > 0) 
    {
      if (data[0] == HEARTBEAT)
      {
        digitalWrite(MOTOR_A, HIGH);
        sendStatus(READY);
      }   
      else if (data[0] == MOTOR)
      {
        digitalWrite(MOTOR_B, HIGH);
        sendSensor(0);
      }
    }
    
    
    // Sensor data to android.
/*    data[0] = 1;
    data[1] = 2;
    acc.write(data, 2);*/
  }
  else
  {
    digitalWrite(LED0, LOW);
    digitalWrite(MOTOR_A, LOW);
    digitalWrite(MOTOR_B, LOW);
  }
  
  delay(10);
}

