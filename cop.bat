for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value ^| find "="') do set dt=%%I
rem yyyyMMdd_hhmmss
set timestamp=%dt:~0,8%_%dt:~8,6%



C:\Windows\SysWOW64\WindowsPowerShell\v1.0\powershell.exe -Command "$ts=Get-Date -Format 'yyyyMMdd_HHmmss'; Rename-Item 'app\build\outputs\apk\debug\app-debug.apk' ('apk_' + $ts + '.apk')"

ren  app\build\outputs\apk\debug\app-debug.apk  "app_%timestamp%.apk"
copy "app\build\outputs\apk\debug\*.apk" C:\Users\ShibuPC\Downloads\5\ /y
