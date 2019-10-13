package com.sisiweather.android;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.sisiweather.android.db.City;
import com.sisiweather.android.db.County;
import com.sisiweather.android.db.Province;
import com.sisiweather.android.util.HttpUtil;
import com.sisiweather.android.util.Utility;

import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.internal.Util;

import static android.content.ContentValues.TAG;

public class ChooseAreaFrament extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView titleText;
    private Button backButton;
    private ListView listView;
    //listview适配器
    private ArrayAdapter<String> adapter;
    //存储信息的可变数组
    private List<String> datalist = new ArrayList<String>();

    /*
    * 省级列表，包括直辖市
    * */
    private List<Province> provincesList;

    /*
    *地级市列表
    * */
    private List<City> cityList;

    /*
    * 县级市列表
    * */
    private List<County> countyList;

    //　选中的省份
    private Province selectedProvince;

    // 选中的地级市
    private City selectedCity;

    // 选中的县级市
    private County seceltedCounty;


    // 当前选中级别
    private int currentLevel;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is optional, and non-graphical fragments can return null. This will be called between
     * {@link #onCreate(Bundle)} and {@link #onActivityCreated(Bundle)}.
     * <p>A default View can be returned by calling {@link #Fragment(int)} in your
     * constructor. Otherwise, this method returns null.
     *
     * <p>It is recommended to <strong>only</strong> inflate the layout in this method and move
     * logic that operates on the returned View to {@link #onViewCreated(View, Bundle)}.
     *
     * <p>If you return a View from here, you will later be called in
     * {@link #onDestroyView} when the view is being released.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment,
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.  The fragment should not add the view itself,
     *                           but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //绑定控件视图
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);

        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,datalist);
        listView.setAdapter(adapter);
        return view;
    }

    /**
     * Called when the fragment's activity has been created and this
     * fragment's view hierarchy instantiated.  It can be used to do final
     * initialization once these pieces are in place, such as retrieving
     * views or restoring state.  It is also useful for fragments that use
     * {@link #setRetainInstance(boolean)} to retain their instance,
     * as this callback tells the fragment when it is fully associated with
     * the new activity instance.  This is called after {@link #onCreateView}
     * and before {@link #onViewStateRestored(Bundle)}.
     *
     * @param savedInstanceState If the fragment is being re-created from
     *                           a previous saved state, this is the state.
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            /**
             * Callback method to be invoked when an item in this AdapterView has
             * been clicked.
             * <p>
             * Implementers can call getItemAtPosition(position) if they need
             * to access the data associated with the selected item.
             *
             * @param parent   The AdapterView where the click happened.
             * @param view     The view within the AdapterView that was clicked (this
             *                 will be a view provided by the adapter)
             * @param position The position of the view in the adapter.
             * @param id       The row id of the item that was clicked.
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVINCE){
                    //如果当前选择的位置是省份、直辖市的级别
                    selectedProvince = provincesList.get(position);
                    //将要向服务器发送的请求，调用后面定义请求的方法
                    queryCities();
                }
                else if(currentLevel == LEVEL_CITY){
                    //如果当前选择的位置是地级市的级别，通过赋值给选中的selectedCity，进行数据查找
                    selectedCity = cityList.get(position);
                    //将要向服务器发送请求。调用后面定义请求的方法
                    queryCounties();

                }

            }
        });
        // 在查看某个页面数据中，用户点击返回按钮点击事件
        backButton.setOnClickListener(new View.OnClickListener(){
            /**
             * Called when a view has been clicked.
             *
             * @param v The view that was clicked.
             */
            @Override
            public void onClick(View v) {
                //如果当前在县级市，则返回地级市级别。
                if(currentLevel == LEVEL_COUNTY){
                  //调用查询地级市的方法
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    //调用查询省份、直辖市级别的查询方法
                    queryProvinces();
                }

            }
        });

        // 进入页面初始化状态，将查询省份、直辖市级别的信息
        queryProvinces();
    }
    /*
    * 查询所有身份、直辖市的信息。
    * 优先从本地数据中查询，本地数据库中没有，则向服务器发送请求
    * */
    private void queryProvinces(){
        currentLevel = LEVEL_PROVINCE;
        //设置当前级别，标题信息
        titleText.setText("中国");
        //省份、直辖市为最大级别，将返回按钮隐藏
        backButton.setVisibility(getView().GONE);

        //从本地数据库中查询所有身份、直辖市的信息.findAll返回值是一个集合
        provincesList = LitePal.findAll(Province.class);
        if(provincesList.size() > 0){
            datalist.clear();
            for(Province province:provincesList){
                datalist.add(province.getProvinceName());

            }
            adapter.notifyDataSetChanged();

            listView.setSelection(0);


        }else{
            Log.d(TAG, "开始查询省级信息");
            String address = "http://guolin.tech/api/china";
            //调用向服务器发起请求方法。向服务器传入地址，并发起请求。
            queryFromServer(address,"province");
        }
    }

    /*
    *根据选中的省份级别，查询地级市级别的数据信息
    * 仍然是先在本地数据库中查询数据，如果不存在数据则向服务器中发起请求，并将结果存储在集合中
    *
    * */
    private void queryCities(){
        currentLevel = LEVEL_CITY;
        //selectedProvince已经在activity创建函数中，赋予了值
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size() > 0){
            datalist.clear();
            for(City city:cityList){
                datalist.add(city.getCityName());

            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);


        }else{

            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            //调用向服务器发起请求方法。向服务器传入地址，并发起请求
            queryFromServer(address,"city");
        }

    }

    /*
    * 根据选中的地级市，查询县级市的信息数据
    * 优先向本地数据库中查询数据，如果不存在，则向服务器发送请求，并将结果存储在集合中
    *
    * */
    private void queryCounties(){
        currentLevel = LEVEL_COUNTY;
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        //查询条件时cityid等于选中地级市id
        countyList = LitePal.where("cityid = ?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size() > 0){
            datalist.clear();
            for(County county:countyList){
                //将数据库中的县级市地名加载到集合当中
                datalist.add(county.getCountyName());

            }
            // activity页面不变，listView发生变换，及更新listView的显示
            adapter.notifyDataSetChanged();
            // 将上一级选中position清空为0
            listView.setSelection(0);

        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            //http请求地址
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" +cityCode;
            //发起请求
            queryFromServer(address,"county");

        }

    }

    /*
    * 根据传入的地址和服务类型从服务器上查询省市县的数据
    * */
    private void queryFromServer(String address,final String type){
        //开启对话进度条
        showProgressDialog();
        HttpUtil.sendOkhttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());

                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //关闭对话进度条
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();

                            }else if("city".equals(type)){
                                queryCities();

                            }else if("county".equals(type)){
                                queryCounties();

                            }
                        }
                    });

                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
                //通过runUitThread()方法回到线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }


        });

    }

    /*
    * 显示对话进度条
    * */
    private void showProgressDialog(){
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载···");
            progressDialog.setCanceledOnTouchOutside(false);

        }
        progressDialog.show();
    }

    /*
    * 关闭进度对话框
    * */

    private void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();

        }

    }
}
