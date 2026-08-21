package com.example.nativeapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    // Native C++ JNI bindings
    external fun stringFromJNI(): String
    external fun calculateFactorial(n: Int): Int

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvNativeMsg = findViewById<TextView>(R.id.tvNativeMsg)
        val btnCompute = findViewById<Button>(R.id.btnCompute)

        tvTitle.text = "NativeCppApp (NDK + C++)"
        tvNativeMsg.text = stringFromJNI()

        btnCompute.setOnClickListener {
            val fact = calculateFactorial(6)
            Toast.makeText(this, "Native C++ 6! = $fact", Toast.LENGTH_LONG).show()
        }
    }
}