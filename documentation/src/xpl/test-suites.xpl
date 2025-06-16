<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                exclude-inline-prefixes="c cx"
                name="main"
                version="3.0">
  <p:output port="result" serialization="map{'indent':true(), 'omit-xml-declaration':true()}"/>

  <p:directory-list path="../../../tests/extra-suite/test-suite/tests"
                    include-filter=".*\.xml"/>

  <p:for-each>
    <p:with-input select="/c:directory/c:file"/>

    <p:variable name="suite"
                select="substring-before(base-uri(/*), '/test-suite/tests')
                        => substring-after('/tests/')"/>
    <p:variable name="output"
                select="resolve-uri(/*/@name,
                                    resolve-uri('../../build/test-suite/' || $suite || '/tests/'))"/>

    <p:store href="{$output}">
      <p:with-input port="source" href="{base-uri(/*)}"/>
    </p:store>

    <p:xslt>
      <p:with-input port="stylesheet" href="../xsl/publish-test.xsl"/>
    </p:xslt>

    <p:store name="store" href="{replace($output, '.xml$', '.html')}"/>

    <p:variable name="test" select="tokenize(/c:result, '/')[last()] => replace('\.html$', '')">
      <p:pipe port="result-uri"/>
    </p:variable>

    <p:identity>
      <p:with-input>
        <test>{$test}</test>
      </p:with-input>
    </p:identity>
  </p:for-each>

  <p:wrap-sequence wrapper="test-catalog"/>
</p:declare-step>
