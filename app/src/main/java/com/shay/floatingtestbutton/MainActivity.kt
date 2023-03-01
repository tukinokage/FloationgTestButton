package com.shay.floatingtestbutton

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    var time = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

         findViewById<TextView>(R.id.text_view).setOnClickListener {
            startActivity(Intent(this, MainActivity3::class.java))
        }
    }


    @TestClick
    fun click(){
        findViewById<TextView>(R.id.text_view).text = time++.toString()
    }


}