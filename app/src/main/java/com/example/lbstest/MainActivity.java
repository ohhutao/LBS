package com.example.lbstest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;
import java.util.ArrayList;
import java.util.List;
public class MainActivity extends AppCompatActivity {
    //地图的总控制器
    private BaiduMap baiduMap;
    private boolean isFirstLocate = true;//防止多次定位调用animateMapStatus
    public LocationClient mLocationClient;
    private TextView positionText;
    private MapView mapView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐私合规接口
        LocationClient.setAgreePrivacy(true);
        //调用SDKInitializer.setAgreePrivacy(Context, boolean)函数来设置是否同意隐私政策。
        SDKInitializer.setAgreePrivacy(getApplicationContext(),true);
        //显示地图初始化，获取全局参数
        SDKInitializer.initialize(getApplicationContext());
        //初始化要在绑定布局之前使用
        setContentView(R.layout.activity_main);

        mapView = (MapView) findViewById(R.id.nmapView);
        baiduMap = mapView.getMap();
        //把我的位置显示在地图上
        baiduMap.setMyLocationEnabled(true);
        try {
            //初始化获取全局的Context参数
            mLocationClient = new LocationClient(getApplicationContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //注册一个定位监听器，获取到位置信息时，回调监听器
        mLocationClient.registerLocationListener(new MyLocationListener());
        positionText  = (TextView) findViewById(R.id.position_text_view);

        //申请权限
        //把权限加入列表操作
        List<String> permisssionList  =new ArrayList<>();
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)!=
                PackageManager.PERMISSION_GRANTED){
            permisssionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_PHONE_STATE)!=
                PackageManager.PERMISSION_GRANTED){
            permisssionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(!permisssionList.isEmpty()){
            String[] permission = permisssionList.toArray(new String[permisssionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permission,1);
        }else {
            requestLocation();
        }
    }

    private void navigateTo(BDLocation location){
        if(isFirstLocate){
            //设置经纬度
            //LatLng存放经纬度
            LatLng ll =new LatLng(location.getLatitude(),location.getLongitude());
            //MapStatusUpdateFactory把ll对象传入返回一个MapStatusUpdate
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(update);
            //设置缩放级别
            //zoomTo返回一个MapStatusUpdate
            update = MapStatusUpdateFactory.zoomTo(16f);
            baiduMap.animateMapStatus(update);
            isFirstLocate = false;//防止多次调用animateMapStatus
        }
        //把经纬度封装到 MyLocationData.Builder当中
        MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
        locationBuilder.latitude(location.getLatitude());
        locationBuilder.longitude(location.getLongitude());
        //MyLocationData用于表示当前设备的位置信息。通过这个类，您可以获取和设置设备的实时位置信息，然后在地图上进行相应的展示或操作。
        MyLocationData locationData = locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
    }
    private void requestLocation(){
        initLocation();
        //调用start开始定位，定位的结果回到监听器中
        mLocationClient.start();
    }
    private void initLocation(){
        //定位，结果返回监听器
        LocationClientOption option = new LocationClientOption();
        //5s更新一次
        option.setScanSpan(5000);
        //准确地址
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }
    //对map进行管理，保证资源合理释放
    @Override
    protected  void onResume(){
        super.onResume();
        mapView.onResume();
    }
    @Override
    protected void onPause(){
        super.onPause();
        mapView.onPause();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy();
        //关闭显示位置的功能
        baiduMap.setMyLocationEnabled(false);
    }
    //申请权限的回调函数
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意所有权限才能使用", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }
    public class MyLocationListener implements BDLocationListener{
        @Override
        //把BDLocation对象传入navigateTo
        public void onReceiveLocation(BDLocation location) {
            if(location.getLocType() == BDLocation.TypeGpsLocation||location.getLocType() == BDLocation.TypeNetWorkLocation){
                navigateTo(location);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    StringBuilder currentPosition = new StringBuilder();
                    currentPosition.append("纬度:").append(location.getLatitude()).append("\n");
                    currentPosition.append("经线:").append(location.getLongitude()).append("\n");
                    currentPosition.append("国家:").append(location.getCountry()).append("\n");
                    currentPosition.append("省:").append(location.getProvince()).append("\n");
                    currentPosition.append("市:").append(location.getCity()).append("\n");
                    currentPosition.append("区:").append(location.getDirection()).append("\n");
                    currentPosition.append("街道:").append(location.getStreet()).append("\n");
                    currentPosition.append("定位方式:");
                    if (location.getLocType()==BDLocation.TypeGpsLocation){
                        currentPosition.append("GPS");
                    } else if (location.getLocType()==BDLocation.TypeNetWorkLocation) {
                        currentPosition.append("网络");
                    }
                    positionText.setText(currentPosition);
                }
            });
        }
    }
}