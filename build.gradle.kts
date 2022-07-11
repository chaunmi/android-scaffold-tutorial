// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {

    val GRADLE_VERSION = "7.2.0"
    val KOTLIN_VERSION = "1.6.10"

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
    }

    dependencies {
        classpath("com.android.tools.build:gradle:$GRADLE_VERSION")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
    }
}

tasks.register("clean", Delete::class.java) {
    delete(buildDir)
}