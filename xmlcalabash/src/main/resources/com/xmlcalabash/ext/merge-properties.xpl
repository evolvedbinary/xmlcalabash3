<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.0">
  <p:declare-step type="cx:merge-properties">
    <p:input port="source" primary="true"/>
    <p:input port="alternate"/>
    <p:output port="result"/>
    <p:option name="merge-maps" as="xs:boolean" select="true()"/>
  </p:declare-step>
</p:library>
