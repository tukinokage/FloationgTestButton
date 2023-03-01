package com.shay.floatingtestbutton

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class MainActivity3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)
    }
    @TestClick
    fun change(){
        findViewById<TextView>(R.id.textView).text = "110"
    }
}