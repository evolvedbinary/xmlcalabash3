<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs" expand-text="yes"
                version="3.0">

<xsl:output method="text" encoding="utf-8" indent="no"/>
<xsl:strip-space elements="*"/>

<xsl:key name="group-artifact" match="repository/group/artifact" use="../@id||':'||@id"/>
<xsl:key name="artifact" match="repository/group/artifact" use="@id"/>

<xsl:template match="/">
  <xsl:apply-templates select="//kotlin"/>
</xsl:template>

<xsl:template match="depends-on/*">
  <xsl:text>        "{local-name(.)}" to listOf(</xsl:text>
  <xsl:apply-templates select="*"/>
  <xsl:if test="following-sibling::*">
    <xsl:text>),&#10;&#10;</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="depends-on/*/artifact">
  <xsl:variable name="artifact" as="element()">
    <xsl:choose>
      <xsl:when test="@group">
        <xsl:sequence select="key('group-artifact', @group||':'||@ref)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="key('artifact', @ref)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:if test="preceding-sibling::*[1]/self::artifact">
    <xsl:text>&#10;</xsl:text>
  </xsl:if>

  <xsl:if test="preceding-sibling::artifact">
    <xsl:variable name="pad" select="'                                        '"/>
    <xsl:variable name="indent" select="12"/>
    <xsl:text>{substring($pad, 1, $indent)}</xsl:text>
  </xsl:if>

  <xsl:text>"{$artifact/../@id/string()}</xsl:text>
  <xsl:text>:{$artifact/@id/string()}</xsl:text>
  <xsl:text>:{$artifact/@version/string()}"</xsl:text>
  <xsl:if test="following-sibling::artifact">,</xsl:if>
</xsl:template>

<xsl:template match="depends-on/*/text">
  <xsl:value-of select="string(.)"/>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

<xsl:template match="depends-on/*/text-line">
  <xsl:choose>
    <xsl:when test="empty(node())">
      <xsl:text>&#10;</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>            {string(.)}</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>&#10;</xsl:text>
</xsl:template>

</xsl:stylesheet>
