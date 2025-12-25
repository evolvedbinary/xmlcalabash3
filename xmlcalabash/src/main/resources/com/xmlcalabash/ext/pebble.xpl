<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.1">

<p:declare-step type="cx:pebble">
  <p:input port="source"/>
  <p:output port="result"/>
  <p:option name="context" as="map(xs:string,item())" required="true"/>
  <p:option name="prefix" as="xs:string?"/>
  <p:option name="defaultLocale" as="xs:string?"/>
  <p:option name="strictVariables" as="xs:boolean" select="false()"/>
  <p:option name="literalDecimalTreatedAsInteger" as="xs:boolean?"/>
  <p:option name="literalNumbersAsBigDecimals" as="xs:boolean?"/>
  <p:option name="maxRenderedSize" as="xs:integer?"/>
  <p:option name="newlineTrimming" as="xs:boolean" select="true()"/>
  <p:option name="autoEscaping" as="xs:boolean" select="true()"/>
  <p:option name="defaultEscapingStrategy" as="xs:string?"/>
</p:declare-step>

</p:library>
