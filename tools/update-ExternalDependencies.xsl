<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="no" omit-xml-declaration="yes"/>

<xsl:param name="updated.xml" as="document-node()" required="yes"/>

<xsl:variable name="updates" select="$updated.xml/*"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template match="/">
  <xsl:apply-templates/>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="repository/group/artifact">
  <xsl:variable name="group" select="../@id/string()"/>
  <xsl:variable name="artifact" select="@id/string()"/>
  <xsl:variable name="version" select="@version/string()"/>

  <xsl:variable name="new-version"
                select="$updates/group[@id=$group]/artifact[@id=$artifact]/@version/string()"/>

  <xsl:choose>
    <xsl:when test="empty($new-version) or $new-version = $version">
      <xsl:next-match/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:message select="$group||':'||$artifact|| ' ' || $version || ' != ' || $new-version"/>
      <xsl:copy>
        <xsl:copy-of select="@*"/>
        <xsl:attribute name="version" select="$new-version"/>
      </xsl:copy>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
