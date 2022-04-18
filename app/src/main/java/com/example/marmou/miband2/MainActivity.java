package com.example.marmou.miband2;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.UUID;



public class MainActivity extends AppCompatActivity implements BLEMiBand2Helper.BLEAction{

    public static final String LOG_TAG = "Batman";

    public EditText textCall;
    public EditText textSms;
    public ArrayList<String> parts;
    public static String MAC;

    public TextView txtTime;
    public RadioGroup radGrp;

    PowerManager pm;
    PowerManager.WakeLock wl;

    static int POS=0;

    public int timer = 9;

    Handler handler = new Handler(Looper.getMainLooper());
    Handler handlerTemporizador = new Handler();
    BLEMiBand2Helper helper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_timer);

        textCall = (EditText) findViewById(R.id.textoLlamada);
        textSms = (EditText) findViewById(R.id.mensaje);

        txtTime = (TextView) findViewById(R.id.txtTime);
        radGrp = (RadioGroup) findViewById(R.id.grp_radio);

        EditText mac=(EditText) findViewById(R.id.txMac);
        MAC=mac.getText().toString();

        helper = new BLEMiBand2Helper(MainActivity.this, handler);
        helper.addListener(this);

        //// whitelist battery optimization, avoids losing connection on lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

        // Setup Bluetooth:
        helper.connect();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        getTouchNotifications();

        // Power manager partial wake lock
        pm =(PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AppName:tag");

        if(!wl.isHeld())
            wl.acquire();
    }

    protected void permissions(){

    }

    @Override
    protected void onDestroy() {
        if (helper != null)
            helper.DisconnectGatt();

        wl.release();

        super.onDestroy();
    }


    /**
     * //button for Connect to miBand
     * @param view
     */
    public void btnRun(View view) {
        EditText mac=(EditText) findViewById(R.id.txMac);
        MAC=mac.getText().toString();
        helper.connect();

    }
    /**
     * button for Disconnect to miBand
     */
     public void btnDescon(View view){
        helper.DisconnectGatt();
    }


    /**
     * Get notifications of the button
     */
    public void getTouchNotifications() {
        helper.getNotifications(
                Consts.UUID_SERVICE_MIBAND_SERVICE,
                Consts.UUID_BUTTON_TOUCH);

    }
    /**
     * button to collect notifications of the button
     */
    public void btnTest(View view) throws InterruptedException {
        getTouchNotifications();
    }

    /**
     * function btnEnviar, Send text as sms
     * @param view
     */
    public void btnEnviar(View view) throws InterruptedException {

        /*
        ANOTACION: LLAMAR AQUI A SENDCALL("MANSAJE") PARA ASEGURARNOS QUE EL MENSAJE VA A SER LEIDO
        Y AL TOCAR EL BOTON QUE SE MANDE EL SMS
         */

        sendText();
    }

    /**
     * Send text as sms, separating the message in 15 characters
     * @throws InterruptedException
     */
    public void sendText() throws InterruptedException {

        String value= textSms.getText().toString();
        helper.alerta();
        java.lang.Thread.sleep(2000);

        aadSplit(value);

        POS = 0;

        helper.sendSms(parts.get(POS));
        POS++;

    }

    /**
     * Send text as call
     * @param view
     */
    public void call(View view){
        Log.v(LOG_TAG,"SACI");
        String value= textCall.getText().toString();
        if(value.length()>18){
            String cut=value.substring(0,18);
            Log.v(LOG_TAG,cut);
            helper.sendCall(cut);
            Toast.makeText(MainActivity.this, "No se puede enviar el texto entero en forma de llamada,\n" +
                    "pruebe a hacerlo en forma de sms. Se ha enviado: "+cut, Toast.LENGTH_LONG).show();
        }
        else{
            Log.v(LOG_TAG,value);
            helper.sendCall(value);
        }
    }


    /* ===========  EVENTS (background thread) =============== */

    @Override
    public void onDisconnect() {

    }

    @Override
    public void onConnect() {
        Log.v(LOG_TAG,"CONNECTED");
    }

    @Override
    public void onRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.v(LOG_TAG,"DISCONNECTED");
    }

    @Override
    public void onWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

    }

    /**
     * Notification from miBand, in this case from the button
     * @param gatt
     * @param characteristic
     */
    @Override
    public void onNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        UUID alertUUID = characteristic.getUuid();
        final String texto = getString(R.string.txtTime);
        final int remainingSeconds = getSelectedSeconds();
        timer = remainingSeconds;
        //Log.v("Test", String.valueOf(timer));

        PowerManager pm =(PowerManager) getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AppName:tag");
        if(!wl.isHeld())
            wl.acquire();

        if (alertUUID.equals(Consts.UUID_BUTTON_TOUCH)) {
            //if(!seEstaEjecutandoTemporizador){
            //    seEstaEjecutandoTemporizador=true;
                try {
                    sendText();

                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                txtTime.setText(texto + " " +String.valueOf(timer));
                                /* do what you need to do */
                                if (timer > 0) {
                                    // There is still time remaining
                                    Log.v("Test","Tiempo restante: " + timer);
                                    timer--;
                                    handlerTemporizador.postDelayed(this, 1000);
                                } else {
                                    // No time remaining, we must take it out of the stack of execution
                                    timer = remainingSeconds;
                                    sendText();
                                    //seEstaEjecutandoTemporizador=false;
                                    handlerTemporizador.removeCallbacks(this);
                                }
                            }catch (Exception ex){
                                ex.printStackTrace();
                            }
                        }
                    };
                    handlerTemporizador.postDelayed(runnable, 100);

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        //}

    }

    /**
     * Function of the Button from miBand2
     *
     */
    public void functionButton() {
        if(POS==0){
            //Toast.makeText(MainActivity.this, "Ok, Mensagem toda recebida", Toast.LENGTH_LONG).show();
        }
        enviarPartSms();
    }

    /**
     *
     *Method to receive the following parts of the sms and say: "okay received"
     *send more than one sms
     */
    private void enviarPartSms(){
        Toast.makeText(MainActivity.this, String.format("Ok, Recebida a %d parte",POS), Toast.LENGTH_LONG).show();
        if( parts != null && parts.size() > POS && POS != 0) {
            helper.sendSms(parts.get(POS));
            POS++;
        }
        else
            POS = 0;
    }

    /**
     * It is added "_" every 15 characters to be able to use split to separate the sms and it is added -> when there are more parts of the sms to read
     *  NOTE: THE XIAOMI MIBAND2 ONLY ALLOWS 18 CHARACTERS FOR EVERY MESSAGE
     * @param messaje sms
     */
    private void aadSplit(String messaje){

        String textfin="";
        ArrayList<String> letters=  new ArrayList<>();

        for (int i=0;i<=messaje.length();i++){
            try {
                letters.add(String.valueOf(messaje.charAt(i)));
            }catch (Exception e){
                System.out.println("Salta exception pero lo almacena bien");
            }
        }
//limited for 45 characteres
        for (int i=0;i<letters.size();i++) {
            if(i==15){
                letters.add(i,"->");
                letters.add(i+1,"_");
            }else if(i==30){
                letters.add(i,"->");
                letters.add(i+1,"_");
            }else if(i==45){
                letters.add(i,"->");
                letters.add(i+1,"_");
            }else if(i==60){
                letters.add(i,"->");
                letters.add(i+1,"_");
            }else if(i==75){
                letters.add(i,"->");
                letters.add(i+1,"_");
            }
        }

        for (String s : letters) {
            textfin+=s;
        }

        fragmentarSms(textfin);

    }

    private int getSelectedSeconds()
    {
        int result = 0;
        int btnSelected = radGrp.getCheckedRadioButtonId();

        switch(btnSelected){
            case R.id.seconds30:
                result = 30;
                break;
            case R.id.seconds45:
                result = 45;
                break;
            case R.id.seconds60:
                result = 60;
                break;
            case R.id.seconds120:
                result = 120;
                break;
            case R.id.seconds180:
                result = 180;
                break;
            default:
                result=0;
                break;
        }

        return result;
    }

    /**
     * fragment the sms in parts to send them separately
     * @param textfin final text with the splits
     */
    private void fragmentarSms(String textfin){
        String[] splitted = textfin.split("_");
        try{
            this.parts = new ArrayList<String>();
            for(int i =0 ; i < splitted.length; i++){
                this.parts.add(splitted[i]);
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }
}



/*
Credit and thanks:
https://github.com/lwis/miband-notifier/
http://allmydroids.blogspot.co.il/2014/12/xiaomi-mi-band-ble-protocol-reverse.html
https://github.com/Freeyourgadget/Gadgetbridge
http://stackoverflow.com/questions/20043388/working-with-ble-android-4-3-how-to-write-characteristics
https://github.com/yonixw/mi-band-2
https://github.com/ZenGod16/unreademailsformiband
*/