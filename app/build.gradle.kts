import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

// Supabase project config lives in local.properties (never committed). The placeholder URL keeps
// client creation valid on machines without keys; sync no-ops when signed out. A release build
// refuses to use it — see verifyReleaseConfig below.
val supabaseUrl: String? = localProps.getProperty("SUPABASE_URL")?.takeIf { it.isNotBlank() }
val supabaseAnonKey: String? = localProps.getProperty("SUPABASE_ANON_KEY")?.takeIf { it.isNotBlank() }

// Release signing, also from local.properties. The keystore path is relative to the project root.
val releaseSigning: Map<String, String?> = listOf(
    "RELEASE_STORE_FILE", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD",
).associateWith { localProps.getProperty(it)?.takeIf { value -> value.isNotBlank() } }
val hasReleaseSigning = releaseSigning.values.all { it != null }

// Versioning lives in gradle.properties so a release bump is a one-line, reviewed change.
// CI can override either with -Pshopmate.versionCode=… / -Pshopmate.versionName=….
val appVersionCode = providers.gradleProperty("shopmate.versionCode").get().toInt()
val appVersionName = providers.gradleProperty("shopmate.versionName").get()

android {
    namespace = "com.shopmate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shopmate"
        minSdk = 28
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl ?: "https://placeholder.supabase.co"}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseAnonKey.orEmpty()}\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseSigning.getValue("RELEASE_STORE_FILE")!!)
                storePassword = releaseSigning.getValue("RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigning.getValue("RELEASE_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
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
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation + ViewModel
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // DataStore (settings, Supabase session, active store)
    implementation(libs.androidx.datastore.preferences)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Supabase (Auth + PostgREST; Ktor's OkHttp engine is its HTTP client)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Vico charts
    implementation(libs.vico.compose.m3)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// A release that ships the placeholder Supabase project or no signature is broken in a way a
// cashier finds out about, not the build. Fail it here instead — only when a release is built,
// so debug builds on machines without keys keep working.
val releaseConfigProblems: List<String> = buildList {
    if (supabaseUrl == null) add("SUPABASE_URL is missing from local.properties")
    if (supabaseAnonKey == null) add("SUPABASE_ANON_KEY is missing from local.properties")
    releaseSigning.filterValues { it == null }.keys.forEach { add("$it is missing from local.properties") }
}
val verifyReleaseConfig by tasks.registering {
    val problems = releaseConfigProblems
    doLast {
        if (problems.isNotEmpty()) {
            val list = problems.joinToString(separator = "\n") { "  - $it" }
            throw GradleException("Release build is not configured:\n$list")
        }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(verifyReleaseConfig) }
