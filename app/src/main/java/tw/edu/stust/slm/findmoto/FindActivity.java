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
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.icu.math.BigDecimal;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class FindActivity extends FragmentActivity implements OnMapReadyCallback,LocationListener,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    final int MSG_UPDATE_RSSI = 1000;
    final int REQUEST_ENABLE_BT = 2000;

    final int DIST_ARRAY_MAX_SIZE = 4;

    private GoogleMap mMap;
    private BluetoothAdapter mBluetoothAdapter;

    TextView tV_direction,tV_dist,tV_rssi;
    ProgressBar pgb_dist;
    Button btn_endFind;
    GoogleMap googleMap;

    Beacon beacon;
    BluetoothGatt bluetoothGatt;
    String name,mac;
    int atOneMeter;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
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

//    Runnable runnable = new Runnable() {
//        @Override
//        public void run() {
//            scanBeacon(true);
//        }
//    };
    Runnable runnableSetEnabled = new Runnable() {
        @Override
        public void run() {
            tV_direction.setText("已找到");
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);

        //初始化
        tV_direction    = findViewById(R.id.tV_direction);
        tV_dist         = findViewById(R.id.tV_dist);
        tV_rssi         = findViewById(R.id.tV_rssi);

        pgb_dist        = findViewById(R.id.pgb_dist);

        btn_endFind     = findViewById(R.id.btn_endFind);

        // google map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

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
                return;
            }
        }

        btn_endFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog dialog = new AlertDialog.Builder(FindActivity.this)
                        .setTitle("結束搜尋")
                        .setMessage("確定要結束尋車嗎?")
                        .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(FindActivity.this,R.color.colorPrimary));
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.LTGRAY);
            }
        });

        Intent it   = getIntent();
        name        = it.getStringExtra("name");
        mac         = it.getStringExtra("mac");
        atOneMeter  = it.getIntExtra("atOneMeter",59);
        setTitle(name);
    }

    @Override
    protected void onStart() {
        super.onStart();
//        scanBeacon(true);
        mGoogleApiClient.connect();
        //連接藍芽
        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mac);
        if(bluetoothGatt != null){
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        Log.d("onStart","bluetoothConnect:" + mac);
        bluetoothGatt = bluetoothDevice.connectGatt(FindActivity.this, false, mGattCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();

        //關閉bluetooth
        mHandler.removeCallbacksAndMessages(null);
        if(bluetoothGatt != null){
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
//        scanBeacon(false);

        //關閉google map
        if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
            mGoogleApiClient.disconnect();
        }
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
//                if(beacon == null) {
//                    beacon = new Beacon(device,rssi,txPower);
//                    beacon.setAtOneMeter(atOneMeter);
//                }
//                else{
//                    beacon.setRssi(rssi);
//                    beacon.setTxPower(txPower);
//                }
//                bluetoothGatt = device.connectGatt(FindActivity.this, false, mGattCallback);
//                Message msg= Message.obtain(mHandler,MSG_UPDATE_RSSI);
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
            super.onConnectionStateChange(gatt, status, newState);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.d("onConnectionStateChange","STATE_DISCONNECTED");
                    if (bluetoothGatt != null){
                        bluetoothGatt.close();// 釋放資源
                        bluetoothGatt = null;
                    }
//                    mHandler.postDelayed(runnable, 400);
                }else if(newState == BluetoothGatt.STATE_CONNECTED){
                    Log.d("onConnectionStateChange","STATE_CONNECTED");
                    beacon = new Beacon(gatt.getDevice());
                    if(beacon.getLestDist() == 0) {
                        Log.d("onConnectionStateChange","GATT_SUCCESS");
                        mHandler.postDelayed(runnableSetEnabled,400);
                        Message msg= Message.obtain(mHandler,MSG_UPDATE_RSSI);
                        msg.sendToTarget();
                        beacon.setAtOneMeter(atOneMeter);
                        distArray.clear();
                    }
                }
            }
        }

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

                for(int i = 1;i<DIST_ARRAY_MAX_SIZE-1;i++){
                    newDist += Double.valueOf(distArray.get(i));
                }
                newDist = newDist / (DIST_ARRAY_MAX_SIZE-2);

//                newDist = 0.75 * newDist + (1-0.75) * lestDist;// 平滑訊號
                BigDecimal bdDist = new BigDecimal(newDist);

                //顯示
                double dist = Double.valueOf(bdDist.setScale(2,BigDecimal.ROUND_HALF_UP)+"");
                tV_dist.setText("大約距離：" + dist + " 公尺");
                distArray.clear();

                //設置progressBar的進度
                double progressNum = 1000 - (dist * 100);
                pgb_dist.setProgress((int)progressNum);
//                tV_direction.setText(progressNum);

                //設定tV_direction顯示的內容
                if(newDist-0.3 < lestDist && lestDist < newDist+0.3){
                    Log.d("tV_direction","誤差30公分以內");
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
    };

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        setUpMap();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private void setUpMap() {
        LocationManager locationManager;
        Location location = null;
        final String contextService = Context.LOCATION_SERVICE;
        String provider;
        locationManager = (LocationManager) getSystemService(contextService);

        //檢查權限要求
        if(Build.VERSION.SDK_INT >=23) {
            int readPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

            if (readPermission !=PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                return;
            }
        }

        //設定高精度、不要求海拔、不要求方位、允許網路流量花費、低功耗
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);

        //從可用的位置提供器中，找到以上標準的最佳提供器
        provider = locationManager.getBestProvider(criteria, true);

        //在地圖上開一個圖層，並以小藍點顯示用戶位置，同時加一個按鈕到地圖上，點擊時移動到用戶座標
        mMap.setMyLocationEnabled(true);

        //取得最後一次變化的位置
        location = locationManager.getLastKnownLocation(provider);
        if(location != null) {
            mLastLocation = location;
            LatLng nowLatLon = new LatLng(location.getLatitude(),location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(nowLatLon));
            CameraPosition cameraPosition = CameraPosition.builder()
                    .target(nowLatLon)
                    .zoom(16)
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            //取得摩托車位置
            SharedPreferences spLocation = getSharedPreferences("location", MODE_PRIVATE);
            double lat = Double.valueOf(spLocation.getString("lat","0"));
            double lon = Double.valueOf(spLocation.getString("lon","0"));
            LatLng motoLocation = new LatLng(lat, lon);

            //標記摩托車位置
            mMap.addMarker(new MarkerOptions().position(motoLocation).title("你的摩托車"));
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
