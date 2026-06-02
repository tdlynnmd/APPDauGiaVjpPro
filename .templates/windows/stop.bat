@echo off
:: Chuyển mã hóa console sang UTF-8 để hiển thị tiếng Việt không lỗi phông
chcp 65001 > nul

:: Di chuyển về thư mục chứa file jar (thư mục release)
cd /d "%~dp0.."

set "TARGET=%~1"

echo ==========================================
echo   Dừng hệ thống (Bản Build Sẵn)
echo ==========================================

set "FOUND="

:: Tìm kiếm PID của Server.jar hoặc Client.jar bằng jps -l
for /f "tokens=1,2" %%a in ('jps -l') do (
    if "%%b"=="Server.jar" (
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
    if "%%b"=="Client.jar" (
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
        echo Không tìm thấy tiến trình Server.jar hoặc Client.jar nào đang chạy.
    ) else (
        echo Không tìm thấy tiến trình %TARGET% nào đang chạy.
    )
) else (
    echo Đã dừng các tiến trình được chọn thành công!
)

echo.
pause
