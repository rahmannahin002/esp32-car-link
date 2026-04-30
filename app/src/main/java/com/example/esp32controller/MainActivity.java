package com.example.esp32controller;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.bluetooth.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter adapter;
    BluetoothSocket socket;
    OutputStream outputStream;

    Spinner deviceList;
    Button connectBtn;
    TextView statusText, speedText;

    int steering = 0, RT = 0, LT = 0;

    boolean brake=false, turbo=false, stop=false, drift=false, reverse=false;

    final UUID UUID_SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceList = findViewById(R.id.deviceList);
        connectBtn = findViewById(R.id.connectBtn);
        statusText = findViewById(R.id.statusText);
        speedText = findViewById(R.id.speedText);

        adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {
            statusText.setText("Bluetooth not supported");
            return;
        }

        askPermissions();   // ask first, DON'T load devices yet

        connectBtn.setOnClickListener(v -> connectSelectedDevice());
    }

    private void loadDevices() {
        try {
            Set<BluetoothDevice> devices = adapter.getBondedDevices();
    
            if (devices == null || devices.size() == 0) {
                statusText.setText("No paired devices");
                return;
            }
    
            ArrayAdapter<String> list = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
    
            for (BluetoothDevice d : devices) {
                list.add(d.getName() + "\n" + d.getAddress());
            }
    
            deviceList.setAdapter(list);
    
        } catch (Exception e) {
            statusText.setText("Device load error");
        }
    }

    private void askPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
    
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    }, 1);
        }
    }
    
    private void connectSelectedDevice() {
        try {
            String info = deviceList.getSelectedItem().toString();
            String address = info.substring(info.length() - 17);

            BluetoothDevice device = adapter.getRemoteDevice(address);

            socket = device.createRfcommSocketToServiceRecord(UUID_SPP);
            socket.connect();

            outputStream = socket.getOutputStream();

            statusText.setText("Connected");

        } catch (Exception e) {
            statusText.setText("Connection Failed");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadDevices();   // now safe
            } else {
                statusText.setText("Permission denied");
            }
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {

        if (event == null) return false;

        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) ==
                InputDevice.SOURCE_JOYSTICK) {

            float lx = event.getAxisValue(MotionEvent.AXIS_X);
            float rt = event.getAxisValue(MotionEvent.AXIS_RTRIGGER);
            float lt = event.getAxisValue(MotionEvent.AXIS_LTRIGGER);

            steering = (int)(lx * 100);
            RT = (int)(rt * 255);
            LT = (int)(lt * 255);

            updateSpeed();
            sendData();
            return true;
        }

        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A: turbo = true; break;
            case KeyEvent.KEYCODE_BUTTON_B: stop = true; break;
            case KeyEvent.KEYCODE_BUTTON_X: drift = true; break;
            case KeyEvent.KEYCODE_BUTTON_Y: reverse = true; break;
            case KeyEvent.KEYCODE_BUTTON_R1: brake = true; break;
        }

        sendData();
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A: turbo = false; break;
            case KeyEvent.KEYCODE_BUTTON_B: stop = false; break;
            case KeyEvent.KEYCODE_BUTTON_X: drift = false; break;
            case KeyEvent.KEYCODE_BUTTON_Y: reverse = false; break;
            case KeyEvent.KEYCODE_BUTTON_R1: brake = false; break;
        }

        sendData();
        return true;
    }

    private void updateSpeed() {
        float speed = ((RT - LT) / 255f) * 2.5f;
        speedText.setText("Speed: " + String.format("%.2f", speed) + " m/s");
    }

    private void sendData() {
        try {
            String msg = "X:" + steering +
                    ",RT:" + RT +
                    ",LT:" + LT +
                    ",B1:" + (brake?1:0) +
                    ",T:" + (turbo?1:0) +
                    ",S:" + (stop?1:0) +
                    ",D:" + (drift?1:0) +
                    ",M:" + (reverse?1:0) + "\n";

            if (outputStream != null)
                outputStream.write(msg.getBytes());

        } catch (Exception e) {
            statusText.setText("Send Error");
        }
    }
}