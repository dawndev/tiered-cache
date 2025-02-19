import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    `java-library`
    java
    distribution
    kotlin("jvm") version Versions.Kotlin apply false
    kotlin("plugin.serialization") version Versions.Kotlin
}

group = "com.github.dawndev.tieredcache"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

apply(plugin = "idea")
apply(plugin = "java")
apply(plugin = "kotlin")
apply(plugin = "java-library")

dependencies {
    api(Deps.Kotlin.Reflect)
    api(Deps.Kotlin.Jvm)
    api(Deps.Kotlinx)
    api(Deps.KotlinxJson)
    api(Deps.Slf4j.Api)
    api(Deps.Slf4j.Impl)

    implementation(Deps.Guava)
    implementation(Deps.Caffeine)
    implementation(Deps.Kryo)
    implementation(Deps.Lettuce)

    testImplementation(Deps.Junit.JupiterEngine)
    testImplementation(Deps.Junit.JupiterApi)
    testImplementation(Deps.Junit.Jupiter)
}

tasks {
    withType<JavaCompile>() {
        // 启用在单独的daemon进程中编译
        options.isFork = true
        options.encoding = "UTF-8"
    }

    withType<Javadoc> {
        options.encoding = "UTF-8"
    }

    withType<KotlinCompile> {
        kotlinOptions {
            // Generate metadata for Java 1.8 reflection on method parameters
            javaParameters = true

            // Target version of the generated JVM bytecode (1.6 or 1.8), default is 1.6
            jvmTarget = "11"

//                useIR = true

            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

configurations {
    implementation {
        resolutionStrategy.failOnVersionConflict()
    }
}
