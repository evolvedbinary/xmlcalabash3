<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           version="3.0">

<p:declare-step xmlns:ex="http://exproc.org/ns/steps"
                type="ex:expand-templates">
  <p:input port="source" content-types="text xml html"/>
  <p:output port="result" content-types="text xml html"/>
  <p:option name="variables" as="map(xs:QName,item()*)?"/>
</p:declare-step>

</p:library>
