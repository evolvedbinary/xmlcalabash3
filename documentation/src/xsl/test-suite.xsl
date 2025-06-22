<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:t="http://xproc.org/ns/testsuite/3.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                expand-text="yes"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="no"/>
<xsl:strip-space elements="*"/>

<xsl:param name="test-suite" as="xs:string" required="yes"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template name="xsl:initial-template">
  <xsl:variable name="dir" select="resolve-uri('../../../tests/' || $test-suite || '/test-suite/tests/',
                                               static-base-uri())"/>
  <xsl:variable name="tests" select="collection($dir||'?select=*.xml;recurse=yes')"/>

  <xsl:variable name="rdir" select="resolve-uri('../../../test-driver/build/test-results/' || $test-suite || '/',
                                               static-base-uri())"/>
  <xsl:variable name="results" as="document-node()*">
    <xsl:try>
      <xsl:for-each select="collection($rdir||'?select=*.xml;recurse=yes;on-error=warning')">
        <xsl:variable name="parts" select="tokenize(base-uri(.), '/')"/>
        <xsl:variable name="name" select="$parts[count($parts) - 1]"/>
        <xsl:document>
          <t:result name="{$name}">
            <xsl:sequence select="."/>
          </t:result>
        </xsl:document>
      </xsl:for-each>
      <xsl:catch>
        <xsl:message select="'Failed to read results for ' || $test-suite || ' test suite'"/>
      </xsl:catch>
    </xsl:try>
  </xsl:variable>

  <xsl:message select="$rdir"/>
  <xsl:message>{count($tests)} tests, {count($results)} results.</xsl:message>

  <t:test-suite test-suite="{$test-suite}">
    <xsl:attribute name="xml:base" select="$dir"/>
    <xsl:for-each select="$tests/t:test">
      <xsl:text>&#10;&#10;</xsl:text>
      <xsl:variable name="name" select="tokenize(base-uri(.), '/')[last()]"/>
      <t:test>
        <xsl:attribute name="xml:base" select="substring-after(base-uri(.), $dir)"/>
        <xsl:attribute name="name" select="$name"/>
        <xsl:apply-templates select="@*,node()"/>
        <xsl:sequence select="$results/t:result[@name = $name]"/>
      </t:test>
    </xsl:for-each>
    <xsl:text>&#10;&#10;</xsl:text>
  </t:test-suite>
</xsl:template>
    
<xsl:template match="t:*[@src]">
  <xsl:element name="{node-name(.)}" namespace="http://xproc.org/ns/testsuite/3.0">
    <xsl:sequence select="@* except @src"/>
    <xsl:sequence select="doc(resolve-uri(@src, base-uri(.)))"/>
  </xsl:element>
</xsl:template>

<xsl:template match="t:file-environment[@src]" priority="10">
  <xsl:sequence select="doc(resolve-uri(@src, base-uri(.)))"/>
</xsl:template>

</xsl:stylesheet>
