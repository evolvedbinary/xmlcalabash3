import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URI
import java.net.URL

buildscript {
  dependencies {
    classpath("org.jetbrains.dokka:dokka-base:1.9.20")
  }
}

repositories {
    mavenLocal()
    mavenCentral()
}

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") version "1.9.20"
}

// Configures only the parent MultiModule task,
// this will not affect subprojects
tasks.dokkaHtmlMultiModule {
    moduleName.set("XML Calabash 3.x")
}
