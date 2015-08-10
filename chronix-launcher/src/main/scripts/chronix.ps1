<#
    .SYNOPSIS
    Conveniance script for Chronix administration.

    .DESCRIPTION

    The script tries to find the java executable in the following locations (in this order):
        * $env:JAVA_HOME
        * Path
    
    Also respects the JAVA_OPTS notations - the $env:JAVA_OPTS variable will be added to the Java
    command line.

    Beware: when creating a service, the environment variables (JAVA_HOME, JAVA_OPTS) are read and 
    set inside the service definition. Future changes of the variables will NOT affect the service.
    You must re-create the service for the new environment variables to be applied to the service.

#>


[CmdletBinding()]
param(
    [string][Parameter(Mandatory=$true, Position=0)]
    [ValidateSet("start", "startconsole", "stop", "status", "installservice", "removeservice")]
    ## The action to perform. See cmdlet description for details.
    $Command,

    [string]
    # For service creation only: Create a service with this user. If not given, LOCAL_SYSTEM is used which is a BAD idea except for dev/demo systems.
    $ServiceUser,
    [string]
    # For service creation only: create a service with this password.
    $ServicePassword
)

$prunsvr= "$PSScriptRoot\..\bin\procrun\x32\chronix-service.exe"
if ([Environment]::Is64BitProcess)
{
    $prunsvr= "$PSScriptRoot\..\bin\procrun\x64\chronix-service.exe"
}

## Get Java
$java = $null
if(Test-Path Env:\JAVA_HOME)
{
    $java = "$Env:JAVA_HOME/bin/java.exe"
}
if (-not $java)
{
    $java = Get-Command java -ErrorAction SilentlyContinue
    if (-not $java)
    {
        throw "Cannot find Java. Check it is in the PATH or set JAVA_HOME"
    }
    $java = $java.Path
}

## Java options
if (! (Test-Path env:/JAVA_OPTS))
{
    $env:JAVA_OPTS = "-Xms32m -Xmx128m -XX:MaxPermSize=96m -XX:MaxHeapFreeRatio=20 -XX:MinHeapFreeRatio=5"
}

## Service methods
function New-Service
{
    & $prunsvr //IS --Description="'Chronix Job Scheduler'" --Startup="auto" --ServiceUser="$ServiceUser" --ServicePassword="$ServicePassword" --StartMode="jvm" --StopMode="jvm" --StartPath="$PSScriptRoot\.." ++StartParams="" --StartClass="org.oxymores.chronix.launcher.Scheduler" --StopClass="org.oxymores.chronix.launcher.Scheduler" --StartMethod="start" --StopMethod="stop" --StopTimeout="60" --LogPath="$PSScriptRoot\..\logs" --StdOutput="auto" --StdError="auto" --Classpath="$PSScriptRoot\..\lib\chronix-launcher.jar" ++JvmOptions=$($env:JAVA_OPTS.Replace(" ", ";"))
    if (!$?)
    {
        throw "could not create service"
    }
}

function Remove-Service
{  
    & $prunsvr //DS
    if (!$?)
    {
        throw "could not remove service"
    }   
}


## Router
switch($Command)
{
    "start" { & $prunsvr //ES }
    "stop" { & $prunsvr //SS }
    "status" { (Get-Service "chronix-service").Status}
    "startconsole" { & $java $env:JAVA_OPTS.split(" ") -jar $PSScriptRoot/../lib/chronix-launcher.jar } 
    "installservice" { New-Service }
    "removeservice" { Remove-Service }    
}