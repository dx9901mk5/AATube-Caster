plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.aayt.caster"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aayt.caster"
    minSdk = 28
    targetSdk = 36
    versionCode = 5
    versionName = "1.3.1"
  }

  signingConfigs {
    create("aatube") {
      storeFile = file("signing/aatube.p12")
      storePassword = "aatube2026"
      keyAlias = "aatube"
      keyPassword = "aatube2026"
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("aatube")
    }
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("aatube")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    isCoreLibraryDesugaringEnabled = true
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "META-INF/DEPENDENCIES"
    }
  }

  testOptions {
    unitTests.all {
      it.jvmArgs("-Dfile.encoding=UTF-8", "-Dsun.jnu.encoding=UTF-8")
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  coreLibraryDesugaring(libs.desugar.jdk.libs.nio)

  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.process)
  implementation(libs.androidx.savedstate.ktx)
  implementation(libs.androidx.activity.compose)

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  debugImplementation(libs.androidx.compose.ui.tooling)

  // Android Auto / Car App Library
  implementation(libs.androidx.car.app)
  implementation(libs.androidx.car.app.projected)

  // Media3 ExoPlayer & MediaSession
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.exoplayer.dash)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.session)

  // Ad-free YouTube Extractor, Network & Images
  implementation(libs.newpipe.extractor)
  implementation(libs.okhttp)
  implementation(libs.coil.compose)

  // Coroutines & JSON Serialization
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)

  // Shizuku (Built-in Rootless Shell Bridge for AA Recognition & OTA)
  implementation(libs.shizuku.api)
  implementation(libs.shizuku.provider)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
