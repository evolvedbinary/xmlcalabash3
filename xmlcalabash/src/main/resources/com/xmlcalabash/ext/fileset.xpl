<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.1">

<p:declare-step type="cx:fileset">
  <p:input port="source" content-types="xml" sequence="true">
    <p:empty/>
  </p:input>
  <p:output port="result" content-types="xml" sequence="true"/>
  <p:option name="path" as="xs:string" required="true"/>
  <p:option name="default-excludes" as="xs:boolean" select="true()"/>
  <p:option name="case-sensitive" as="xs:boolean" select="true()"/>
  <p:option name="error-on-missing-dir" as="xs:boolean" select="true()"/>
  <p:option name="follow-symlinks" as="xs:boolean" select="true()"/>
  <p:option name="includes" as="xs:string?"/>
  <p:option name="excludes" as="xs:string?"/>
  <p:option name="detailed" as="xs:boolean" select="false()"/>
</p:declare-step>

</p:library>
