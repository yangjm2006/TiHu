@echo off
REM 鹈鹕后端系统 - 快速启动脚本（Windows）

echo ========================================
echo   鹈鹕图书分享平台 - 后端启动工具
echo ========================================
echo.

REM 检查 Java 版本
echo [1/5] 检查 Java 环境...
java -version 2>&1 | findstr /i "version"
if %ERRORLEVEL% NEQ 0 (
    echo 错误: 未找到 Java, 请确保 JDK 21+ 已安装
    pause
    exit /b 1
)

REM 检查 Maven
echo [2/5] 检查 Maven 环境...
call mvnw --version 2>&1 | findstr /i "Apache"
if %ERRORLEVEL% NEQ 0 (
    echo 错误: Maven 初始化失败
    pause
    exit /b 1
)

REM 清理项目
echo [3/5] 清理项目...
call mvnw clean

REM 编译项目
echo [4/5] 编译项目...
call mvnw -DskipTests compile
if %ERRORLEVEL% NEQ 0 (
    echo 错误: 编译失败，请检查代码
    pause
    exit /b 1
)

REM 启动应用
echo [5/5] 启动应用（Docker可选）...
echo.
echo 选择启动方式:
echo 1. 直接启动（需要本地 MySQL + Redis）
echo 2. Docker 启动（需要 Docker Desktop）
echo 3. 只打包不启动
echo.
set /p choice=请选择 (1-3):

if "%choice%"=="1" (
    echo 启动应用... 访问 http://localhost:9090/api
    call mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
) else if "%choice%"=="2" (
    echo 构建镜像...
    call mvnw clean package -DskipTests
    docker build -t tihu-backend:latest .
    echo 启动容器...
    docker run -d -p 9090:9090 --name tihu-backend tihu-backend:latest
    echo 容器已启动，访问 http://localhost:9090/api
) else if "%choice%"=="3" (
    echo 打包应用...
    call mvnw package -DskipTests
    echo 打包完成: target/backend-0.0.1-SNAPSHOT.jar
) else (
    echo 无效选择
    pause
    exit /b 1
)

echo.
echo 启动完成！
echo.
echo 常用命令:
echo - 查看日志: docker logs -f tihu-backend
echo - 停止容器: docker stop tihu-backend
echo - 删除容器: docker rm tihu-backend
echo.
pause

