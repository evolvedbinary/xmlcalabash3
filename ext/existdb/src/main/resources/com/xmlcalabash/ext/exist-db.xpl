<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           version="3.0">

  <p:declare-step type="cx:exist-db">
    <p:input port="source" content-types="any" sequence="true" primary="true">
      <p:empty/>
    </p:input>
    <p:input port="query" content-types="text xml"/>
    <p:output port="result" sequence="true" content-types="any"/>
    <p:option name="parameters" as="map(xs:QName,item()*)?"/>
    <p:option name="version" as="xs:string?"/>
    <p:option name="database-uri" as="xs:anyURI?"/>
    <p:option name="username" as="xs:string?"/>
    <p:option name="password" as="xs:string?"/>
    <p:option name="query-parameters" as="map(xs:QName, item()*)?"/>
    <p:option name="query-properties" as="map(xs:QName, item()*)?"/>
  </p:declare-step>

</p:library>
