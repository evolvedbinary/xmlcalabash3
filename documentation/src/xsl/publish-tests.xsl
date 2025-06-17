<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:t="http://xproc.org/ns/testsuite/3.0"
                xmlns:f="http://xproc.org/ns/functions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="h t xs"
                expand-text="yes" version="3.0">

<xsl:output method="html" html-version="5" encoding="utf-8" indent="no"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:variable name="max-width" select="50"/>
<xsl:variable name="test-suite" select="/t:test-suite/@test-suite/string()"/>

<xsl:template match="/t:test-suite">
  <xsl:variable name="dir" select="resolve-uri('../../build/test-suites/' || @test-suite || '/tests/')"/>
  <xsl:for-each select="t:test">
    <xsl:result-document href="{$dir}{replace(@name, '.xml$', '.html')}">
      <xsl:apply-templates select="."/>
    </xsl:result-document>
    <xsl:result-document href="{$dir}{@name}" method="xml" indent="yes">
      <xsl:sequence select="."/>
    </xsl:result-document>
  </xsl:for-each>

  <html>
    <head>
      <title>{@test-suite/string()}</title>
      <link rel="stylesheet" href="../css/test-suite.css"/>
    </head>
    <body>
      <div class="nav">
        <a href="../index.html">Test suites</a>
      </div>
      <article>
        <h1>{@test-suite/string()}</h1>

        <table>
          <thead>
            <tr>
              <th>Test suite</th>
              <th>Test</th>
              <th>Expected result</th>
            </tr>
          </thead>
          <tbody>
            <xsl:for-each select="t:test">
              <xsl:sort select="lower-case(@name)"/>

              <tr>
                <td>{../@test-suite/string()}</td>
                <td>
                  <a href="tests/{replace(@name, '.xml$', '.html')}">
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
</xsl:template>

<xsl:template name="test">
  <xsl:param name="test" as="element(t:test)?"/>
  <td>
    <xsl:if test="exists($test)">
      <a href="tests/{replace($test/@name, '.xml$', '.html')}">{replace($test/@name, '.xml$', '')}</a>
    </xsl:if>
  </td>
</xsl:template>

<xsl:template match="t:test">
  <html>
    <head>
      <title>{t:info/t:title/string()}</title>
      <link rel="stylesheet" href="../../css/test-suite.css"/>
      <link rel="stylesheet" href="../../css/pygments.css"/>
    </head>
    <body>
      <div class="nav">
        <a href="../../index.html">Test index</a>
        <xsl:text> | </xsl:text>
        <a href="../index.html">Test suite</a>
      </div>

      <xsl:if test="@features">
        <div class="features">
          <p><xsl:text>Features: </xsl:text>
          <xsl:for-each select="tokenize(@features, '\s+')">
            <xsl:sort select="."/>
            <a href="../../{f:feature-filename(.)}">{.}</a>
            <xsl:if test="position() gt 1">, </xsl:if>
          </xsl:for-each>
          </p>
        </div>
      </xsl:if>

      <article>
        <h1>{t:info/t:title/string()}</h1>
        <xsl:apply-templates select="t:description"/>

        <xsl:variable name="test-uri" select="tokenize(base-uri(.), '/')[last()]"/>

        <xsl:choose>
          <xsl:when test="@expected = 'pass'">
            <p>Test <a href="{$test-uri}">{test-uri}</a> is expected to pass.</p>
          </xsl:when>
          <xsl:when test="@expected = 'fail'">
            <xsl:variable name="codes" select="tokenize(@code, '\s+')"/>
            <p>
              <xsl:text>Test </xsl:text>
              <a href="{$test-uri}">{test-uri}</a>
              <xsl:text> is expected to fail with </xsl:text>
              <xsl:choose>
                <xsl:when test="count($codes) = 1">
                  <xsl:text>error code </xsl:text>
                  <xsl:value-of select="$codes"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:text>with one of these error codes: </xsl:text>
                  <xsl:value-of select="string-join($codes, ', ')"/>
                </xsl:otherwise>
              </xsl:choose>
              <xsl:text>.</xsl:text>
            </p>
          </xsl:when>
          <xsl:otherwise>
            <xsl:message select="'@expected = ' || @expected || ' ??? (' || $test-uri || ')'"/>
          </xsl:otherwise>
        </xsl:choose>

        <xsl:apply-templates select="t:input"/>
        <xsl:apply-templates select="t:option"/>
        <xsl:apply-templates select="t:catalog"/>
        <xsl:apply-templates select="t:pipeline"/>
        <xsl:apply-templates select="t:result"/>
        <xsl:apply-templates select="t:file-environment"/>
        <xsl:apply-templates select="t:schematron"/>
        <xsl:apply-templates select="t:info/t:revision-history"/>
      </article>
    </body>
  </html>
</xsl:template>

<xsl:template match="t:info"/>

<xsl:template match="t:description">
  <div class="description">
    <xsl:apply-templates/>
  </div>
</xsl:template>

<xsl:template match="t:option[empty(preceding-sibling::t:option)]" priority="10">
  <div class="options">
    <h2>Options</h2>
    <table>
      <thead>
        <tr><th>Name</th><th>Initializer</th></tr>
      </thead>
      <tbody>
        <xsl:for-each select="../t:option">
          <tr>
            <td>{@name/string()}</td>
            <td>{@select/string()}</td>
          </tr>
        </xsl:for-each>
      </tbody>
    </table>
  </div>
</xsl:template>

<xsl:template match="t:option"/>

<xsl:template match="t:input">
  <xsl:call-template name="file-contents">
    <xsl:with-param name="title" select="'Input: ' || @port"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="t:catalog">
  <xsl:call-template name="file-contents">
    <xsl:with-param name="title" select="'Catalog'"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="t:pipeline">
  <xsl:call-template name="file-contents">
    <xsl:with-param name="title" select="'The pipeline'"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="t:result">
  <xsl:call-template name="file-contents">
    <xsl:with-param name="title" select="'Result'"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="t:schematron">
  <xsl:call-template name="file-contents">
    <xsl:with-param name="title" select="'Schematron checks'"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="t:revision-history">
  <div class="revhistory">
    <h2>Revision history</h2>
    <dl>
      <xsl:for-each select="t:revision">
        <dt>
          <xsl:value-of select="format-date(xs:date(substring(t:date, 1, 10)),
                                            '[D01] [MNn,*-3] [Y0001]')"/>
          <xsl:text>, </xsl:text>
          <xsl:value-of select="t:author/t:name"/>
        </dt>
        <dd>
          <xsl:apply-templates select="t:description/*"/>
        </dd>
      </xsl:for-each>
    </dl>
  </div>
</xsl:template>

<xsl:template match="t:*">
  <xsl:message>Unsupported: {local-name(.)} ({tokenize(base-uri(.), '/')[last()]})</xsl:message>
  <div style="color:red">{local-name(.)}</div>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="h:*">
  <element name="{local-name(.)}" namespace="http://www.w3.org/1999/xhtml">
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates/>
  </element>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="t:file-environment">
  <xsl:variable name="env" select="."/>
  <div>
    <h2>Filesystem environment</h2>
    <xsl:if test="$env//t:folder/@writable='false' or $env//*/@readable='false'">
      <p>This configuration cannot be achieved on Windows.</p>
    </xsl:if>
    <xsl:apply-templates select="$env" mode="table"/>
  </div>
</xsl:template>

<xsl:template match="t:file-environment" mode="table">
  <xsl:variable name="env" select="."/>

  <table class="fileenv">
    <thead>
      <tr>
        <th>Directory</th>
        <th>Perm</th>
        <th>Filename</th>
        <th>Last-modified</th>
        <th>Size</th>
        <th>Encoding</th>
        <th>Contents (abbreviated)</th>
      </tr>
    </thead>
    <tbody>
      <xsl:variable name="nested" as="xs:string*">
        <xsl:for-each select="$env/t:folder">
          <xsl:variable name="path" select="@path/string()"/>
          <xsl:sequence select="$env/t:file[starts-with(@path, $path)] ! generate-id(.)"/>
        </xsl:for-each>
      </xsl:variable>

      <xsl:apply-templates select="$env/t:file[not(generate-id(.) = $nested)]"/>

      <xsl:for-each select="$env/t:folder">
        <xsl:sort select="@path"/>
        <xsl:variable name="path" select="@path/string()"/>
        <tr>
          <td>
            <code>{$path}</code>
          </td>
          <td>
            <code>
              <xsl:value-of select="'d'"/>
              <xsl:value-of select="if (@readable = 'false') then '-' else 'r'"/>
              <xsl:value-of select="if (@writable = 'false') then '-' else 'w'"/>
              <xsl:value-of select="if (@executable != 'true') then '-' else 'x'"/>
            </code>
          </td>
          <td></td>
          <td></td>
          <td></td>
          <td></td>
          <td></td>
        </tr>
        <xsl:for-each select="$env/t:file[starts-with(@path, $path)]">
          <xsl:sort select="@path"/>
          <xsl:apply-templates select=".">
            <xsl:with-param name="path" select="$path"/>
          </xsl:apply-templates>
        </xsl:for-each>
      </xsl:for-each>
    </tbody>
  </table>
</xsl:template>

<xsl:template match="t:file">
  <xsl:param name="path" as="xs:string" select="''"/>
  <tr>
    <td>  </td>
    <td>
      <code>
        <xsl:value-of select="'.'"/>
        <xsl:value-of select="if (@readable = 'false') then '-' else 'r'"/>
        <xsl:value-of select="if (@writable = 'false') then '-' else 'w'"/>
        <xsl:value-of select="if (@executable != 'true') then '-' else 'x'"/>
      </code>
    </td>
    <td>
      <code>{if ($path = '') then @path/string() else substring-after(@path, $path||'/')}</code>
    </td>
    <td><code>{@last-modified/string()}</code></td>
    <td align="right"><code>{string-length(.)}</code></td>
    <td><code>{@encoding/string()}</code></td>

    <xsl:variable name="all-content" as="xs:string">
      <xsl:choose>
        <xsl:when test="contains(string(.), '&#10;')">
          <xsl:value-of select="substring-before(., '&#10;') || '…'"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="string(.)"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="content"
                  select="if (string-length($all-content) gt 32)
                          then substring($all-content, 0, 32) || '…'
                          else $all-content"/>

    <td><code>{$content}</code></td>
  </tr>
</xsl:template>

<!-- ============================================================ -->

<xsl:template name="file-contents">
  <xsl:param name="title" as="xs:string" required="yes"/>

  <xsl:variable name="input" as="document-node()">
    <xsl:document>
      <xsl:sequence select="node()"/>
    </xsl:document>
  </xsl:variable>

  <xsl:variable name="serial"
                select="serialize($input, map{'method': 'xml', 'indent':true()})"/>
  <div>
    <h2>{$title}</h2>
    <pre class="xml highlight">
      <code>
        <xsl:try>
          <xsl:variable name="xml" select="parse-xml($serial)"/>
          <xsl:apply-templates select="$xml/*" mode="pretty-print"/>
          <xsl:catch>
            <xsl:value-of select="$serial"/>
          </xsl:catch>
        </xsl:try>
      </code>
    </pre>
  </div>
</xsl:template>

<xsl:template match="*" mode="pretty-print">
  <xsl:variable name="preceding-ws" select="preceding::text()[normalize-space(.) = '']"/>
  <xsl:variable name="ws" select="tokenize(string-join($preceding-ws, ''), '&#10;')[last()]"/>
  <xsl:variable name="t5" select="'                         '"/>
  <xsl:variable name="tag" select="string(node-name(.))"/>
  <xsl:variable name="pad" select="substring(concat($t5,$t5,$t5,$t5,$t5), 1,
                                             string-length($ws) + string-length($tag) + 2)"/>

  <xsl:variable name="ns" as="namespace-node()*"
                select="f:ns(../namespace::*, namespace::*)"/>

  <span>
    <span class="nt">
      <span>&lt;</span>
      <span>
        <xsl:value-of select="node-name(.)"/>
      </span>
    </span>

    <xsl:call-template name="attributes">
      <xsl:with-param name="values" select="$ns, attribute::*"/>
      <xsl:with-param name="width" select="string-length($pad)"/>
      <xsl:with-param name="pad" select="$pad"/>
      <xsl:with-param name="first" select="true()"/>
    </xsl:call-template>

    <span class="nt">
      <xsl:choose>
        <xsl:when test="empty(node())">
          <xsl:text>/&gt;</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>&gt;</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </span>
  </span>

  <xsl:apply-templates mode="pretty-print"/>

  <xsl:if test="exists(node())">
    <span class="nt">
      <xsl:text>&lt;/</xsl:text>
      <span>
        <xsl:value-of select="node-name(.)"/>
      </span>
      <xsl:text>&gt;</xsl:text>
    </span>
  </xsl:if>
</xsl:template>

<xsl:template match="text()" mode="pretty-print">
  <span>
    <xsl:value-of select="."/>
  </span>
</xsl:template>

<xsl:template name="attributes">
  <xsl:param name="values" as="node()*"/>
  <xsl:param name="width" as="xs:integer"/>
  <xsl:param name="pad" as="xs:string"/>
  <xsl:param name="first" as="xs:boolean"/>

  <xsl:choose>
    <xsl:when test="empty($values)"/>
    <xsl:when test="$values[1]/self::namespace-node()">
      <xsl:variable name="nswidth" select="string-length(local-name($values[1])) + 4
                                           + string-length($values[1])"/>

      <xsl:choose>
        <xsl:when test="not($first) and $width + $nswidth gt $max-width">
          <xsl:text>&#10;</xsl:text>
          <xsl:sequence select="$pad"/>
          <xsl:apply-templates select="$values[1]" mode="attribute"/>
          <xsl:call-template name="attributes">
            <xsl:with-param name="values" select="$values[position() gt 1]"/>
            <xsl:with-param name="width" select="string-length($pad)"/>
            <xsl:with-param name="pad" select="$pad"/>
            <xsl:with-param name="first" select="false()"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text> </xsl:text>
          <xsl:apply-templates select="$values[1]" mode="attribute"/>
          <xsl:call-template name="attributes">
            <xsl:with-param name="values" select="$values[position() gt 1]"/>
            <xsl:with-param name="width" select="$width + $nswidth"/>
            <xsl:with-param name="pad" select="$pad"/>
            <xsl:with-param name="first" select="false()"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="awidth" select="string-length(string(node-name($values[1]))) + 1
                                          + string-length($values[1])"/>
      <xsl:choose>
        <xsl:when test="not($first) and $width + $awidth gt $max-width">
          <xsl:text>&#10;</xsl:text>
          <xsl:sequence select="$pad"/>
          <xsl:apply-templates select="$values[1]" mode="attribute"/>
          <xsl:call-template name="attributes">
            <xsl:with-param name="values" select="$values[position() gt 1]"/>
            <xsl:with-param name="width" select="string-length($pad)"/>
            <xsl:with-param name="pad" select="$pad"/>
            <xsl:with-param name="first" select="false()"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text> </xsl:text>
          <xsl:apply-templates select="$values[1]" mode="attribute"/>
          <xsl:call-template name="attributes">
            <xsl:with-param name="values" select="$values[position() gt 1]"/>
            <xsl:with-param name="width" select="$width + $awidth"/>
            <xsl:with-param name="pad" select="$pad"/>
            <xsl:with-param name="first" select="false()"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="namespace-node()" mode="attribute">
  <span class="na">
    <xsl:choose>
      <xsl:when test="local-name(.) = ''">
        <xsl:attribute name="class" select="'nsdecl default'"/>
        <span>xmlns</span>
        <span>=</span>
        <span class="s">
          <span>"</span>
          <span>
            <xsl:value-of select="replace(., '&quot;', '&amp;quot;')"/>
          </span>
          <span>"</span>
        </span>
      </xsl:when>
      <xsl:otherwise>
        <span>xmlns</span>
        <span>:</span>
        <span>
          <xsl:value-of select="local-name(.)"/>
        </span>
        <span>=</span>
        <span class="s">
          <span>"</span>
          <span class="uri">
            <xsl:value-of select="replace(., '&quot;', '&amp;quot;')"/>
          </span>
          <span>"</span>
        </span>
      </xsl:otherwise>
    </xsl:choose>
  </span>
</xsl:template>

<xsl:template match="attribute()" mode="attribute">
  <xsl:variable name="q" select="if (contains(., '&quot;')) then '''' else '&quot;'"/>
  <xsl:variable name="v"
                select="if (contains(., '&quot;'))
                        then replace(., '''', '&amp;apos;')
                        else ."/>

  <span class="na">
    <span>
      <xsl:value-of select="node-name(.)"/>
    </span>
    <span>=</span>
    <span>{$q}</span>

    <xsl:variable name="base"
                  select="resolve-uri('../../build/test-suites/'
                                      || $test-suite || '/tests/')"/>

    <xsl:variable name="link" as="xs:boolean?">
      <xsl:if test="namespace-uri(.) = '' and local-name(.) = 'href'">
        <xsl:try>
          <xsl:variable name="resolved" select="resolve-uri(., $base)"/>
          <xsl:sequence
              select="not(starts-with($resolved, 'https://xmlcalabash.com/ext/library/'))
                      and unparsed-text-available($resolved)"/>
          <xsl:catch>
            <xsl:sequence select="false()"/>
          </xsl:catch>
        </xsl:try>
      </xsl:if>
    </xsl:variable>

    <span class="s">
      <xsl:choose>
        <xsl:when test="$link = true()">
          <a href="{.}">{$v}</a>
        </xsl:when>
        <xsl:otherwise>{$v}</xsl:otherwise>
      </xsl:choose>
    </span>

    <span>{$q}</span>
  </span>
</xsl:template>

<xsl:function name="f:ns" as="namespace-node()*">
  <xsl:param name="inscope-namespaces" as="namespace-node()*"/>
  <xsl:param name="namespaces" as="namespace-node()*"/>

  <xsl:for-each select="$namespaces">
    <xsl:variable name="ns" select="."/>
    <xsl:if test="empty($inscope-namespaces[local-name(.) = local-name($ns)
                        and namespace-uri(.) = namespace-uri($ns)])
                  and local-name(.) != 'xml'">
      <xsl:sequence select="$ns"/>
    </xsl:if>
  </xsl:for-each>
</xsl:function>

<xsl:function name="f:feature-filename" as="xs:string">
  <xsl:param name="name" as="xs:string"/>
  <xsl:sequence select="'feature-' || replace($name, ':', '-') => replace('/', '-') || '.html'"/>
</xsl:function>

</xsl:stylesheet>
