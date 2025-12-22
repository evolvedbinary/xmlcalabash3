<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:s="http://purl.oclc.org/dsdl/schematron"
                name="main" version="3.1">
  <p:input port="source">
    <p:pipeinfo>
      <s:schema queryBinding="xslt2">
        <s:pattern>
          <s:rule context="/">
            <s:assert test="a">The source document element is not “a”</s:assert>
          </s:rule>
        </s:pattern>
      </s:schema>
    </p:pipeinfo>
  </p:input>
  <p:output port="result">
    <p:pipeinfo>
      <s:schema queryBinding="xslt2">
        <s:pattern>
          <s:rule context="/">
            <s:assert test="b">The result document element is not “b”</s:assert>
            <s:assert test="b/a">The result document does not match “b/a”</s:assert>
          </s:rule>
        </s:pattern>
      </s:schema>
    </p:pipeinfo>
  </p:output>
  <p:option name="wrap" as="xs:NCName" select="'a'"/>

  <p:wrap-sequence wrapper="{$wrap}"/>
</p:declare-step>
