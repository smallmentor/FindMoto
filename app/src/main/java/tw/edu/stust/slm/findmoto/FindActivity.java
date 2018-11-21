package tw.edu.stust.slm.findmoto;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

public class FindActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap map;

    TextView tV_direction,tV_dist,tV_rssi;

    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);

        setTitle("尋找中...");
        tV_direction = findViewById(R.id.tV_direction);
        tV_dist = findViewById(R.id.tV_dist);
        tV_rssi = findViewById(R.id.tV_rssi);

        db= openOrCreateDatabase("test2DB.db",MODE_PRIVATE,null);



    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
}
