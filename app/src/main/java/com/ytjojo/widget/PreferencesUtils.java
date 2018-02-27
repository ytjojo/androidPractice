package com.ytjojo.widget;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.ytjojo.utils.ApplicationUtils;
import com.ytjojo.utils.ReflectUtils;

/**
 * Created by Administrator on 2018/2/9 0009.
 */

class PreferencesUtils {
	static Application sApplication;
	public static void saveInt(String translucentState, int changeStateFail) {
		if(sApplication == null){
			try {
				sApplication = ApplicationUtils.getApplication();
			} catch (ReflectUtils.ReflectException e) {
				e.printStackTrace();
			}
		}
		SharedPreferences sp =sApplication.getSharedPreferences(translucentState, Context.MODE_PRIVATE);
		sp.edit().putInt(translucentState,changeStateFail).apply();
	}

	public static int getInt(String translucentState, int init) {
		if(sApplication == null){
			try {
				sApplication = ApplicationUtils.getApplication();
			} catch (ReflectUtils.ReflectException e) {
				e.printStackTrace();
			}
		}
		SharedPreferences sp =sApplication.getSharedPreferences(translucentState, Context.MODE_PRIVATE);
		return sp.getInt(translucentState,init);

	}
}
