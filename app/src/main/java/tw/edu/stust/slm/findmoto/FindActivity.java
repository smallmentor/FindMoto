package tw.edu.stust.slm.findmoto;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.math.BigDecimal;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.util.ArrayList;

public class FindActivity extends AppCompatActivity implements OnMapReadyCallback {

    final int REQUEST_ENABLE_BT = 2000;

    final int MSG_UPDATE_RSSI = 1000;

    final int DIST_ARRAY_MAX_SIZE = 4;

    GoogleMap map;
    private BluetoothAdapter mBluetoothAdapter;

    TextView tV_direction,tV_dist,tV_rssi;
    ProgressBar pgb_dist;
    Button btn_endFind;
    Beacon beacon;
    BluetoothGatt bluetoothGatt;
    String name,mac;
    ArrayList<String> distArray = new ArrayList<String>();

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what)
            {
                case MSG_UPDATE_RSSI:
                {
                    bluetoothGatt.readRemoteRssi();
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_RSSI, 100);
                }
            }
        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            scanBeacon(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);
        setTitle("尋找中...");

        //初始化
        tV_direction    = findViewById(R.id.tV_direction);
        tV_dist         = findViewById(R.id.tV_dist);
        tV_rssi         = findViewById(R.id.tV_rssi);

        pgb_dist        = findViewById(R.id.pgb_dist);

        btn_endFind     = findViewById(R.id.btn_endFind);

        //判斷裝置是否支援超低耗藍芽
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            new AlertDialog.Builder(this)
                    .setTitle("裝置不支援")
                    .setMessage("您的裝置不支援超低耗藍芽(BLE)！無法使用此軟體！")
                    .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    })
                    .show()//改選項顏色
                    .getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        //取得BluetoothAdapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //判斷是否有藍芽，若沒開起就要求使用者開啟藍芽
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //請求取得定位權限
        if (Build.VERSION.SDK_INT >= 23) {
            int readPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

            if (readPermission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }
        }

        btn_endFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanBeacon(false);
                scanBeacon(true);
            }
        });

        Intent it   = getIntent();
        name        = it.getStringExtra("name");
        mac         = it.getStringExtra("mac");
        Log.d("onCreate",mac);
    }

    @Override
    protected void onStart() {
        super.onStart();
        scanBeacon(true);

    }

    @Override
    protected void onStop() {
        super.onStop();
        mHandler.removeCallbacksAndMessages(null);
        if(bluetoothGatt != null){
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        scanBeacon(false);
    }

    private void scanBeacon(final boolean enable) {
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (enable){
            Log.d("scanBeacon","開始搜尋");
            bluetoothLeScanner.startScan(scanCallback);
        }
        else{
            Log.d("scanBeacon","結束搜尋");
            bluetoothLeScanner.stopScan(scanCallback);
        }

    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            Log.d("scanCallback",device.getAddress());
            //判斷掃到的是不是我們要掃的beacon
            if(device.getAddress().equals(mac)){

                int rssi = result.getRssi();
                int txPower = result.getTxPower();
                Log.d("scanCallback","是");
                //如果是第一次掃到beacon就新增，否則更新Rssi及TxPower
                if(beacon == null)
                    beacon = new Beacon(device,rssi,txPower);
                else{
                    beacon.setRssi(rssi);
                    beacon.setTxPower(txPower);
                }
                bluetoothGatt = device.connectGatt(FindActivity.this, false, mGattCallback);
                Message msg= Message.obtain(mHandler,MSG_UPDATE_RSSI);
                msg.sendToTarget();
                scanBeacon(false);

            }else{
                Log.d("scanCallback","不是");
//                tV_direction.setText("尚未搜索到裝置");
            }
        }
    };

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.d("BluetoothGattCallback",rssi+"");
            beacon.setRssi(rssi);

            if(distArray.size() != DIST_ARRAY_MAX_SIZE){
                distArray.add(String.valueOf(beacon.getDistance1()));
            }else{
                //把距離跟訊號強度顯示到畫面上
                double newDist = 0;
                double lestDist = beacon.getLestDist();

                for(int i = 1;i<=DIST_ARRAY_MAX_SIZE-1;i++){
                    newDist += Double.valueOf(distArray.get(i));
            }
            newDist = newDist / (DIST_ARRAY_MAX_SIZE-2);
            BigDecimal bdDist = new BigDecimal(newDist);

            newDist = 0.75 * newDist + (1-0.75) * lestDist;// 平滑訊號

                //顯示
                double dist = Double.valueOf(bdDist.setScale(2,BigDecimal.ROUND_HALF_UP)+"");
                tV_dist.setText("大約距離：" + dist + " 公尺");
                distArray.clear();

                //設置progressBar的進度
                double progressNum = 1000 - (dist * 100);
                pgb_dist.setProgress((int)progressNum);
//                tV_direction.setText(progressNum);

                //設定tV_direction顯示的內容
                if(newDist-0.5 < lestDist && lestDist < newDist+0.5){
                    Log.d("tV_direction","誤差50公分以內");
                }else if(newDist < lestDist){
                    tV_direction.setText("方向正確");
                    tV_direction.setTextColor(ContextCompat.getColor(FindActivity.this,R.color.colorSuccess));
                }else{
                    tV_direction.setText("方向錯誤");
                    tV_direction.setTextColor(ContextCompat.getColor(FindActivity.this,R.color.colorError));
                }
                beacon.setLestDist(newDist);
            }
            tV_rssi.setText("訊號強度：" + rssi + " dBm");
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d("onConnectionStateChange","STATE_DISCONNECTED");
                    if (bluetoothGatt != null){
                        bluetoothGatt.close();// 釋放資源
                        bluetoothGatt = null;
                    }
                    mHandler.postDelayed(runnable, 400);
                }else if(newState == BluetoothGatt.STATE_CONNECTED){
                    Log.d("onConnectionStateChange","STATE_CONNECTED");
                }
            }
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
}
