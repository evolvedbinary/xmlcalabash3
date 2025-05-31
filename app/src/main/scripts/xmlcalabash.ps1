# This is a fairly naive Powershell script that constructs a classpath for
# running XML Calabash. The idea is that it puts jar files from the "extra"
# directory ahead of jar files from the "lib" directory. This should support
# overriding jars. And supports running steps that require extra libraries.

$cp = Join-Path -Path $PSScriptRoot -ChildPath "xmlcalabash-app-@@VERSION@@.jar"

if (![System.IO.File]::Exists("$cp")) {
  Write-Host "XML Calabash script did not find the @@VERSION@@ distribution jar"
  Exit 1
}

$cpdelim = if($IsLinux -or $IsMacOS) {":"} else {";"}
$slash = if($IsLinux -or $IsMacOS) {"/"} else {"\"}

$extraRoot = Join-Path -Path $PSScriptRoot -ChildPath "extra"
Get-ChildItem $extraRoot -Filter *.jar |
ForEach-Object {
  $cp = "${cp}${cpdelim}${PSScriptroot}${slash}extra${slash}$_"
}

$libRoot = Join-Path -Path $PSScriptRoot -ChildPath "lib"
Get-ChildItem $libRoot -Filter *.jar |
ForEach-Object {
  $cp = "${cp}${cpdelim}${PSScriptroot}${slash}lib${slash}$_"
}

# FIXME: should there be some attempt to look for $Env:JAVA_HOME here?

java -cp "$cp" com.xmlcalabash.app.Main $args
