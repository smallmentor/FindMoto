package tw.edu.stust.slm.findmoto;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

/**
 * Implementation of App Widget functionality.
 */
public class GetLocationWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.get_location_widget);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            // Create an Intent to launch UpdateService
            Intent intent = new Intent(context, UpdateService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
            // Get the layout for the App Widget and attach an on-click listener to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.get_location_widget);
            views.setOnClickPendingIntent(R.id.btn_setLocation, pendingIntent);
            // Tell the AppWidgetManager to perform an update on the current App Widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    public static class UpdateService extends Service {
        SharedPreferences spLocation;

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            //process your click here
            Log.d("UpdateService","onStartCommand");
            spLocation = getSharedPreferences("location", MODE_PRIVATE);
            getLocation();
            return START_NOT_STICKY;
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            Log.d("UpdateService","onBind");
            return null;
        }

        public void getLocation() {
            //檢查權限要求
            if(Build.VERSION.SDK_INT >=23) {
                int readPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

                if (readPermission !=PackageManager.PERMISSION_GRANTED) {
                    /** ======== 無法在此要求權限，尋找方法中 ========== */
//                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                    return;
                }
            }

            LocationManager locationManager;
            Location location;
            final String contextService = Context.LOCATION_SERVICE;
            String provider;
            locationManager = (LocationManager) getSystemService(contextService);

            //設定高精度、不要求海拔、不要求方位、允許網路流量花費、低功耗
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);
            criteria.setPowerRequirement(Criteria.POWER_LOW);

            //從可用的位置提供器中，找到以上標準的最佳提供器
            provider = locationManager.getBestProvider(criteria, true);

            //取得最後一次變化的位置
            location = locationManager.getLastKnownLocation(provider);
            spLocation.edit()
                    .putString("lat", String.valueOf(location.getLatitude()))
                    .putString("lon", String.valueOf(location.getLongitude()))
                    .commit();

            // 主程式開著，toast才有用
            Toast.makeText(this, "已儲存現在位置", Toast.LENGTH_LONG).show();
        }
    }
}

