# This is a fairly naive Powershell script that constructs a classpath for
# running XML Calabash. The idea is that it puts jar files from the "extra"
# directory ahead of jar files from the "lib" directory. This should support
# overriding jars. And supports running steps that require extra libraries.

# 1. Construct the classpath

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

# 2. Fix the argument parsing. The way arguments are parsed when
#    passed to another program breaks strings at ":". That's
#    inconvenient, so try to fix it. FYI:
#    https://github.com/PowerShell/PowerShell/issues/23819

$Param = ""
$NewArgs = foreach ($Item in $args)
{
   If ($Item.EndsWith(':'))
   {
      $Param = $Item
      continue
   }

   if ($Param -ne "")
   {
     $Param + $Item
     $Param = ""
   }
   else
   {
      $Item
   }
}

# 3. Sort out where the java executable lives; assume it's either in
#    $env:JAVA_HOME\bin\java.exe or is on the classpath.

$java = "java"
if ($env:JAVA_HOME -ne "")
{
   $java = Join-Path -Path $env:JAVA_HOME -ChildPath "bin\java.exe"
}

# 4. Run XML Calabash

& $java -cp "$cp" com.xmlcalabash.app.Main $NewArgs
