@echo off
rem TradeSquares build helper.
rem Usage: build.bat [task]   e.g. build.bat compileJava  |  build.bat build
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.10"
call gradlew.bat %*
echo.
pause
