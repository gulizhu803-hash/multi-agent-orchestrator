FROM nginx:alpine

# 复制静态文件
COPY index.html /usr/share/nginx/html/index.html
COPY archetype-catalog.xml /usr/share/nginx/html/archetype-catalog.xml
COPY maven-repo /usr/share/nginx/html/maven-repo
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 启动脚本（替换 __REPO_URL__ 占位符）
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
