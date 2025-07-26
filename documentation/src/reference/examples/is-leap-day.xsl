<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:f="http://example.com/ns/functions"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:function name="f:is-leap-day">
  <xsl:sequence select="f:is-leap-day(current-date())"/>
</xsl:function>

<xsl:function name="f:is-leap-day">
  <xsl:param name="date"/>
  <xsl:choose>
    <xsl:when test="$date instance of xs:date">
      <xsl:sequence select="month-from-date($date) = 2
                            and day-from-date($date) = 29"/>
    </xsl:when>
    <xsl:when test="$date instance of xs:dateTime">
      <xsl:sequence select="month-from-dateTime($date) = 2
                            and day-from-dateTime($date) = 29"/>
    </xsl:when>
    <xsl:when test="$date castable as xs:date">
      <xsl:variable name="dt" select="$date cast as xs:date"/>
      <xsl:sequence select="month-from-date($dt) = 2
                            and day-from-date($dt) = 29"/>
    </xsl:when>
    <xsl:when test="$date castable as xs:dateTime">
      <xsl:variable name="dt" select="$date cast as xs:dateTime"/>
      <xsl:sequence select="month-from-date($dt) = 2
                            and day-from-date($dt) = 29"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="false()"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

</xsl:stylesheet>
