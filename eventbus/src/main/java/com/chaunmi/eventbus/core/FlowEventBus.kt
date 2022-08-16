package com.chaunmi.eventbus.core

import androidx.lifecycle.*
import com.chaunmi.eventbus.util.ILogger
import com.chaunmi.eventbus.util.LogUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object FlowEventBus {
    private val busMap = mutableMapOf<String, EventBus<*>>()
    private val busStickMap = mutableMapOf<String, StickyEventBus<*>>()

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
    fun <T> withSticky(key: String): StickyEventBus<T> {
        var eventBus = busStickMap[key]
        if (eventBus == null) {
            eventBus = StickyEventBus<T>(key)
            LogUtils.d(" new Event Bus $key, $eventBus")
            busStickMap[key] = eventBus
        }
        return eventBus as StickyEventBus<T>
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
                        if(checkRemoveBus() && isActive) {
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

        private var lifecycleOwnerCount =  AtomicInteger(0)
        /**
         * 带生命周期自动解除监听
         * @param lifecycleOwner LifecycleOwner
         * @param eventObserver EventObserver<T>
         * @param dispatcher CoroutineDispatcher?
         * @param minState  Lifecycle.State? 什么时候开始更新值
         */
        fun observer(lifecycleOwner: LifecycleOwner, eventObserver: EventObserver<T>,
                     dispatcher: CoroutineDispatcher? = null, minState: Lifecycle.State? = null) {
            lifecycleOwner.lifecycle.addObserver(this)
            val count = lifecycleOwnerCount.incrementAndGet()
            LogUtils.d(" observer lifecycleOwnerCount: $count")
            val scope = lifecycleOwner.lifecycleScope
            if(dispatcher != null) {
                scope.launch(dispatcher) {
                    if(minState != null) {
                        lifecycleOwner.repeatOnLifecycle(minState) {
                            observerOnChanged(eventObserver)
                        }
                    }else {
                        observerOnChanged(eventObserver)
                    }
                }
            }else {
                scope.launch {
                    if(minState != null) {
                        lifecycleOwner.repeatOnLifecycle(minState) {
                            observerOnChanged(eventObserver)
                        }
                    }else {
                        observerOnChanged(eventObserver)
                    }
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
                    LogUtils.d(" onChanged thread:  ${Thread.currentThread().name}, $it")
                    eventObserver.onChanged(it)
                    startListenSubscriptionCount()
                } catch (e: Exception) {
                    LogUtils.e(" onChanged error ", e)
                    e.printStackTrace()
                }
            }
        }

        /**
         * 调用此函数后，一定要调用removeObserver，否则可能会有泄露
         * @param eventObserver EventObserver<T>
         * @param dispatcher CoroutineDispatcher?
         * @return Job
         */
        fun observerForever(eventObserver: EventObserver<T>, dispatcher: CoroutineDispatcher? = null): Job {
            var job = jobCacheMap[eventObserver]
            if(job != null) {
                return job
            }
            job = if(dispatcher != null) {
                viewModelScope.launch(dispatcher) {
                    observerOnChanged(eventObserver)
                }
            }else {
                viewModelScope.launch {
                    observerOnChanged(eventObserver)
                }
            }
            jobCacheMap[eventObserver] = job
            return job
        }

        fun removeObserver(eventObserver: EventObserver<T>) {
            jobCacheMap[eventObserver]?.apply {
                if(isActive) {
                    cancel()
                }
                jobCacheMap.remove(eventObserver)
            }
            checkRemoveBus()
        }

        private fun checkRemoveBus(): Boolean {
            val subscriptCount = events.subscriptionCount.value
            if (subscriptCount <= 0 && lifecycleOwnerCount.get() == 0) {
                clearEvents()
                removeEventBus()
                LogUtils.d(" remove Event Bus $key, ${this@EventBus}")
                return true
            }
            return false
        }

        /**
         * 默认主线程中发送数据
         * @param event T
         * @return Job
         */
        fun post(event: T, delayMillis: Long = 0): Job {
          return viewModelScope.launch {
              if(delayMillis > 0) {
                  delay(delayMillis)
              }
              events.emit(event)
          }
        }

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            LogUtils.d(" onStateChanged $event")
            if(event == Lifecycle.Event.ON_DESTROY) {
                val subscriptCount = events.subscriptionCount.value
                val count = lifecycleOwnerCount.decrementAndGet()
                LogUtils.d(" onStateChanged onDestroy subscriptCount: $subscriptCount, lifecycleOwnerCount: $count")
                checkRemoveBus()
            }
        }
    }

    class StickyEventBus<T>(key: String) : EventBus<T>(key) {
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