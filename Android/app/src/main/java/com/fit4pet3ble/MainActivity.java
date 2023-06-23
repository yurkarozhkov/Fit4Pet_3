package com.fit4pet3ble;


import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
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

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {
    // Константы
    public static final int MAX_SPEED = 160;
    public static final int MAX_TOK = 100;
    public static final int TOK_INC = 5;
    public static final int SPEED_INC = 2;

    private int REQUEST_ENABLE_BT = 1;
    private static String MAC = "30:AE:A4:90:2E:72";
    private static String TAG = "myTAG";
    private static String SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    private static String CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";

    private BluetoothAdapter bluetoothAdapter;
    public BleDevice myBleDevice;

    MediaPlayer mp;
    ImageButton button1;
    ImageButton button2;
    ImageButton button3;
    ImageButton buttonStart;
    ImageButton buttonRevers;
    ImageButton buttonStop;
    ImageButton buttonTokMinus;
    ImageButton buttonTokPlus;
    ImageButton buttonSpeedMinus;
    ImageButton buttonSpeedPlus;
    TextView textLine1;
    TextView textLine2;
    TextView textLine3;
    TextView textView1;
    ImageView imageInfo;
    TextView textInfo;
    TextView textTime;
    TextView textViewOnline;

    // Переменные, значения которых сохраняются при переходе между активити
    public int rezhim = 0;
    public int rezhim1 = 0;
    public int dataToSend = 0;
    public int nRezhim = 0;
    public int speed10 = 0;
    public int tok = 0;
    public boolean showInfo = true;
    public long startTime;
    public long startTime1;
    public float probeg = 0;
    public boolean reversing = false;
    boolean onLine = false;
    public boolean sending = false;

    byte longButton = 0;
    public String line1 = "";
    public String line2 = "";
    public String line3 = "";
    public String line4 = "";
    public String line5 = "";
    public String line6 = "";
    public long time = 0, time1 = 0;
    public float probeg1 = 0;
    public float probeg2 = 0;

    private Timer timer1s;
    private Timer timerLongPress;
    private Timer timerRevers;
    private Timer timer;
    TimerTask timerTask;
    Intent batteryStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); //для альбомного режима
        setContentView(R.layout.activity_main);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(5000)
                .setOperateTimeout(1000);

        button1 = (ImageButton) findViewById(R.id.imageButton1);
        button1.setOnClickListener(this);
        button2 = (ImageButton) findViewById(R.id.imageButton2);
        button2.setOnClickListener(this);
        button3 = (ImageButton) findViewById(R.id.imageButton3);
        button3.setOnClickListener(this);
        buttonStart = (ImageButton) findViewById(R.id.imageButtonStart);
        buttonStart.setOnClickListener(this);
        buttonRevers = (ImageButton) findViewById(R.id.imageButtonRevers);
        buttonRevers.setOnClickListener(this);
        buttonStop = (ImageButton) findViewById(R.id.imageButtonStop);
        buttonStop.setOnClickListener(this);

        buttonTokMinus = (ImageButton) findViewById(R.id.imageButtonTokMinus);
        buttonTokMinus.setOnClickListener(this);
        buttonTokMinus.setOnTouchListener(this);
        buttonTokPlus = (ImageButton) findViewById(R.id.imageButtonTokPlus);
        buttonTokPlus.setOnClickListener(this);
        buttonTokPlus.setOnTouchListener(this);
        buttonSpeedMinus = (ImageButton) findViewById(R.id.imageButtonSpeedMinus);
        buttonSpeedMinus.setOnClickListener(this);
        buttonSpeedMinus.setOnTouchListener(this);
        buttonSpeedPlus = (ImageButton) findViewById(R.id.imageButtonSpeedPlus);
        buttonSpeedPlus.setOnClickListener(this);
        buttonSpeedPlus.setOnTouchListener(this);

        textInfo = (TextView) findViewById(R.id.textInfo);
        textInfo.setOnClickListener(this);
        textTime = (TextView) findViewById(R.id.textTime);
        textTime.setOnClickListener(this);


        textLine1 = (TextView) findViewById(R.id.textLine1);
        textLine2 = (TextView) findViewById(R.id.textLine2);
        textLine3 = (TextView) findViewById(R.id.textLine3);

        textView1 = (TextView) findViewById(R.id.textView1);
        textViewOnline = (TextView) findViewById(R.id.textViewOnline);
        imageInfo = (ImageView) findViewById((R.id.imageInfoView));

        Intent intent = getIntent();
        nRezhim = intent.getIntExtra("nRezhim", 0);
        rezhim1 = intent.getIntExtra("rezhim1", 0);
        startTime = intent.getLongExtra("startTime", 0);
        startTime1 = intent.getLongExtra("startTime1", 0);
        probeg = intent.getFloatExtra("probeg", 0);
        speed10 = intent.getIntExtra("speed10", 0);
        tok = intent.getIntExtra("tok", 0);

        rezhim = (int) (speed10 << 24) + (int) (tok << 16) + (short) rezhim1;


        sendData(rezhim);
        sendData(rezhim);

        if ((rezhim1 & (1 << 6)) != 0) {// Показываем скорость, если включен мотор
            showInfo = false;
            startTimer();
        }

        timer = new Timer();
        timerTask = new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (onLine) {
                            textViewOnline.setText("Связь - установлена!");
                        } else {
                            textViewOnline.setText("Нет связи");
                        }
                        //IntentFilter Служит неким фильтром данных, которые мы хотим получить.
                        //ACTION_BATTERY_CHANGED - отслеживает изменение батареи
                        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                        //Чтобы получить текущее состояние батареи в виде намерения, нужно вызвать registerReceiver, передав null в качестве приемника, как показано в коде ниже.
                        batteryStatus = registerReceiver(null, ifilter);

                        int status = batteryStatus.getIntExtra("level", -1);
                        textViewOnline.setText(textViewOnline.getText() + ". Заряд батареи: " + status + "%");
                        sendData(rezhim);
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, 10000, 1000);
        setMainScreen();
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


    }

    public void onServiseClick(View view) {
        if (!reversing) {

            Intent intent = new Intent(MainActivity.this, Activity2.class);
            intent.putExtra("nRezhim", nRezhim);
            intent.putExtra("rezhim1", rezhim1);
            intent.putExtra("startTime", startTime);
            intent.putExtra("startTime1", startTime1);
            intent.putExtra("probeg", probeg);
            intent.putExtra("speed10", speed10);
            intent.putExtra("tok", tok);
            intent.putExtra("myBleDevice", myBleDevice);
            timer.cancel();
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        openQuitDialog();
    }

    private void openQuitDialog() {
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(
                MainActivity.this);
        quitDialog.setTitle("Выход: Вы уверены?");

        quitDialog.setPositiveButton("Да!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                android.os.Process.killProcess(android.os.Process.myPid());
                //finish();
            }
        });

        quitDialog.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
            }
        });

        quitDialog.show();
    }

    @Override
    public void onClick(View v) {
        if (v == buttonStop) {
            nRezhim = 0;
            rezhim1 = 0;
            tok = 0;
            speed10 = 0;
            showInfo = true;
            reversing = false;
            longButton = 0;
            if (timerRevers != null) timerRevers.cancel();
            if (timer1s != null) timer1s.cancel();
            setMainScreen();
        } else if (v == button1) {
            if (nRezhim == 1) {
                nRezhim = 0;
                rezhim1 &= ~(1 << 0);// Устанавливаем бит режима 1 в значение 0
            } else {
                nRezhim = 1;
                tok = 0;
                showInfo = true;
                rezhim1 |= (1 << 0);//Устанавливаем бит режима 1 в значение 1
                rezhim1 &= ~(0b11110);// выключаем остальные биты режимов 2-5
            }
        } else if (v == button2) {
            if (nRezhim == 2) {
                nRezhim = 0;
                rezhim1 &= ~(1 << 1);// Устанавливаем бит режима 2 в значение 0
                textLine3.setText("");
            } else {
                nRezhim = 2;
                tok = 0;
                showInfo = true;
                rezhim1 |= (1 << 1);//Устанавливаем бит режима 2 в значение 1
                rezhim1 &= ~(0b11101);// выключаем остальные биты режимов 1 и 3-5
                textLine3.setText("СЛИВ ИЗ ДОРОЖКИ");
            }
        } else if (v == button3) {
            if (nRezhim == 3) {
                nRezhim = 0;
                rezhim1 &= ~(1 << 2);// Устанавливаем бит режима 3 в значение 0
                tok = 0;
                textLine1.setText("");
            } else if (nRezhim == 0) {
                nRezhim = 3;
                showInfo = true;
                rezhim1 |= (1 << 2);//Устанавливаем бит режима 3 в значение 1
                rezhim1 &= ~(0b11011);// выключаем остальные биты режимов 1,2 и 4-5
            }
        } else if (v == buttonStart) {
            if ((rezhim1 & (1 << 6)) != 0) {// Проверяем шестой бит на равенство 1
                rezhim1 &= ~(1 << 6);// Устанавливаем бит включения мотора в значение 0
                timer1s.cancel();
                showInfo = true;
                speed10 = 0;
            } else {
                rezhim1 |= (1 << 6);//Устанавливаем бит включения мотора в значение 1
                showInfo = false;
                speed10 = 0;
                probeg = 0;
                probeg1 = 0;
                // Засечка времени для расчета прошедшего времени
                startTime = SystemClock.elapsedRealtime();
                startTime1 = startTime;
                startTimer();
            }
        } else if (v == buttonRevers) {
            if (reversing) reversing = false;
            else {
                if (speed10 > 0) {
                    reversing = true;
                    showInfo = false;
                    TimerRevers();
                } else rezhim1 ^= (1 << 7); //Меняем бит реверса
            }
        } else if (v == buttonTokMinus) {
            tok -= TOK_INC;
            if (tok <= 0) tok = 0;
            showInfo = true;
        } else if (v == buttonTokPlus) {
            tok += TOK_INC;
            if (tok >= MAX_TOK) tok = MAX_TOK;
            showInfo = true;
        } else if (v == buttonSpeedPlus) {
            startTime1 = SystemClock.elapsedRealtime();
            probeg += probeg1;
            speed10 += SPEED_INC;
            if (speed10 >= MAX_SPEED) speed10 = MAX_SPEED;
            showInfo = false;
        } else if (v == buttonSpeedMinus) {
            startTime1 = SystemClock.elapsedRealtime();
            probeg += probeg1;
            speed10 -= SPEED_INC;
            if (speed10 <= 0) speed10 = 0;
            showInfo = false;
        } else if (v == textInfo) showInfo = true;
        else if (v == textTime) showInfo = false;

        rezhim = (int) (speed10 << 24) + (int) (tok << 16) + (short) rezhim1;

        setMainScreen();
        sendData(rezhim);
        sendData(rezhim);

    }

    public void setMainScreen() {
        switch (nRezhim) {
            case 0:
                button1.setImageResource(R.drawable.button_11);
                button2.setImageResource(R.drawable.button_21);
                button3.setImageResource(R.drawable.button_31);
                buttonTokMinus.setVisibility(View.GONE);
                buttonTokPlus.setVisibility(View.GONE);
                line1 = "";
                line2 = "";
                break;
            case 1:
                button1.setImageResource(R.drawable.button_12);
                button2.setImageResource(R.drawable.button_21);
                button3.setImageResource(R.drawable.button_31);
                buttonTokMinus.setVisibility(View.GONE);
                buttonTokPlus.setVisibility(View.GONE);
                line1 = "";
                line2 = "ЗАЛИВ В ДОРОЖКУ";
                break;
            case 2:
                button1.setImageResource(R.drawable.button_11);
                button2.setImageResource(R.drawable.button_22);
                button3.setImageResource(R.drawable.button_31);
                buttonTokMinus.setVisibility(View.GONE);
                buttonTokPlus.setVisibility(View.GONE);
                line1 = "";
                line2 = "СЛИВ ИЗ ДОРОЖКИ";
                break;
            case 3:
                button1.setImageResource(R.drawable.button_11);
                button2.setImageResource(R.drawable.button_21);
                button3.setImageResource(R.drawable.button_32);
                buttonTokMinus.setVisibility(View.VISIBLE);
                buttonTokPlus.setVisibility(View.VISIBLE);
                line1 = "ПРОТИВОТОК " + tok + " %";
                line2 = "";
                break;
            case 4:
                button1.setImageResource(R.drawable.button_11);
                button2.setImageResource(R.drawable.button_21);
                button3.setImageResource(R.drawable.button_31);
                buttonTokMinus.setVisibility(View.GONE);
                buttonTokPlus.setVisibility(View.GONE);
                line1 = "";
                line2 = "СЛИВ ИЗ ДОРОЖКИ В КАНАЛИЗАЦИЮ";
                break;
            case 5:
                button1.setImageResource(R.drawable.button_11);
                button2.setImageResource(R.drawable.button_21);
                button3.setImageResource(R.drawable.button_31);
                buttonTokMinus.setVisibility(View.GONE);
                buttonTokPlus.setVisibility(View.GONE);
                line1 = "";
                line2 = "СЛИВ ИЗ БАКА В КАНАЛИЗАЦИЮ";
                break;

        }
        // Кнопка СТАРТ
        if ((rezhim1 & (1 << 6)) != 0) {// Проверяем шестой бит на равенство 1
            buttonStart.setImageResource(R.drawable.button_start2);
            buttonSpeedMinus.setVisibility(View.VISIBLE);
            buttonSpeedPlus.setVisibility(View.VISIBLE);
        } else {
            buttonStart.setImageResource(R.drawable.button_start1);
            buttonSpeedMinus.setVisibility(View.GONE);
            buttonSpeedPlus.setVisibility(View.GONE);
        }
        // Кнопка РЕВЕРС
        if ((rezhim1 & (1 << 7)) != 0) // Проверяем седьмой бит на равенство 1
            buttonRevers.setImageResource(R.drawable.button_revers2);
        else buttonRevers.setImageResource(R.drawable.button_revers1);

        if ((rezhim1 & (1 << 5)) != 0) // Проверяем пятый бит на равенство 1
            line3 = "НАПОЛНЕНИЕ БАКА";
        else line3 = "";

        showText();
    }

    public void showText() {
        if (showInfo) {
            imageInfo.setImageResource(R.drawable.info_info);
            textLine1.setText(line1);
            textLine2.setText(line2);
            textLine3.setText(line3);

        } else {
            imageInfo.setImageResource(R.drawable.info_time);
            textLine1.setText(line4);
            if ((rezhim1 & (1 << 6)) != 0) // Проверяем шестой бит на равенство 1

                textLine2.setText("СКОРОСТЬ: " + (float) speed10 / 10 + " км/ч");
            else
                textLine2.setText("");
            textLine3.setText("" + line6);
        }
    }

    public static byte[] intToBytes(int value) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) value;
        bytes[1] = (byte) (value >> 8);
        bytes[2] = (byte) (value >> 16);
        bytes[3] = (byte) (value >> 24);
        return bytes;
    }

    public void startTimer() {
        timer1s = new Timer();
        TimerTask timerTask = new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        time1 = (SystemClock.elapsedRealtime() - startTime1);
                        time = (SystemClock.elapsedRealtime() - startTime) / 1000;
                        line4 = "ВРЕМЯ: " + secToTime(time);
                        probeg1 = (float) ((float) speed10 * (float) time1 / (float) 36000000);
                        probeg2 = (float) probeg + (float) probeg1;
                        line6 = "РАССТОЯНИЕ: " + String.format("%.2f", (float) probeg2) + " км";
                        showText();
                    }
                });
            }
        };
        timer1s.scheduleAtFixedRate(timerTask, 0, 1000);
    }

    public void startTimerLongPress() {
        timerLongPress = new Timer();
        TimerTask timerTask1 = new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (longButton == 4) {
                            speed10 += SPEED_INC;
                            showInfo = false;
                            if (speed10 >= MAX_SPEED) speed10 = MAX_SPEED;
                            textLine2.setText("СКОРОСТЬ: " + (float) speed10 / 10 + " км/ч");
                            time1 = (SystemClock.elapsedRealtime() - startTime1);
                            startTime1 = SystemClock.elapsedRealtime();
                            probeg1 = (float) ((float) speed10 * (float) time1 / (float) 36000000);
                            probeg += probeg1;
                            probeg2 = (float) probeg + (float) probeg1;
                            line6 = "РАССТОЯНИЕ: " + String.format("%.2f", (float) probeg2) + " км";
                        } else if (longButton == 3) {
                            speed10 -= SPEED_INC;
                            showInfo = false;
                            if (speed10 <= 0) speed10 = 0;
                            textLine2.setText("СКОРОСТЬ: " + (float) speed10 / 10 + " км/ч");
                            time1 = (SystemClock.elapsedRealtime() - startTime1);
                            startTime1 = SystemClock.elapsedRealtime();
                            probeg1 = (float) ((float) speed10 * (float) time1 / (float) 36000000);
                            probeg += probeg1;
                            probeg2 = (float) probeg + (float) probeg1;
                            line6 = "РАССТОЯНИЕ: " + String.format("%.2f", (float) probeg2) + " км";
                        } else if (longButton == 1) {
                            tok -= TOK_INC;
                            if (tok <= 0) tok = 0;
                            showInfo = true;
                            line1 = "ПРОТИВОТОК " + tok + " %";
                            line2 = "";
                            showText();
                        } else if (longButton == 2) {
                            tok += TOK_INC;
                            if (tok >= MAX_TOK) tok = MAX_TOK;
                            showInfo = true;
                            line1 = "ПРОТИВОТОК " + tok + " %";
                            line2 = "";
                            showText();
                        }
                        rezhim = (int) (speed10 << 24) + (int) (tok << 16) + (int) rezhim1;
                        sendData(rezhim);
                        sendData(rezhim);
                    }
                });
            }
        };
        timerLongPress.scheduleAtFixedRate(timerTask1, 500, 100);
    }

    public static String secToTime(long sec) {
        long s = sec % 60;
        long m = (sec / 60) % 60;
        long h = (sec / (60 * 60)) % 24;
        return String.format("%d:%02d:%02d", h, m, s);
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: // нажатие
                if (v == buttonSpeedPlus) {
                    longButton = 4;
                    startTimerLongPress();
                } else if (v == buttonSpeedMinus) {
                    longButton = 3;
                    startTimerLongPress();
                } else if (v == buttonTokMinus) {
                    longButton = 1;
                    startTimerLongPress();
                } else if (v == buttonTokPlus) {
                    longButton = 2;
                    startTimerLongPress();
                }
                break;
            case MotionEvent.ACTION_UP: // отпускание
                longButton = 0;
                timerLongPress.cancel();
                rezhim = (int) (speed10 << 24) + (int) (tok << 16) + (int) rezhim1;
                sendData(rezhim);
                break;
        }
        return false;
    }

    public void TimerRevers() {
        timerRevers = new Timer();
        TimerTask timerTask2 = new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (reversing) {
                            if (speed10 > 0) {
                                speed10--;

                                if (speed10 <= 0) speed10 = 0;
                                time1 = (SystemClock.elapsedRealtime() - startTime1);
                                startTime1 = SystemClock.elapsedRealtime();
                                probeg1 = (float) ((float) speed10 * (float) time1 / (float) 36000000);
                                probeg += probeg1;
                                probeg2 = (float) probeg + (float) probeg1;
                                if (!showInfo) {
                                    textLine2.setText("СКОРОСТЬ: " + (float) speed10 / 10 + " км/ч");
                                    line6 = "РАССТОЯНИЕ: " + String.format("%.2f", (float) probeg2) + " км";
                                }
                                if (speed10 == 0) {
                                    timerRevers.cancel();
                                    reversing = false;
                                    rezhim1 ^= (1 << 7); //Меняем бит реверса
                                    if ((rezhim1 & (1 << 7)) != 0) // Проверяем седьмой бит на равенство 1
                                        buttonRevers.setImageResource(R.drawable.button_revers2);
                                    else buttonRevers.setImageResource(R.drawable.button_revers1);
                                }
                            } else {
                                timerRevers.cancel();
                                //new MyAsyncTask().execute();
                                reversing = false;
                                rezhim1 ^= (1 << 7); //Меняем бит реверса
                                if ((rezhim1 & (1 << 7)) != 0) // Проверяем седьмой бит на равенство 1
                                    buttonRevers.setImageResource(R.drawable.button_revers2);
                                else buttonRevers.setImageResource(R.drawable.button_revers1);

                            }
                        } else timerRevers.cancel();
                        rezhim = (int) (speed10 << 24) + (int) (tok << 16) + (int) rezhim1;
                        sendData(rezhim);
                        sendData(rezhim);
                    }
                });
            }
        };
        timerRevers.scheduleAtFixedRate(timerTask2, 100, 100);
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

    class MyAsyncTask extends AsyncTask<String, String, String> {
        byte[] data;

        @Override
        protected void onPreExecute() {
            data = intToByteArray(rezhim);
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            if (BleManager.getInstance().isConnected(myBleDevice)) {
                BleManager.getInstance().write(
                        myBleDevice,
                        SERVICE_UUID,
                        CHARACTERISTIC_UUID,
                        data,
                        new BleWriteCallback() {
                            @Override
                            public void onWriteSuccess(int i, int i1, byte[] bytes) {
                                Log.i(TAG, "Writed " + data.toString());
                            }

                            @Override
                            public void onWriteFailure(BleException e) {
                                Log.i(TAG, "Error to Write");
                            }
                        });
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.i(TAG, "Writed Post Execute");

        }

    }

    public static byte[] intToByteArray(int value) {
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = (byte) (value & 0xFF);
            value >>>= 8;
        }
        return result;
    }

    public static int byteArrayToInt(byte[] bytes) {
        int result = 0;
        int l = bytes.length - 1;
        for (int i = 0; i < bytes.length; i++)
            if (i == l) result += bytes[i] << i * 8;
            else result += (bytes[i] & 0xFF) << i * 8;
        return result;
    }
}
