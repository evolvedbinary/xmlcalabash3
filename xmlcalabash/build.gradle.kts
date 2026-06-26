import com.xmlcalabash.build.XmlCalabashBuildExtension
import com.xmlcalabash.build.ExternalDependencies

import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration

import java.net.URI
import java.net.URL
import java.io.PrintStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.zip.ZipFile

import de.undercouch.gradle.tasks.download.Download

buildscript {
  dependencies {
    classpath("org.jetbrains.dokka:dokka-base:1.9.20")
  }
}

plugins {
  id("buildlogic.kotlin-library-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
  id("com.github.gmazzo.buildconfig") version "5.5.0"
  id("org.jetbrains.dokka") version "1.9.20"
  id("de.undercouch.download") version "5.6.0"
  id("maven-publish")
}

val xmlbuild = the<XmlCalabashBuildExtension>()

configurations.forEach {
  it.exclude("com.sun.xml.ibind.jaxp")
  it.exclude("isorelax")
  it.exclude("relaxngDatatype")
  //it.exclude("net.sf.saxon")
}

dependencies {
  // implementation("name.dmaus.schxslt:schxslt:${dep_schxslt}")
  // The SchXslt2 transpiler is included in our resources
  implementation(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar"))))
}

buildConfig {
  className("XmlCalabashBuildConfig")
  packageName("com.xmlcalabash")
  useKotlinOutput { internalVisibility = false }

  buildConfigField("NAME", xmlbuild.name.get())
  buildConfigField("VERSION", xmlbuild.version.get())
  buildConfigField("PRODUCT_NAME", xmlbuild.productName.get())
  buildConfigField("VENDOR_NAME", xmlbuild.vendorName.get())
  buildConfigField("VENDOR_URI", xmlbuild.vendorUri.get())
  buildConfigField("BUILD_DATE", xmlbuild.buildDate.get())
  buildConfigField("BUILD_ID", xmlbuild.buildId())
  buildConfigField("SCHXSLT2", project.findProperty("schxslt2").toString())

  val depends = mutableSetOf<String>()
  for ((_, depList) in ExternalDependencies.steps) {
    depends.addAll(depList)
  }

  val sb = StringBuilder()
  sb.append("mapOf(\n")
  sb.append("  \"net.sf.saxon:Saxon-HE\" to \"${project.findProperty("saxonVersion").toString()}\"")
  for (dep in depends) {
    sb.append(",\n")
    val colon = dep.lastIndexOf(":")
    val pkg = dep.substring(0, colon)
    val version = dep.substring(colon+1)
    sb.append("  \"").append(pkg).append("\" to ")
    sb.append("\"").append(version).append("\"")
  }
  sb.append(")\n")

  buildConfigField("java.util.Map<String,String>", "DEPENDENCIES", sb.toString())
}

tasks.jar {
  archiveFileName.set(xmlbuild.jarArchiveFilename())
}

tasks.javadoc {
  if (JavaVersion.current().isJava9Compatible) {
    (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
  }
}

val exprocDownload = tasks.register<Download>("downloadContrib") {
  outputs.file(layout.buildDirectory.file("exproc-contrib.zip"))
  src("https://exproc.org/contrib/exproc-contrib.zip")
  dest(layout.buildDirectory)
  onlyIf { !layout.buildDirectory.file("exproc-contrib.zip").get().asFile.exists() }
}

val exprocContribDate = tasks.register("exprocContribDate") {
  dependsOn(exprocDownload)
  doLast {
    val zipFile = ZipFile(layout.buildDirectory.file("exproc-contrib.zip").get().asFile)
    val maxtime = zipFile.entries().toList().maxByOrNull { it.getTime() }?.getTime()
    if (maxtime != null) {
      val fn = "com/xmlcalabash/exproc-contrib-last-modified.txt"
      val path = layout.buildDirectory.file("exproc-contrib/com/xmlcalabash").get().asFile
      path.mkdirs()
      val dt = Date(maxtime)
      val sdf = SimpleDateFormat("dd MMM yyyy 'at' HH:mm")
      val ps = PrintStream(FileOutputStream(layout.buildDirectory.file("exproc-contrib/${fn}").get().asFile))
      ps.println(sdf.format(dt))
      ps.close()
    }
  }
}

val exprocContrib = tasks.register<Copy>("exprocContrib") {
  dependsOn(exprocDownload)
  dependsOn(exprocContribDate)
  into(layout.buildDirectory.dir("exproc-contrib"))
  from(zipTree(layout.buildDirectory.file("exproc-contrib.zip"))) {
    into("org/xmlresolver")
  }
}

sourceSets {
  main {
    resources {
      srcDir(exprocContrib)
    }
  }
}

val javadocJar = tasks.register<Jar>("javadocJar") {
  dependsOn("dokkaJavadoc")
  archiveClassifier = "javadoc"
  from(tasks.dokkaJavadoc)
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
  archiveClassifier = "sources"
  from(sourceSets.main.get().allSource)
}

tasks.withType<DokkaTaskPartial>().configureEach {
  pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
    customStyleSheets = listOf(file("../documentation/src/dokka/resources/css/xmlcalabash.css"))
    //templatesDir = file("../documentation/src/dokka/resources/templates")
    footerMessage = "© 2024 Norm Tovey-Walsh"
    separateInheritedMembers = false
    mergeImplicitExpectActualDeclarations = false
  }

  dokkaSourceSets {
    named("main") {
      documentedVisibilities.set(setOf(Visibility.PUBLIC, Visibility.PROTECTED))
      moduleName.set("XMLCalabash")
      includes.from("Module.md")
      sourceLink {
        localDirectory.set(file("src/main/kotlin"))
        remoteUrl.set(URI("https://codeberg.org/xmlcalabash/xmlcalabash3").toURL())
        remoteLineSuffix.set("#L")
      }
    }
  }
}

tasks.register("apidocs") {
  dependsOn("dokkaJavadoc")
  doLast {
    val stream = PrintStream(layout.buildDirectory.file("dokka/javadoc/details.json").get().asFile)
    stream.println("{\"version\": \"${xmlbuild.version.get()}\", \"pubdate\": \"${xmlbuild.buildTime.get()}\"}")
    stream.close()
  }
}

publishing {
  publications {
    create<MavenPublication>("mavenXmlCalabash") {
      pom {
        groupId = project.findProperty("xmlcalabashGroup").toString()
        version = project.findProperty("xmlcalabashVersion").toString()
        name = "XML Calabash"
        packaging = "jar"
        description = "An XProc 3.0 processor"
        url = "https://codeberg.org/xmlcalabash/xmlcalabash3"

        scm {
          url = "scm:git@codeberg.org:xmlcalabash/xmlcalabash3.git"
          connection = "scm:git@codeberg.org:xmlcalabash/xmlcalabash3.git"
          developerConnection = "scm:git@codeberg.org:xmlcalabash/xmlcalabash3.git"
        }

        licenses {
          license {
            name = "MIT License"
            url = "https://opensource.org/api/license/mit"
            distribution = "repo"
          }
        }

        developers {
          developer {
            id = "ndw"
            name = "Norm Tovey-Walsh"
          }
        }
      }

      from(components["java"])
      artifact(sourcesJar.get())
      artifact(javadocJar.get())
    }
  }

  repositories {
    maven {
      url = layout.buildDirectory.dir("maven-release").get().asFile.toURI()
    }
  }
}
