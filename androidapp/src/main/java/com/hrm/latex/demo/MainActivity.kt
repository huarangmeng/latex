package com.hrm.latex.demo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hrm.latex.App
import com.hrm.latex.base.log.ILogger
import com.hrm.latex.sdk.LatexSDK

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        LatexSDK.initialize(
            config = LatexSDK.Config(
                logger = object : ILogger {
                    override fun v(tag: String, message: String) {
                        Log.v(tag, message)
                    }

                    override fun d(tag: String, message: String) {
                        Log.d(tag, message)
                    }

                    override fun i(tag: String, message: String) {
                        Log.i(tag, message)
                    }

                    override fun w(tag: String, message: String) {
                        Log.w(tag, message)
                    }

                    override fun e(
                        tag: String,
                        message: String,
                        throwable: Throwable?
                    ) {
                        Log.e(tag, message, throwable)
                    }

                }
            ))

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
