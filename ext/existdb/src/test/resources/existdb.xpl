<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:exist="http://exist.sourceforge.net/NS/exist"
                exclude-inline-prefixes="cx xs" version="3.0">

<p:import href="https://xmlcalabash.com/ext/library/exist-db.xpl"/>

<p:output port="result"/>

<p:xquery cx:processor="https://exist-db.org/"
          parameters="map{'cx:database-uri': 'http://existdb:8080/exist/rest/db/'}">
  <p:with-input port="source"><p:empty/></p:with-input>
  <p:with-input port='query'>
    <p:inline content-type="text/plain">&lt;doc>{3+4}&lt;/doc></p:inline>
  </p:with-input>
</p:xquery>

<!--
<cx:exist-db p:expand-text="false"
             database-uri="http://localhost:8080/exist/rest/db/"
             query-parameters="map{'cache':'yes'}"
             query-properties="map{'omit-xml-declaration':'yes'}">
  <p:with-input><p:empty/></p:with-input>
  <p:with-input port="query">
    <p:inline content-type="text/plain">
declare namespace output = "http://www.w3.org/2010/xslt-xquery-serialization"; 

declare option output:media-type "application/xml";

let $x := 1
return
  &lt;doc/> (:map{"greeting-sent": true()}:)

    </p:inline>
  </p:with-input>
</cx:exist-db>
-->

<!--
<p:variable name="elem" as="element()" select=".">
  <testing-content/>
</p:variable>

<cx:exist-db parameters="map{'name': 'EEE', 'seq': (1,2,3), 'elem': $elem,
                             'map': map{'one': 1}}" p:expand-text="false"
             database-uri="http://localhost.charlesproxy.com:8080/exist/rest/db/">
  <p:with-input><p:empty/></p:with-input>
  <p:with-input port="query">
    <p:inline content-type="text/plain">
declare namespace output = "http://www.w3.org/2010/xslt-xquery-serialization"; 

declare variable $name as xs:string* external;
declare variable $seq as xs:string* external;
declare variable $elem as element() external;
declare variable $map as map(*) external;

declare option output:media-type "application/xml";

element { $name } { 
  attribute { "seq-count" } { count($seq) },
  attribute { "one" } { $map.get("one") },
  &lt;content>
  { $elem }
  &lt;/content>
}
    </p:inline>
  </p:with-input>
</cx:exist-db>
-->

</p:declare-step>
