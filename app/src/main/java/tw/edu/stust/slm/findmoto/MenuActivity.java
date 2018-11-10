package tw.edu.stust.slm.findmoto;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import tw.edu.stust.slm.findmoto.ui.UIMain;

public class MenuActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    View startLayout,listLayout,settingLayout;
    Button btnGotoList_start,btnGotoList_setting,btnFind,btnStartCorrection;
    ListView beacon_List;
    FloatingActionButton fabAdd;

    BLEListAdapter ListAdapter	= null;
    SQLiteDatabase db;

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
                Intent it = new Intent();
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

        //Beacon List建立
        db= openOrCreateDatabase("test2DB.db",MODE_PRIVATE,null);

        String createTable = "create table if not exists test " +
                "(_id integer primary key autoincrement, " +
                "name text, " +
                "detail text, " +
                "uuid text, " +
                "major text, " +
                "minor text, " +
                "mac text)";

        db.execSQL(createTable);
        showData();

        startLayout.post(new Runnable() {
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
        ListAdapter	= new BLEListAdapter(this,cursor);

        beacon_List.setAdapter(ListAdapter);
    }

    private void goToList() {
        startLayout.setVisibility(View.GONE);
        listLayout.setVisibility(View.VISIBLE);
        settingLayout.setVisibility(View.GONE);
        showData();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        showData();
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


}

/** ============================================================== */
class BLEListAdapter extends BaseAdapter
{
    private Context context;

    private Cursor cursor;

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
//            TextView tV_UUID	= view.findViewById(R.id.tV_UUID);
//            TextView tV_major	= view.findViewById(R.id.tV_major);
//            TextView tV_minor	= view.findViewById(R.id.tV_minor);
//            TextView tV_Mac	= view.findViewById(R.id.tV_mac);


            tV_name.setText(cursor.getString(1));
            tV_scanned.setText("偵測範圍內");
            tV_detail.setText(cursor.getString(2));
//            tV_UUID.setText(item.UUID);
//            tV_major.setText(item.major);
//            tV_minor.setText(item.minor);
//            tV_Mac.setText(item.tV_mac);
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
        if(cursor.getCount() <= position)
            return false;

        return true;
    }
}
