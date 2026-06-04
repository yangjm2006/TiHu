@echo off
setlocal

set "APP_DIR=%~dp0"
set "MODULE_PATH=%APP_DIR%frontend-1.0-SNAPSHOT.jar"

for %%F in ("%APP_DIR%lib\*.jar") do call set "MODULE_PATH=%%MODULE_PATH%%;%%~fF"

java --enable-native-access=javafx.graphics --sun-misc-unsafe-memory-access=allow --module-path "%MODULE_PATH%" --module com.tihu.frontend/com.tihu.frontend.MainApplication
