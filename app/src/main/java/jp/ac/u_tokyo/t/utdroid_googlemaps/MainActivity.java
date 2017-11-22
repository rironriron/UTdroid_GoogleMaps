package jp.ac.u_tokyo.t.utdroid_googlemaps;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    /* Viewを格納するための変数 */
    private GoogleMap mapView;
    private TextView textViewStatus;

    /* 位置情報取得のための変数 */
    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
    private static final int LOCATION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapView = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        textViewStatus.setText("現在地を取得中…");

        if (mapView == null) {
            /* うまく行かない時は、Google PLAY開発者サービスのアップデートを勧める */
            showGooglePlayServiceUpdateDialog();
            return;
        }

        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            /* 位置情報APIにコールバック関数を登録 */
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        } else {
            /* うまく行かない時は、Google PLAY開発者サービスのアップデートを勧める */
            showGooglePlayServiceUpdateDialog();
        }

        /* GoogleMAPのセットアップ */
        MapsInitializer.initialize(this);
        mapView.getUiSettings().setCompassEnabled(true);
        mapView.getUiSettings().setIndoorLevelPickerEnabled(false);
        mapView.getUiSettings().setMapToolbarEnabled(false);
        mapView.getUiSettings().setMyLocationButtonEnabled(false);
        mapView.getUiSettings().setRotateGesturesEnabled(true);
        mapView.getUiSettings().setScrollGesturesEnabled(true);
        mapView.getUiSettings().setTiltGesturesEnabled(false);
        mapView.getUiSettings().setZoomGesturesEnabled(true);
        mapView.getUiSettings().setZoomControlsEnabled(false);

        /* デフォルトの位置を東京駅に設定 */
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(35.681382, 139.766084), 14);
        mapView.moveCamera(cameraUpdate);

        /* Android 6.0以上かどうかで条件分岐 */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /* Permissionを取得済みかどうか確認 */
            String[] dangerousPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                /* 未取得ならPermissionを要求 */
                requestPermissions(dangerousPermissions, LOCATION_REQUEST_CODE);
            }else{
                /* 取得済みなら位置情報の取得を開始 */
                connectGoogleApiClient();
            }
        }else{
            /* Android 6.0未満なら位置情報の取得を開始 */
            connectGoogleApiClient();
        }
    }

    @Override
    public void onPause() {
        /* 位置情報の取得を停止する */
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        super.onPause();
    }

    /*
     * Android 6.0以上のDANGEROUS_PERMISSION対策
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            // Permissionが許可された
            if (grantResults.length == 0) {
                return;
            }else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectGoogleApiClient();
            } else {
                Toast.makeText(this, "現在地の取得を許可して下さい。", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void connectGoogleApiClient() {
        /* 位置情報の取得を開始する */
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }


    /*
     * GooglePLAY開発者サービス関連のメソッド（3件）
     */
    @Override
    public void onConnected(Bundle bundle) {
        Location location = fusedLocationProviderApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            /* 緯度経度をTextViewに表示 */
            textViewStatus.setText("経度："+location.getLongitude()+"，緯度："+location.getLatitude());

            /* Geocoder APIで住所を逆引き */
            try {
                Geocoder geocoder = new Geocoder(this, Locale.JAPAN);
                List<Address> list_address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if(!list_address.isEmpty()) {
                    Toast.makeText(this, list_address.get(0).getAddressLine(1), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "住所を特定できませんでした。", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            /* GoogleMapの中心を移動 */
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 14);
            mapView.moveCamera(cameraUpdate);
            mapView.setMyLocationEnabled(true);
        } else {
            textViewStatus.setText("現在地を取得できませんでした。");
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        textViewStatus.setText("現在地を取得できませんでした。");
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        textViewStatus.setText("現在地を取得できませんでした。");
    }

    /*
     * GooglePLAY開発者サービスのアップデートを促すダイアログを表示
     */
    private void showGooglePlayServiceUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage("Google PLAY開発者サービスをアップデートして下さい。\nお手数ですが、アップデート完了後に再度アプリを開いて下さい。");
        builder.setPositiveButton("アップデートする", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Uri uri = Uri.parse("market://details?id=com.google.android.gms");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                finish();
            }
        });
        builder.setNegativeButton("アプリを終了する", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        builder.show();
    }
}
