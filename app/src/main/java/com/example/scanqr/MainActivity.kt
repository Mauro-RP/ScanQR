package com.example.scanqr

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import android.widget.TextView


private const val REQUEST_CODE_SCAN = 123

class MainActivity : AppCompatActivity() {

    private lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultTextView = findViewById(R.id.resultTextView)
    }

    fun startScan(view: android.view.View) {
        val integrator = IntentIntegrator(this)
        integrator.setOrientationLocked(false)
        integrator.setPrompt("Escanea un código QR")
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result: IntentResult? = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                resultTextView.text = "Escaneo cancelado"
            } else {
                resultTextView.text = "Resultado: ${result.contents}"
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
