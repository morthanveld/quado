int MOTOR_CURRENT_MIN = 145;
int MOTOR_CURRENT_MAX = 245;

int outputValue = 140;        // value output to the PWM (analog out)

String inputString = "";
boolean stringComplete = false;

void setup() 
{
  inputString.reserve(200);
  Serial.begin(9600);
}

void loop() 
{
  outputValue = 0;
  
  // Handle serial data.
  if (stringComplete) 
  {
    outputValue = 250;
    Serial.println(inputString); 
    inputString = "";
    stringComplete = false;
  }


  // Execute commands.
  // > 100 = no beep
  // 145 -  245
  /*
  outputValue = outputValue + 1; // 0 - 255
  if (outputValue > 255)
  {
    outputValue = 140;
  }
  */
  analogWrite(9, outputValue);

  Serial.print("output = " );                       
  Serial.println(outputValue);   

  delay(500); // ms                     
}

void serialEvent() 
{
  while (Serial.available()) 
  {
    char inChar = (char)Serial.read(); 
    inputString += inChar;

    if (inChar == '\n') 
    {
      stringComplete = true;
    } 
  }
}


/*
Motor ID

    0
3 - | - 1
    2
    
*/
void setMotorSpeed(int motor, int current)
{
  analogWrite(motor + 8, current);
}
