package com.example.administrator.bdsend;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private EditText number;
    private EditText content;
    private Button btnSend;
    private SmsManager sManager;
    private LocationManager lm;
    private Timer mTimer = new Timer("BeidouSampleTask");
    static final String ACTION_MSG_BD_INFO_RECEIVED =
            "android.intent.action.beidou.msg.bd.info.received";
    private TextView BDnumber;
    private TextView BDversion;
    private TextView service_number;
    private Button get;
    private Button set;
    private Button report;
    private BdInfoReceiver mBdInfoReceiver;
    private int msgId = 1;
    private int Rflag = 0;
    private Location mLocation;
    private TextView Receiver;
    private EditText serverNumber;
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BDnumber = (TextView)findViewById(R.id.BDnumber);
        BDversion = (TextView)findViewById(R.id.BDversion);
        service_number = (TextView)findViewById(R.id.service_number);
        number = (EditText)findViewById(R.id.edit1);
        content = (EditText)findViewById(R.id.edit2);
        serverNumber = (EditText)findViewById(R.id.serverNumber);
        get = (Button)findViewById(R.id.get);
        btnSend = (Button)findViewById(R.id.Button);
        set = (Button)findViewById(R.id.server);
        report =(Button)findViewById(R.id.report);
        mBdInfoReceiver = new BdInfoReceiver();
        IntentFilter moduleFilter = new IntentFilter(ACTION_MSG_BD_INFO_RECEIVED);
        registerReceiver(mBdInfoReceiver,moduleFilter);
        lm=(LocationManager)getSystemService(Context.LOCATION_SERVICE);

        //判断GPS是否正常启动
        if(!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(this, "请开启GPS导航...", Toast.LENGTH_SHORT).show();
            //返回开启GPS导航设置界面
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent,0);
            return;
        }
       get.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                Log.i("onclick", "onclick");
                requestBdInfo();
                if(Rflag==1){
                    get.setClickable(false);
                }

            }

        });
        btnSend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("sendonclick", "sendonclick");
                String bdnumber = number.getText().toString();
                if (bdnumber.length() <= 0) {
                    Toast.makeText(MainActivity.this, "请输入接收者号码",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                bdnumber = "U"+bdnumber;
                String bdtext = content.getText().toString();
                if (bdtext.length() <= 0) {
                    Toast.makeText(MainActivity.this, "请输入短消息内容",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                sendmessage(bdnumber,bdtext);
                btnSend.setClickable(false);
                int mServicefreq = 60;
                new CountDownTimer(mServicefreq*1000, 1000){

                    @Override
                    public void onFinish() {
                        btnSend.setClickable(true);
                        btnSend.setText("发送");
                    }

                    @Override
                    public void onTick(long millisUntilFinished) {
                        btnSend.setText("("+millisUntilFinished/1000+")");
                    }

                }.start();
            }
        });
        set.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(serverNumber.getText()!=null) {
                    String centernumber = serverNumber.getText().toString();
                    Log.i("server",centernumber);
                    setcenterNumber(centernumber);
                }
                else{
                    Toast.makeText(MainActivity.this, "请输入中心站卡号",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        report.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            report();
                report.setClickable(false);
                int mServicefreq = 60;
                new CountDownTimer(mServicefreq*1000, 1000){

                    @Override
                    public void onFinish() {
                        report.setClickable(true);
                        report.setText("上报");
                    }

                    @Override
                    public void onTick(long millisUntilFinished) {
                        report.setText("("+millisUntilFinished/1000+")");
                    }

                }.start();
            }
        });
    }
    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {


            if(intent.getAction().equals("android.intent.action.beidou.msg.received")){
                Log.d("MessageReceiver", "received a message");
                Bundle bundle = intent.getExtras();
                String number = bundle.getString("number");
                number = number.substring(1); // 去掉字母U
                byte[] data = bundle.getByteArray("msgcontent");
                String content = null;
                try {
                    content = new String(data, "GB2312");

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return;
                }
                Log.i("send","ok");
                Receiver.setText("北斗卡号："+number+"/r"+"内容"+content);
            }


        }

    }
    private void requestBdInfo() {

            mTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Log.i("sendExtra", "sendExtra");

                    lm.sendExtraCommand(LocationManager.GPS_PROVIDER,
                            "request_bd_info", null);
                }

            }, 50, 1000);
            Rflag = 1;
    }
    private void cacleRequestBdInfo() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        if (mBdInfoReceiver != null) {
            unregisterReceiver(mBdInfoReceiver);
            mBdInfoReceiver = null;
        }
    }
    private class BdInfoReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ACTION_MSG_BD_INFO_RECEIVED)){
                Log.i("BDinforeceiver", "BDinforeceiver");
                if (intent.getAction().equals(ACTION_MSG_BD_INFO_RECEIVED)) {
                    Bundle bundle = intent.getExtras();
                    //int service_frequency = bundle.getInt("service_frequency");
                    //int communication_level = bundle.getInt("communication_level");
                    String number = bundle.getString("number");
                    String version = bundle.getString("version");
                    String severnumber = bundle.getString("service_number");
                    cacleRequestBdInfo();
                    updateBdInfo(number,version,severnumber);
                }
            }
        }
    }
    private void updateBdInfo(String icNumber,String version,String serviceNumber) {
        Log.i("settext", "settext");
        BDnumber.setText("本机卡号：" + icNumber);
        BDversion.setText("北斗模块版本号：" + version);
        service_number.setText("中心站号："+serviceNumber);
    }
    private void sendmessage(String number,String content) {
        Log.i("message", "message");
        SmsManager messagemanager = SmsManager.getDefault();
        if (msgId  < 9999 && msgId > 0) {
            msgId++;
        } else {
            msgId = 1;
        }
        Toast.makeText(MainActivity.this, "发送成功",
                Toast.LENGTH_SHORT).show();
        Bundle bundle = new Bundle();
        bundle.putInt("msgid", msgId);
        bundle.putInt("msgtype", 1);
        bundle.putInt("msgsubtype", 0);
        bundle.putInt("msgpage", 1);

        if (mLocation!= null) {
            bundle.putChar("msgflag", 'T');
            double latitude = mLocation.getLatitude();
            double longitude = mLocation.getLongitude();
            if (latitude < 0) {
                bundle.putChar("latitude_f", 'S');
                latitude = -latitude;
            } else {
                bundle.putChar("latitude_f", 'N');
            }
            bundle.putDouble("latitude", latitude);
            if (longitude < 0) {
                bundle.putChar("longitude_f", 'W');
                longitude = -longitude;
            } else {
                bundle.putChar("longitude_f", 'E');
            }
            bundle.putDouble("longitude", longitude);
            bundle.putDouble("altitude", mLocation.getAltitude());
        } else {
            bundle.putChar("msgflag", 'F');
            bundle.putDouble("latitude", 0);
            bundle.putChar("latitude_f", 'N');
            bundle.putDouble("longitude", 0);
            bundle.putChar("longitude_f", 'E');
            bundle.putDouble("altitude", 0);
        }
        bundle.putString("number",number);
        byte[] MsgContent = null;
        try {
            MsgContent = content.getBytes("GB2312");
            if (MsgContent != null) {
                bundle.putInt("msglenth", MsgContent.length);
                bundle.putByteArray("msgcontent", MsgContent);
                Log.i("message","message.bdtext!=null");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "系统不支持该编码类型",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        //Intent intent = new Intent( "android.intent.action.beidou.msg.send");
        //intent.putExtras(bundle);
        //sendBroadcast(intent);
        if (lm != null
                && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            lm.sendExtraCommand(LocationManager.GPS_PROVIDER,
                    "beidou_send_sms", bundle);
            Log.i("message", "new message");
        }


    }

    private void setcenterNumber(String str){

        Bundle extras = new Bundle();
        extras.putString("set_service_number", str);
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Log.i("setcenter", str);
            lm.sendExtraCommand(LocationManager.GPS_PROVIDER,
                    "set_service_number", extras);
        }

    }
    private void report(){
         String q ="234";//544554
        String sos = "123";
        byte[] SosContent ;
        try {
            SosContent = sos.getBytes("GB2312");
            byte[]S0S;
            byte[] mASCII = new byte[]{(byte)0xA4,(byte)0x89,0x00,0x00,0x00};
            S0S = new byte[SosContent.length+mASCII.length];
            int slength = 0;
            slength = S0S.length;
            for(int i=0;i<slength;i++){
                if(i<mASCII.length){
                    S0S[i]=mASCII[i];
                }
                else{
                    S0S[i]=SosContent[i-mASCII.length];
                }
            }

            String number = "135618";
            Bundle bundle = new Bundle();
            bundle.putString("number", number);
            bundle.putInt("type", 1);
            bundle.putInt("len", S0S.length);
            bundle.putByteArray("content", S0S);
            if (lm != null
                    && lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.sendExtraCommand(LocationManager.GPS_PROVIDER,
                        "beidou_send_data", bundle);
                Log.i("report", "report");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

}
