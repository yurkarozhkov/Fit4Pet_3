package com.fit4pet3ble;

import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;


public class Activity2 extends AppCompatActivity implements View.OnClickListener {
    MediaPlayer mp;
    ImageButton button4;
    ImageButton button5;
    ImageButton button6;
    ImageButton buttonStop;
    ImageView imageViewAkb;

    TextView textView1;
    TextView textViewOnline2;

    boolean onLine2 = false;

    public int rezhim = 0;
    public int rezhim1 = 0;
    public int nRezhim = 0;
    public int speed10 = 0;
    public int tok = 0;
    public long startTime, startTime1;
    public float probeg = 0;

    private Timer timer;
    TimerTask timerTask;
    Intent batteryStatus;
    public BleDevice myBleDevice;

    private static String MAC = "30:AE:A4:90:2E:72";
    private static String TAG = "myTAG";
    private static String SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    private static String CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //для альбомного режима
        setContentView(R.layout.activity_2);
        Intent intent = getIntent();
        nRezhim = intent.getIntExtra("nRezhim", 0);
        rezhim1 = intent.getIntExtra("rezhim1", 0);
        startTime = intent.getLongExtra("startTime", 0);
        startTime1 = intent.getLongExtra("startTime1", 0);
        probeg = intent.getFloatExtra("probeg", 0);
        speed10 = intent.getIntExtra("speed10", 0);
        tok = intent.getIntExtra("tok", 0);
        rezhim = (int) (speed10 << 24) + (int) (tok << 16) + (short) rezhim1;

        button4 = (ImageButton) findViewById(R.id.imageButton4);
        button4.setOnClickListener(this);
        button5 = (ImageButton) findViewById(R.id.imageButton5);
        button5.setOnClickListener(this);
        button6 = (ImageButton) findViewById(R.id.imageButton6);
        button6.setOnClickListener(this);
        buttonStop = (ImageButton) findViewById(R.id.imageButtonStop);
        buttonStop.setOnClickListener(this);

        textView1 = (TextView) findViewById(R.id.textView1);
        textViewOnline2 = (TextView) findViewById(R.id.textViewOnline2);


        timer = new Timer();
        timerTask = new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (onLine2) textViewOnline2.setText("Связь - установлена!");
                        else textViewOnline2.setText("Нет связи");
                        //IntentFilter Служит неким фильтром данных, которые мы хотим получить.
                        //ACTION_BATTERY_CHANGED - отслеживает изменение батареи
                        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                        //Чтобы получить текущее состояние батареи в виде намерения, нужно вызвать registerReceiver, передав null в качестве приемника, как показано в коде ниже.
                        batteryStatus = registerReceiver(null, ifilter);
                        int status = batteryStatus.getIntExtra("level", -1);
                        textViewOnline2.setText(textViewOnline2.getText() + ". Заряд батареи: " + status + "%");
                        sendData(rezhim);
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, 1000, 1000);

        BleManager.getInstance().connect(MAC, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                Log.i(TAG, "Connecting...");
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {

                Log.i(TAG, "Connecting Fail...");
                textView1.setText("Connecting...");
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                myBleDevice = bleDevice;
                Log.i(TAG, "Connected...");
                textView1.setText("Connected...");

                BleManager.getInstance().notify(
                        bleDevice,
                        SERVICE_UUID,
                        CHARACTERISTIC_UUID,
                        new BleNotifyCallback() {
                            @Override
                            public void onNotifySuccess() {
                                Log.i(TAG, "Notify success...");
                            }

                            @Override
                            public void onNotifyFailure(BleException exception) {
                                Log.i(TAG, "Notify Failure...");
                            }

                            @Override
                            public void onCharacteristicChanged(byte[] data) {
                                Log.i(TAG, "Notified..." + data[0]);
                                textView1.setText("Notified..." + Integer.toBinaryString(data[0]));
                            }
                        });

            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {

                Log.i(TAG, "DisConnected...");
                textView1.setText("Disconnected...");
            }
        });

        setServiceScreen();
    }

    public void onClickBack(View view) {
       //воспроизвести звук
        /*
        new Thread() {
            public void run() {
                mp = MediaPlayer.create(Activity2.this, R.raw.knopka);
                mp.start();
            }
        }.start();
        */
        timer.cancel();
        Intent intent = new Intent(Activity2.this, MainActivity.class);
        intent.putExtra("nRezhim", nRezhim);
        intent.putExtra("rezhim1", rezhim1);
        intent.putExtra("startTime", startTime);
        intent.putExtra("startTime1", startTime1);
        intent.putExtra("probeg", probeg);
        intent.putExtra("speed10", speed10);
        intent.putExtra("tok", tok);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
    }

    public void setServiceScreen() {
        switch (nRezhim) {
            case 0:
            case 1:
            case 2:
            case 3:
                button4.setImageResource(R.drawable.button_41);
                button5.setImageResource(R.drawable.button_51);
                break;
            case 4:
                button4.setImageResource(R.drawable.button_42);
                button5.setImageResource(R.drawable.button_51);
                break;
            case 5:
                button4.setImageResource(R.drawable.button_41);
                button5.setImageResource(R.drawable.button_52);
                break;
            case 6:
                button4.setImageResource(R.drawable.button_41);
                button5.setImageResource(R.drawable.button_51);
                break;
        }
        if ((rezhim1 & (1 << 5)) != 0) // Проверяем пятый бит = 1
            button6.setImageResource(R.drawable.button_62);
        else button6.setImageResource(R.drawable.button_61);

    }

    @Override
    public void onClick(View v) {
        if (v == buttonStop) {
            nRezhim = 0;
            rezhim1 = 0;
            tok = 0;
            speed10 = 0;
            setServiceScreen();
        } else if (v == button4) {
            if (nRezhim == 4) {
                nRezhim = 0;
                rezhim1 &= ~(1 << 3);// Устанавливаем бит режима 4 в значение 0
            } else {
                nRezhim = 4;
                rezhim1 &= ~(0b11111111 << 24);// Устанавливаем значение tok = 0 (4й байт rezhim)
                rezhim1 |= (1 << 3);//Устанавливаем бит режима 4 в значение 1
                rezhim1 &= ~(0b10111);// Устанавливаем бит режима 4 в значение 0
            }
            setServiceScreen();
        } else if (v == button5) {
            if (nRezhim == 5) {
                nRezhim = 0;
                rezhim1 &= ~(1 << 4);// Устанавливаем бит режима 5 в значение 0
            } else {
                nRezhim = 5;
                rezhim1 &= ~(0b11111111 << 24);// Устанавливаем значение tok = 0 (4й байт rezhim)
                rezhim1 |= (1 << 4);//Устанавливаем бит режима 5 в значение 1
                rezhim1 &= ~(0b01111);// выключаем остальные биты режимов 1 и 3-5
            }
            setServiceScreen();
        } else if (v == button6) {
            if ((rezhim1 & (1 << 5)) != 0) {
                rezhim1 &= ~(1 << 5);// Устанавливаем бит режима 6 в значение 0
            } else {
                rezhim1 |= (1 << 5);//Устанавливаем бит режима 6 в значение 1
            }


        }
        rezhim = (int) (speed10 << 24) + (int) (tok << 16) + (short) rezhim1;
        setServiceScreen();
        sendData(rezhim);
        sendData(rezhim);

    }

    public static byte[] intToByteArray(int value) {
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = (byte) (value & 0xFF);
            value >>>= 8;
        }
        return result;
    }
    public void sendData(int intData) {
        byte[] data = intToByteArray(intData);

        if (BleManager.getInstance().isConnected(myBleDevice)) {
            BleManager.getInstance().write(
                    myBleDevice,
                    SERVICE_UUID,
                    CHARACTERISTIC_UUID,
                    data,
                    true,
                    true,
                    50,
                    new BleWriteCallback() {
                        @Override
                        public void onWriteSuccess(int i, int i1, byte[] bytes) {
                            Log.i(TAG, "Writed " + bytes.toString());

                        }

                        @Override
                        public void onWriteFailure(BleException e) {
                            Log.i(TAG, "Error to Write");
                        }
                    });
        }
    }


}
