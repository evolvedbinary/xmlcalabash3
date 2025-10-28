<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="no"/>

<xsl:param name="report-directory" select="resolve-uri('../../build')"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template name="xsl:initial-template">
  <xsl:variable name="path" select="substring-after($report-directory, 'file:')"/>
  <xsl:variable name="reports"
                select="collection('file://'||$path||'?select=*.xml')"/>

  <xsl:if test="empty($reports)">
    <xsl:message terminate="yes" select="'No reports in ' || $report-directory"/>
  </xsl:if>

  <xsl:for-each select="$reports">
    <xsl:if test="empty(/testsuite)">
      <xsl:message terminate="yes" select="'Non-report XML in ' || $report-directory"/>
    </xsl:if>
  </xsl:for-each>

  <xsl:variable name="R" select="$reports[1]/testsuite"/>
  <testsuite>
    <xsl:copy-of select="$R/@* except ($R/@time | $R/@tests | $R/@errors | $R/@skipped)"/>
    <xsl:for-each select="('time', 'tests', 'errors', 'skipped')">
      <xsl:variable name="att" select="."/>
      <xsl:attribute name="{$att}"
                     select="sum($reports/testsuite/@*[local-name(.)=$att])"/>
    </xsl:for-each>
    <xsl:sequence select="$R/properties"/>

    <xsl:if test="contains($R/properties/property[@name='saxonVersion'], 'HE')">
      <xsl:message terminate="yes" select="'Test suites run with HE not EE'"/>
    </xsl:if>

    <xsl:sequence select="$reports/testsuite/testcase"/>
  </testsuite>
</xsl:template>
