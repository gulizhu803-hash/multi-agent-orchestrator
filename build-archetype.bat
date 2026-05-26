@echo off
REM ==========================================
REM  一键构建 archetype 并生成 Nginx 部署目录
REM  在项目根目录运行
REM ==========================================

echo [1/4] 生成 archetype 项目结构...
call mvn archetype:create-from-project -DoutputDirectory=target/ai-agent-scaffold-lite-archetype

echo [2/4] 清理 archetype 中的无用配置...
if exist "target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources\.idea" rmdir /s /q "target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources\.idea"
if exist "target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources\.vscode" rmdir /s /q "target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources\.vscode"
if exist "target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources\.claude" rmdir /s /q "target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources\.claude"
if exist "target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources\data\log" rmdir /s /q "target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources\data\log"
if exist "target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources\__rootArtifactId__-app\data\log" rmdir /s /q "target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources\__rootArtifactId__-app\data\log"
if exist "target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources\__rootArtifactId__-app\env-config.properties" del /f /q "target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources\__rootArtifactId__-app\env-config.properties"

echo [2b/4] 修复模板中的硬编码 scaffold 名称...
set ARCH_RES=target\ai-agent-scaffold-lite-archetype\src\main\resources\archetype-resources
set PSCMD=Get-ChildItem -Path '%ARCH_RES%' -Recurse -Filter 'pom.xml' ^| ForEach-Object { (Get-Content $_.FullName) -replace '<finalName>ai-agent-scaffold-app</finalName>', '<finalName>${artifactId}</finalName>' -replace '<finalName>ai-agent-scaffold-domain</finalName>', '<finalName>${artifactId}</finalName>' -replace '<finalName>ai-agent-scaffold-trigger</finalName>', '<finalName>${artifactId}</finalName>' -replace '<finalName>ai-agent-scaffold-infrastructure</finalName>', '<finalName>${artifactId}</finalName>' -replace '<finalName>ai-agent-scaffold-types</finalName>', '<finalName>${artifactId}</finalName>' ^| Set-Content $_.FullName }
powershell -Command "%PSCMD%"

REM mainClass
powershell -Command "(Get-Content '%ARCH_RES%\__rootArtifactId__-app\pom.xml') -replace '<mainClass>org.example.Application</mainClass>', '<mainClass>${package}.Application</mainClass>' ^| Set-Content '%ARCH_RES%\__rootArtifactId__-app\pom.xml'"

REM application.yml
powershell -Command "(Get-Content '%ARCH_RES%\__rootArtifactId__-app\src\main\resources\application.yml') -replace 'name: ai-agent-scaffold-app', 'name: ${rootArtifactId}-app' ^| Set-Content '%ARCH_RES%\__rootArtifactId__-app\src\main\resources\application.yml'"

REM Dockerfile
powershell -Command "(Get-Content '%ARCH_RES%\__rootArtifactId__-app\Dockerfile') -replace 'ai-agent-scaffold-app.jar', '${rootArtifactId}-app.jar' ^| Set-Content '%ARCH_RES%\__rootArtifactId__-app\Dockerfile'"

REM build.sh
powershell -Command "(Get-Content '%ARCH_RES%\__rootArtifactId__-app\build.sh') -replace 'ai-agent-scaffold-app', '${rootArtifactId}-app' ^| Set-Content '%ARCH_RES%\__rootArtifactId__-app\build.sh'"

REM push.sh
powershell -Command "(Get-Content '%ARCH_RES%\__rootArtifactId__-app\push.sh') -replace 'IMAGE_NAME=\"ai-agent-scaffold-app\"', 'IMAGE_NAME=\"${rootArtifactId}-app\"' ^| Set-Content '%ARCH_RES%\__rootArtifactId__-app\push.sh'"

REM docs/dev-ops 部署脚本
powershell -Command "(Get-Content '%ARCH_RES%\docs\dev-ops\docker-compose-app.yml') -replace 'ai-agent-scaffold', '${rootArtifactId}' ^| Set-Content '%ARCH_RES%\docs\dev-ops\docker-compose-app.yml'"
powershell -Command "(Get-Content '%ARCH_RES%\docs\dev-ops\app\stop.sh') -replace 'docker stop ai-agent-scaffold', 'docker stop ${rootArtifactId}' ^| Set-Content '%ARCH_RES%\docs\dev-ops\app\stop.sh'"
powershell -Command "(Get-Content '%ARCH_RES%\docs\dev-ops\app\start.sh') -replace 'CONTAINER_NAME=ai-agent-scaffold', 'CONTAINER_NAME=${rootArtifactId}' -replace 'IMAGE_NAME=system/ai-agent-scaffold:1.0', 'IMAGE_NAME=system/${rootArtifactId}:1.0' ^| Set-Content '%ARCH_RES%\docs\dev-ops\app\start.sh'"

REM 父 pom 中的日志路径和 archetype 输出目录
powershell -Command "(Get-Content '%ARCH_RES%\pom.xml') -replace 'ai-agent-scaffold-lite-archetype', '${rootArtifactId}-lite-archetype' -replace '/export/Logs/ai-agent-scaffold-boot', '/export/Logs/${rootArtifactId}-boot' -replace 'gc-ai-agent-scaffold-boot', 'gc-${rootArtifactId}-boot' ^| Set-Content '%ARCH_RES%\pom.xml'"

echo [2c/4] 修复 archetype-metadata.xml（非 XML 文件添加 filtered=true）...
set ARCH_MDATA=target\ai-agent-scaffold-lite-archetype\src\main\resources\META-INF\maven\archetype-metadata.xml
powershell -ExecutionPolicy Bypass -File "%~dp0fix-archetype-metadata.ps1" -filePath "%ARCH_MDATA%"

echo [3/4] 构建 archetype JAR...
cd target\ai-agent-scaffold-lite-archetype
call mvn clean package
cd ..\..

echo [4/4] 生成 Nginx 部署目录...
set REPO_DIR=target\archetype-nginx-repo
set ARCH_GROUP_DIR=%REPO_DIR%\org\example\ai-agent-scaffold-archetype\1.0

if not exist %ARCH_GROUP_DIR% mkdir %ARCH_GROUP_DIR%

copy /Y target\ai-agent-scaffold-lite-archetype\target\ai-agent-scaffold-archetype-1.0.jar %ARCH_GROUP_DIR%\
copy /Y target\ai-agent-scaffold-lite-archetype\pom.xml %ARCH_GROUP_DIR%\ai-agent-scaffold-archetype-1.0.pom

echo 完成！部署目录: target\archetype-nginx-repo\
echo 将整个目录复制到 Nginx 服务器即可使用。
