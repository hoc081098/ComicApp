plugins {
  `comic-app-plugin`
  id("com.android.application")
  id("kotlin-kapt")
}

comicApp {
  viewBinding = true
  parcelize = true
}

dependencies {
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
  implementation(project(deps.module.baseUi))
  implementation(project(deps.module.utils))
  implementation(project(deps.module.navigation))

  implementation(deps.kotlin.stdlib)
  val coroutineDepConfig: (ExternalModuleDependency).() -> Unit = {
    version {
      strictly(versions.kotlin.coroutines)
    }
  }
  implementation(deps.kotlin.coroutinesCore, coroutineDepConfig)
  implementation(deps.kotlin.coroutinesAndroid, coroutineDepConfig)
  implementation(deps.kotlin.coroutinesRx3, coroutineDepConfig)
  implementation(deps.kotlin.coroutinesPlayServices, coroutineDepConfig)

  implementation(deps.androidX.activity)
  implementation(deps.androidX.appCompat)
  implementation(deps.androidX.core)
  implementation(deps.androidX.fragment)
  implementation(deps.androidX.startUp)

  implementation(deps.androidX.view.constraintLayout)
  implementation(deps.androidX.view.recyclerView)
  implementation(deps.androidX.view.material)

  implementation(deps.androidX.navigation.fragment)
  implementation(deps.androidX.navigation.ui)

  implementation(deps.androidX.lifecycle.viewModel)
  implementation(deps.androidX.lifecycle.liveData)
  implementation(deps.androidX.lifecycle.reactiveStreams)
  implementation(deps.androidX.lifecycle.common)

  implementation(deps.androidX.room.runtime)
  kapt(deps.androidX.room.compiler)
  implementation(deps.androidX.room.ktx)
  implementation(deps.androidX.room.rxJava3)

  implementation(deps.androidX.work.runtimeKtx)

  implementation(deps.koin.android)

  implementation(deps.moshiKotlin)
  implementation(deps.okHttpLoggingInterceptor)
  implementation(deps.retrofit.retrofit)
  implementation(deps.retrofit.converterMoshi)

  debugImplementation(deps.leakCanaryAndroid)
  implementation(deps.timber)

  implementation(deps.rxBinding.platform)
  implementation(deps.rxBinding.core)
  implementation(deps.rxBinding.material)
  implementation(deps.rxBinding.swipeRefreshLayout)
  implementation(deps.rxBinding.recyclerView)

  implementation(deps.reactiveX.kotlin)
  implementation(deps.reactiveX.java)
  implementation(deps.reactiveX.android)
  implementation(deps.rxRelay)

  implementation(deps.glide.glide)
  kapt(deps.glide.compiler)
  implementation(deps.glide.integration) { exclude(group = "glide-parent") }

  implementation(deps.customView.materialSearchView)
  implementation(deps.customView.expandableTextView)
  implementation(deps.customView.materialSpinner)
  implementation(deps.customView.circularProgressIndicator)
  implementation(deps.customView.photoView)
  implementation(deps.customView.swipeRevealLayout)
  implementation(deps.customView.circleImageView)
  implementation(deps.customView.textDrawable)

  testImplementation("junit:junit:4.13")
  androidTestImplementation("androidx.test:runner:1.3.0")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")

  implementation(deps.firebase.auth)
  implementation(deps.firebase.storage)
  implementation(deps.firebase.firestore)
  implementation(deps.firebase.analytics)
  implementation(deps.firebase.crashlytics)

  implementation(deps.arrow.core)
  implementation(deps.arrow.fxCoroutines)

  implementation(deps.listenableFuture)
  implementation(deps.viewBindingDelegate)
}

apply {
  plugin("com.google.gms.google-services")
  plugin("com.google.firebase.crashlytics")
}
