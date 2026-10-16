import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    application
    alias(libs.plugins.javafx)
    alias(libs.plugins.jlink)
    alias(libs.plugins.errorprone)
}

group = "de.fallout2d20"
// version: gradle.properties (single source; the release workflow checks the tag against it)

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
    implementation(libs.pdfbox)

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

// Version for the About dialog, read by AppInfo.
val generateVersionResource by tasks.registering {
    val appVersion = project.version.toString()
    val output = layout.buildDirectory.dir("generated/version")
    inputs.property("version", appVersion)
    outputs.dir(output)
    doLast {
        val file = output.get().file("de/fallout2d20/charbuilder/version.properties").asFile
        file.parentFile.mkdirs()
        file.writeText("version=$appVersion\n")
    }
}
sourceSets.main {
    resources.srcDir(generateVersionResource)
}

val osName: String = System.getProperty("os.name").lowercase()

jlink {
    // Portable zip per OS (and architecture on macOS), e.g. Fallout2d20CharacterBuilder-1.0.0-linux.zip.
    imageZip.set(layout.buildDirectory.file(
        "portable/${application.applicationName}-${project.version}-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "zip-6", "--no-header-files", "--no-man-pages"))
    launcher {
        name = application.applicationName
    }
    // Installer for the OS the build runs on (jpackage cannot build for other systems): msi, deb or dmg.
    jpackage {
        appVersion = project.version.toString()
        vendor = "GrimReaper84"
        val common = listOf(
            "--description", "Character builder for Fallout: The Roleplaying Game (2d20)",
            "--copyright", "Copyright (c) 2026 GrimReaper84, MIT License")
        when {
            osName.contains("win") -> {
                installerType = "msi"
                installerOptions.set(common + listOf(
                    "--win-menu", "--win-menu-group", "Fallout 2d20",
                    "--win-shortcut", "--win-dir-chooser", "--win-per-user-install",
                    // Fixed, so a newer MSI replaces an installed older version.
                    "--win-upgrade-uuid", "3148054a-36fb-40b4-aa75-c97c1b827ad2"))
            }
            osName.contains("mac") -> {
                installerType = "dmg"
                installerOptions.set(common + listOf("--mac-package-identifier", "de.fallout2d20.charbuilder"))
            }
            else -> {
                installerType = "deb"
                installerOptions.set(common + listOf(
                    "--linux-package-name", "fallout2d20-character-builder",
                    "--linux-shortcut", "--linux-menu-group", "Game"))
            }
        }
    }
}
