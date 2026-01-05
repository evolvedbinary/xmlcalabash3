<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xvrl="http://www.xproc.org/ns/xvrl"
                exclude-inline-prefixes="#all"
                name="main" version="3.1">
  <p:output port="result" sequence="true"/>

  <p:validate-with-relax-ng assert-valid="false">
    <p:with-input port="schema">
      <p:document href="vtestdoc.rnc"/>
    </p:with-input>
    <p:with-input port="source">
      <p:document href="vtestdoc.xml"/>
    </p:with-input>
  </p:validate-with-relax-ng>

  <p:identity>
    <p:with-input pipe="report" select="/xvrl:report/xvrl:detection[1]"/>
  </p:identity>

</p:declare-step>
