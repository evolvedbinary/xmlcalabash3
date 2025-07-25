<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                name="main" version="3.1">
<p:output port="result"/>

<p:input port="source" content-types="json text/plain"/>

<p:identity>
  <p:with-input select="p:document-properties(.)"/>
</p:identity>

<p:cast-content-type content-type="application/xml"/>

</p:declare-step>
