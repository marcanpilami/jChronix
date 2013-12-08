if (! (Test-Path .\html ))
{
    mkdir .\html
}
C:\Python27\Scripts\sphinx-build.exe -a -b html . html
Read-Host # allow to read errors
