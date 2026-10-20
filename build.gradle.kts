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
        languageVersion = JavaLanguageVersion.of(25)
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
    // The module name keeps the digits of "fallout2d20" (user decision 2026-10-17).
    options.compilerArgs.add("-Xlint:-module")
    options.errorprone {
        option("NullAway:AnnotatedPackages", "de.fallout2d20")
        option("NullAway:ExcludedFieldAnnotations", "javafx.fxml.FXML")
        // FXML handlers are called by the FXMLLoader through reflection.
        option("UnusedMethod:ExemptingMethodAnnotations", "javafx.fxml.FXML")
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
    // Java preferences written by tests (view state) go to the build folder, not the user's real ones
    // (honoured by the file-based backend on Linux and macOS).
    systemProperty("java.util.prefs.userRoot", layout.buildDirectory.dir("test-prefs").get().asFile.absolutePath)
}

// Version for the About dialog, read by AppInfo.
val generateVersionResource = tasks.register("generateVersionResource") {
    group = "build"
    description = "Writes the app version into a resource for the About dialog and the save files."
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

// The deb takes its icon from a jpackage resource folder (see the jpackage block below).
val linuxIconDir = layout.buildDirectory.dir("jpackage-resources")
val prepareLinuxIcon = tasks.register<Copy>("prepareLinuxIcon") {
    group = "distribution"
    description = "Copies the app icon into the jpackage resource folder for the deb."
    from(layout.projectDirectory.file("packaging/icon.png"))
    into(linuxIconDir)
    rename { "${application.applicationName}.png" }
}

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
        // Application icon (generateIcons): ico for Windows, icns for macOS, png for Linux.
        val iconFile = when {
            osName.contains("win") -> "icon.ico"
            osName.contains("mac") -> "icon.icns"
            else -> "icon.png"
        }
        icon = layout.projectDirectory.file("packaging/$iconFile").asFile.absolutePath
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
                // jpackage replaces the app image's icon by its default when building the deb from the image,
                // unless the resource folder holds <name>.png (filled by the prepareLinuxIcon task).
                installerOptions.set(common + listOf(
                    "--resource-dir", linuxIconDir.get().asFile.absolutePath,
                    "--linux-package-name", "fallout2d20-character-builder",
                    "--linux-shortcut", "--linux-menu-group", "Game"))
            }
        }
    }
}

// Beta menu with the test characters (E9): only for development runs. Deliberately not in
// applicationDefaultJvmArgs, which would end up in the start scripts and the installers.
tasks.named<JavaExec>("run") {
    systemProperty("charbuilder.beta", layout.projectDirectory.dir("src/test/resources").asFile.absolutePath)
}

// Recreates the generated test characters in src/test/resources/samples (E9); run by hand, not by build.
tasks.register<JavaExec>("generateSamples") {
    group = "development"
    description = "Writes the generated sample characters for the Beta menu."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("de.fallout2d20.charbuilder.samples.GenerateSamples")
    args(layout.projectDirectory.dir("src/test/resources/samples").asFile.absolutePath)
}

// Draws the application icon (window PNGs and installer icons) from the Font Awesome radiation glyph; run by hand.
tasks.register<JavaExec>("generateIcons") {
    group = "development"
    description = "Writes the window and installer icons."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("de.fallout2d20.charbuilder.icons.GenerateIcons")
    systemProperty("java.awt.headless", "true")
    args(
        layout.projectDirectory.dir("src/main/resources/de/fallout2d20/charbuilder").asFile.absolutePath,
        layout.projectDirectory.dir("packaging").asFile.absolutePath)
}

// The icon is passed as an option, not a declared input: rebuild the images when it changes.
tasks.named("jpackageImage") {
    inputs.dir(layout.projectDirectory.dir("packaging"))
}
tasks.named("jpackage") {
    inputs.dir(layout.projectDirectory.dir("packaging"))
    if (!osName.contains("win") && !osName.contains("mac")) {
        dependsOn(prepareLinuxIcon)
    }
}
