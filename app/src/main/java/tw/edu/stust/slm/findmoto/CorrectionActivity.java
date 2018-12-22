package tw.edu.stust.slm.findmoto;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.icu.math.BigDecimal;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

public class CorrectionActivity extends AppCompatActivity {

    final int MSG_UPDATE_RSSI = 1000;
    final int REQUEST_ENABLE_BT = 2000;
    final int DIST_ARRAY_MAX_SIZE = 4;
    Toolbar toolbar;
    TextView tV_deviceName,tV_rssi;
    TextView[] tV_mark = new TextView[4];
    ProgressBar progressBar;
    Button btn_cancel,btn_next;
    int markStep = -1;
    double sumRssi = 0;
    String name,mac;

    SQLiteDatabase db;
    ArrayList<String> distArray = new ArrayList<String>();
    double lestRssi = 0;

    private BluetoothAdapter mBluetoothAdapter;
    BluetoothGatt bluetoothGatt;

    Handler handlerUpdateRssi = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what)
            {
                case MSG_UPDATE_RSSI:
                {
                    bluetoothGatt.readRemoteRssi();
                    handlerUpdateRssi.sendEmptyMessageDelayed(MSG_UPDATE_RSSI, 100);
                }
            }
        }
    };
    Runnable runnableSetEnabled = new Runnable() {
        @Override
        public void run() {
            btn_next.setEnabled(true);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_correction);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorTitleBackground)));

        //初始化
        tV_deviceName   = findViewById(R.id.tV_deviceName);
        tV_mark[0]      = findViewById(R.id.tV_mark1);
        tV_mark[1]      = findViewById(R.id.tV_mark2);
        tV_mark[2]      = findViewById(R.id.tV_mark3);
        tV_mark[3]      = findViewById(R.id.tV_mark4);
        tV_rssi         = findViewById(R.id.tV_rssi);
        progressBar     = findViewById(R.id.progressBar);
        btn_cancel      = findViewById(R.id.btn_cancel);
        btn_next        = findViewById(R.id.btn_next);

        btn_next.setOnClickListener(btnNextClick);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //取出預設裝置資料
        Intent it       = getIntent();
        name            = it.getStringExtra("name");
        mac             = it.getStringExtra("mac");
        tV_deviceName.setText("裝置名稱：" + name);

        //Beacon database建立
        db= openOrCreateDatabase("test2DB.db",MODE_PRIVATE,null);

        String createTable = "create table if not exists test " +
                "(_id integer primary key autoincrement, " +
                "name text, " +
                "detail text, " +
                "mac text, " +
                "defaul text, " +
                "atOneMeter integer)";

        db.execSQL(createTable);

        //判斷是否有藍芽，若沒開起就要求使用者開啟藍芽
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //取得BluetoothAdapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //請求取得定位權限
        if (Build.VERSION.SDK_INT >= 23) {
            int readPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

            if (readPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        scanBeacon(true);

        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mac);
        if(bluetoothGatt != null){
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

        bluetoothGatt = bluetoothDevice.connectGatt(CorrectionActivity.this, false, mGattCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();

        //關閉bluetooth
        handlerUpdateRssi.removeCallbacksAndMessages(null);
        if(bluetoothGatt != null){
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        lestRssi = 0;

//        scanBeacon(false);
    }

//    private void scanBeacon(final boolean enable) {
//        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
//        if (enable){
//            Log.d("scanBeacon","開始搜尋");
//            bluetoothLeScanner.startScan(scanCallback);
//        }
//        else{
//            Log.d("scanBeacon","結束搜尋");
//            bluetoothLeScanner.stopScan(scanCallback);
//        }
//
//    }

//    private ScanCallback scanCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(int callbackType, ScanResult result) {
//            BluetoothDevice device = result.getDevice();
//            Log.d("scanCallback",device.getAddress());
//            //判斷掃到的是不是我們要掃的beacon
//            if(device.getAddress().equals(mac)){
//
//                int rssi = result.getRssi();
//                int txPower = result.getTxPower();
//                Log.d("scanCallback","是");
//                //如果是第一次掃到beacon就新增，否則更新Rssi及TxPower
//                if(beacon == null)
//                    beacon = new Beacon(device,rssi,txPower);
//                else{
//                    beacon.setRssi(rssi);
//                    beacon.setTxPower(txPower);
//                }
//                bluetoothGatt = device.connectGatt(CorrectionActivity.this, false, mGattCallback);
//                Message msg= Message.obtain(handlerUpdateRssi,MSG_UPDATE_RSSI);
//                msg.sendToTarget();
//                scanBeacon(false);
//
//            }else{
//                Log.d("scanCallback","不是");
////                tV_direction.setText("尚未搜索到裝置");
//            }
//        }
//    };

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    //第一次進來
                    if(lestRssi == 0) {
                        Log.d("onConnectionStateChange","GATT_SUCCESS");
                        handlerUpdateRssi.postDelayed(runnableSetEnabled,500);
                        Message msg= Message.obtain(handlerUpdateRssi,MSG_UPDATE_RSSI);
                        msg.sendToTarget();
                        distArray.clear();
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("onConnectionStateChange","DISCONNECTED");
                }
                Log.d("onConnectionStateChange","DISCONNECTED2");
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.d("mGattCallback","onReadRemoteRssi：" + rssi);
            if(distArray.size() != DIST_ARRAY_MAX_SIZE){

                //distArray未滿
                distArray.add(String.valueOf(rssi));
            }else {

                //distArray滿的話
                double newRssi = 0;
                for(int i = 1;i<DIST_ARRAY_MAX_SIZE-1;i++) {
                    newRssi += Integer.valueOf(distArray.get(i));
                }
                newRssi = newRssi / (DIST_ARRAY_MAX_SIZE-2);

                //如果是第一次取出信號
                if(lestRssi == 0) {
                    lestRssi = newRssi;
                }

                //取出上次訊號並平滑
//                newRssi = 0.75 * newRssi + 0.25 * lestRssi;
                BigDecimal bdRssi = new BigDecimal(newRssi);
                double smoothRssi = Double.valueOf(bdRssi.setScale(0,BigDecimal.ROUND_HALF_UP)+"");
                Log.d("mGattCallback","smoothRssi：" + smoothRssi);
                lestRssi = smoothRssi;
                distArray.clear();
                tV_rssi.setText("偵測到的訊號強度：" + smoothRssi + " dbm");
            }
        }
    };

    Button.OnClickListener btnNextClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            //設定progressBar進度
            progressBar.setProgress(++markStep + 1);

            //sumRSSI並把每次的紀錄顯示在畫面上
            sumRssi += lestRssi;
            tV_mark[markStep].setText(lestRssi+"");

            //判斷是不是最後一次 如果是就儲存新的atOneMeter
            if(markStep == 3) {
                final int avgRssi = (int) Math.round(sumRssi / 4);
                AlertDialog dialog = new AlertDialog.Builder(CorrectionActivity.this)
                        .setTitle("取樣完畢")
                        .setMessage("取樣結果：" + avgRssi + "\n請問確定要以這次測量結果做為往後此裝置判斷距離之依據嗎?")
                        .setPositiveButton("完成", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                ContentValues cv = new ContentValues();
                                cv.put("atOneMeter",Math.abs(avgRssi)); //運算需要絕對值
                                db.update("test", cv, "defaul=?", new String[]{"true"});
                                finish();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(CorrectionActivity.this,R.color.colorPrimary));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.LTGRAY);
            }
        }
    };
}
