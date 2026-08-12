import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val versionProps: Provider<Properties> = provider {
    val props = Properties()
    val f = rootProject.file("version.properties")
    if (f.exists()) {
        f.inputStream().use { props.load(it) }
    }
    props
}

android {
    namespace = "com.fitnessquest.rpg.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.fitnessquest.rpg"
        minSdk = 30
        targetSdk = 36
        versionCode = versionProps.get().getProperty("wearVersionCode", "21").toInt()
        versionName = versionProps.get().getProperty("versionName", "1.0.3")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.versionCode.set(versionProps.map { it.getProperty("wearVersionCode", "50").toInt() })
            output.versionName.set(versionProps.map { it.getProperty("versionName", "1.0.3") })
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.health.services)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.guava)
    implementation(libs.androidx.concurrent.futures)
    debugImplementation(libs.androidx.ui.tooling)
}
