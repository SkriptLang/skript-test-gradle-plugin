plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
}

java {
    withSourcesJar()
}

gradlePlugin {
    plugins {
        create("skriptTestGradlePlugin") {
            id = "skript-test"
            group = "org.skriptlang"
            implementationClass = "org.skriptlang.gradle.test.plugin.SkriptTestPlugin"
            version = "1.0.0"
            description = "A Gradle plugin to run Skript tests"
            website = "https://github.com/SkriptLang/skript-test-gradle-plugin"
        }
    }
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}
