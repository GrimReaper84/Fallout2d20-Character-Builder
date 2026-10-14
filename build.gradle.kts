import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    application
    alias(libs.plugins.javafx)
    alias(libs.plugins.jlink)
    alias(libs.plugins.errorprone)
}

group = "de.fallout2d20"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainModule.set("de.fallout2d20.charbuilder")
    mainClass.set("de.fallout2d20.charbuilder.App")
    applicationName = "Fallout2d20CharacterBuilder"
}

javafx {
    version = libs.versions.javafx.runtime.get()
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    compileOnly(libs.jspecify)
    implementation(libs.jackson.databind)
    implementation(libs.controlsfx)
    implementation(libs.miglayout.javafx)
    implementation(libs.ikonli.javafx)
    implementation(libs.ikonli.fontawesome6.pack)

    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)

    testCompileOnly(libs.jspecify)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.errorprone {
        option("NullAway:AnnotatedPackages", "de.fallout2d20")
        option("NullAway:ExcludedFieldAnnotations", "javafx.fxml.FXML")
        error("NullAway")
    }
}

tasks.named<JavaCompile>("compileTestJava") {
    options.errorprone {
        disable("NullAway")
    }
}

tasks.test {
    useJUnitPlatform()
}

jlink {
    imageZip.set(layout.buildDirectory.file("distributions/${application.applicationName}-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "zip-6", "--no-header-files", "--no-man-pages"))
    launcher {
        name = application.applicationName
    }
}
