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
  <xsl:variable name="tests" select="collection($dir||'?select=*.xml;recurse=true')"/>
  <t:test-suite test-suite="{$test-suite}">
    <xsl:attribute name="xml:base" select="$dir"/>
    <xsl:for-each select="$tests/t:test">
      <xsl:text>&#10;&#10;</xsl:text>
      <t:test>
        <xsl:attribute name="xml:base" select="substring-after(base-uri(.), $dir)"/>
        <xsl:attribute name="name" select="tokenize(base-uri(.), '/')[last()]"/>
        <xsl:apply-templates select="@*,node()"/>
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
