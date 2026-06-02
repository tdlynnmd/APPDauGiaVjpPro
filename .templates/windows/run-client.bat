@echo off
:: Chuyển mã hóa console sang UTF-8 để hiển thị tiếng Việt không lỗi phông
chcp 65001 > nul

:: Di chuyển về thư mục chứa file jar (thư mục release)
cd /d "%~dp0.."

echo ==========================================
echo   Khởi chạy Client (Bản Build Sẵn)
echo ==========================================

:: Khởi chạy Client Fat JAR
echo Đang chạy Client JAR...
java -Dfile.encoding=UTF-8 -jar Client.jar
pause
