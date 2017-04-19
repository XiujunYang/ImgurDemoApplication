package com.example.mobilabassignment;

import android.util.Log;

/**
 * Created by Jean on 2017/4/18.
 */

public class MyLog {

    public static void i(String str){
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Log.i(stackTraceElements[3].getClassName()+"."+stackTraceElements[3].getMethodName(),
                "["+Thread.currentThread().getId()+"] "+str);
    }

    public static void d(String str){
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Log.d(stackTraceElements[3].getClassName()+"."+stackTraceElements[3].getMethodName(),
                "["+Thread.currentThread().getId()+"] "+str);
    }

    public static void e(String str){
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Log.e(stackTraceElements[3].getClassName()+"."+stackTraceElements[3].getMethodName(),
                "["+Thread.currentThread().getId()+"] "+str);
    }
}
