package com.daylight.arccarema;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtil {
    public static void setIpAddress(Context context, String account){
        SharedPreferences.Editor spe=context.getSharedPreferences("ip",Context.MODE_PRIVATE).edit();
        spe.putString("address",account);
        spe.apply();
    }
    public static String getIpAddress(Context context){
        SharedPreferences sp=context.getSharedPreferences("ip",Context.MODE_PRIVATE);
        return sp.getString("address","");
    }
}
