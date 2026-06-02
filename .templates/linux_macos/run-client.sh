#!/bin/bash
# Di chuyển về thư mục chứa file jar (thư mục release)
cd "$(dirname "$0")/.."

echo "=========================================="
echo "  Khởi chạy Client (Bản Build Sẵn)"
echo "=========================================="

# Khởi chạy Client Fat JAR
echo "Đang chạy Client JAR..."
java -Dfile.encoding=UTF-8 -jar Client.jar
