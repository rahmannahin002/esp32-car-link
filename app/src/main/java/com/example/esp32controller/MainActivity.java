package com.example.esp32controller;

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

        loadDevices();

        connectBtn.setOnClickListener(v -> connectSelectedDevice());
    }

    private void loadDevices() {
        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        ArrayAdapter<String> list = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);

        for (BluetoothDevice d : devices) {
            list.add(d.getName() + "\n" + d.getAddress());
        }

        deviceList.setAdapter(list);
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
    public boolean onGenericMotionEvent(MotionEvent event) {

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
        return super.onGenericMotionEvent(event);
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