package tw.edu.stust.slm.findmoto;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MenuActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    View startLayout,listLayout,settingLayout;
    Button btnGotoList_start,btnGotoList_setting,btnFind,btnStartCorrection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startLayout = findViewById(R.id.contentStart);
        listLayout = findViewById(R.id.contentList);
        settingLayout = findViewById(R.id.contentSetting);

        btnGotoList_start = findViewById(R.id.btnGotoList_start);
        btnGotoList_setting = findViewById(R.id.btnGotoList_setting);
        btnFind = findViewById(R.id.btnFind);
        btnStartCorrection = findViewById(R.id.btnStartCorrection);

        btnGotoList_setting.setOnClickListener(btnClick);
        btnGotoList_start.setOnClickListener(btnClick);
        btnFind.setOnClickListener(btnClick);
        btnStartCorrection.setOnClickListener(btnClick);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // 跳單事件
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

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void goToList() {
        startLayout.setVisibility(View.GONE);
        listLayout.setVisibility(View.VISIBLE);
        settingLayout.setVisibility(View.GONE);
    }

    View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            Intent it = new Intent();

            if(view==btnGotoList_setting || view==btnGotoList_start){
                goToList();
            }else if(view==btnFind){
                it.setClass(MenuActivity.this,FindActivity.class);
                startActivity(it);
            }else if(view==btnStartCorrection) {
                it.setClass(MenuActivity.this,CorrectionActivity.class);
                startActivity(it);
            }
        }
    };


}
