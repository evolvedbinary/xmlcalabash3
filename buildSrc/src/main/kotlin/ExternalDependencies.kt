package com.xmlcalabash.build

import org.gradle.api.GradleException

/**************************************************************
 * This file is generated from tools/ExternalDependencies.xml *
 * with tools/external-dependencies.xsl; do not update this   *
 * file by hand!                                              *
 **************************************************************/

object ExternalDependencies {
    val steps = mapOf<String,List<String>>(
        "xmlcalabash" to listOf("org.jetbrains.kotlin:kotlin-stdlib:2.1.20",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2",
            "org.jetbrains.kotlin:kotlin-reflect:2.1.20",

            "com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.21.3",
            "com.fasterxml.jackson.dataformat:jackson-dataformat-toml:2.21.3",
            "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.21.3",
            "com.github.f4b6a3:uuid-creator:6.1.1",
            "com.networknt:json-schema-validator:1.5.9",
            "com.nwalsh:sinclude:5.6.0",
            "com.vladsch.flexmark:flexmark-all:0.64.8",
            "commons-codec:commons-codec:1.22.0",
            "javax.activation:activation:1.1.1", // For mimetype mapping
            "nu.validator:htmlparser:1.4.16",
            "org.apache.commons:commons-compress:1.28.0",
            "org.apache.httpcomponents.client5:httpclient5:5.6.1",
            "org.brotli:dec:0.1.2",
            "org.jline:jline:3.30.6",
            "org.jline:jline-terminal-jansi:3.30.6",
            "org.nineml:coffeefilter:3.3.8",
            "org.nineml:coffeegrinder:3.3.8",
            "org.relaxng:jing:20241231",
            "org.tukaani:xz:1.12",
            "org.xmlresolver:xmlresolver:6.0.21",

            // I was using log4j but httpclient5 uses slf4j.
            // Could I get httpclient5 to use log4j? Maybe. ¯\_(ツ)_/¯
            // But I got tired of trying to figure it out so I did this instead.
            "org.slf4j:slf4j-api:2.0.17",
            "ch.qos.logback:logback-classic:1.5.18",
            "org.apache.logging.log4j:log4j-to-slf4j:2.25.4"),

        "asciidoctor" to listOf("org.asciidoctor:asciidoctorj:3.0.1",
            "org.asciidoctor:asciidoctorj-pdf:2.3.23"),

        "diagramming" to listOf("net.sourceforge.plantuml:plantuml:1.2026.2", // plantuml includes ditaa
            "de.rototor.jeuclid:jeuclid-core:3.1.14"),

        "plantuml" to listOf("net.sourceforge.plantuml:plantuml:1.2026.2"),

        "ditaa" to listOf("net.sourceforge.plantuml:plantuml:1.2026.2"),

        "mathml-to-svg" to listOf("de.rototor.jeuclid:jeuclid-core:3.1.14"),

        "ebnf-convert" to listOf("de.bottlecaps.ebnf-convert:ebnf-convert-lib:0.72"),

        "epubcheck" to listOf("org.w3c:epubcheck:5.3.0",
            "javax.activation:activation:1.1.1"),

        "find" to listOf("com.jayway.jsonpath:json-path:2.10.0",
            "net.minidev:json-smart:2.6.0"),

        "jsonpath" to listOf("com.jayway.jsonpath:json-path:2.10.0",
            "net.minidev:json-smart:2.6.0"),

        "json-diff" to listOf("com.github.java-json-tools:json-patch:1.13"),

        "json-patch" to listOf("com.github.java-json-tools:json-patch:1.13"),

        "markup-blitz" to listOf("de.bottlecaps:markup-blitz:1.9"),

        "metadata-extractor" to listOf("com.drewnoakes:metadata-extractor:2.20.0",
            "org.apache.pdfbox:pdfbox:3.0.7", // pdfbox dep for parsing XML in pdfbox
            "org.apache.pdfbox:pdfbox-io:3.0.7",
            "org.apache.pdfbox:xmpbox:3.0.7",
            "javax.xml.bind:jaxb-api:2.3.1"),

        "paged-media-fop" to listOf("org.apache.xmlgraphics:fop:2.11",
            "org.apache.avalon.framework:avalon-framework-api:4.3.1",
            "org.apache.avalon.framework:avalon-framework-impl:4.3.1",
            "javax.media:jai-core:1.1.3",
            "com.sun.media:jai-codec:1.1.3"),

        "paged-media-prince" to listOf("com.princexml:prince-java-wrapper:1.5.0"),

        "pdf" to listOf("org.apache.pdfbox:pdfbox:3.0.7",
            "org.apache.pdfbox:pdfbox-io:3.0.7",
            "org.apache.pdfbox:xmpbox:3.0.7"),

        "polyglot" to listOf("org.graalvm.polyglot:polyglot:23.1.5",
            "org.graalvm.polyglot:js:23.1.5",
            "org.graalvm.polyglot:python:23.1.5"),

        "railroad" to listOf("de.bottlecaps.rr:rr-lib:2.5",
            "de.bottlecaps.ebnf-convert:ebnf-convert-lib:0.72"),

        "rdf" to listOf("javax.activation:activation:1.1.1", // For mimetype mapping
            "org.apache.jena:jena:6.0.0",
            "org.apache.jena:jena-arq:6.0.0",
            "org.apache.jena:jena-core:6.0.0",
            "org.apache.jena:jena-iri:5.6.0",
            "org.semarglproject:semargl-core:0.7",
            "org.semarglproject:semargl-rdfa:0.7"),

        "selenium" to listOf("org.nineml:coffeegrinder:3.3.8",
            "org.nineml:coffeefilter:3.3.8",
            "org.seleniumhq.selenium:selenium-java:4.43.0"),

        "send-mail" to listOf("javax.activation:activation:1.1.1",
            "javax.mail:javax.mail-api:1.6.2",
            "com.sun.mail:javax.mail:1.6.2"),

        "tesseract" to listOf("net.sourceforge.tess4j:tess4j:5.19.0"),

        "trang" to listOf("org.relaxng:trang:20241231"),

        "unique-id" to listOf("com.github.f4b6a3:uuid-creator:6.1.1"),

        "xmlunit" to listOf("org.xmlunit:xmlunit-core:2.11.0"),

        "pebble" to listOf("io.pebbletemplates:pebble:4.1.1"))

  val implSteps = listOf("xmlcalabash", "send-mail")
  val exclSteps = listOf("polyglot")
  
  val compileSteps: List<String>
      get() {
        val compileSteps = mutableListOf<String>();
        for ((step, _) in steps) {
          if (step !in implSteps) {
            compileSteps.add(step)
          }
        }
        return compileSteps
      }

  val distributionSteps: List<String>
      get() {
        val distSteps = mutableListOf<String>();
        for ((step, _) in steps) {
          if (step !in implSteps && step !in exclSteps) {
            distSteps.add(step)
          }
        }
        return distSteps
      }

  fun of(keys: List<String>): List<String> {
    val unifiedDeps = mutableSetOf<String>()
    val versionCheck = mutableMapOf<String,String>()
    for (key in keys) {
      val deps = steps[key] ?: throw GradleException("No dependencies for ${key}")
      for (dep in deps) {
        unifiedDeps.add(dep)
        val colon = dep.lastIndexOf(":")
        val version = dep.substring(colon+1)
        val library = dep.substring(0, colon)
        if (library in versionCheck) {
          if (versionCheck[library] != version) {
            throw GradleException("Library version mismatch: ${dep} (also ${versionCheck[library]})")
          }
        }
        versionCheck[library] = version
      }
    }
    return unifiedDeps.toList()
  }

  fun version(pkg: String): String {
    for ((_, deps) in steps) {
      for (dep in deps) {
        val colon = dep.lastIndexOf(":")
        val version = dep.substring(colon+1)
        val library = dep.substring(0, colon)
        if (pkg == library) {
          return version
        }
      }
    }

    throw GradleException("No version for ${pkg} in ${this}")
  }
}
