package tw.edu.stust.slm.findmoto;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

public class FindActivity extends AppCompatActivity implements OnMapReadyCallback {

    final int REQUEST_ENABLE_BT = 2000;

    GoogleMap map;
    private BluetoothAdapter mBluetoothAdapter;

    TextView tV_direction,tV_dist,tV_rssi;
    Beacon beacon;
    String name,mac;
    double distArray[] = new double[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);
        setTitle("尋找中...");

        //初始化
        tV_direction    = findViewById(R.id.tV_direction);
        tV_dist         = findViewById(R.id.tV_dist);
        tV_rssi         = findViewById(R.id.tV_rssi);

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
        scanBeacon(false);
    }

    private void scanBeacon(final boolean enable) {
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        Log.d("scanBeacon","開始搜尋");
        if (enable)
            bluetoothLeScanner.startScan(scanCallback);
        else
            bluetoothLeScanner.stopScan(scanCallback);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();

            //判斷掃到的是不是我們要掃得beacon
            if(device.getAddress().equals(mac)){

                int rssi = result.getRssi();
                int txPower = result.getTxPower();

                //如果是第一次掃到beacon就新增，否則更新Rssi及TxPower
                if(beacon == null)
                    beacon = new Beacon(device,rssi,txPower);
                else{
                    beacon.setRssi(rssi);
                    beacon.setTxPower(txPower);
                }

                //把距離跟訊號強度顯示到畫面上
                BigDecimal dist = new BigDecimal(smoothDist(beacon));
                tV_dist.setText("大約距離：" + dist.setScale(2,BigDecimal.ROUND_HALF_UP) + " 公尺");
                tV_rssi.setText("訊號強度：" + rssi + " dBm");

                //設定tV_direction顯示的內容
                if(beacon.getDistance1()<beacon.getLestDist()){
                    tV_direction.setText("方向正確");
                    tV_direction.setTextColor(ContextCompat.getColor(FindActivity.this,R.color.colorSuccess));
                }else if(beacon.getDistance1() == beacon.getLestDist()){
                    tV_direction.setText("搜尋中...");
                    tV_direction.setTextColor(ContextCompat.getColor(FindActivity.this,R.color.colorContentText));
                }else{
                    tV_direction.setText("方向錯誤");
                    tV_direction.setTextColor(ContextCompat.getColor(FindActivity.this,R.color.colorError));
                }
            }
        }
    };

    double smoothDist(Beacon beacon) {
        final double a = 0.75;
        double dist;

        if(beacon.getLestDist() == 0) {
            Log.d("smoothDist","true");
            dist = beacon.getDistance1();
        } else {
            Log.d("smoothDist","else");
            dist = a*beacon.getDistance1() + (1-a) * beacon.getLestDist();
        }
        beacon.setLestDist(dist);
        return dist;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
}
