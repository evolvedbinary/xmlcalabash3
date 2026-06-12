<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.1">

<p:declare-step type="cx:pdf-info">
  <p:input port="source" content-types="application/pdf"/>
  <p:output port="result" content-types="xml"/> 
  <p:option name="password" as="xs:string?"/>
  <p:option name="page-details" as="xs:boolean?"/>
  <p:option name="page-text" as="xs:boolean?"/>
  <p:option name="form-details" as="xs:boolean?"/>
</p:declare-step>

<p:declare-step type="cx:pdf-merge">
  <p:input port="source" primary="true" content-types="application/pdf"/>
  <p:input port="secondary" sequence="true" content-types="application/pdf"/>
  <p:output port="result" content-types="application/pdf"/> 
  <p:option name="password" as="xs:string?"/>
  <p:option name="compression" values="('none', 'default')"
            select="'default'"/>
</p:declare-step>

<p:declare-step type="cx:pdf-extract">
  <p:input port="source" content-types="application/pdf"/>
  <p:output port="result" content-types="application/pdf"/> 
  <p:option name="password" as="xs:string?"/>
  <p:option name="pages" as="xs:string"/>
  <p:option name="compression" values="('none', 'default')"
            select="'default'"/>
</p:declare-step>

<p:declare-step type="cx:pdf-to-images">
  <p:input port="source" content-types="application/pdf"/>
  <p:output port="result" sequence="true"
            content-types="image/png image/jpeg image/bmp"/> 
  <p:option name="password" as="xs:string?"/>
  <p:option name="dpi" as="xs:integer" select="300"/>
  <p:option name="format" values="('png','jpeg','bmp')" select="'png'"/>
</p:declare-step>

<p:declare-step type="cx:pdf-decrypt">
  <p:input port="source" content-types="application/pdf"/>
  <p:output port="result"/> 
  <p:option name="password" as="xs:string?"/>
  <p:option name="compression" values="('none', 'default')"
            select="'default'"/>
</p:declare-step>

<p:declare-step type="cx:pdf-copy">
  <p:input port="source" content-types="application/pdf"/>
  <p:output port="result"/> 
  <p:option name="password" as="xs:string?"/>
  <p:option name="compression" values="('none', 'default')"
            select="'default'"/>
</p:declare-step>

<p:declare-step type="cx:pdf-encrypt">
  <p:input port="source" content-types="application/pdf"/>
  <p:output port="result" content-types="application/pdf"/> 
  <p:option name="owner-password" as="xs:string" required="true"/>
  <p:option name="user-password" as="xs:string" required="true"/>
  <p:option name="key-size" as="xs:integer" select="256"/>
  <p:option name="assemble" as="xs:boolean?"/>
  <p:option name="extract" as="xs:boolean?"/>
  <p:option name="extract-for-accessibility" as="xs:boolean?"/>
  <p:option name="fill-in-form" as="xs:boolean?"/>
  <p:option name="modify" as="xs:boolean?"/>
  <p:option name="modify-annotations" as="xs:boolean?"/>
  <p:option name="print" as="xs:boolean?"/>
  <p:option name="print-faithful" as="xs:boolean?"/>
  <p:option name="readonly" as="xs:boolean?"/>
  <p:option name="compression" values="('none', 'default')"
            select="'default'"/>
</p:declare-step>

<p:declare-step type="cx:pdf-form">
  <p:input port="source" content-types="application/pdf"/>
  <p:input port="data" content-types="application/xml"/>
  <p:output port="result" content-types="application/pdf"/>
  <p:option name="password" as="xs:string?"/>
  <p:option name="compression" values="('none', 'default')"
            select="'default'"/>
</p:declare-step>


</p:library>
