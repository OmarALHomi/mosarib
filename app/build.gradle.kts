import java.util.Properties
import java.io.FileInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.baynana"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    // هوية التطبيق المعتمدة: «بيننا». لا تثبيت سابق لأي مستخدم (قرار المالك 2026-10-03)،
    // فالتغيير الآن لا يكسر مسار تحديث أحد. بعد أول توزيع حقيقي: لا يُغيَّر هذا السطر أبدًا.
    applicationId = "com.baynana.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Release signing is read from keystore.properties or environment variables.
  val keystorePropertiesFile = rootProject.file("keystore.properties")
  val keystoreProperties = Properties()
  if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
  }

  val releaseKeystoreFile = if (keystoreProperties.containsKey("storeFile")) {
    rootProject.file(keystoreProperties.getProperty("storeFile"))
  } else {
    file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
  }

  val releaseStorePassword = keystoreProperties.getProperty("storePassword") ?: System.getenv("STORE_PASSWORD")
  val releaseKeyAlias = keystoreProperties.getProperty("keyAlias") ?: (System.getenv("KEY_ALIAS") ?: "mosarib_key")
  val releaseKeyPassword = keystoreProperties.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD")

  val hasReleaseKeystore = releaseKeystoreFile.exists() &&
      !releaseStorePassword.isNullOrBlank() &&
      !releaseKeyPassword.isNullOrBlank()

  signingConfigs {
    if (hasReleaseKeystore) {
      create("release") {
        storeFile = releaseKeystoreFile
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
        enableV1Signing = true
        enableV2Signing = true
      }
    }
    val debugKeystore = file("${rootDir}/debug.keystore")
    if (debugKeystore.exists()) {
      create("debugConfig") {
        storeFile = debugKeystore
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      // Shrinking and minification are enabled so the APK only ships the code that is actually
      // reachable; the required keep rules live in app/proguard-rules.pro.
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // RELEASE-SIGNING-GUARD: ممنوع التوقيع بمفتاح التصحيح في نسخة الجمهور.
      // عند غياب مفتاح الإصدار تُبنى الحزمة غير موقّعة (لا يمكن نشرها ولا تثبيتها كتحديث)،
      // ولا يوجد أي مسار احتياطي إلى debug كما كان سابقًا.
      signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else null
    }
    debug {
      signingConfig = signingConfigs.findByName("debugConfig") ?: signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  // مخططات Room المصدَّرة إلى app/schemas تُستخدم كأصول في اختبارات الأجهزة (androidTest).
  // اختبارات الوحدة تقرأ الملفات من مسار المشروع مباشرة: AGP لا يدمج assets في اختبارات الوحدة.
  sourceSets {
    getByName("androidTest").assets.srcDir("$projectDir/schemas")
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  lint {
    // بوابة v4 §14: فشل lint يمنع الإصدار، ولا يُتجاهل.
    checkReleaseBuilds = true
    abortOnError = true
  }
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
  arg("room.incremental", "true")
}

// Every dependency below is actually referenced by the source code. The previous template
// declared a large set of unused libraries (Firebase/Gemini, Retrofit + OkHttp + Moshi, Coil,
// Navigation, DataStore, CameraX, Play Services Location ...). They only bloated the APK and
// made the build depend on a `.env` file and a `google-services.json` that were never present,
// so they were removed together with the secrets/google-services Gradle plugins.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.play.services.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.googleid)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
