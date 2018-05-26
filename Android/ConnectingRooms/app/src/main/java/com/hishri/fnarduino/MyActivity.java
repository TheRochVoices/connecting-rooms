package com.hishri.fnarduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MyActivity extends AppCompatActivity {
    TextView bluetoothstatus, bluetoothPaired;
    Button btndisconnect;
    BluetoothAdapter myBluetooth;
    boolean status;
    ArrayList<String> devicesList;
    ArrayList<BluetoothDevice> ListDevices;
    ArrayAdapter<String> adapter;
    InputStream taInput;
    OutputStream taOut;
    SeekBar lampIntensity, fanIntensity;
    BluetoothDevice pairedBluetoothDevice = null;
    BluetoothSocket blsocket = null ;
    ImageButton mainLight, power, lamp, fan, door;
    TextToSpeech tts;
    private final int REQ_CODE_SPEECH_INPUT = 100;


    /**variables to keep track of the status of the devices**/
    boolean lightStat = false;
    boolean lampStat = false;
    boolean fanStat = false;
    boolean doorStat = false;



    ListView listt;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        bluetoothstatus = (TextView) findViewById(R.id.bluetooth_state);
        bluetoothPaired = (TextView) findViewById(R.id.bluetooth_paired);
       // enableLedButton = (Button) findViewById(R.id.buttonlightup);
        //btnshut = (Button) findViewById(R.id.buttonShut);
        btndisconnect = (Button) findViewById(R.id.buttondisconnect);
        listt = (ListView) findViewById(R.id.mylist);
        mainLight = (ImageButton) findViewById(R.id.mainLight);
        power = (ImageButton) findViewById(R.id.power);
        lamp = (ImageButton) findViewById(R.id.lamp);
        door = (ImageButton) findViewById(R.id.lock);
        fan = (ImageButton) findViewById(R.id.fan);
        lampIntensity = (SeekBar) findViewById(R.id.lampIntensity);
        fanIntensity = (SeekBar) findViewById(R.id.fanIntensity);


        ListDevices = new ArrayList<BluetoothDevice>();
        devicesList = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.listitem, R.id.txtlist,  devicesList);
        listt.setAdapter(adapter);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.UK);
                }
            }
        });


        mainLight.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if(blsocket != null && blsocket.isConnected()) {
                    if (lightStat == true) {
                        mainOff();
                    } else {
                        mainOn();
                    }
                }
            }
        });
        door.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (blsocket != null && blsocket.isConnected()) {
                    if (doorStat == true) {
                        doorOff();
                    } else {
                        doorOn();
                    }
                }
            }
        });
        fan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (blsocket != null && blsocket.isConnected()) {
                    if (fanStat == true) {
                        fanOff();
                    } else {
                        fanOn();
                    }
                }
            }
        });
        lamp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (blsocket != null && blsocket.isConnected()) {
                    if (lampStat == true) {
                        lampOff();
                    } else {
                        lampOn();
                    }
                }
            }
        });
        power.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(blsocket != null && blsocket.isConnected())
                {
                    leaving();
                }
            }
        });


        btndisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(blsocket != null && blsocket.isConnected())
                {
                    try
                    {
                        blsocket.close();
                        Toast.makeText(getApplicationContext(), "disconnected", Toast.LENGTH_LONG).show();
                        bluetoothPaired.setText("DISCONNECTED");
                        bluetoothPaired.setTextColor(getResources().getColor(R.color.red));

                    }catch (IOException ioe)
                    {
                        Log.e("app>", "Cannot close socket");
                        pairedBluetoothDevice = null;
                        Toast.makeText(getApplicationContext(), "Could not disconnect", Toast.LENGTH_LONG).show();

                    }

                }
            }
        });

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        listt.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getApplicationContext(), "item with address: " + devicesList.get(i) + " clicked", Toast.LENGTH_LONG).show();

            connect2LED(ListDevices.get(i));
            }
        });



        // getting seekbar current value

        lampIntensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            int currentVal = 0 ;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            {
                currentVal = i ;
                if(currentVal<100)
                {
                    send2Bluetooth(-299, 100);
                }
                else
                {
                    send2Bluetooth(200, currentVal);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                //send2Bluetooth ( 300, currentVal );


               // Toast.makeText(getApplicationContext(), "LED 1 : "+ currentVal, Toast.LENGTH_SHORT).show();
            }
        });
        fanIntensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            int currentVal = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b)
            {
                currentVal = i ;
                if(currentVal<100)
                {
                    send2Bluetooth(-199, 100);
                }
                else
                {
                    send2Bluetooth(-199, currentVal);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getApplicationContext(), "LED 2 : "+ currentVal, Toast.LENGTH_SHORT).show();

                send2Bluetooth ( -199, currentVal );
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        client.connect();

        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        status = myBluetooth.isEnabled();
        myBluetooth.startDiscovery();
        if (status)
        {
            bluetoothstatus.setText("ENABLED");
            registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
        else {
            bluetoothstatus.setText("NOT READY");
        }
    }


    void connect2LED(BluetoothDevice device)
    {
         UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") ;
        try {
            // ATTENTION: This was auto-generated to implement the App Indexing API.
            blsocket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            blsocket.connect();
            pairedBluetoothDevice = device;
            bluetoothPaired.setText("PAIRED: "+device.getName());
            bluetoothPaired.setTextColor(getResources().getColor(R.color.green));

            Toast.makeText(getApplicationContext(), "Device paired successfully!",Toast.LENGTH_LONG).show();
        }catch(IOException ioe)
        {
            Log.e("taha>", "cannot connect to device :( " +ioe);
            Toast.makeText(getApplicationContext(), "Could not connect",Toast.LENGTH_LONG).show();
            pairedBluetoothDevice = null;
        }
    }

    void send2Bluetooth(int device, int stat)
    {
        //make sure there is a paired device
        if ( pairedBluetoothDevice != null && blsocket != null )
        {
             try
             {
                 taOut = blsocket.getOutputStream();
                 taOut.write(device + stat);
                 Toast.makeText(this, (device+stat) + "", Toast.LENGTH_LONG).show();

                 taOut.flush();
             }
             catch(IOException ioe)
             {
                 Log.e( "app>" , "Could not open a output stream "+ ioe );
             }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }



    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "My Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,

                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.hishri.fnarduino/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {

        public void onReceive(Context context, Intent intent)
        {

            Log.i("app>", "broadcast received") ;
            String action = intent.getAction();


            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                devicesList.add(device.getName() + " @"+device.getAddress());
                ListDevices.add(device);

                adapter.notifyDataSetChanged();
            }
        }
    };

    public void mainClicked(View view)
    {
        promptSpeechInput();
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    Toast.makeText(this, result.get(0), Toast.LENGTH_LONG).show();
                    whatToDo(result.get(0));
                }
                break;
            }

        }
    }
    protected void whatToDo(String s)
    {
        if(s.contains("main light") && s.contains("on"))
        {
            Toast.makeText(this, "Main Light On", Toast.LENGTH_LONG).show();
            mainOn();
            tts.speak("Main Light is turned on now, Sir", TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else if(s.contains("main light") && s.contains("off"))
        {
            Toast.makeText(this, "Main Light Off", Toast.LENGTH_LONG).show();
            mainOff();
            tts.speak("Main Light is turned off now, Sir", TextToSpeech.QUEUE_FLUSH, null, null);
            //send2Bluetooth(13, 13);
        }
        else if(s.contains("lamp") && s.contains("on"))
        {
            Toast.makeText(this, "Lamp On", Toast.LENGTH_LONG).show();
            lampOn();
            tts.speak("Turning the lamp on, Sir", TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else if(s.contains("lamp") && s.contains("off"))
        {
            Toast.makeText(this, "Lamp off", Toast.LENGTH_LONG).show();
            lampOff();
            tts.speak("Turning off the lamp, Sir", TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else if(s.contains("fan") && s.contains("on"))
        {
            Toast.makeText(this, "Fan on", Toast.LENGTH_LONG).show();
            fanOn();
            tts.speak("Indeed it is hot in here, Sir", TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else if(s.contains("fan") && s.contains("off"))
        {
            Toast.makeText(this, "Fan off", Toast.LENGTH_LONG).show();
            fanOff();
            tts.speak("Good choice. I'll just turn off the fan, Sir", TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else if(s.contains("door") && s.contains("open"))
        {
            Toast.makeText(this, "Door open ", Toast.LENGTH_LONG).show();
            doorOff();
            tts.speak("Unlocking the door, Sir", TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else if(s.contains("door") && s.contains("close"))
        {
            Toast.makeText(this, "Door close", Toast.LENGTH_LONG).show();
            doorOn();
            tts.speak("Locking the door, Sir", TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else if(s.contains("leaving") || (s.contains("off") && s.contains("everything")))
        {
            Toast.makeText(this, "Everything is off", Toast.LENGTH_LONG).show();
            leaving();
            tts.speak("Have a nice time out there, Sir. See you later", TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else if(s.contains("back") || (s.contains("on") && s.contains("everything")))
        {
            Toast.makeText(this, "Welcome back, Sir. How was your visit?", Toast.LENGTH_LONG).show();
            back();

            tts.speak("Welcome back, Sir. How was your visit?", TextToSpeech.QUEUE_FLUSH, null, null);
        }
        else if(s.contains("sleep"))
        {
            lampOff();
            mainOff();
            fanOn();
            doorOn();
        }
        else if(s.contains("study"))
        {
            lampOn();
            mainOff();
            doorOff();
        }

    }

    public void mainOn()
    {
        send2Bluetooth(10, 1);
        lightStat = true;
        mainLight.setImageResource(R.drawable.lighton1);
    }
    public void mainOff()
    {
        send2Bluetooth(10, 0);
        lightStat = false;
        mainLight.setImageResource(R.drawable.lightoff1);
    }
    public void lampOn()
    {
        send2Bluetooth(20, 1);
        lampStat = true;
        lamp.setImageResource(R.drawable.lampon1);
    }
    public void lampOff()
    {
        send2Bluetooth(20, 0);
        lampStat = false;
        lamp.setImageResource(R.drawable.lampoff);
    }
    public void fanOn()
    {
        send2Bluetooth(30, 1);
        fanStat = true;
        fan.setImageResource(R.drawable.fanon1);
    }
    public void fanOff()
    {
        send2Bluetooth(30, 0);
        fanStat = false;
        fan.setImageResource(R.drawable.fanoff1);
    }
    public void doorOn()
    {
        send2Bluetooth(40, 1);
        doorStat = true;
        door.setImageResource(R.drawable.doorlock1);
    }
    public void doorOff()
    {
        send2Bluetooth(40, 0);
        doorStat = false;
        door.setImageResource(R.drawable.doorunlock1);
    }
    public void leaving()
    {
        send2Bluetooth(50, 0);
        lampStat = false;
        lightStat = false;
        fanStat = false;
        doorStat = true;
        door.setImageResource(R.drawable.doorlock1);
        fan.setImageResource(R.drawable.fanoff1);
        lamp.setImageResource(R.drawable.lampoff);
        mainLight.setImageResource(R.drawable.lightoff1);

    }
    public void back()
    {
        send2Bluetooth(50, 1);
        lightStat = true;
        fanStat = true;
        doorStat = false;
        lampStat = false;
        door.setImageResource(R.drawable.doorunlock1);
        fan.setImageResource(R.drawable.fanon1);
        lamp.setImageResource(R.drawable.lampoff);
        mainLight.setImageResource(R.drawable.lighton1);

    }
}


