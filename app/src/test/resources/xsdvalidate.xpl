<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:s="http://purl.oclc.org/dsdl/schematron"
                name="main" version="3.1">
  <p:input port="source"/>
  <p:output port="result">
    <p:pipeinfo>
      <s:schema queryBinding="xslt2">
        <s:pattern>
          <s:rule context="/">
            <s:assert test="doc/@status">The status attribute is missing</s:assert>
            <s:assert test="doc/@status='draft'">The status attribute has the wrong value</s:assert>
          </s:rule>
        </s:pattern>
      </s:schema>
    </p:pipeinfo>
  </p:output>

  <p:validate-with-xml-schema>
    <p:with-input port="schema">
      <p:empty/>
    </p:with-input>
  </p:validate-with-xml-schema>

</p:declare-step>
