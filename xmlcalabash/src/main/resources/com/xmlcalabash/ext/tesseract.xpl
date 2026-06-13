<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           xmlns:e="http://www.w3.org/1999/XSL/Spec/ElementSyntax"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="3.1">

<p:declare-step type="cx:tesseract">
  <p:input port="source"/>
  <p:output port="result"/>
  <p:option name="language" as="xs:string" required="true"/>
  <p:option name="data-path" as="xs:string?"/>
  <p:option name="engine-mode"
            values="('tesseract-only', 'lstm-only', 'lstm-combined', 'default')"
            select="'default'"/>
  <!-- Values not in the Tesseract OCR documentat have been removed -->
  <p:option name="page-segmentation-mode"
            values="('osd-only',
                     'auto-osd',
                     (: 'auto-only', :)
                     'auto',
                     'single-column',
                     (: 'single-block-vert-text', :)
                     'single-block',
                     'single-line',
                     (: 'single-word', :)
                     (: 'circle-word', :)
                     (: 'single-char', :)
                     'sparse-text',
                     (: 'sparse-text-osd', :)
                     'raw-line')"
            select="'auto'"/>
  <p:option name="output-format" values="('text','hocr','tsv', 'alto', 'lstmbox', 'wordstrbox')"
            select="'text'"/>
  <p:option name="variables" as="map(xs:string,xs:string)?"/>     
  <p:option name="debug-output" as="xs:string?"/>
</p:declare-step>

</p:library>
