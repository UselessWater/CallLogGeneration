import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 默认使用仓库里的版本号；打 tag 发布时由 CI 通过 -PversionName/-PversionCode 覆盖。
val appVersionName = providers.gradleProperty("versionName").orNull
    ?: providers.environmentVariable("VERSION_NAME").orNull
    ?: "3.0.0"
val appVersionCode = (providers.gradleProperty("versionCode").orNull
    ?: providers.environmentVariable("VERSION_CODE").orNull)?.toIntOrNull()
    ?: 3

// 签名信息优先读环境变量（CI），其次读项目根目录的 keystore.properties（本地）。
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

fun signingValue(environmentName: String, propertyName: String): String? =
    providers.environmentVariable(environmentName).orNull?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("RELEASE_KEYSTORE_FILE", "storeFile")
    ?.let { rootProject.file(it) }
    ?.takeIf { it.isFile }
val releaseStorePassword = signingValue("RELEASE_STORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingValue("RELEASE_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingValue("RELEASE_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning = releaseStoreFile != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

if (signingValue("RELEASE_KEYSTORE_FILE", "storeFile") != null && !hasReleaseSigning) {
    logger.warn("检测到签名配置，但 storePassword/keyAlias/keyPassword 不完整，release 构建将不签名。")
}

android {
    namespace = "com.uselesswater.multicallloggeneration"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.uselesswater.multicallloggeneration"
        minSdk = 22
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    // 使用最简单且兼容的方法
    applicationVariants.all {
        val variant = this
        val buildType = variant.buildType.name
        val versionName = variant.versionName
        val appName = "CallLogGeneration"

        // 获取当前时间
        val currentTime = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())

        // 设置输出文件名 - 使用正确的方法
        variant.outputs.all {
            //如果为正式发行版，则命名为appName-v版本号.apk
            if (buildType == "release") {
                val fileName = "${appName}-v${versionName}.apk"
                (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = fileName
            }else{
                //如果为测试版，则命名为appName_buildType_v版本号_时间.apk
                val fileName = "${appName}_${buildType}_v${versionName}_${currentTime}.apk"
                (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = fileName
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.threetenabp)

    // 网络请求依赖
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.junit.ktx)

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}