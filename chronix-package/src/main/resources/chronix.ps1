## Get Java
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java)
{
    $java = $env:JAVA_HOME
    if (-not $env:JAVA_HOME)
    {
        throw "Cannot find Java. Check it is in the PATH or set JAVA_HOME"
    }
}
else
{
    $java = $java.Path
}

if (! (Test-Path env:/JAVA_OPTS))
{
    $env:JAVA_OPTS = "-Xms32m -Xmx128m -XX:MaxPermSize=32m"
}

cd $PSScriptRoot/..

$mainJar = (ls ./plugins/bootstrap/chronix-package*.jar)[0]

& $java $env:JAVA_OPTS.split(" ") -jar $mainJar