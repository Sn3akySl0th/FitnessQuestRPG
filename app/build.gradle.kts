import com.android.build.api.variant.VariantOutput
import org.gradle.api.provider.Provider
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
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

val generatedWearAssetsDir = layout.projectDirectory.dir("src/main/assets/wear")

android {
    namespace = "com.fitnessquest.rpg"
    compileSdk = 37
    assetPacks += listOf(":local_ai_model")

    defaultConfig {
        applicationId = "com.fitnessquest.rpg"
        minSdk = 26
        targetSdk = 36
        testInstrumentationRunner = "com.fitnessquest.rpg.FitQuestTestRunner"
        versionCode = versionProps.get().getProperty("phoneVersionCode", "20").toInt()
        versionName = versionProps.get().getProperty("versionName", "1.0.3")

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProps.getProperty("GEMINI_API_KEY", "")}\""
        )
        val fitQuestLlmModelUrl = localProps.getProperty("FITQUEST_LLM_MODEL_URL", "")
        val fitQuestLlmModelVersion = localProps.getProperty("FITQUEST_LLM_MODEL_VERSION", "not-configured")
        val fitQuestLlmModelSha256 = localProps.getProperty("FITQUEST_LLM_MODEL_SHA256", "")
        val fitQuestLlmModelBytes = localProps.getProperty("FITQUEST_LLM_MODEL_BYTES", "0").toLongOrNull() ?: 0L
        val fitQuestLlmModelSizeMb = localProps.getProperty("FITQUEST_LLM_MODEL_SIZE_MB", "0").toIntOrNull() ?: 0
        fun String.asBuildConfigString(): String = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        buildConfigField("String", "FITQUEST_LLM_MODEL_URL", fitQuestLlmModelUrl.asBuildConfigString())
        buildConfigField("String", "FITQUEST_LLM_MODEL_VERSION", fitQuestLlmModelVersion.asBuildConfigString())
        buildConfigField("String", "FITQUEST_LLM_MODEL_SHA256", fitQuestLlmModelSha256.asBuildConfigString())
        buildConfigField("long", "FITQUEST_LLM_MODEL_BYTES", "${fitQuestLlmModelBytes}L")
        buildConfigField("int", "FITQUEST_LLM_MODEL_SIZE_MB", fitQuestLlmModelSizeMb.toString())
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
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
            }
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
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.md",
                "META-INF/NOTICE.txt"
            )
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            if (output is VariantOutput) {
                output.versionCode.set(versionProps.map { it.getProperty("phoneVersionCode", "100").toInt() })
                output.versionName.set(versionProps.map { it.getProperty("versionName", "0.11.0") })
            }
        }
    }
}

val embedWearApk by tasks.registering(Copy::class) {
    val isRelease = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
    val variant = if (isRelease) "release" else "debug"
    group = "build"
    description = "Embed the Wear OS APK for on-device Wi-Fi install"
    dependsOn(":wear:assemble${variant.replaceFirstChar { it.uppercase() }}")
    from(project(":wear").layout.buildDirectory.file("outputs/apk/$variant/wear-$variant.apk"))
    into(generatedWearAssetsDir)
    rename { "fitnessrpg-wear.apk" }
}

tasks.named("preBuild").configure { dependsOn(embedWearApk) }

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.crashlytics.ndk)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.health.connect)
    implementation(libs.mediapipeGenai)
    implementation(libs.play.asset.delivery)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    implementation(libs.dadb) {




        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.platform")
        exclude(group = "org.junit.vintage")
        exclude(group = "junit")
    }
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.json:json:20240303")
    testImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
}
