package com.rh.materialdemo.fragment;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import com.rh.materialdemo.R;
import com.rh.materialdemo.Util.HttpUtils;
import com.rh.materialdemo.Util.MyToast;
import com.rh.materialdemo.Util.ParseJsonUtils;
import com.rh.materialdemo.activity.WeatherActivity;
import com.rh.materialdemo.activity.WeatherLocationActivity;
import com.rh.materialdemo.db.City;
import com.rh.materialdemo.db.County;
import com.rh.materialdemo.db.Province;
import org.litepal.crud.DataSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Response;

/**
 *
 * @author RH
 * @date 2017/11/15
 */

public class ChooseAreaFragment extends Fragment {
    private static final String TAG = "ChooseAreaFragment";
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    /**
     * 省列表
     */
    private List<Province> provinceList;
    /**
     * 市列表
     */
    private List<City> cityList;
    /**
     * 县列表
     */
    private List<County> countyList;
    /**
     * 选中的省份
     */
    private Province selectedProvince;
    /**
     * 选中的城市
     */
    private City selectedCity;
    /**
     * 当前选中的级别
     */
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.weather_choose_area, container, false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (currentLevel == LEVEL_PROVINCE) {
                selectedProvince = provinceList.get(position);
                queryCities();
            } else if (currentLevel == LEVEL_CITY) {
                selectedCity = cityList.get(position);
                queryCounties();
            } else if (currentLevel == LEVEL_COUNTY) {
                String weatherId = countyList.get(position).getWeatherId();
                //将weather_id存入SharedPreferences，一遍在WeatherLocationActivity中通过weather_id判断是否获取了地址信息，若是，直接进入WeatherActivity
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                editor.putString("weather_id", weatherId);
                editor.apply();
                //instanceof关键字可以用来判断一个对象是否属于某个类的实例
                if (getActivity() instanceof WeatherLocationActivity) {
                   /* Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    intent.putExtra("weather_id", weatherId);
                    startActivity(intent);*/
                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                    Log.d(TAG, "碎片在WeatherLocationActivity");
                } else if (getActivity() instanceof WeatherActivity) {
                    WeatherActivity weatherActivity = (WeatherActivity) getActivity();
                    weatherActivity.drawerLayout.closeDrawers();
                    weatherActivity.swipeRefreshLayout.setRefreshing(true);
                    weatherActivity.requestWeather(weatherId);
                    Log.d(TAG, "碎片在WeatherActivity");
                }
            }
        });
        backButton.setOnClickListener(v -> {
            if (currentLevel == LEVEL_COUNTY) {
                queryCities();
            } else if (currentLevel == LEVEL_CITY) {
                queryProvinces();
            }
        });
        queryProvinces();
    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                Log.d(TAG, "queryCounties: " + county.toString());
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            Log.d(TAG, "网上抓取县");
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromrServer(address, "county");
        }
    }

    /**
     * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                Log.d(TAG, "queryCities: " + city.toString());
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            Log.d(TAG, "网上抓取市");
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromrServer(address, "city");
        }
    }


    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                Log.d(TAG, "从数据库queryProvinces:"+province);
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            Log.d(TAG, "网上抓取省份");
            String address = "http://guolin.tech/api/china";
            queryFromrServer(address, "province");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据
     *
     **/
    private void queryFromrServer(String address, final String type) {
        showProgressDialog();
        HttpUtils.sendOkHttpRequestWithGET(address, new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d(TAG, "网上获取数据失败");
                getActivity().runOnUiThread(() -> {
                    closeProgressDialog();
                    MyToast.show("加载失败");
                });
            }

            @Override
            public void onResponse(Call call, @NonNull Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    Log.d(TAG, "解析返回的省信息");
                    result = ParseJsonUtils.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    Log.d(TAG, "解析返回的市信息");
                    result = ParseJsonUtils.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    Log.d(TAG, "解析返回的县信息");
                    result = ParseJsonUtils.handleCountyResponse(responseText, selectedCity.getId());
                }
                if (result) {
                    //线程中进行主线程操作
                    getActivity().runOnUiThread(() -> {
                        closeProgressDialog();
                        if ("province".equals(type)) {
                            queryProvinces();
                        } else if ("city".equals(type)) {
                            queryCities();
                        } else if ("county".equals(type)) {
                            queryCounties();
                        }
                    });
                }

            }
        });
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载.....");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }


}
