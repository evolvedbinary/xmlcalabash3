import java.io.PrintStream
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.xmlcalabash.build.XmlCalabashBuildExtension
import com.xmlcalabash.build.ExternalDependencies

plugins {
  id("buildlogic.kotlin-application-conventions")
  id("com.xmlcalabash.build.xmlcalabash-build")
}

repositories {
  mavenLocal()
  mavenCentral()
  maven { url = uri("https://maven.saxonica.com/maven") }
}

val saxonVersion = project.properties["saxonVersion"].toString()
val requirePass = project.findProperty("requirePass")?.toString() ?: "true"
val consoleOutput = project.findProperty("xmlcalabash.testDriver.consoleOutput")?.toString() ?: "false"

val transformation by configurations.creating
val testrunner by configurations.creating {
  extendsFrom(configurations["runtimeClasspath"])
}

configurations.forEach {
  it.exclude("net.sf.saxon")
}

dependencies {
  implementation(project(":xmlcalabash"))
  testrunner(project(":test-driver"))
  implementation(project(":ext:polyglot"))
  implementation(project(":ext:basex"))
  implementation(project(":ext:existdb"))
  implementation(project(":app"))
  implementation("com.saxonica:Saxon-EE:${saxonVersion}")

  transformation ("com.saxonica:Saxon-EE:${saxonVersion}")

  ExternalDependencies.of(ExternalDependencies.compileSteps).forEach {
    implementation(it) {
      exclude(group="net.sf.saxon", module="Saxon-HE")
    }
  }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val xmlbuild = the<XmlCalabashBuildExtension>()

val WHOST = project.findProperty("WHOST") ?: "http://localhost:8246"
val SHOST = project.findProperty("SHOST") ?: "localhost"
val SMTPPORT = project.findProperty("APIPORT") ?: "1025"
val APIPORT = project.findProperty("APIPORT") ?: "1080"
val DEBUG = project.findProperty("DEBUG") ?: "false"

if (project.findProperty("testPattern") == null) {
  tasks.register("run-test") {
    doLast {
      println("Configure test with -PtestPattern and -PtestDir")
    }
  }
} else {
  tasks.register<JavaExec>("run-test") {
    val testPattern = project.findProperty("testPattern").toString()
    val testDir = project.findProperty("testDir")?.toString()
        ?: "../tests/3.0-test-suite/test-suite/tests"

    classpath = configurations.named("testrunner").get()
    mainClass = "com.xmlcalabash.testdriver.Main"

    inputs.dir(layout.projectDirectory.dir(testDir))
    inputs.file(layout.projectDirectory.file("src/test/resources/exclusions.txt"))

    args("-t:${testPattern}",
         "--debug:${DEBUG}",
         "SHOST=${SHOST}", "SMTPPORT=${SMTPPORT}", "APIPORT=${APIPORT}",
         "WHOST=${WHOST}")
  }
}

tasks.register<JavaExec>("test-suite") {
  dependsOn("build")

  classpath = configurations.named("testrunner").get()
  mainClass = "com.xmlcalabash.testdriver.Main"

  inputs.dir(layout.projectDirectory.dir("../tests/3.0-test-suite"))
  inputs.file(layout.projectDirectory.file("src/test/resources/exclusions.txt"))
  outputs.file(layout.buildDirectory.file("test-suite-results.xml"))

  args("--title:XProc 3.0 Test Suite",
       "--require-pass:${requirePass}",
       "--console:${consoleOutput}",
       "--dir:${layout.projectDirectory.dir("../tests/3.0-test-suite/test-suite/tests")}",
       "--report:${layout.buildDirectory.file("test-suite-results.xml").get().asFile}",
       "--debug:${DEBUG}",
       "--save-results",
       "SHOST=${SHOST}", "SMTPPORT=${SMTPPORT}", "APIPORT=${APIPORT}",
       "WHOST=${WHOST}")
}

tasks.register<JavaExec>("extra-suite") {
  dependsOn("build")

  classpath = configurations.named("testrunner").get()
  mainClass = "com.xmlcalabash.testdriver.Main"

  inputs.dir(layout.projectDirectory.dir("../tests/extra-suite"))
  inputs.file(layout.projectDirectory.file("src/test/resources/exclusions.txt"))
  outputs.file(layout.buildDirectory.file("extra-suite-results.xml"))

  args("--title:XML Calabash Extra Test Suite",
       "--require-pass:${requirePass}",
       "--console:${consoleOutput}",
       "--dir:${layout.projectDirectory.dir("../tests/extra-suite/test-suite/tests")}",
       "--report:${layout.buildDirectory.file("extra-suite-results.xml").get().asFile}",
       "--debug:${DEBUG}",
       "--save-results",
       "SHOST=${SHOST}", "SMTPPORT=${SMTPPORT}", "APIPORT=${APIPORT}",
       "WHOST=${WHOST}")
}

tasks.register<JavaExec>("selenium") {
  dependsOn("build")

  classpath = configurations.named("testrunner").get()
  mainClass = "com.xmlcalabash.testdriver.Main"

  inputs.dir(layout.projectDirectory.dir("../tests/extra-suite/test-suite/selenium"))
  inputs.file(layout.projectDirectory.file("src/test/resources/exclusions.txt"))
  outputs.file(layout.buildDirectory.file("selenium-results.xml"))

  args("--title:XML Calabash Selenium Test Suite",
       "--require-pass:${requirePass}",
       "--console:${consoleOutput}",
       "--dir:${layout.projectDirectory.dir("../tests/extra-suite/test-suite/selenium")}",
       "--report:${layout.buildDirectory.file("selenium-results.xml").get().asFile}",
       "--debug:${DEBUG}",
       "--save-results",
       "SHOST=${SHOST}", "SMTPPORT=${SMTPPORT}", "APIPORT=${APIPORT}",
       "WHOST=${WHOST}")
}

tasks.register<JavaExec>("test-report") {
  dependsOn("test-suite")
  dependsOn("extra-suite")
  dependsOn("selenium")
  dependsOn("copy-extra")

  classpath = configurations.named("transformation").get()
  mainClass = "net.sf.saxon.Transform"

  inputs.file(layout.buildDirectory.file("test-suite-results.xml"))
  inputs.file(layout.buildDirectory.file("extra-suite-results.xml"))
  inputs.file(layout.buildDirectory.file("selenium-results.xml"))
  inputs.file(layout.projectDirectory.file("src/xsl/test-report.xsl"))
  outputs.file(layout.buildDirectory.file("test-report/index.html"))
  outputs.file(layout.buildDirectory.file("test-report/version.json"))
  
  args("-s:${layout.buildDirectory.file("test-suite-results.xml").get().asFile}",
       "-xsl:${layout.projectDirectory.file("src/xsl/test-report.xsl")}",
       "-o:${layout.buildDirectory.file("test-report/index.html").get().asFile}")

  doLast {
    val output = layout.buildDirectory.file("test-report/version.json").get().asFile
    val stream = PrintStream(output)
    stream.println("{")
    stream.println("\"version\": \"${xmlbuild.version.get()}\"")
    stream.println("}")
    stream.close()
  }
}

// Doesn't run the most time consuming tests...
tasks.register("quick-report") {
  dependsOn("test-suite")
  dependsOn("extra-suite")
  doLast {
    println("Report finished")
  }
}

tasks.register("copy-extra") {
  inputs.dir(layout.projectDirectory.dir("src/css"))
  inputs.dir(layout.projectDirectory.dir("src/js"))
  outputs.dir(layout.buildDirectory.dir("test-report"))

  doLast {
    copy {
      into(layout.buildDirectory.dir("test-report"))
      from(layout.projectDirectory.file("src/css"))
    }
  }
  doLast {
    copy {
      into(layout.buildDirectory.dir("test-report"))
      from(layout.projectDirectory.file("src/js"))
    }
  }
}

tasks.register("helloWorld") {
  doLast {
    println("Building with Java version ${System.getProperty("java.version")}")
    println("DEBUG=${DEBUG}")
    testrunner.forEach { println(it) }
  }
}
