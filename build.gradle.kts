plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.9"
    id("com.github.gmazzo.buildconfig") version "6.0.7"
}

group = "ua.nanit"
version = "1.11.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.spongepowered:configurate-yaml:4.2.0")

    implementation("io.netty:netty-handler:4.2.9.Final")
    implementation("io.netty:netty-transport-native-epoll:4.2.9.Final:linux-x86_64")
    implementation("io.netty:netty-transport-native-epoll:4.2.9.Final:linux-aarch_64")
    implementation("io.netty:netty-transport-native-io_uring:4.2.9.Final:linux-x86_64")
    implementation("io.netty:netty-transport-native-io_uring:4.2.9.Final:linux-aarch_64")
    implementation("io.netty:netty-transport-native-kqueue:4.2.9.Final:osx-x86_64")
    implementation("io.netty:netty-transport-native-kqueue:4.2.9.Final:osx-aarch_64")

    implementation("net.kyori:adventure-api:4.26.1")
    implementation("net.kyori:adventure-text-serializer-gson:4.26.1")
    implementation("net.kyori:adventure-text-serializer-legacy:4.26.1")
    implementation("net.kyori:adventure-text-serializer-json-legacy-impl:4.26.1")
    implementation("net.kyori:adventure-text-serializer-plain:4.26.1")
    implementation("net.kyori:adventure-text-minimessage:4.26.1")
    implementation("net.kyori:adventure-nbt:4.26.1")

    implementation("com.google.code.gson:gson:2.13.2")

    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}


buildConfig {
    className("BuildConfig")
    packageName("ua.nanit.limbo")
    buildConfigField("LIMBO_VERSION", provider { "${project.version}" })
}

tasks.shadowJar {
    from("LICENSE")

    archiveClassifier.set("")
    archiveVersion.set("")

    manifest {
        attributes(
            mapOf(
                "Main-Class" to "ua.nanit.limbo.NanoLimbo"
            )
        )
    }

    minimize {
        exclude(dependency("ch.qos.logback:logback-classic:.*:.*"))
    }
}