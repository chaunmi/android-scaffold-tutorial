object AndroidX {
    const val coreKtx = "androidx.core:core-ktx:1.7.0"
    const val appcompat = "androidx.appcompat:appcompat:1.3.0"
    const val multidex = "androidx.multidex:multidex:2.0.1"
    const val constraintlayout = "androidx.constraintlayout:constraintlayout:2.1.3"
    const val material = "com.google.android.material:material:1.5.0"


    val lifecycleKtx = Lifecycle
    object Lifecycle {
        private val lifecycle_version = "2.5.1"
        val viewmodel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version"
        val livedata = "androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_version"
        // Lifecycles only (without ViewModel or LiveData)
        val runtime = "androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version"
        // Saved state module for ViewModel
        val savedstate = "androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycle_version"

        // Annotation processor
        val compiler =  "androidx.lifecycle:lifecycle-compiler:$lifecycle_version"
        // alternately - if using Java8, use the following instead of lifecycle-compiler
        val commonJava8 =  "androidx.lifecycle:lifecycle-common-java8:$lifecycle_version"

        // optional - helpers for implementing LifecycleOwner in a Service
        val service = "androidx.lifecycle:lifecycle-service:$lifecycle_version"

        // optional - ProcessLifecycleOwner provides a lifecycle for the whole application process
        val process = "androidx.lifecycle:lifecycle-process:$lifecycle_version"

        // optional - ReactiveStreams support for LiveData
        val reactivestreams = "androidx.lifecycle:lifecycle-reactivestreams:$lifecycle_version"
    }
}