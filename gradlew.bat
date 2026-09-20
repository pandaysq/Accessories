@echo off
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    gradle %*
    exit /b %ERRORLEVEL%
)
set GRADLE_VERSION=8.10.2
set GRADLE_HOME=%USERPROFILE%\.gradle\wrapper\dists\gradle-%GRADLE_VERSION%
set GRADLE_BIN=%GRADLE_HOME%\gradle-%GRADLE_VERSION%\bin\gradle.bat
if not exist "%GRADLE_BIN%" (
    mkdir "%GRADLE_HOME%" >nul 2>nul
    powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_HOME%\gradle.zip'"
    powershell -NoProfile -Command "Expand-Archive -Force '%GRADLE_HOME%\gradle.zip' '%GRADLE_HOME%'"
    del "%GRADLE_HOME%\gradle.zip"
)
call "%GRADLE_BIN%" %*