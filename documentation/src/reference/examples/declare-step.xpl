<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema" 
                name="main" version="3.1">
  <p:documentation>
    <div xmlns="http://www.w3.org/1999/xhtml">
      <p>This pipeline reads all of the files in a directory and
      updates the copyright element.</p>
    </div>
  </p:documentation>

  <p:input port="copyright" content-types="xml"/>
  <p:output port="result" content-types="text"/>
  <p:option name="path" required="true" as="xs:string"/>
  <p:option name="output-path" required="true" as="xs:anyURI"/>
  <p:option name="recurse" select="false()" as="xs:boolean"/>

  <p:directory-list name="listing" path="{$path}"
                    include-filter=".*\.xml$">
    <p:with-option name="max-depth"
                   select="if ($recurse) then 'unbounded' else '1'"/>
  </p:directory-list>

  <p:for-each name="loop">
    <p:with-input select="//c:file"/>
    <p:variable name="filename" select="/*/@name"/>

    <p:load href="{resolve-uri(/*/@name, base-uri(/*))}"/>

    <p:viewport match="copyright[. = 'Someone Random']">
      <p:identity>
        <p:with-input pipe="copyright@main"/>
      </p:identity>
    </p:viewport>

    <p:store href="{resolve-uri($filename, resolve-uri($output-path, static-base-uri()))}"/>
  </p:for-each>

  <p:variable name="total" select="count(//c:file)">
    <p:pipe step="listing"/>
  </p:variable>
    
  <p:identity>
    <p:with-input xmlns:f="http://example.com/ns/functions">
      <p:inline content-type="text/plain">Processed {$total} files; {f:is-leap-day()}&#10;</p:inline>
    </p:with-input>
  </p:identity>
</p:declare-step>
