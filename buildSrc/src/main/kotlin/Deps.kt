object Deps {

    object Kotlin {
        private const val Version = Versions.Kotlin
        const val Compiler = "org.jetbrains.kotlin:kotlin-compiler-embeddable:$Version"
        const val Jvm = "org.jetbrains.kotlin:kotlin-stdlib:$Version"
        const val Reflect = "org.jetbrains.kotlin:kotlin-reflect:$Version"
        const val Gradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:$Version"
        const val GradleApi = "org.jetbrains.kotlin:kotlin-gradle-plugin-api:$Version"
        const val AllOpen = "org.jetbrains.kotlin:kotlin-allopen:$Version"
    }

    object Slf4j {
        private const val Version = Versions.Slf4j
        const val Api = "org.slf4j:slf4j-api:$Version"
        const val Impl = "org.slf4j:slf4j-log4j12:$Version"
    }

    object Junit {
        private const val Version = Versions.Junit
        const val JupiterEngine = "org.junit.jupiter:junit-jupiter-engine:$Version"
        const val JupiterApi = "org.junit.jupiter:junit-jupiter-api:$Version"
        const val Jupiter = "org.junit.jupiter:junit-jupiter:$Version"
    }


    const val Kotlinx = "org.jetbrains.kotlinx:kotlinx-cli:${Versions.Kotlinx}"
    const val KotlinxJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KotlinxJson}"
    const val Asm = "org.ow2.asm:asm:${Versions.Asm}"
    const val Guava = "com.google.guava:guava:${Versions.Guava}"
    const val CommonsLang3 = "org.apache.commons:commons-lang3:${Versions.Lang3}"
    const val CommonsIO = "commons-io:commons-io:${Versions.CommonsIO}"
    const val Caffeine = "com.github.ben-manes.caffeine:caffeine:${Versions.Caffeine}"
    const val AnnoApi = "jakarta.annotation:jakarta.annotation-api:${Versions.Jakarta}"
    const val Aviator = "com.googlecode.aviator:aviator:${Versions.Aviator}"
    const val Kryo = "com.esotericsoftware:kryo:${Versions.Kryo}"
    const val ProtobufJava = "com.google.protobuf:protobuf-java:${Versions.Protobuf}"
    const val Micrometer = "io.micrometer:micrometer-core:${Versions.Micrometer}"
    const val Redisson = "org.redisson:redisson:${Versions.Redisson}"
    const val Gson = "com.google.code.gson:gson:${Versions.Gson}"
    const val Lettuce = "io.lettuce:lettuce-core:${Versions.Lettuce}"
}