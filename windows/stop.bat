@echo off
:: Chuyển mã hóa console sang UTF-8 để hiển thị tiếng Việt không lỗi phông
chcp 65001 > nul

:: Di chuyển về thư mục gốc của dự án
cd /d "%~dp0.."

set "TARGET=%~1"

echo ==========================================
echo   Dừng hệ thống Auction System
echo ==========================================

set "FOUND="

:: Tìm kiếm và diệt tiến trình
for /f "tokens=1,2" %%a in ('jps -l') do (
    if "%%b"=="com.auction.ServerMain" (
        if "%TARGET%"=="" (
            echo Đang tắt Server (PID: %%a)...
            taskkill /f /pid %%a > nul 2>&1
            set "FOUND=1"
        ) else if /i "%TARGET%"=="server" (
            echo Đang tắt Server (PID: %%a)...
            taskkill /f /pid %%a > nul 2>&1
            set "FOUND=1"
        )
    )
    if "%%b"=="com.auction.Launcher" (
        if "%TARGET%"=="" (
            echo Đang tắt Client (PID: %%a)...
            taskkill /f /pid %%a > nul 2>&1
            set "FOUND=1"
        ) else if /i "%TARGET%"=="client" (
            echo Đang tắt Client (PID: %%a)...
            taskkill /f /pid %%a > nul 2>&1
            set "FOUND=1"
        )
    )
)

if not defined FOUND (
    if "%TARGET%"=="" (
        echo Không tìm thấy tiến trình ServerMain hoặc Launcher nào đang chạy.
    ) else (
        echo Không tìm thấy tiến trình %TARGET% nào đang chạy.
    )
) else (
    echo Đã dừng các tiến trình được chọn thành công!
)

echo.
pause
