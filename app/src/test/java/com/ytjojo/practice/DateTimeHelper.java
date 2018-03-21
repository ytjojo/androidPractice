package com.ytjojo.practice;

import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Administrator on 2018/3/20 0020.
 */

public class DateTimeHelper {
    public final static String sMdFormat = "M月d日";
    public static final String[] sWeekOfDays = {"星期日", "星期一", "星期二", "星期三",
            "星期四", "星期五", "星期六"};
    public static final String[] s_weekOfDays_2={"周日","周一","周二","周三","周四","周五","周六"};
    public final static String YYYYnianMMyueDDHHMM = "yyyy年MM月dd日 HH:mm";
    public final static String MD_HHMM = "M/d HH:mm";
    public final static String MMyueDD = "MM月dd日";
    public final static String yyyyMMdd = "yyyy-MM-dd";
    public final static String YYYMMDDHHMM = "yyyy-MM-dd HH:mm";
    public final static String YYMMDDHHMMSS = "yyyy-MM-dd HH:mm:ss";
    public final static String ENGLISH_MMMddYYYY= "MMM dd, yyyy";//Apr 25, 2016
    public final static String yyMMdd = "yy-MM-dd";
    public final static String YYMMDDHHMM = "yyyy-MM-dd HH:mm";
    public final static String ORMAT_kkmm = "kk:mm";
    public final static String FORMAT_HHmm = "HH:mm";
    public final static String FORMAT_yesterday = "昨天";
    public final static String YYYYnianMMyueDD = "yyyy年MM月dd日";

    /**
     * 返回Date类型
     * @param dateStr 支持三种格式的输入字符串
     * @return Date值
     */
    public static Date getDateFromString(String dateStr) {
        if (TextUtils.isEmpty(dateStr)) {
            return null;
        }
        final String trimStr = dateStr.trim();
        SimpleDateFormat df = null;
        switch (trimStr.length()) {
            case 19:
                df = new SimpleDateFormat(YYMMDDHHMMSS);
                try {
                    return df.parse(trimStr);

                } catch (ParseException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;

            case 16:
                df = new SimpleDateFormat(YYYMMDDHHMM);
                try {
                    return df.parse(trimStr);

                } catch (ParseException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            case 10:
                df = new SimpleDateFormat(yyyyMMdd);
                try {
                    return df.parse(trimStr);

                } catch (ParseException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            case 12:
                df = new SimpleDateFormat(ENGLISH_MMMddYYYY, Locale.ENGLISH);
                try {
                    return df.parse(trimStr);

                } catch (ParseException e) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            default:
                return null;
        }
    }

}
