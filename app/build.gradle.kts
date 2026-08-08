import java.util.Properties

plugins {
    id("com.android.application")
}

val versionMajor = 4
val versionMinor = 1
val basePatch = 0
val appVersionCode = (versionMajor * 10_000) + (versionMinor * 100) + basePatch
val releasePropertiesFile = rootProject.file("keystore.properties")
val releaseProperties = Properties().apply {
    if (releasePropertiesFile.isFile) {
        releasePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.quietphoto.clock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.quietphoto.clock"
        minSdk = 17
        targetSdk = 36
        versionCode = appVersionCode
        versionName = "$versionMajor.$versionMinor.$basePatch"
    }

    flavorDimensions += "fontBundle"
    productFlavors {
        create("storopiaTest") {
            dimension = "fontBundle"
            applicationIdSuffix = ".storopia"
            versionCode = appVersionCode
            versionName = "$versionMajor.$versionMinor.$basePatch-test-android4.2-storopia"
            buildConfigField("boolean", "INCLUDE_STOROPIA", "true")
        }
        create("standard") {
            dimension = "fontBundle"
            versionCode = appVersionCode
            versionName = "$versionMajor.$versionMinor.$basePatch"
            buildConfigField("boolean", "INCLUDE_STOROPIA", "false")
        }
    }

    signingConfigs {
        create("release") {
            if (releasePropertiesFile.isFile) {
                storeFile = rootProject.file(releaseProperties.getProperty("storeFile"))
                storePassword = releaseProperties.getProperty("storePassword")
                keyAlias = releaseProperties.getProperty("keyAlias")
                keyPassword = releaseProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releasePropertiesFile.isFile) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }
}

tasks.configureEach {
    if (name.startsWith("package") && name.contains("Release")) {
        doFirst {
            check(releasePropertiesFile.isFile) {
                "Release signing requires keystore.properties. See README.md."
            }
        }
    }
}

dependencies {
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    compileOnly("androidx.annotation:annotation:1.10.0")
    testImplementation("junit:junit:4.13.2")
}

val bundledFontNames = setOf(
    "font_audiowide.ttf",
    "font_digital.ttf",
    "font_heavy.ttf",
    "font_huninn.ttf",
    "font_iansui.ttf",
    "font_kai.ttf",
    "font_orbitron.ttf",
    "font_oxanium.ttf",
    "font_rounded.ttf",
    "font_sairastencil.ttf",
    "font_sans.ttf",
    "font_serif.ttf",
    "font_zendots.ttf",
)

val verifyBundledFonts by tasks.registering {
    val fontDirectory = layout.projectDirectory.dir("src/main/assets/fonts")
    inputs.dir(fontDirectory)

    doLast {
        val directory = fontDirectory.asFile
        val actualNames = directory.listFiles()
            ?.filter { it.isFile && it.extension.equals("ttf", ignoreCase = true) }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()

        check(actualNames == bundledFontNames) {
            "Bundled fonts differ from the catalog. Missing: ${bundledFontNames - actualNames}; " +
                "unexpected: ${actualNames - bundledFontNames}"
        }

        bundledFontNames.forEach { name ->
            val font = directory.resolve(name)
            check(font.length() > 1_000L) { "Font asset is empty or invalid: $name" }
            val signature = font.inputStream().use { input ->
                ByteArray(4).also { bytes ->
                    check(input.read(bytes) == bytes.size) { "Cannot read font header: $name" }
                }
            }
            val isTrueType = signature.contentEquals(byteArrayOf(0, 1, 0, 0))
            val isOpenType = signature.contentEquals("OTTO".toByteArray(Charsets.US_ASCII))
            check(isTrueType || isOpenType) { "Unsupported font header: $name" }
        }

        val storopia = layout.projectDirectory
            .file("src/storopiaTest/assets/fonts/font_storopia.ttf").asFile
        check(storopia.isFile && storopia.length() > 1_000L) {
            "Storopia test flavor font is missing or invalid"
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyBundledFonts)
}
