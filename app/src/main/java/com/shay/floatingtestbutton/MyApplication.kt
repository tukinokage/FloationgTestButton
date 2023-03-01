package com.shay.floatingtestbutton

import android.app.Application
import android.content.Context

/**
 * PACK com.masadora.testapplication
 * CREATE BY Shay
 * DATE BY 2023/2/24 18:19 星期五
 * <p>
 * DESCRIBE
 * <p>
 */
// TODO:2023/2/24 

class MyApplication : Application() {

    val instance:Context by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED){
        this
    }
    override fun onCreate() {
        super.onCreate()
        GlobalFloatButton.init(this)
    }
}