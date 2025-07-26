<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:f="http://example.com/ns/functions"
                name="main" version="3.1">
  <p:documentation>
    <div xmlns="http://www.w3.org/1999/xhtml">
      <p>Example of importing functions. This requires Saxon EE.</p>
    </div>
  </p:documentation>

  <p:import-functions href="is-leap-day.xsl"/>

  <p:output port="result" serialization="map{'indent':true()}"/>
    
  <p:identity>
    <p:with-input exclude-inline-prefixes="#all">
      <leap-days>
        <today date="{substring(string(current-date()), 1, 10)}"
               >{f:is-leap-day()}</today>
        <other date="2028-02-29"
               >{f:is-leap-day('2028-02-29')}</other>
      </leap-days>
    </p:with-input>
  </p:identity>
</p:declare-step>
