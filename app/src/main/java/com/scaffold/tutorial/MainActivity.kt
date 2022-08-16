package com.scaffold.tutorial

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import com.chaunmi.eventbus.core.FlowEventBus
import com.chaunmi.eventbus.util.LogUtils
import com.scaffold.tutorial.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    val EVENT_KEY = "event_key"
    var count = 0
    lateinit var eventObserver: FlowEventBus.EventObserver<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        eventObserver = object : FlowEventBus.EventObserver<String> {
            override fun onChanged(t: String) {
                runOnUiThread {
                    binding.collectText.text = t
                }
            }
        }

        FlowEventBus.withSticky<String>(EVENT_KEY).post(" change txt ${count++}")

        FlowEventBus.withSticky<String>(EVENT_KEY).observerForever(eventObserver, dispatcher = Dispatchers.IO)

        binding.startCollect.setOnClickListener {
            FlowEventBus.withSticky<String>(EVENT_KEY).post(" change txt ${count++}")
        }

        binding.stopCollect.setOnClickListener {
            FlowEventBus.withSticky<String>(EVENT_KEY).removeObserver(eventObserver)
        }
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onBackPressed() {
        finish()
    }
}