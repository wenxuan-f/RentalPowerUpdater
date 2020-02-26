package com.wwg;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtil {
    // Utility function: return last month's first day
    public static String lastMonthDayOne(){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, -1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(c.getTime());
    }

    // Utility function: return this month's first day
    public static String thisMonthDayOne() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(c.getTime());
    }

    // Utility function: return next month's first day
    public static String nextMonthDayOne() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, 1);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(c.getTime());
    }

    /**
     * Helper function: convert string date to Date type
     */
    public static Date stringToDate(String s){
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(s);
        } catch (ParseException pe){
            pe.printStackTrace();
        }
        return null;
    }

    /**
     * Helper function : convert Date type to string
     */
    public static String dateToString(Date d){
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(d);
    }

    /**
     * Helper function: add x months to date
     */
    public static Date addMonth(Date date, int x){
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MONTH, x);
        return c.getTime();
    }

}
