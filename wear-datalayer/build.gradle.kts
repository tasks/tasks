plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.protobuf")
}

android {
    namespace = "org.tasks.wear"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

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
    kotlinOptions {
        jvmTarget = "17"
    }
}

val archSuffix = if (System.getProperty("os.name").contains("mac", ignoreCase = true))
    ":osx-x86_64"
else
    ""

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.stnd.get().toString() + archSuffix
    }
    plugins {
        create("javalite") {
            artifact = libs.protobuf.protoc.gen.javalite.get().toString() + archSuffix
        }
        create("grpc") {
            artifact = libs.protobuf.protoc.gen.grpc.java.get().toString()
        }
        create("grpckt") {
            artifact = libs.protobuf.protoc.gen.grpc.kotlin.get().toString()
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
                create("grpckt") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    api(libs.io.grpc.grpc.kotlin)
    api(libs.io.grpc.protobuf.lite)

    api(libs.io.grpc.grpc.android)
    api(libs.io.grpc.grpc.binder)
    implementation(libs.horologist.datalayer.core)

    implementation(libs.androidx.datastore)
    implementation(libs.protobuf.kotlin.lite)
}