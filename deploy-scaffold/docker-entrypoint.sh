#!/bin/sh
# 启动时替换 __REPO_URL__ 为实际的 SERVER_URL
# 默认为当前容器所在主机的 IP

if [ -z "$SERVER_URL" ]; then
    SERVER_URL="http://localhost:8080"
fi

echo "Using SERVER_URL: $SERVER_URL"

# 替换 index.html 和 archetype-catalog.xml 中的占位符
sed -i "s|__REPO_URL__|$SERVER_URL|g" /usr/share/nginx/html/index.html
sed -i "s|__REPO_URL__|$SERVER_URL|g" /usr/share/nginx/html/archetype-catalog.xml

echo "Replacement done. Starting nginx..."

exec nginx -g "daemon off;"
