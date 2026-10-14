@echo off
cd /d "%~dp0"

if not defined JAVA_HOME (
    if exist "%USERPROFILE%\.jdks\jdk-21.0.12.1+1" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\jdk-21.0.12.1+1"
    ) else if exist "%USERPROFILE%\.jdks\corretto-25.0.4.1" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\corretto-25.0.4.1"
    )
)

if exist "%JAVA_HOME%\bin\javaw.exe" (
    if exist "build\libs\dynamicisland.jar" (
        start "" "%JAVA_HOME%\bin\javaw.exe" -jar "build\libs\dynamicisland.jar"
        exit
    )
)

call gradlew.bat run
