package com.chaunmi.eventbus.core

import androidx.lifecycle.*
import com.chaunmi.eventbus.util.ILogger
import com.chaunmi.eventbus.util.LogUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap

object FlowEventBus {
    private val busMap = mutableMapOf<String, EventBus<*>>()
    private val busStickMap = mutableMapOf<String, StickEventBus<*>>()

    fun init(logger: ILogger? = null) {
        LogUtils.setLogger(logger)
    }

    @Synchronized
    fun <T> with(key: String): EventBus<T> {
        var eventBus = busMap[key]
        if (eventBus == null) {
            eventBus = EventBus<T>(key)
            LogUtils.d(" new Event Bus $key, $eventBus")
            busMap[key] = eventBus
        }
        return eventBus as EventBus<T>
    }

    @Synchronized
    fun <T> withSticky(key: String): StickEventBus<T> {
        var eventBus = busStickMap[key]
        if (eventBus == null) {
            eventBus = StickEventBus<T>(key)
            LogUtils.d(" new Event Bus $key, $eventBus")
            busStickMap[key] = eventBus
        }
        return eventBus as StickEventBus<T>
    }

    @Synchronized
    fun clear() {
        for(bus in busMap.values) {
            bus.clearEvents()
        }
        busMap.clear()
        for(busSticky in busStickMap.values) {
            busSticky.clearEvents()
        }
        busStickMap.clear()
    }

    //真正实现类
    open class EventBus<T>(private val key: String) : LifecycleEventObserver, ViewModel() {
        //私有对象用于发送消息
        private val events: MutableSharedFlow<T> by lazy {
            obtainEvent()
        }

        @Volatile
        private var hasStartListenSubscriptionCount = false
        private fun startListenSubscriptionCount() {
            if(hasStartListenSubscriptionCount) {
                return
            }
            hasStartListenSubscriptionCount = true
            viewModelScope.launch {
                events.subscriptionCount.collect {
                    LogUtils.d(" $key subscriptionCount changed, $it")
                    if(it <= 0) {
                        checkRemoveBus()
                        if(isActive) {
                            this.cancel()
                        }
                    }
                }
            }
            LogUtils.d(" startListenSubscriptionCount Event Bus $key, $this")
        }

        private val jobCacheMap = ConcurrentHashMap<EventObserver<T>, Job>()

        protected open fun obtainEvent(): MutableSharedFlow<T> = MutableSharedFlow(0, Int.MAX_VALUE)

        protected open fun removeEventBus(key: String = this.key) = busMap.remove(key)

        /**
         * 带生命周期自动解除监听
         * @param lifecycleOwner LifecycleOwner
         * @param eventObserver EventObserver<T>
         * @param dispatcher CoroutineDispatcher?
         */
        fun register(lifecycleOwner: LifecycleOwner, eventObserver: EventObserver<T>, dispatcher: CoroutineDispatcher? = null) {
            lifecycleOwner.lifecycle.addObserver(this)
            val scope = lifecycleOwner.lifecycleScope
            if(dispatcher != null) {
                scope.launch(dispatcher) {
                   observerOnChanged(eventObserver)
                }
            }else {
                scope.launch() {
                    observerOnChanged(eventObserver)
                }
            }
        }

        fun clearEvents() {
            if(viewModelScope.isActive) {
                if(viewModelScope is Closeable) {
                    (viewModelScope as Closeable).close()
                }
            }
            jobCacheMap.clear()
        }

        private suspend fun observerOnChanged(eventObserver: EventObserver<T>) {
            events.collect {
                try {
                    LogUtils.d(" onChanged thread:  ${Thread.currentThread().name}")
                    eventObserver.onChanged(it)
                    startListenSubscriptionCount()
                } catch (e: Exception) {
                    LogUtils.e(" onChanged error ", e)
                    e.printStackTrace()
                }
            }
        }

        /**
         * 调用此函数后，一定要调用unregister，否则可能会有泄露
         * @param eventObserver EventObserver<T>
         * @param coroutineScope CoroutineScope?
         * @param dispatcher CoroutineDispatcher?
         * @return Job
         */
        fun registerForever(eventObserver: EventObserver<T>, coroutineScope: CoroutineScope? = null, dispatcher: CoroutineDispatcher? = null): Job {
            var job = jobCacheMap[eventObserver]
            if(job != null) {
                return job
            }
            val scope = (coroutineScope ?: viewModelScope)
            job = if(dispatcher != null) {
                scope.launch(dispatcher) {
                    observerOnChanged(eventObserver)
                }
            }else {
                scope.launch {
                    observerOnChanged(eventObserver)
                }
            }
            jobCacheMap[eventObserver] = job
            return job
        }

        fun unregister(eventObserver: EventObserver<T>) {
            jobCacheMap[eventObserver]?.apply {
                if(isActive) {
                    cancel()
                }
                jobCacheMap.remove(eventObserver)
            }
            checkRemoveBus()
        }

        private fun checkRemoveBus() {
            val subscriptCount = events.subscriptionCount.value
            if (subscriptCount <= 0) {
                clearEvents()
                removeEventBus()
                LogUtils.d(" remove Event Bus $key, ${this@EventBus}")
            }
        }

        /**
         * 默认主线程中发送数据
         * @param event T
         * @param scope CoroutineScope?
         * @return Job
         */
        fun post(event: T, scope: CoroutineScope? = null): Job {
          return scope?.launch {
              events.emit(event)
          } ?: viewModelScope.launch {
                  events.emit(event)
              }
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            LogUtils.d(" onStateChanged $event")
            if(event == Lifecycle.Event.ON_DESTROY) {
                val subscriptCount = events.subscriptionCount.value
                LogUtils.d(" onStateChanged onDestroy $subscriptCount")
                checkRemoveBus()
            }
        }
    }

    class StickEventBus<T>(key: String) : EventBus<T>(key) {
        override fun obtainEvent(): MutableSharedFlow<T> = MutableSharedFlow(1, Int.MAX_VALUE)

        override fun removeEventBus(key: String): EventBus<*>? {
            return busStickMap.remove(key)
        }
    }

    interface EventObserver<T> {
        /**
         * Called when the data is changed.
         * @param t  The new data
         */
        fun onChanged(t: T)
    }

}