// Read the configuration properties from the "main" project so that I don't have to
// maintain them in two places.
layout.projectDirectory.file("../gradle.properties").asFile.readLines().forEach { input ->
  val line = input.trim()
  if (!line.isBlank() && !line.startsWith("#") && !line.startsWith("//")) {
    val parts = line.split("=").map { it.trim() }
    val property = parts.first()
    val value = parts.last()

    // Ignore the build-related properties
    if (!property.startsWith("kotlin.") && !property.startsWith("org.gradle.")) {
      project.extra[property] = value
      //println("${property}=${value}")
    }
  }
}

plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://maven.saxonica.com/maven") }
}

val saxonGroup = project.findProperty("saxonGroup")
val saxonArtifact = project.findProperty("saxonArtifact")
val saxonVersion = project.findProperty("saxonVersion")

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
  implementation(libs.kotlin.gradle.plugin)
  implementation("nu.validator.htmlparser:htmlparser:1.4")
  implementation("${saxonGroup}:${saxonArtifact}:${saxonVersion}")

  // These aren't needed here; but if I don't include them, the documentation
  // tasks don't seem to find or load the xslTNG classes so...¯\_(ツ)_/¯
  implementation("org.docbook:schemas-docbook:5.2")
  implementation("org.docbook:docbook-xslTNG:${project.findProperty("xslTNGversion")}")
}

gradlePlugin {
  plugins {
    create("xmlcalabash-build") {
      id = "com.xmlcalabash.build.xmlcalabash-build"
      implementationClass = "com.xmlcalabash.build.XmlCalabashBuildPlugin"
    }
  }
}
