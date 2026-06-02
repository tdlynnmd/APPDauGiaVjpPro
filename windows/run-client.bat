@echo off
:: Chuyển mã hóa console sang UTF-8 để hiển thị tiếng Việt không lỗi phông
chcp 65001 > nul

:: Di chuyển về thư mục gốc của dự án
cd /d "%~dp0.."

echo ==========================================
echo   Khởi chạy Auction System Client
echo ==========================================

:: Thiết lập UTF-8 cho Maven
set MAVEN_OPTS=-Dfile.encoding=UTF-8

:: Khởi chạy Client qua Maven
echo Đang biên dịch và khởi chạy Client...
mvn compile exec:java -pl Client
pause
