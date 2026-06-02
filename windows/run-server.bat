@echo off
:: Chuyển mã hóa console sang UTF-8 để hiển thị tiếng Việt không lỗi phông
chcp 65001 > nul

:: Di chuyển về thư mục gốc của dự án
cd /d "%~dp0.."

echo ==========================================
echo   Khởi chạy Auction System Server
echo ==========================================

:: 1. Kiểm tra và tự động tạo file .env nếu thiếu
if not exist .env (
    echo [CẢNH BÁO] Không tìm thấy file .env ở thư mục gốc.
    echo Đang tự động tạo file .env từ .env.example...
    copy .env.example .env > nul
    echo.
    echo ======================================================================
    echo   ĐÃ TẠO FILE .env THÀNH CÔNG!
    echo   Vui lòng mở file .env ở thư mục gốc và kiểm tra cấu hình tài khoản
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

:: 3. Thiết lập UTF-8 cho Maven để log tiếng Việt hiển thị đẹp
set MAVEN_OPTS=-Dfile.encoding=UTF-8

:: 4. Khởi chạy Server qua Maven
echo Đang biên dịch và khởi chạy Server...
mvn compile exec:java -pl Server
pause
