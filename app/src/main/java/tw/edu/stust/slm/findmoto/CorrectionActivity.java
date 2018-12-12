package tw.edu.stust.slm.findmoto;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CorrectionActivity extends AppCompatActivity {

    Toolbar toolbar;

    TextView tV_deviceName,tV_mark1,tV_mark2,tV_mark3,tV_mark4,tV_rssi;
    ProgressBar progressBar;

    String name,mac;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_correction);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorTitleBackground)));

        //初始化
        tV_deviceName = findViewById(R.id.tV_deviceName);
        tV_mark1 = findViewById(R.id.tV_mark1);
        tV_mark2 = findViewById(R.id.tV_mark2);
        tV_mark3 = findViewById(R.id.tV_mark3);
        tV_mark4 = findViewById(R.id.tV_mark4);
        tV_rssi = findViewById(R.id.tV_rssi);
        progressBar = findViewById(R.id.progressBar);

        Intent it   = getIntent();
        name        = it.getStringExtra("name");
        mac         = it.getStringExtra("mac");

        tV_deviceName.setText("裝置名稱：" + name);
    }
}
