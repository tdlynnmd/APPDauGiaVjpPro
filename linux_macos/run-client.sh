#!/bin/bash

# Di chuyển về thư mục gốc của dự án
cd "$(dirname "$0")/.."

echo "=========================================="
echo "  Khởi chạy Auction System Client (Linux/macOS)"
echo "=========================================="

# Thiết lập UTF-8 cho Maven
export MAVEN_OPTS="-Dfile.encoding=UTF-8"

# Khởi chạy Client qua Maven
echo "Đang biên dịch và khởi chạy Client..."
mvn compile exec:java -pl Client
