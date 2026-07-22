plugins {
    id("com.android.application")
}

android {
    namespace = "com.quietphoto.clock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.quietphoto.clock"
        minSdk = 17
        targetSdk = 36
        versionCode = 130
        versionName = "1.3.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

dependencies {
}
