@echo off
chcp 65001 > nul
cd /d "%~dp0"

echo ==================================================
echo   Dong goi du an Auction System va tao Release
echo ==================================================

REM 1. Bien dich va dong goi du an
echo Dang clean va package du an...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo [LOI] Dong goi du an that bai!
    pause
    exit /b
)

REM 2. Tao thu muc release va cac thu muc con
if exist release (
    echo Dang don dep thu muc release cu...
    rd /s /q release
)
mkdir release
mkdir release\windows
mkdir release\linux_macos

REM 3. Sao chep cac file JAR
echo Dang sao chep cac file JAR...
copy Server\target\Server-1.0-SNAPSHOT-jar-with-dependencies.jar release\Server.jar > nul
copy Client\target\Client-1.0-SNAPSHOT-jar-with-dependencies.jar release\Client.jar > nul

REM 4. Sao chep file cau hinh vi du
copy .env.example release\.env.example > nul
copy .env.example release\.env > nul

REM 5. Sao chep cac file script chay ban build san tu template
echo Dang sao chep script Windows chay ban build san...
copy .templates\windows\run-server.bat release\windows\run-server.bat > nul
copy .templates\windows\run-client.bat release\windows\run-client.bat > nul
copy .templates\windows\stop.bat release\windows\stop.bat > nul

echo Dang sao chep script Linux/macOS chay ban build san...
copy .templates\linux_macos\run-server.sh release\linux_macos\run-server.sh > nul
copy .templates\linux_macos\run-client.sh release\linux_macos\run-client.sh > nul
copy .templates\linux_macos\stop.sh release\linux_macos\stop.sh > nul


echo ==================================================
echo   DONG GOI HOAN TAT!
echo   Thu muc release/ da san sang tai thu muc goc.
echo ==================================================
