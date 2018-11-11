package com.example.enevl.guh2018gyroscop;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private int previousAngleX, previousAngleY, previousAngleZ;
    private int futureStandardX, standardX;
    private Button calobrateButton;
    private TextView xtextView, ytextView, ztextView;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice device;
    BluetoothSocket socket;
    OutputStream outputStream;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private int move, angle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);

        xtextView = (TextView) findViewById(R.id.xtextview);
        ytextView = (TextView) findViewById(R.id.ytextview);
        ztextView = (TextView) findViewById(R.id.ztextview);

        calobrateButton = (Button) findViewById(R.id.calibrate);

        calobrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                standardX = futureStandardX;
            }
        });

        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {

            Toast.makeText(getApplicationContext(),"Device doesnt Support Bluetooth",Toast.LENGTH_SHORT).show();

        }
        if(!bluetoothAdapter.isEnabled())

        {

            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            startActivityForResult(enableAdapter, 0);

        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        if(bondedDevices.isEmpty()) {

            Toast.makeText(getApplicationContext(),"Please Pair the Device first",Toast.LENGTH_SHORT).show();

        } else {

            for (BluetoothDevice iterator : bondedDevices) {
                {

                    device=iterator; //device is an object of type BluetoothDevice
                    break;

                } } }
        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID); socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            outputStream=socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[16];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            // Remap coordinate system
            float[] remappedRotationMatrix = new float[16];
            SensorManager.remapCoordinateSystem(rotationMatrix,
                    SensorManager.AXIS_X,
                    SensorManager.AXIS_Y,
                    remappedRotationMatrix);


            // Convert to orientations
            float[] orientations = new float[3];
            SensorManager.getOrientation(remappedRotationMatrix, orientations);

            for (int i = 0; i < 3; i++) {
                orientations[i] = (int) (Math.toDegrees(orientations[i]));
            }
            if (Math.abs(previousAngleX - orientations[0]) > 20) {
                Log.d("X", Float.toString(orientations[0]));
                previousAngleX = (int) orientations[0];
            }
            if (Math.abs(previousAngleY - orientations[1]) > 20) {
                Log.d("Y", Float.toString(orientations[1]));
                previousAngleY = (int) orientations[1];
            }
            if(Math.abs(previousAngleZ - orientations[2]) > 20)
            {
                Log.d("Z",Float.toString(orientations[2]));
                previousAngleZ = (int) orientations[2];
            }

            futureStandardX = (int) orientations[0];

            if(orientations[1] >= 10 && orientations[1] <= 70)
                move = 2;
            else if(orientations[1] <= -10 && orientations[1] >= -70)
                move = 1;
            else move = 0;

            if(orientations[0] - standardX >= 10)
                angle = (int) orientations[0] - standardX;
            else if(orientations[0] - standardX <= -10)
                angle = 360 - (int)(Math.abs(orientations[0] - standardX));
            else angle = 0;


            xtextView.setText("MOVE: " + move);
            ytextView.setText("Angle: " + angle);
            //ztextView.setText("Z: " + orientations[2]);
            try {
                outputStream.write(angle);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.write(move);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
