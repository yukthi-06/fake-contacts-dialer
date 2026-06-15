for /f "tokens=2 delims==" %%I in ('wmic os get localdatetime /value ^| find "="') do set dt=%%I
rem yyyyMMdd_hhmmss
set timestamp=%dt:~0,8%_%dt:~8,6%

ren  app\build\outputs\apk\debug\app-debug.apk  "app_%timestamp%.apk"
copy "app\build\outputs\apk\debug\app_%timestamp%.apk" C:\Users\ShibuPC\Downloads\5\ /y
