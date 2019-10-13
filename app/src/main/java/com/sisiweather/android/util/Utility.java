package com.sisiweather.android.util;

import android.text.TextUtils;
import android.util.Log;

import com.sisiweather.android.db.City;
import com.sisiweather.android.db.County;
import com.sisiweather.android.db.Province;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import static android.content.ContentValues.TAG;

/*
* 省份信息处理
* 在这里对服务器返回的JSON格式数据进行解析，并存储到可变数组中
* */
public class Utility {
    public static boolean handleProvinceResponse(String response){
        Log.d(TAG, "开始处理省级信息");
        if(!TextUtils.isEmpty(response)){
            try{
                JSONArray allProvinces = new JSONArray(response);
                for(int i = 0;i<allProvinces.length();i++){
                    JSONObject provinceObjects = allProvinces.getJSONObject(i);

                    Province province = new Province();
                    province.setProvinceName(provinceObjects.getString("name"));
                    province.setProvinceCode(provinceObjects.getInt("id"));
                    province.save();

                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();

            }

        }

        return false;
    }
/*
* 地级市信息处理
* 对服务器返回的JSON格式的信息进行解析，并存储到可变数组中
* */

    public static boolean handleCityResponse(String response,int provinceId){
        if(!TextUtils.isEmpty(response)){
            try{
                JSONArray allCites = new JSONArray(response);
                for (int i = 0;i<allCites.length();i++){

                    JSONObject cityObjects = allCites.getJSONObject(i);
                    City city = new City();
                    city.setCityName(cityObjects.getString("name"));
                    city.setCityCode(cityObjects.getInt("id"));
                    city.setProvinceId(provinceId);
                    city.save();

                }
                return true;
            }catch (JSONException e){
             e.printStackTrace();
            }

        }

        return false;
    }

    /*
    * 县级市信息处理
    * 对服务器返回的JSON格式的信息进行解析，并存在可变数组中
    * */

    public static boolean handleCountyResponse(String response,int cityId){
        if(!TextUtils.isEmpty(response)){
            try {
                JSONArray allCounties = new JSONArray(response);
                for (int i = 0; i < allCounties.length(); i++) {
                    //在这里
                    JSONObject countyObject = allCounties.getJSONObject(i);
                    County county = new County();
                    county.setCountyName(countyObject.getString("name"));
                    county.setWeatherId(countyObject.getString("weather_id"));
                    county.setCityId(cityId);
                    //Litepal中的方法进行对象存储
                    county.save();

                }
                return true;
            }catch (JSONException e){
                e.printStackTrace();

            }
        }
        return false;
    }
}
