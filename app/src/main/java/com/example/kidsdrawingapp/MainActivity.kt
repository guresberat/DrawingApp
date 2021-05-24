package com.example.kidsdrawingapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val drawing_view = findViewById<DrawingView>(R.id.drawing_view)

        drawing_view.setSizeForBrush(20.toFloat())

    }
}