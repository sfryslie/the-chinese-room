package com.sfryslie.chineseroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets

class MainActivity : ComponentActivity() {

    private val httpClient = HttpClient(OkHttp) {
        install(WebSockets)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                App(httpClient)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        httpClient.close()
    }
}
