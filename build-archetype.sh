#!/bin/bash
# ==========================================
#  一键构建 archetype 并生成 Nginx 部署目录
#  在项目根目录运行
# ==========================================

set -e

echo "[1/4] 生成 archetype 项目结构..."
mvn archetype:create-from-project -DoutputDirectory=target/ai-agent-scaffold-lite-archetype

echo "[2/4] 清理 archetype 中的无用配置..."
rm -rf target/ai-agent-scaffold-lite-archetype/src/main/resources/archetype-resources/.idea
rm -rf target/ai-agent-scaffold-lite-archetype/src/main/resources/archetype-resources/.vscode
rm -rf target/ai-agent-scaffold-lite-archetype/src/main/resources/archetype-resources/.claude
rm -rf target/ai-agent-scaffold-lite-archetype/src/main/resources/archetype-resources/data/log
rm -rf target/ai-agent-scaffold-lite-archetype/src/main/resources/archetype-resources/__rootArtifactId__-app/data/log

echo "[2b/4] 修复模板中的硬编码 scaffold 名称..."
ARCH_RES="target/ai-agent-scaffold-lite-archetype/src/main/resources/archetype-resources"

# finalName → 用 ${artifactId} 替换，生成的项目 jar 名自动匹配模块名
find "$ARCH_RES" -name "pom.xml" -exec sed -i 's|<finalName>ai-agent-scaffold-app</finalName>|<finalName>${artifactId}</finalName>|g' {} +
find "$ARCH_RES" -name "pom.xml" -exec sed -i 's|<finalName>ai-agent-scaffold-domain</finalName>|<finalName>${artifactId}</finalName>|g' {} +
find "$ARCH_RES" -name "pom.xml" -exec sed -i 's|<finalName>ai-agent-scaffold-trigger</finalName>|<finalName>${artifactId}</finalName>|g' {} +
find "$ARCH_RES" -name "pom.xml" -exec sed -i 's|<finalName>ai-agent-scaffold-infrastructure</finalName>|<finalName>${artifactId}</finalName>|g' {} +
find "$ARCH_RES" -name "pom.xml" -exec sed -i 's|<finalName>ai-agent-scaffold-types</finalName>|<finalName>${artifactId}</finalName>|g' {} +

# mainClass → 用 ${package} 替换，生成的项目包名自动匹配
sed -i 's|<mainClass>org.example.Application</mainClass>|<mainClass>${package}.Application</mainClass>|g' "$ARCH_RES/__rootArtifactId__-app/pom.xml"

# application.yml spring.config.name
sed -i 's|name: ai-agent-scaffold-app|name: ${rootArtifactId}-app|g' "$ARCH_RES/__rootArtifactId__-app/src/main/resources/application.yml"

# Dockerfile jar 包名
sed -i 's|ai-agent-scaffold-app.jar|${rootArtifactId}-app.jar|g' "$ARCH_RES/__rootArtifactId__-app/Dockerfile"

# build.sh / push.sh
sed -i 's|ai-agent-scaffold-app|${rootArtifactId}-app|g' "$ARCH_RES/__rootArtifactId__-app/build.sh"
sed -i 's|IMAGE_NAME="ai-agent-scaffold-app"|IMAGE_NAME="${rootArtifactId}-app"|g' "$ARCH_RES/__rootArtifactId__-app/push.sh"

# docs/dev-ops 部署脚本
sed -i 's|ai-agent-scaffold|${rootArtifactId}|g' "$ARCH_RES/docs/dev-ops/docker-compose-app.yml"
sed -i 's|docker stop ai-agent-scaffold|docker stop ${rootArtifactId}|g' "$ARCH_RES/docs/dev-ops/app/stop.sh"
sed -i 's|CONTAINER_NAME=ai-agent-scaffold|CONTAINER_NAME=${rootArtifactId}|g' "$ARCH_RES/docs/dev-ops/app/start.sh"
sed -i 's|IMAGE_NAME=system/ai-agent-scaffold:1.0|IMAGE_NAME=system/${rootArtifactId}:1.0|g' "$ARCH_RES/docs/dev-ops/app/start.sh"

# 父 pom 中的日志路径和 archetype 输出目录
sed -i 's|ai-agent-scaffold-lite-archetype|${rootArtifactId}-lite-archetype|g' "$ARCH_RES/pom.xml"
sed -i 's|/export/Logs/ai-agent-scaffold-boot|/export/Logs/${rootArtifactId}-boot|g' "$ARCH_RES/pom.xml"
sed -i 's|gc-ai-agent-scaffold-boot|gc-${rootArtifactId}-boot|g' "$ARCH_RES/pom.xml"

# 修复 archetype-metadata.xml：application.yml / Dockerfile / build.sh / push.sh
# 默认不替换非 XML 文件中的变量，需要加 filtered="true"
ARCH_MDATA="target/ai-agent-scaffold-lite-archetype/src/main/resources/META-INF/maven/archetype-metadata.xml"

python3 -c "
import re
with open('$ARCH_MDATA', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. 把 yml 从 png 的 fileSet 中分出来，加上 filtered=true
old = '''        <fileSet encoding=\"UTF-8\">
          <directory>src/main/resources</directory>
          <includes>
            <include>**/*.png</include>
            <include>**/*.yml</include>
          </includes>
        </fileSet>'''
new = '''        <fileSet encoding=\"UTF-8\">
          <directory>src/main/resources</directory>
          <includes>
            <include>**/*.png</include>
          </includes>
        </fileSet>
        <fileSet filtered=\"true\" encoding=\"UTF-8\">
          <directory>src/main/resources</directory>
          <includes>
            <include>**/*.yml</include>
          </includes>
        </fileSet>'''
content = content.replace(old, new)

# 2. 给 build.sh / Dockerfile / push.sh 的 fileSet 加上 filtered=true
old = '''        <fileSet encoding=\"UTF-8\">
          <directory></directory>
          <includes>
            <include>build.sh</include>
            <include>Dockerfile</include>
            <include>push.sh</include>
          </includes>
        </fileSet>'''
new = '''        <fileSet filtered=\"true\" encoding=\"UTF-8\">
          <directory></directory>
          <includes>
            <include>build.sh</include>
            <include>Dockerfile</include>
            <include>push.sh</include>
          </includes>
        </fileSet>'''
content = content.replace(old, new)

# 3. 给 docs/dev-ops 的 fileSet 加上 filtered=true
old = '''    <fileSet encoding=\"UTF-8\">
      <directory>docs/dev-ops</directory>
      <includes>
        <include>**/*.sh</include>
        <include>**/*.yml</include>
        <include>**/*.sql</include>
      </includes>
    </fileSet>'''
new = '''    <fileSet filtered=\"true\" encoding=\"UTF-8\">
      <directory>docs/dev-ops</directory>
      <includes>
        <include>**/*.sh</include>
        <include>**/*.yml</include>
        <include>**/*.sql</include>
      </includes>
    </fileSet>'''
content = content.replace(old, new)

with open('$ARCH_MDATA', 'w', encoding='utf-8') as f:
    f.write(content)

print('archetype-metadata.xml 已修复')
"

echo "[3/4] 构建 archetype JAR..."
cd target/ai-agent-scaffold-lite-archetype
mvn clean package
cd ../..

echo "[4/4] 生成 Nginx 部署目录..."
REPO_DIR="target/archetype-nginx-repo"
ARCH_GROUP_DIR="$REPO_DIR/org/example/ai-agent-scaffold-archetype/1.0"

mkdir -p "$ARCH_GROUP_DIR"

cp target/ai-agent-scaffold-lite-archetype/target/ai-agent-scaffold-archetype-1.0.jar "$ARCH_GROUP_DIR/"
cp target/ai-agent-scaffold-lite-archetype/pom.xml "$ARCH_GROUP_DIR/ai-agent-scaffold-archetype-1.0.pom"

echo "完成！部署目录: $REPO_DIR"
echo "将整个目录复制到 Nginx 服务器即可使用。"
