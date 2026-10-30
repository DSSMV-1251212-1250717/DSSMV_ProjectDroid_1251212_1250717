import java.io.File
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

secrets {
    defaultPropertiesFileName = "secrets.defaults.properties"
}

android {
    namespace = "pt.isep.dssmv.mapaisep"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "pt.isep.dssmv.mapaisep"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Native Google Maps for Android.
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.activity:activity:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

// Permite executar a aplicação através de uma configuração Gradle no IntelliJ,
// mesmo quando o IDE não oferece a configuração Android App para este módulo.
val sdkProperties = Properties().apply {
    rootProject.file("local.properties").inputStream().use { load(it) }
}
val androidSdkPath = sdkProperties.getProperty("sdk.dir")
if (androidSdkPath == null) {
    error("sdk.dir não está definido em local.properties")
}
val adbExecutable = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
    "adb.exe"
} else {
    "adb"
}

tasks.register<Exec>("runDebugOnEmulator") {
    group = "application"
    description = "Instala e abre MapaISEP no emulador Android ligado."
    dependsOn("installDebug")
    executable = File(androidSdkPath, "platform-tools/$adbExecutable").absolutePath
    args("shell", "am", "start", "-n", "pt.isep.dssmv.mapaisep/.LoginActivity")
}
