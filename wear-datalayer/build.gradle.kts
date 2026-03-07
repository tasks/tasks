plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.protobuf")
}

android {
    namespace = "org.tasks.wear"
    compileSdk = 34

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

val isNixOS = System.getenv("PROTOC_GEN_GRPC_JAVA") != null

val archSuffix = if (System.getProperty("os.name").contains("mac", ignoreCase = true))
    ":osx-x86_64"
else
    ""

protobuf {
    protoc {
        if (isNixOS) {
            path = "${System.getenv("DEVENV_PROFILE") ?: "/run/current-system/sw"}/bin/protoc"
        } else {
            artifact = libs.protobuf.protoc.stnd.get().toString() + archSuffix
        }
    }
    plugins {
        create("javalite") {
            if (!isNixOS) {
                artifact = libs.protobuf.protoc.gen.javalite.get().toString() + archSuffix
            }
        }
        create("grpc") {
            if (isNixOS) {
                path = System.getenv("PROTOC_GEN_GRPC_JAVA") ?: "${System.getenv("DEVENV_PROFILE") ?: "/run/current-system/sw"}/bin/protoc-gen-grpc-java"
            } else {
                artifact = libs.protobuf.protoc.gen.grpc.java.get().toString()
            }
        }
        create("grpckt") {
            // grpckt plugin not available in nixpkgs, only use on non-NixOS
            if (!isNixOS) {
                artifact = libs.protobuf.protoc.gen.grpc.kotlin.get().toString()
            }
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
            }
            task.plugins {
                create("grpc") {
                    option("lite")
                }
                if (!isNixOS) {
                    create("grpckt") {
                        option("lite")
                    }
                }
            }
        }
    }
}

dependencies {
    api(libs.io.grpc.grpc.kotlin)
    api(libs.io.grpc.protobuf.lite)
    api(libs.io.grpc.grpc.stub)

    api(libs.io.grpc.grpc.android)
    api(libs.io.grpc.grpc.binder)
    implementation(libs.horologist.datalayer.core)

    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.kotlin.lite)
}