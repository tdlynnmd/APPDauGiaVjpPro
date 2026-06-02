@echo off
:: Chuyển mã hóa console sang UTF-8 để hiển thị tiếng Việt không lỗi phông
chcp 65001 > nul

:: Di chuyển về thư mục chứa file jar (thư mục release)
cd /d "%~dp0.."

echo ==========================================
echo   Khởi chạy Server (Bản Build Sẵn)
echo ==========================================

:: 1. Kiểm tra và tự động tạo file .env nếu thiếu
if not exist .env (
    echo [CẢNH BÁO] Không tìm thấy file .env.
    echo Đang tự động tạo file .env từ .env.example...
    copy .env.example .env > nul
    echo.
    echo ======================================================================
    echo   ĐÃ TẠO FILE .env THÀNH CÔNG!
    echo   Vui lòng mở file .env ở thư mục release và kiểm tra cấu hình tài khoản
    echo   MySQL (DB_USER, DB_PASSWORD) của bạn trước khi chạy lại.
    echo ======================================================================
    echo.
    pause
    exit /b
)

:: 2. Kiểm tra cổng 3306 (MySQL local) có đang lắng nghe hay không
netstat -ano | findstr "3306" | findstr "LISTENING" > nul
if %errorlevel% neq 0 (
    echo [LỖI] Không tìm thấy dịch vụ MySQL đang hoạt động trên cổng 3306.
    echo Vui lòng bật MySQL Server của bạn lên trước khi chạy Server!
    echo.
    pause
    exit /b
)

:: 3. Khởi chạy Server Fat JAR
echo Đang chạy Server JAR...
java -Dfile.encoding=UTF-8 -jar Server.jar
pause
