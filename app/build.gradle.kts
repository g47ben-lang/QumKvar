plugins {
    id("com.android.application") version "8.2.2"
    id("org.jetbrains.kotlin.android") version "1.9.22"
}

android {
    namespace = "com.example.shmuelkum"
    compileSdk = 34
    
    defaultConfig { 
        applicationId = "com.example.shmuelkum"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "1.5" 
    }
    
    compileOptions { 
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8 
    }
    
    kotlinOptions { 
        jvmTarget = "1.8" 
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
