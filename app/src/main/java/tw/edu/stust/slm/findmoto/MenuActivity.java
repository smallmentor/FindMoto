package tw.edu.stust.slm.findmoto;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.THLight.USBeacon.App.Lib.BatteryPowerData;
import com.THLight.USBeacon.App.Lib.USBeaconConnection;
import com.THLight.USBeacon.App.Lib.iBeaconData;
import com.THLight.USBeacon.App.Lib.iBeaconScanManager;

import java.util.ArrayList;
import java.util.List;

import tw.edu.stust.slm.findmoto.ui.UIMain;

public class MenuActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, iBeaconScanManager.OniBeaconScan {

    View startLayout,listLayout,settingLayout;
    Button btnGotoList_start,btnGotoList_setting,btnFind,btnStartCorrection;
    ListView beacon_List;
    FloatingActionButton fabAdd;

    BLEListAdapter listAdapter = null;
    SQLiteDatabase db;

    final int REQ_ENABLE_BT		= 2000;
    final int REQ_ENABLE_WIFI	= 2001;

    final int MSG_SCAN_IBEACON			= 1000;
    final int MSG_UPDATE_BEACON_LIST	= 1001;
    final int MSG_START_SCAN_BEACON		= 2000;
    final int MSG_STOP_SCAN_BEACON		= 2001;
    final int MSG_SERVER_RESPONSE		= 3000;
    final int TIME_BEACON_TIMEOUT		= 10000;

    iBeaconScanManager miScaner	= null;
    USBeaconConnection mBServer	= new USBeaconConnection();
    List<ScanediBeacon> miBeacons	= new ArrayList<ScanediBeacon>();    // a beacon list
    BluetoothAdapter mBLEAdapter= BluetoothAdapter.getDefaultAdapter();
    Handler mHandler= new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch(msg.what)
            {
                case MSG_SCAN_IBEACON:
                {
                    int timeForScaning		= msg.arg1;
                    int nextTimeStartScan	= msg.arg2;

                    miScaner.startScaniBeacon(timeForScaning);   //Start scan iBeacon
                    this.sendMessageDelayed(Message.obtain(msg), nextTimeStartScan);
                }
                break;

                // 更新裝置清單
                case MSG_UPDATE_BEACON_LIST:
                    synchronized(listAdapter)
                    {
                        verifyiBeacons();
                        listAdapter.notifyDataSetChanged();
                        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BEACON_LIST, 200);
                    }
                    break;

                case MSG_SERVER_RESPONSE:
                    switch(msg.arg1)
                    {
                        case USBeaconConnection.MSG_NETWORK_NOT_AVAILABLE:
                            break;

                        // Get the data from Server by the "QUERY_UUID"
                        case USBeaconConnection.MSG_HAS_UPDATE:
                            System.out.println("USBeaconConnection.MSG_HAS_UPDATE-1");
                            mBServer.downloadBeaconListFile();
                            System.out.println("USBeaconConnection.MSG_HAS_UPDATE-2");
                            Toast.makeText(MenuActivity.this, "HAS_UPDATE.", Toast.LENGTH_SHORT).show();
                            break;

                        case USBeaconConnection.MSG_HAS_NO_UPDATE:
                            Toast.makeText(MenuActivity.this, "No new BeaconList.", Toast.LENGTH_SHORT).show();
                            break;

                        case USBeaconConnection.MSG_DOWNLOAD_FINISHED:
                            break;

                        case USBeaconConnection.MSG_DOWNLOAD_FAILED:
                            Toast.makeText(MenuActivity.this, "Download file failed!", Toast.LENGTH_SHORT).show();
                            break;

                        case USBeaconConnection.MSG_DATA_UPDATE_FAILED:
                            Toast.makeText(MenuActivity.this, "UPDATE_FAILED!", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //開始搜尋、裝置列表、矯正等 介面之引用
        startLayout = findViewById(R.id.contentStart);
        listLayout = findViewById(R.id.contentList);
        settingLayout = findViewById(R.id.contentSetting);

        //開啟裝置列表按鈕
        btnGotoList_start = findViewById(R.id.btnGotoList_start);
        btnGotoList_setting = findViewById(R.id.btnGotoList_setting);

        btnFind = findViewById(R.id.btnFind);

        btnStartCorrection = findViewById(R.id.btnStartCorrection);
        //浮動按鈕
        fabAdd = findViewById(R.id.fabAdd);

        //裝置清單
        beacon_List = findViewById(R.id.beacon_List);

        miScaner		= new iBeaconScanManager(this, this);

        //事件
        btnGotoList_setting.setOnClickListener(btnGoToListClick);
        btnGotoList_start.setOnClickListener(btnGoToListClick);

        //新增裝置畫面
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent it = new Intent();
                it.setClass(MenuActivity.this,UIMain.class);
                startActivity(it);
            }
        });

        //開啟 activity_find 畫面
        btnFind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Cursor cursor = db.rawQuery("select * from test ", null);

                Intent it = new Intent();

                for(int i=0;i<cursor.getCount();i++) {
                    cursor.moveToPosition(i);
                    if(cursor.getString(4).equals("true")){
                        it.putExtra("name",cursor.getString(1));
                        it.putExtra("mac",cursor.getString(3));
                    }
                }
                it.setClass(MenuActivity.this,FindActivity.class);
                startActivity(it);
            }
        });

        //開啟 activity_correction 畫面
        btnStartCorrection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent it = new Intent();
                it.setClass(MenuActivity.this,CorrectionActivity.class);
                startActivity(it);
            }
        });

        beacon_List.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                Intent it = new Intent();
                it.putExtra("position",position);
                it.setClass(MenuActivity.this,BeaconDetailActivity.class);
                startActivity(it);
            }
        });

        //Beacon database建立
        db= openOrCreateDatabase("test2DB.db",MODE_PRIVATE,null);

        String createTable = "create table if not exists test " +
                "(_id integer primary key autoincrement, " +
                "name text, " +
                "detail text, " +
                "mac text, " +
                "defaul text)";

        db.execSQL(createTable);
        showData();

        //確認藍芽是否開啟
        if(!mBLEAdapter.isEnabled()) {
            Intent intent= new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQ_ENABLE_BT);   // 要求開啟藍芽
        }
        else {
            // 如果已經開啟藍芽就開始搜尋
            Message msg= Message.obtain(mHandler, MSG_SCAN_IBEACON, 1000, 1100);
            msg.sendToTarget();
        }
        showData();

        //設定beacon_list高度和畫面一樣高
        listLayout.post(new Runnable() {
            @Override
            public void run() {
                Rect r = new Rect();
                Window window = getWindow();
                window.getDecorView().getWindowVisibleDisplayFrame(r);
//                int iStatusBatHight = r.top;//Status高度
                int iStatusBarPlusActionBarHeight = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                int iAppWindowHeight = dm.heightPixels - iStatusBarPlusActionBarHeight;
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) beacon_List.getLayoutParams();
                lp.height = iAppWindowHeight;
                beacon_List.setLayoutParams(lp);
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void showData() {
        Cursor cursor = db.rawQuery("select * from test ", null);
        listAdapter = new BLEListAdapter(this,cursor);

        beacon_List.setAdapter(listAdapter);
        listAdapter.notifyDataSetChanged();
    }

    private void goToList() {
        startLayout.setVisibility(View.GONE);
        listLayout.setVisibility(View.VISIBLE);
        settingLayout.setVisibility(View.GONE);

        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BEACON_LIST, 500);
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestStoragePermission();
        requestLocationPermission();

        showData();

        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BEACON_LIST, 500);
    }

    View.OnClickListener btnGoToListClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            goToList();
        }
    };

    //跟跳單有關的東西
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // 跳單事件
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.nav_Start) {
            startLayout.setVisibility(View.VISIBLE);
            listLayout.setVisibility(View.GONE);
            settingLayout.setVisibility(View.GONE);

        } else if (id == R.id.nav_list) {
            goToList();

        } else if (id == R.id.nav_setting) {
            startLayout.setVisibility(View.GONE);
            listLayout.setVisibility(View.GONE);
            settingLayout.setVisibility(View.VISIBLE);

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_end) {
            this.finish();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //取得內部記憶體權限
    private void requestStoragePermission(){
        if(Build.VERSION.SDK_INT >=23) {
            int readPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (readPermission !=PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},2);
                return;
            }
        }
    }

    //驗證iBeacon
    public void verifyiBeacons()
    {
        {
            long currTime	= System.currentTimeMillis();

            int len= miBeacons.size();
            ScanediBeacon beacon= null;

            for(int i= len- 1; 0 <= i; i--)
            {
                beacon= miBeacons.get(i);

                if(null != beacon && TIME_BEACON_TIMEOUT < (currTime- beacon.lastUpdate))
                {
                    miBeacons.remove(i);
                }
            }
        }
        listAdapter.updataScanned(miBeacons);
    }

    @Override
    public void onScaned(iBeaconData iBeacon) {
        synchronized(listAdapter)
        {
            addOrUpdateiBeacon(iBeacon);
        }
    }

    @Override
    public void onBatteryPowerScaned(BatteryPowerData batteryPowerData) {
        Log.d("debug", batteryPowerData.batteryPower+"");
        for(int i = 0 ; i < miBeacons.size() ; i++)
        {
            if(miBeacons.get(i).macAddress.equals(batteryPowerData.macAddress))
            {
                ScanediBeacon ib = miBeacons.get(i);
                ib.batteryPower = batteryPowerData.batteryPower;
                miBeacons.set(i, ib);
            }
        }
    }

    public void addOrUpdateiBeacon(iBeaconData iBeacon)
    {
        long currTime= System.currentTimeMillis();

        ScanediBeacon beacon= null;

        for(ScanediBeacon b : miBeacons)
        {
            if(b.equals(iBeacon, false))
            {
                beacon= b;
                break;
            }
        }

        if(null == beacon)
        {
            beacon= ScanediBeacon.copyOf(iBeacon);
            miBeacons.add(beacon);
        }
        else
        {
            beacon.rssi= iBeacon.rssi;
        }

        beacon.lastUpdate= currTime;
    }

    private void requestLocationPermission(){
        if(Build.VERSION.SDK_INT >=23) {
            int readPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

            if (readPermission !=PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                return;
            }
        }
    }
}

/** ============================================================== */
class BLEListAdapter extends BaseAdapter
{
    private Context context;

    private Cursor cursor;

    List<ScanediBeacon> miBeacons = new ArrayList<ScanediBeacon>();
    private String mac;
    private String defaul;

    /** ================================================ */
    public BLEListAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    /** ================================================ */
    public int getCount() { return cursor.getCount(); }

    /** ================================================ */
    public Object getItem(int position)
    {
        if((cursor.getCount() > 0) && cursor.getCount() > position)
        {
            return position;
        }

        return null;
    }

    public String getItemText(int position)
    {
        if((cursor.getCount() > 0) && cursor.getCount() > position)
        {
            return cursor.getString(0);
        }

        return null;
    }

    /** ================================================ */
    public long getItemId(int position) { return 0; }

    /** ================================================ */
    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view= convertView;

        cursor.moveToPosition(position);

        if(null == view)
            view= View.inflate(context, R.layout.beacon_list, null);

        if((cursor.getCount() > 0) && cursor.getCount() > position)
        {
            TextView tV_name	= view.findViewById(R.id.tV_name);
            TextView tV_scanned = view.findViewById(R.id.tV_scanned);
            TextView tV_detail	= view.findViewById(R.id.tV_detail);
            Button btn_defaultIcon = view.findViewById(R.id.btn_defaultIcon);

            tV_name.setText(cursor.getString(1));
            tV_detail.setText(cursor.getString(2));
            mac = cursor.getString(3);
            defaul = cursor.getString(4);

            //先預設成範圍外
            tV_scanned.setText("偵測範圍外");
            tV_scanned.setTextColor(ContextCompat.getColor(context,R.color.colorError));

            //檢查是否有掃到裝置，一個裝置都沒有就直接跳過
            if(miBeacons.size()>0) {
                for(ScanediBeacon beacon : miBeacons) {
                    //有掃到的改成"偵測範圍內"
                    if(mac.equals(beacon.macAddress)) {
                        tV_scanned.setText("偵測範圍內");
                        tV_scanned.setTextColor(ContextCompat.getColor(context,R.color.colorSuccess));
                    }
                }
            }

            //設置"預設"圖示
            if("true".equals(defaul)) {
                btn_defaultIcon.setVisibility(View.VISIBLE);
            }else {
                btn_defaultIcon.setVisibility(View.GONE);
            }

        }
        else
        {
            view.setVisibility(View.GONE);
        }

        return view;
    }

    public void updataScanned(List<ScanediBeacon> miBeacons) {
        this.miBeacons = miBeacons;
    }

    /** ================================================ */
    @Override
    public boolean isEnabled(int position)
    {
//        if(cursor.getCount() <= position)
//            return false;

        return true;
    }

    public void clear()
    {
        cursor.close();
    }
}
