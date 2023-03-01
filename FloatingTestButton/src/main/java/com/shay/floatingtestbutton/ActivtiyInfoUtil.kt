package com.shay.floatingtestbutton

import android.app.Activity
import android.app.Application
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import java.lang.reflect.Field
import java.lang.reflect.Method


/**
 * PACK com.shay.floatingtestbutton
 * CREATE BY Shay
 * DATE BY 2023/2/27 18:58 星期一
 * <p>
 * DESCRIBE
 *
 * <p>
 */
// TODO:2023/2/27 
//{@link https://www.freesion.com/article/815824625/}
inline fun getActivitiesByApplication(application: Application): MutableList<Activity?>? {
    var list: MutableList<Activity?>? = ArrayList()
    try {
        val applicationClass = Application::class.java
        val mLoadedApkField: Field = applicationClass.getDeclaredField("mLoadedApk")
        mLoadedApkField.isAccessible = true
        val mLoadedApk: Any = mLoadedApkField.get(application)
        val mLoadedApkClass: Class<*> = mLoadedApk.javaClass
        val mActivityThreadField: Field = mLoadedApkClass.getDeclaredField("mActivityThread")
        mActivityThreadField.isAccessible = true
        val mActivityThread: Any = mActivityThreadField.get(mLoadedApk)
        val mActivityThreadClass: Class<*> = mActivityThread.javaClass
        val mActivitiesField: Field = mActivityThreadClass.getDeclaredField("mActivities")
        mActivitiesField.isAccessible = true
        val mActivities: Any = mActivitiesField.get(mActivityThread)
        // 注意这里一定写成Map，低版本这里用的是HashMap，高版本用的是ArrayMap
        if (mActivities is Map<*, *>) {
            val arrayMap = mActivities as Map<Any, Any>
            for (entry in arrayMap) {
                val activityClientRecordClass: Class<*> = entry.value.javaClass
                val activityField: Field = activityClientRecordClass.getDeclaredField("activity")
                activityField.isAccessible = true
                val o: Any = activityField.get(entry.value)
                list!!.add(o as Activity)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        list = null
    }
    return list
}

inline fun getAnnotationClickMethod(obj:Any, func:(MutableList<Method>)->Unit){
    val methods = obj.javaClass.methods
    val listOfMethod = mutableListOf<Method>()
    for(method in methods){
        if ( null != method.getAnnotation(TestClick::class.java)){
            listOfMethod.add(method)
        }
    }
    func.invoke(listOfMethod)

}