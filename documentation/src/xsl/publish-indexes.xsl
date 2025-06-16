<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:f="http://xproc.org/ns/functions"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:t="http://xproc.org/ns/testsuite/3.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="#all"
                expand-text="yes"
                version="3.0">

<xsl:output method="html" html-version="5" encoding="utf-8" indent="no"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template name="xsl:initial-template">
  <xsl:variable name="dir" select="resolve-uri('../../build/suites')"/>
  <xsl:variable name="tests" select="collection($dir||'?select=*.xml')"/>

  <xsl:variable name="p-elements"
                select="distinct-values(
                        ($tests/t:test-suite/t:test/t:pipeline//p:*
                         except ($tests/t:test-suite/t:test/t:pipeline//p:declare-step))
                         ! node-name(.))"/>
  <xsl:variable name="cx-elements"
                select="distinct-values($tests/t:test-suite/t:test/t:pipeline//cx:* ! node-name(.))"/>

  <html>
    <head>
      <title>Test suite indexes</title>
      <link rel="stylesheet" href="css/test-suite.css"/>
      <link rel="stylesheet" href="css/pygments.css"/>
    </head>
    <body>
      <article>
        <h1>Test suites</h1>
        <dl>
          <xsl:for-each select="$tests/t:test-suite">
            <xsl:sort select="@test-suite"/>
            <dt><a href="{@test-suite}/index.html">{@test-suite/string()}</a></dt>
          </xsl:for-each>
        </dl>

        <div class="bytype">
          <h2>Index by element type</h2>
          <xsl:variable  name="list" as="xs:QName*">
            <xsl:for-each select="$p-elements">
              <xsl:sort select="local-name-from-QName(.)"/>
              <xsl:sequence select="."/>
            </xsl:for-each>
            <xsl:for-each select="$cx-elements">
              <xsl:sort select="local-name-from-QName(.)"/>
              <xsl:sequence select="."/>
            </xsl:for-each>
          </xsl:variable>

          <table>
            <tbody>
              <xsl:variable name="rows" select="(count($list) + 4) idiv 5"/>
              <xsl:for-each select="1 to $rows">
                <xsl:variable name="index" select="."/>
                <tr>
                  <xsl:call-template name="element-type">
                    <xsl:with-param name="name" select="$list[$index]"/>
                    <xsl:with-param name="suites" select="$tests"/>
                  </xsl:call-template>
                  <xsl:call-template name="element-type">
                    <xsl:with-param name="name" select="$list[$index + $rows]"/>
                    <xsl:with-param name="suites" select="$tests"/>
                  </xsl:call-template>
                  <xsl:call-template name="element-type">
                    <xsl:with-param name="name" select="$list[$index + (2*$rows)]"/>
                    <xsl:with-param name="suites" select="$tests"/>
                  </xsl:call-template>
                  <xsl:call-template name="element-type">
                    <xsl:with-param name="name" select="$list[$index + (3*$rows)]"/>
                    <xsl:with-param name="suites" select="$tests"/>
                  </xsl:call-template>
                  <xsl:call-template name="element-type">
                    <xsl:with-param name="name" select="$list[$index + (4*$rows)]"/>
                    <xsl:with-param name="suites" select="$tests"/>
                  </xsl:call-template>
                </tr>
              </xsl:for-each>
            </tbody>
          </table>
        </div>

        <div class="byfeature">
          <h2>Index by feature</h2>
          <xsl:variable name="list" as="xs:string*">
            <xsl:for-each select="distinct-values($tests/t:test-suite/t:test/@features ! tokenize(., '\s+'))">
              <xsl:sort select="."/>
              <xsl:sequence select="."/>
            </xsl:for-each>
          </xsl:variable>

          <table>
            <tbody>
              <xsl:variable name="rows" select="(count($list) + 4) idiv 5"/>
              <xsl:for-each select="1 to $rows">
                <xsl:variable name="index" select="."/>
                <tr>
                  <xsl:call-template name="feature">
                    <xsl:with-param name="name" select="$list[$index]"/>
                    <xsl:with-param name="suites" select="$tests"/>
                  </xsl:call-template>
                  <xsl:call-template name="feature">
                    <xsl:with-param name="name" select="$list[$index + $rows]"/>
                    <xsl:with-param name="suites" select="$tests"/>
                  </xsl:call-template>
                  <xsl:call-template name="feature">
                    <xsl:with-param name="name" select="$list[$index + (2*$rows)]"/>
                    <xsl:with-param name="suites" select="$tests"/>
                  </xsl:call-template>
                  <xsl:call-template name="feature">
                    <xsl:with-param name="name" select="$list[$index + (3*$rows)]"/>
                    <xsl:with-param name="suites" select="$tests"/>
                  </xsl:call-template>
                  <xsl:call-template name="feature">
                    <xsl:with-param name="name" select="$list[$index + (4*$rows)]"/>
                    <xsl:with-param name="suites" select="$tests"/>
                  </xsl:call-template>
                </tr>
              </xsl:for-each>
            </tbody>
          </table>
        </div>
      </article>
    </body>
  </html>
</xsl:template>

<xsl:template name="element-type">
  <xsl:param name="name" as="xs:QName?"/>
  <xsl:param name="suites" as="document-node()*"/>
  <td>
    <xsl:if test="exists($name)">
      <xsl:variable name="prefix"
                    select="if (namespace-uri-from-QName($name) = 'http://www.w3.org/ns/xproc')
                            then 'p'
                            else 'cx'"/>
      <xsl:variable name="tests" select="$suites/t:test-suite/t:test[t:pipeline//*[node-name(.)=$name]]"/>

      <xsl:choose>
        <xsl:when test="count($tests) = 1">
          <a href="{$tests/../@test-suite}/tests/{replace($tests/@name, '.xml$', '.html')}">
            <xsl:sequence select="concat($prefix, ':', local-name-from-QName($name))"/>
          </a>
        </xsl:when>
        <xsl:otherwise>
          <a href="{$prefix}-{local-name-from-QName($name)}.html">
            <xsl:sequence select="concat($prefix, ':', local-name-from-QName($name))"/>
          </a>
          <xsl:call-template name="element-index">
            <xsl:with-param name="name" select="$name"/>
            <xsl:with-param name="tests" select="$tests"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>

      <xsl:text> (</xsl:text>
      <xsl:value-of select="count($tests)"/>
      <xsl:text>)</xsl:text>
    </xsl:if>
  </td>
</xsl:template>

<xsl:template name="element-index">
  <xsl:param name="name" as="xs:QName?"/>
  <xsl:param name="tests" as="element(t:test)*"/>

  <xsl:variable name="prefix"
                select="if (namespace-uri-from-QName($name) = 'http://www.w3.org/ns/xproc')
                        then 'p'
                        else 'cx'"/>

  <xsl:result-document href="{$prefix}-{local-name-from-QName($name)}.html">
    <html>
      <head>
        <title>
          <xsl:text>Tests that use </xsl:text>
          <xsl:sequence select="concat($prefix, ':', local-name-from-QName($name))"/>
        </title>
        <link rel="stylesheet" href="css/test-suite.css"/>
        <link rel="stylesheet" href="css/pygments.css"/>
      </head>
      <body>
        <div class="nav">
          <a href="index.html">Test suites</a>
        </div>
        <article>
          <h1>
            <xsl:text>Tests that use </xsl:text>
            <xsl:sequence select="concat($prefix, ':', local-name-from-QName($name))"/>
          </h1>
          <table>
            <thead>
              <tr>
                <th>Test suite</th>
                <th>Test</th>
                <th>Expected result</th>
              </tr>
            </thead>
            <tbody>
              <xsl:for-each select="$tests">
                <xsl:sort select="../@test-suite"/>
                <xsl:sort select="@name"/>
                <tr>
                  <td>{../@test-suite/string()}</td>
                  <td>
                    <a href="{../@test-suite}/tests/{replace(@name, '.xml$', '.html')}">
                      <xsl:value-of select="replace(@name, '.xml$', '')"/>
                    </a>
                  </td>
                  <td>{@expected/string()}</td>
                </tr>
              </xsl:for-each>
            </tbody>
          </table>
        </article>
      </body>
    </html>
  </xsl:result-document>
</xsl:template>

<xsl:template name="feature">
  <xsl:param name="name" as="xs:string?"/>
  <xsl:param name="suites" as="document-node()*"/>
  <td>
    <xsl:if test="exists($name)">
      <xsl:variable name="tests" select="$suites/t:test-suite/t:test[contains-token(@features, $name)]"/>

      <xsl:choose>
        <xsl:when test="count($tests) = 1">
          <a href="{$tests/../@test-suite}/tests/{replace($tests/@name, '.xml$', '.html')}">
            <xsl:sequence select="$name"/>
          </a>
        </xsl:when>
        <xsl:otherwise>
          <a href="{f:feature-filename($name)}">
            <xsl:sequence select="$name"/>
          </a>
          <xsl:call-template name="feature-index">
            <xsl:with-param name="name" select="$name"/>
            <xsl:with-param name="tests" select="$tests"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>

      <xsl:text> (</xsl:text>
      <xsl:value-of select="count($tests)"/>
      <xsl:text>)</xsl:text>
    </xsl:if>
  </td>
</xsl:template>

<xsl:template name="feature-index">
  <xsl:param name="name" as="xs:string"/>
  <xsl:param name="tests" as="element(t:test)*"/>

  <xsl:result-document href="{f:feature-filename($name)}">
    <html>
      <head>
        <title>
          <xsl:text>Tests that use the “</xsl:text>
          <xsl:sequence select="$name"/>
          <xsl:text>” feature</xsl:text>
        </title>
        <link rel="stylesheet" href="css/test-suite.css"/>
        <link rel="stylesheet" href="css/pygments.css"/>
      </head>
      <body>
        <div class="nav">
          <a href="index.html">Test suites</a>
        </div>
        <article>
          <h1>
            <xsl:text>Tests that use the “</xsl:text>
            <xsl:sequence select="$name"/>
            <xsl:text>” feature</xsl:text>
          </h1>
          <table>
            <thead>
              <tr>
                <th>Test suite</th>
                <th>Test</th>
                <th>Expected result</th>
              </tr>
            </thead>
            <tbody>
              <xsl:for-each select="$tests">
                <xsl:sort select="../@test-suite"/>
                <xsl:sort select="@name"/>
                <tr>
                  <td>{../@test-suite/string()}</td>
                  <td>
                    <a href="{../@test-suite}/tests/{replace(@name, '.xml$', '.html')}">
                      <xsl:value-of select="replace(@name, '.xml$', '')"/>
                    </a>
                  </td>
                  <td>{@expected/string()}</td>
                </tr>
              </xsl:for-each>
            </tbody>
          </table>
        </article>
      </body>
    </html>
  </xsl:result-document>
</xsl:template>

<xsl:function name="f:feature-filename" as="xs:string">
  <xsl:param name="name" as="xs:string"/>
  <xsl:sequence select="'feature-' || replace($name, ':', '-') => replace('/', '-') || '.html'"/>
</xsl:function>

</xsl:stylesheet>
