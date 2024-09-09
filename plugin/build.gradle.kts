plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {

}

java {
    withSourcesJar()
}

gradlePlugin {
    plugins {
        create("skriptTestGradlePlugin") {
            id = "org.skriptlang.gradle.test.plugin"
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
            jvmTarget = "21"
        }
    }
}
