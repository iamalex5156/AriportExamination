package com.xiaok.ariportexamination;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.ScaleAnimation;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import static com.amap.api.location.AMapLocationClientOption.AMapLocationMode.Hight_Accuracy;


public class MainActivity extends AppCompatActivity implements AMap.OnMarkerClickListener, View.OnClickListener, AMap.InfoWindowAdapter {

    private MapView mv_main;
    private AMap aMap;

    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明mLocationOption对象
    public AMapLocationClientOption mLocationOption = null;
    private double lat;
    private double lon;
    private UiSettings mUiSettings;


    private List<FloatingActionMenu> menus = new ArrayList<>();
    private FloatingActionMenu menu_blue;
    private FloatingActionButton fab_calculate_route;
    private FloatingActionButton fab_flush_data;
    private FloatingActionButton fab_data_analyse;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fab_reset_map;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fab_zoom_in;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fab_zoom_out;
    private Toolbar toolbar;

    private Handler mUiHandler = new Handler();

    private Marker carMarker; //小车位置点

    private double longitude = 121.50645;
    private double latitude = 31.28159;

    private final String MARKER_ID = "序号：";
    private final String MARKER_PEOBLEM_TYPE = "问题类型：";
    private final String MARKER_PROBLEM_INFO = "问题描述：";
    private final String MARKER_PROBLEM_NUMBER = "所在位置编号：";
    private final String MARKER_LAT_LNG = "经纬坐标：";

    private final String ANALYSE_TIME = "本次检测共花费时间：";
    private final String ANALYSE_AMOUNT = "问题点总数：";
    private final String ANALYSE_NEED_HANDLE = "需要处理问题数：";
    private final String ANALYSE_DAI_DING = "待定内容：";


    private int count = 1; //小车位置计数器，调试用，todo 接入正式数据后删除
    private int crackCount = 1; //问题点个数， todo 服务器测试完成后删除

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView(); //初始化控件
        initViewListener(); //初始化监听器

        setSupportActionBar(toolbar); //设置自定义toolbar
        toolbar.inflateMenu(R.menu.main_toolbar);//填充menu

//
//        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
//        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);//设置其为定位完成后的回调函数

        mv_main.onCreate(savedInstanceState);
        aMap = mv_main.getMap();
        mUiSettings = aMap.getUiSettings();
        if (aMap != null) {
            //设置地图中心（113.53591,34.817077）
            CameraPosition cameraPosition = new CameraPosition(new LatLng(31.28159, 121.50645), 15.2f, 0f, 0f);
            aMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            //设置隐藏自带缩放按钮和logo
            UiSettings uiSettings = aMap.getUiSettings();
            uiSettings.setZoomControlsEnabled(false);
            //绑定Marker点击事件
            aMap.setOnMarkerClickListener(this);
            aMap.setInfoWindowAdapter(this);
        }


        aMap.getUiSettings().setMyLocationButtonEnabled(true);//地图的定位标志是否可见
        aMap.setMyLocationEnabled(true);//地图定位标志是否可以点击
        setUpMap();


        //初始化弹出式FAB并配置动画
        menu_blue.setIconAnimated(true);
        menu_blue.hideMenuButton(false);
        menus.add(menu_blue);
        int delay = 400;
        for (final FloatingActionMenu menu : menus) {
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    menu.showMenuButton(true);
                }
            }, delay);
            delay += 150;
        }

    }

    /**
     * 创建Toolbar
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar, menu);
        return true;
    }

    //初始化控件
    private void initView() {
        menu_blue = findViewById(R.id.menu_blue);
        fab_calculate_route = findViewById(R.id.fab_calculate_route);
        fab_flush_data = findViewById(R.id.fab_flush_data);
        fab_data_analyse = findViewById(R.id.fab_data_analyse);
        fab_reset_map = findViewById(R.id.fab_reset_map);
        fab_zoom_in = findViewById(R.id.fab_zoom_in);
        fab_zoom_out = findViewById(R.id.fab_zoom_out);
        mv_main = findViewById(R.id.mv_main);
        toolbar = findViewById(R.id.toolbar);
    }

    //初始化监听器
    private void initViewListener() {
        fab_calculate_route.setOnClickListener(this);
        fab_flush_data.setOnClickListener(this);
        fab_data_analyse.setOnClickListener(this);
        fab_reset_map.setOnClickListener(this);
        fab_zoom_in.setOnClickListener(this);
        fab_zoom_out.setOnClickListener(this);
    }

    //所有点击事件回调
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_calculate_route:
                //计算路线
                new CalculateAsyncTask().execute();
                break;
            case R.id.fab_flush_data:
                //刷新数据
                refreshCarAndCrack();
                break;
            case R.id.fab_data_analyse:
                //进行数据分析
                new AnalyseDataTask().execute();
                break;

            case R.id.fab_reset_map:
                //重置地图缩放等级和旋转角度
                CameraPosition cameraPosition = new CameraPosition(new LatLng(34.817077, 113.53591), 15.2f, 0f, 0f);
                aMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                break;
            case R.id.fab_zoom_in:
                //放大
                aMap.animateCamera(CameraUpdateFactory.zoomIn());
                break;
            case R.id.fab_zoom_out:
                aMap.animateCamera(CameraUpdateFactory.zoomOut());
                break;
            default:
                break;
        }
    }

    /**
     * Toolbar菜单点击回调
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.main_help) {
            //帮助
            startActivity(new Intent(MainActivity.this, HelpActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void calculateRouteLine(List<LatLng> latLngs) {

        PolylineOptions options = new PolylineOptions();
        options.width(40);
        options.setCustomTexture(BitmapDescriptorFactory.fromResource(R.drawable.ic_main_route_texture));
        options.addAll(latLngs);
        options.color(Color.RED);

        aMap.addPolyline(options);
    }

    /**
     * 从服务器上获取路线各关键点坐标
     *
     * @return 关键点坐标组成的经纬度集合
     */
    private List<LatLng> getLatLngs() {
        List<LatLng> latLngs = new ArrayList<>();
        latLngs.add(new LatLng(31.28159, 121.50645));
        latLngs.add(new LatLng(31.28159, 121.50645));
        latLngs.add(new LatLng(31.28159, 121.50645));
        latLngs.add(new LatLng(31.28159, 121.50645));
        latLngs.add(new LatLng(31.28159, 121.50645));
        latLngs.add(new LatLng(31.28159, 121.50645));
        latLngs.add(new LatLng(31.28159, 121.50645));
        return latLngs;
    }

    /**
     * 刷新小车及裂缝数据
     */
    private void refreshCarAndCrack() {
        //刷新小车在地图上的位置
        new Thread(new Runnable() {
            @Override
            public void run() {
                refershCarPosition();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                refershCrack();
            }
        }).start();

    }

    /*
     * 刷新小车在地图上的位置
     */
    private void refershCarPosition() {

        if (carMarker != null) {
            carMarker.destroy(); //清除之前Marker
        }


        //从服务器上获取当前坐标
        if (count <= 10) {
            latitude = latitude + 0.02 / 10;
            addCarMarker(new LatLng(latitude, longitude)); //添加新的Marker
            count++;
        } else {
            latitude = 34.8078;
            count = 1;
        }
    }

    private void addCarMarker(LatLng latLng) {
        MarkerOptions options = new MarkerOptions();
        options.position(latLng);
        options.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_main_car_marker)));
        carMarker = aMap.addMarker(options);
        carMarker.setTitle("car");
        Animation markerAnimation = new ScaleAnimation(0, 1, 0, 1); //初始化生长效果动画
        markerAnimation.setDuration(1000);  //设置动画时间 单位毫秒
        carMarker.setAnimation(markerAnimation);
    }

    /*
     * 刷新实时监测出来的裂缝地图上
     */
    private void refershCrack() {
        Random r = new Random();
        r.setSeed(System.currentTimeMillis());
        double numLat = r.nextInt(200);
        double numLong = r.nextInt(130);
        double crackLatitude = 34.8071 + numLat / 10000;
        double crackLongitude = 113.529191 + numLong / 10000;
        addCrackMarker(new LatLng(crackLatitude, crackLongitude));

    }

    /*
     * 裂缝点的添加
     */
    private void addCrackMarker(LatLng latLng) {
        aMap.addMarker(new MarkerOptions().position(latLng).draggable(false).title(getCrackNumber(crackCount)).snippet(getMqttString()));

    }


    private String getCrackNumber(int i) {
        crackCount += 1;
        if (i <= 9) {
            return "00" + i;
        } else if (i <= 99 && i >= 10) {
            return "0" + i;
        } else {
            return "" + i;
        }

    }

    private String getMqttString() {
        return "地面裂缝ZZU裂缝平均宽度约0.4cm，长度约1cmZZUA32";

    }


    /**
     * markerClick回调启动marker动画
     *
     * @return
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTitle().equals("car")) {
            marker.startAnimation();
            return true;
        } else {
            if (marker.isInfoWindowShown()) {
                marker.hideInfoWindow();
            } else {
                marker.showInfoWindow();
            }
        }
        return false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时销毁地图
        mv_main.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时重新绘制加载地图
        mv_main.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时暂停地图的绘制
        mv_main.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时保存地图当前的状态
        mv_main.onSaveInstanceState(outState);
    }

    @Override
    public View getInfoWindow(Marker marker) {

        View infoWindow = getLayoutInflater().inflate(R.layout.dialogs_marker_window, null);//display为自定义layout文件
        TextView name = infoWindow.findViewById(R.id.tv_title);
        name.setText(MARKER_ID + marker.getTitle());
        String snippetStr = marker.getSnippet();
        String[] snippets = snippetStr.split("ZZU");
        LatLng l = marker.getPosition();// 获取标签的位置

        if (snippets.length == 3) {
            TextView tv_marker_type = infoWindow.findViewById(R.id.tv_marker_type);
            tv_marker_type.setText(MARKER_PEOBLEM_TYPE + snippets[0]);

            TextView tv_marker_info = infoWindow.findViewById(R.id.tv_marker_info);
            tv_marker_info.setText(MARKER_PROBLEM_INFO + snippets[1]);

            TextView tv_marker_number = infoWindow.findViewById(R.id.tv_marker_number);
            tv_marker_number.setText(MARKER_PROBLEM_NUMBER + snippets[2]);

            TextView tv_marker_latlng = infoWindow.findViewById(R.id.tv_marker_latlng);
            tv_marker_latlng.setText(MARKER_LAT_LNG + l.latitude + ", " + longitude);

        } else {
            Toast.makeText(MainActivity.this, "snippets长度为：" + snippets.length, Toast.LENGTH_SHORT).show();
        }

        return infoWindow;

    }

    @Override
    public View getInfoContents(Marker marker) {

        return null;
    }


    //异步绘制导航路线
    @SuppressLint("StaticFieldLeak")
    private class CalculateAsyncTask extends AsyncTask<Void, String, Boolean> {

        ProgressDialog progressDialog;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle("规划路线");
            progressDialog.setMessage("正在初始化相关数据...");
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMax(100);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            publishProgress("正在执行规划...");
            SystemClock.sleep(500);
            calculateRouteLine(getLatLngs());
            return true;
        }

        @Override
        protected void onProgressUpdate(String... values) {// String... 表示不定参数，即调用时可以传入多个String对象
            super.onProgressUpdate(values);

            progressDialog.setMessage(values[0] + "，请勿关闭此界面...");
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            progressDialog.dismiss();
            if (aBoolean) {
                Toast.makeText(MainActivity.this, "路线已规划！", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "路线规划失败，请检查网络或稍后重试", Toast.LENGTH_SHORT).show();

            }
        }
    }


    private boolean dataAnalyse(List<String> mResultAttributes) {
        new MaterialDialog.Builder(MainActivity.this)
                .title("数据分析")
                .canceledOnTouchOutside(false)
                .items(mResultAttributes)
                .positiveText("确定")
                .negativeText("导航到最近问题点")
                .onAny(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (which == DialogAction.POSITIVE) {
                            dialog.dismiss();
                        } else if (which == DialogAction.NEGATIVE) {
                            //导航到最近点
                            Toast.makeText(MainActivity.this, "正在导航", Toast.LENGTH_SHORT).show();
                        }

                    }
                })
                .show();
        return true;
    }


    private List<String> getAnalyseResult() {
        List<String> mResults = new ArrayList<>();
        mResults.add(ANALYSE_TIME + "2小时34分钟27秒");
        mResults.add(ANALYSE_AMOUNT + "3个");
        mResults.add(ANALYSE_NEED_HANDLE + "2个");
        mResults.add(ANALYSE_DAI_DING + "待定内容");
        return mResults;
    }

    //异步分析数据
    @SuppressLint("StaticFieldLeak")
    private class AnalyseDataTask extends AsyncTask<Void, String, Boolean> {

        ProgressDialog progressDialog;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setTitle("分析数据");
            progressDialog.setMessage("正在整合相关数据...");
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMax(100);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {

            publishProgress("正在进行分析...");
            SystemClock.sleep(500);
            return true;
        }

        @Override
        protected void onProgressUpdate(String... values) {// String... 表示不定参数，即调用时可以传入多个String对象
            super.onProgressUpdate(values);

            progressDialog.setMessage(values[0] + "，请勿关闭此界面...");
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            progressDialog.dismiss();
            if (aBoolean) {
                dataAnalyse(getAnalyseResult());
                Toast.makeText(MainActivity.this, "分析完成！", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "分析失败失败，请检查网络或稍后重试", Toast.LENGTH_SHORT).show();

            }
        }
    }


    /**
     * 配置定位参数
     */
    private void setUpMap() {

        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();

        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(Hight_Accuracy);

        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);

        //设置是否只定位一次,默认为false
        mLocationOption.setOnceLocation(false);


        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);

        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);

        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);

        //启动定位
//        mLocationClient.startLocation();
    }

    public AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (amapLocation != null) {
                if (amapLocation.getErrorCode() == 0) {

                    Log.v("getLocationType", "" + amapLocation.getLocationType());
                    lat = amapLocation.getLatitude();
                    lon = amapLocation.getLongitude();

                    Log.v("getAccuracy", "" + amapLocation.getAccuracy() + " 米");//获取精度信息
                    Log.v("joe", "lat :-- " + lat + " lon :--" + lon);
                    Log.v("joe", "Country : " + amapLocation.getCountry() + " province : " + amapLocation.getProvince() + " City : " + amapLocation.getCity() + " District : " + amapLocation.getDistrict());
                    //清空缓存位置
                    aMap.clear();


                    // 设置显示的焦点，即当前地图显示为当前位置
//                    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 18));
                    //aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
                    //aMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat, lon)));


                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(new LatLng(lat, lon));
                    markerOptions.title("我的位置");
                    markerOptions.visible(true);
                    markerOptions.draggable(true);
                    Marker marker = aMap.addMarker(markerOptions);
                    marker.showInfoWindow();
                } else {
                    //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                    Log.e("joe", "location Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                }
            }
        }
    };


}
