#include <SoftwareSerial.h>
#include <stdio.h>
#include <Servo.h>


int lamp = 9;
int light = 2;
int fan = 3;
Servo lock;



int bluetoothTX = 11 ;
int bluetoothRX = 10 ;
char receivedValue ;

SoftwareSerial bluetooth ( bluetoothTX, bluetoothRX );

void setup()
{
  Serial.begin(9600);  
  Serial.println("console> ");
  
  pinMode(lamp, OUTPUT);
  pinMode(light, OUTPUT);
  pinMode(fan, OUTPUT);
  lock.attach(8);
 
  bluetooth.begin(115200);
  bluetooth.print("$$$");
  delay(100);
  bluetooth.println("U,9600,N");
  bluetooth.begin(9600);

}


void loop()
{

signed int data = 0;

 if( bluetooth.available() )
  {
    data = (int) bluetooth.read();
    Serial.println( data );                 // for debugging, show received data

      if(data == 11) 
      {
        digitalWrite(light, HIGH);
      }
      else if(data == 10)
      {
        digitalWrite(light, LOW) ;
      }
      else if(data == 21)
      {
       digitalWrite(lamp, HIGH); 
      }
      else if(data == 20)
      {
       digitalWrite(lamp, LOW); 
      }
      else if(data == 31)
      {
       digitalWrite(fan, HIGH); 
      }
      else if(data == 30)
      {
       digitalWrite(fan, LOW); 
      }
      else if(data == 41)
      {
       lock.write(0);
      }
      else if(data == 40)
      {
       lock.write(70); 
      }
      else if(data >= 57 && data <=144)
      {
        int val = 111+data;
        Serial.println("lamp");
        Serial.println(val);
        analogWrite(lamp, data);
        Serial.println("Leaving lamp");
      }
      else if(data>=157 && data <=233)
      {
       Serial.println("Entering fan");
       Serial.println("With an intensity of");
       Serial.println(data);
       analogWrite(fan, data); 
       Serial.println("Leaving fan");
      }
      else if(data == 50)
      {
        digitalWrite(light, LOW);
        digitalWrite(lamp, LOW);
        digitalWrite(fan, LOW);
        lock.write(70);
      }
      else if(data == 51)
      {
       digitalWrite(light, HIGH);
       digitalWrite(fan, HIGH);
       lock.write(0);
       
      }    

    bluetooth.flush();                       // IMPORTANT clean bluetooth stream, flush stuck data

  }

}


