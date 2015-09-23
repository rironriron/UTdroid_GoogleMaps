package jp.ac.u_tokyo.t.utdroid_googlemaps;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

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

public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    /* Viewを格納するための変数 */
    private GoogleMap mapView;
    private TextView textViewStatus;

    /* 位置情報取得のための変数 */
    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;

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
        mapView.setMyLocationEnabled(true);
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
    }

    @Override
    public void onResume() {
        super.onResume();

        /* 位置情報の取得を開始する */
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
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
     * GooglePLAY開発者サービス関連のメソッド（3件）
     */
    @Override
    public void onConnected(Bundle bundle) {
        Location location = fusedLocationProviderApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            /* 緯度経度をTextViewに表示 */
            textViewStatus.setText("緯度："+location.getLongitude()+"，経度："+location.getLatitude());

            /* GoogleMapの中心を移動 */
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 14);
            mapView.moveCamera(cameraUpdate);
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
