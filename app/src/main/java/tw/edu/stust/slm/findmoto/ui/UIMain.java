/** ============================================================== */
package tw.edu.stust.slm.findmoto.ui;
/** ============================================================== */
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.THLight.USBeacon.App.Lib.BatteryPowerData;
import com.THLight.USBeacon.App.Lib.USBeaconConnection;
import com.THLight.USBeacon.App.Lib.USBeaconData;
import com.THLight.USBeacon.App.Lib.USBeaconList;
import com.THLight.USBeacon.App.Lib.USBeaconServerInfo;
import com.THLight.USBeacon.App.Lib.iBeaconData;
import com.THLight.USBeacon.App.Lib.iBeaconScanManager;
import tw.edu.stust.slm.findmoto.R;
import tw.edu.stust.slm.findmoto.ScanediBeacon;
import tw.edu.stust.slm.findmoto.THLApp;
import tw.edu.stust.slm.findmoto.THLConfig;
import com.THLight.Util.THLLog;

/** ============================================================== */
public class UIMain extends Activity implements iBeaconScanManager.OniBeaconScan, USBeaconConnection.OnResponse
{
	/** this UUID is generate by Server while register a new account. */
	final UUID QUERY_UUID		= UUID.fromString("BB746F72-282F-4378-9416-89178C1019FC");
	/** server http api url. */
	final String HTTP_API		= "http://www.usbeacon.com.tw/api/func";
	
	static String STORE_PATH	= Environment.getExternalStorageDirectory().toString()+ "/USBeaconSample/";

//	ContextWrapper cw = new ContextWrapper(getApplicationContext());
//	File dir = cw.getDir(getFilesDir().getName(), Context.MODE_PRIVATE);
//	String STORE_PATH = dir.getAbsolutePath();

	final int REQ_ENABLE_BT		= 2000;
	final int REQ_ENABLE_WIFI	= 2001;
	
	final int MSG_SCAN_IBEACON			= 1000;
	final int MSG_UPDATE_BEACON_LIST	= 1001;
	final int MSG_START_SCAN_BEACON		= 2000;
	final int MSG_STOP_SCAN_BEACON		= 2001;
	final int MSG_SERVER_RESPONSE		= 3000;
	
	final int TIME_BEACON_TIMEOUT		= 30000;

    SQLiteDatabase db;

	THLApp App		= null;
	THLConfig Config= null;

	THLApp thlApp;
	
	BluetoothAdapter mBLEAdapter= BluetoothAdapter.getDefaultAdapter();

	/** scanner for scanning iBeacon around. */
	iBeaconScanManager miScaner	= null;
	
	/** USBeacon server. Connect to the Server.*/
	USBeaconConnection mBServer	= new USBeaconConnection();
	
	USBeaconList mUSBList		= null;
	
	ListView beaconList = null;
	
	BLEListAdapter listAdapter = null;
	
	List<ScanediBeacon> miBeacons	= new ArrayList<ScanediBeacon>();    // a beacon list
	
	/** ================================================ */
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
						mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BEACON_LIST, 500);
					}
					break;
				
				case MSG_SERVER_RESPONSE:
					switch(msg.arg1)
					{
						case USBeaconConnection.MSG_NETWORK_NOT_AVAILABLE:
							break;

                        // Get the data from Server by the "QUERY_UUID"
						case USBeaconConnection.MSG_HAS_UPDATE:
						    //Download beacon data to a zip file, and send MSG_DATA_UPDATE_FINISHED
							System.out.println("USBeaconConnection.MSG_HAS_UPDATE-1");
							mBServer.downloadBeaconListFile();
							System.out.println("USBeaconConnection.MSG_HAS_UPDATE-2");
							Toast.makeText(UIMain.this, "HAS_UPDATE.", Toast.LENGTH_SHORT).show();
							break;
							
						case USBeaconConnection.MSG_HAS_NO_UPDATE:
							Toast.makeText(UIMain.this, "No new BeaconList.", Toast.LENGTH_SHORT).show();
							break;
							
						case USBeaconConnection.MSG_DOWNLOAD_FINISHED:
							break;
		
						case USBeaconConnection.MSG_DOWNLOAD_FAILED:
							Toast.makeText(UIMain.this, "Download file failed!", Toast.LENGTH_SHORT).show();
							break;
							
						case USBeaconConnection.MSG_DATA_UPDATE_FINISHED:
							{
								USBeaconList BList= mBServer.getUSBeaconList();  //Get the beacon list that was from Server

								if(null == BList)
								{
									Toast.makeText(UIMain.this, "Data Updated failed.", Toast.LENGTH_SHORT).show();
									THLLog.d("debug", "update failed.");
								}
								else if(BList.getList().isEmpty())
								{
									Toast.makeText(UIMain.this, "Data Updated but empty.", Toast.LENGTH_SHORT).show();
									THLLog.d("debug", "this account doesn't contain any devices.");
								}
								else
								{
								    String BeaconData = "";
									Toast.makeText(UIMain.this, "Data Updated("+ BList.getList().size()+ ")", Toast.LENGTH_SHORT).show();
									
									for(USBeaconData data : BList.getList())
									{
									    BeaconData = BeaconData + "Name("+ data.name+ "), Ver("+ data.major+ "."+ data.minor+ ")\n";
										THLLog.d("debug", "Name("+ data.name+ "), Ver("+ data.major+ "."+ data.minor+ ")");
									}

                                    showBeaconFromServerOnDialog(BeaconData);
								}
							}
							break;
							
						case USBeaconConnection.MSG_DATA_UPDATE_FAILED:
							Toast.makeText(UIMain.this, "UPDATE_FAILED!", Toast.LENGTH_SHORT).show();
							break;
					}
					break;
			}
		}
	};
	
	/** ================================================ */
	@Override
	protected void onCreate(Bundle savedInstanceState)	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_main);
		Log.d("debug","1");
		thlApp = new THLApp(this);
		App		= thlApp.getApp();
		Config	= thlApp.Config;

		//資料庫建立
        db= openOrCreateDatabase("test2DB.db",MODE_PRIVATE,null);

		String createTable = "create table if not exists test " +
				"(_id integer primary key autoincrement, " +
				"name text, " +
				"detail text, " +
				"mac text, " +
				"defaul text)";

        db.execSQL(createTable);

		requestStoragePermission();

		/** create instance of iBeaconScanManager. */
		miScaner		= new iBeaconScanManager(this, this);
		
		listAdapter 	= new BLEListAdapter(this);
		
		beaconList 		= findViewById(R.id.beacon_list);
		beaconList.setAdapter(listAdapter);

		beaconList.setOnItemClickListener(listViewNoItemClick);
		Log.d("debug","3");
		//確認藍芽是否開啟
		if(!mBLEAdapter.isEnabled())
		{
			Intent intent= new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(intent, REQ_ENABLE_BT);   // 要求開啟藍芽
		}
		else
		{
		    // 如果已經開啟藍芽就開始搜尋
			Message msg= Message.obtain(mHandler, MSG_SCAN_IBEACON, 1000, 1100);
			msg.sendToTarget();
		}

		/** create store folder. */
		File file= new File(STORE_PATH);
		if(!file.exists())
		{
			if(!file.mkdirs())
			{
				Toast.makeText(this, "Create folder("+ STORE_PATH+ ") failed.", Toast.LENGTH_SHORT).show();
			}
		}
		
//		/** check network is available or not. */
//		ConnectivityManager cm	= (ConnectivityManager)getSystemService(UIMain.CONNECTIVITY_SERVICE);
//		if(null != cm)
//		{
//			NetworkInfo ni = cm.getActiveNetworkInfo();
//			if(null == ni || (!ni.isConnected()))
//			{
//				dlgNetworkNotAvailable();     //Show a dialog to inform users to enable  the network.
//			}
//			else
//			{
//				THLLog.d("debug", "NI not null");
//
//				NetworkInfo niMobile= cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
//				if(null != niMobile)
//				{
//					boolean isMobileInt	= niMobile.isConnectedOrConnecting();
//
//					if(isMobileInt)
//					{
//						dlgNetworkMobile();  //Show a dialog to make sure to use the Mobile Internet
//					}
//					else
//					{
//						USBeaconServerInfo info= new USBeaconServerInfo();
//
//						info.serverUrl		= HTTP_API;
//						info.queryUuid		= QUERY_UUID;
//						info.downloadPath	= STORE_PATH;
//
//						mBServer.setServerInfo(info, this);
//						//Check is there is data to download from Server or not(By QUERY_UUID).
//                        // If yes, send MSG_HAS_UPDATE.
//                        // If no, send MSG_HAS_NO_UPDATE.
//						mBServer.checkForUpdates();
//					}
//				}
//			}
//		}
//		else
//		{
//			THLLog.d("debug", "CM null");
//		}
		
		mHandler.sendEmptyMessageDelayed(MSG_UPDATE_BEACON_LIST, 500);
	}

    /** ================================================ */
	//點下左鍵，新增裝置
	ContentValues cv = new ContentValues();
	private AdapterView.OnItemClickListener listViewNoItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            TextView tV_mac = view.findViewById(R.id.tV_mac);
            String name = "";
            String detail = "";

			cv.put("mac", tV_mac.getText().toString());

			//檢查輸入規則
			final EditText et = new EditText(UIMain.this);
			new AlertDialog.Builder(UIMain.this).setTitle("命名裝置(請勿超過8個字元)")
					.setView(et)
					.setPositiveButton("確定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							String input = et.getText().toString();
							Log.d("命名裝置","if前");
							if ("".equals(input)) {
								Toast.makeText(getApplicationContext(), "內容不能為空！", Toast.LENGTH_LONG).show();
							}
							else if(input.length()>8){
								Toast.makeText(getApplicationContext(), "請勿超過8個字元！", Toast.LENGTH_LONG).show();
							}
							else {
								cv.put("name", input);
								final EditText et = new EditText(UIMain.this);
								new AlertDialog.Builder(UIMain.this).setTitle("輸入描述(請勿超過30個字元)")
										.setView(et)
										.setPositiveButton("確定", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface dialog, int which) {
												String input = et.getText().toString();
												Log.d("輸入描述","if前");
												if(input.length()>30){
													Toast.makeText(getApplicationContext(), "請勿超過30個字元！", Toast.LENGTH_LONG).show();
												}
												else {
													cv.put("detail", input);
													cv.put("defaul","true");

													//先把其他裝置預設取消
													Cursor cursor = db.rawQuery("select * from test ", null);
													if (cursor.getCount() > 0) {  //如果沒有資料則離開
														cursor.moveToLast(); //移至最後一筆，由後往前面讀取
														String id;
														do{
															id =String.valueOf(cursor.getInt(0));
															ContentValues cv = new ContentValues(1);
															cv.put("defaul","false");
															db.update("test", cv, "_id=?", new String[]{id});
														}while(cursor.moveToPrevious());
													}

													//設新增的為預設裝置
													db.insert("test", null, cv);
													cv.clear();
													finish();
												}
											}
										})
										.setNegativeButton("取消", null)
										.show();
							}
						}
					})
					.setNegativeButton("取消", null)
					.show();
        }
    };

	/** ================================================ */
	@Override
	public void onResume()
	{
		super.onResume();
	}
	
	/** ================================================ */
	@Override
	public void onPause()
	{
		super.onPause();
	}

	/** ================================================ */
	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
	}

	/** ================================================ */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
  	{
  		THLLog.d("DEBUG", "onActivityResult()");

  		switch(requestCode)
  		{
  			case REQ_ENABLE_BT:
	  			if(RESULT_OK == resultCode)
	  			{
				}
	  			break;

  			case REQ_ENABLE_WIFI:
  				if(RESULT_OK == resultCode)
	  			{
				}
  				break;
  		}
  	}

    /** ================================================ */
    /** implementation of {@link iBeaconScanManager#OniBeaconScan } */
	@Override
	public void onScaned(iBeaconData iBeacon)
	{
		synchronized(listAdapter)
		{
			addOrUpdateiBeacon(iBeacon);
		}
	}
	/** ================================================ */
    /** implementation of {@link iBeaconScanManager#OniBeaconScan } */
	@Override
	public void onBatteryPowerScaned(BatteryPowerData batteryPowerData) {
		// TODO Auto-generated method stub
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
	
	/** ========================================================== */
	public void onResponse(int msg)
	{
		THLLog.d("debug", "Response("+ msg+ ")");
		mHandler.obtainMessage(MSG_SERVER_RESPONSE, msg, 0).sendToTarget();
	}

    public void showBeaconFromServerOnDialog(String sBeaconDate)
    {
        final AlertDialog dlg = new AlertDialog.Builder(UIMain.this).create();

        dlg.setTitle("Beacon from Server. " + QUERY_UUID);
        dlg.setMessage(sBeaconDate);

        dlg.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                dlg.dismiss();
            }
        });

        dlg.show();
    }
	/** ========================================================== */
	public void dlgNetworkNotAvailable()
	{
		final AlertDialog dlg = new AlertDialog.Builder(UIMain.this).create();
		
		dlg.setTitle("Network");
		dlg.setMessage("Please enable your network for updating beacon list.");

		dlg.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				dlg.dismiss();
			}
		});
		
		dlg.show();
	}
	
	/** ========================================================== */
	public void dlgNetworkMobile()
	{
		final AlertDialog dlg = new AlertDialog.Builder(UIMain.this).create();
		
		dlg.setTitle("3G");
		dlg.setMessage("App will send/recv data via Mobile Internet, this may result in significant data charges.");

		// To check yes or no of using mobile Internet.
		dlg.setButton(AlertDialog.BUTTON_POSITIVE, "Allow", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
                requestLocationPermission();
                Log.d("debug","dlgNetworkMobile-1");
				Config.allow3G= true;
				Log.d("debug","dlgNetworkMobile-2");
				dlg.dismiss();
				USBeaconServerInfo info= new USBeaconServerInfo();

				info.serverUrl		= HTTP_API;
				info.queryUuid		= QUERY_UUID;
				info.downloadPath	= STORE_PATH;
				mBServer.setServerInfo(info, UIMain.this);
				mBServer.checkForUpdates();
				Log.d("debug","dlgNetworkMobile-4");
			}
		});
		
		dlg.setButton(AlertDialog.BUTTON_NEGATIVE, "Reject", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
				Config.allow3G= false;
				dlg.dismiss();
			}
		});
	
		dlg.show();
	}

	private void requestStoragePermission(){
		if(Build.VERSION.SDK_INT >=23) {
			int readPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

			if (readPermission !=PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},2);
				return;
			}
		}
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
	
	/** ========================================================== */
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
	
	/** ========================================================== */
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
		
		{
			listAdapter.clear();

			//Add beacon to the list that it could show on the screen.
			for(ScanediBeacon beacon : miBeacons)
			{
				listAdapter.addItem(new ListItem(beacon.beaconUuid.toUpperCase(), ""+ beacon.major, ""+ beacon.minor, ""+ beacon.rssi,""+beacon.batteryPower, beacon.macAddress));
			}
		}
	}
	
	/** ========================================================== */
	public void cleariBeacons()
	{
		listAdapter.clear();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		thlApp.onTerminate();
	}
}

/** ============================================================== */
class BLEListAdapter extends BaseAdapter
{
	private Context mContext;
	  
	List<ListItem> mListItems= new ArrayList<ListItem>();

	/** ================================================ */
	public BLEListAdapter(Context context) { mContext= context; }
	
	/** ================================================ */
	public int getCount() { return mListItems.size(); }
	
	/** ================================================ */
	public Object getItem(int position)
	{
		if((!mListItems.isEmpty()) && mListItems.size() > position)
		{
			return mListItems.toArray()[position];
		}
		
		return null;
	}
	  
	public String getItemText(int position)
	{
		if((!mListItems.isEmpty()) && mListItems.size() > position)
		{
			return ((ListItem)mListItems.toArray()[position]).UUID;
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
	     
	    if(null == view)
	    	view= View.inflate(mContext, R.layout.item_text_3, null);
	
	    // view.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

	    if((!mListItems.isEmpty()) && mListItems.size() > position)
	    {
		    TextView tV_UUID	= view.findViewById(R.id.tV_UUID);
		    TextView tV_major	= view.findViewById(R.id.tV_major);
		    TextView tV_minor	= view.findViewById(R.id.tV_minor);
		    TextView tV_rssi	= view.findViewById(R.id.tV_rssi);
		    TextView tV_batteryPower	= view.findViewById(R.id.tV_batteryPower);
            TextView tV_Mac	= view.findViewById(R.id.tV_mac);

	    	ListItem item= (ListItem)mListItems.toArray()[position];

			tV_UUID.setText(item.UUID);
			tV_major.setText(item.major);
			tV_minor.setText(item.minor);
			tV_rssi.setText(item.rssi + " dbm");
			tV_batteryPower.setText(item.tV_batteryPower + " V");
			tV_Mac.setText(item.tV_mac);
		}
	    else
	    {
	    	view.setVisibility(View.GONE);
	    }

	    return view;
	}

	/** ================================================ */
	@Override
    public boolean isEnabled(int position) 
    {
		if(mListItems.size() <= position)
			return false;

        return true;
    }

	/** ================================================ */
	public boolean addItem(ListItem item)
	{
		mListItems.add(item);
	  	return true;
	}
  
	/** ================================================ */
	public void clear()
	{
		mListItems.clear();
	}
}

/** ============================================================== */
