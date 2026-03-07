@echo off

set APP_NAME=encore
set JAR_SOURCE=build\tasks\_%APP_NAME%_executableJarJvm\%APP_NAME%-jvm-executable.jar
set JAR_TARGET=deploy\%APP_NAME%.jar

echo Building server JAR...
call amper package --format=executable-jar
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Amper package failed!
    pause
    exit /b 1
)

if not exist deploy mkdir deploy
copy /y "%JAR_SOURCE%" "%JAR_TARGET%" >nul
xcopy /e /i /y "assets" "deploy\assets" >nul
xcopy /e /i /y "backstage" "deploy\backstage" >nul

echo.
echo Build success.

set /p BUILDDOCS=Build documentation website? (y/n):

if /i "%BUILDDOCS%"=="y" (
    echo.
    echo Building documentation...
    pushd docs

    if exist package.json (
        call npm install
        if %errorlevel% neq 0 (
            echo [ERROR] npm install failed!
            popd
            pause
            exit /b 1
        )
    )

    call npm run build
    if %errorlevel% neq 0 (
        echo [ERROR] npm build failed!
        popd
        pause
        exit /b 1
    )

    echo Moving built docs to deploy/docs/ ...
    popd

    if exist deploy\docs rmdir /s /q deploy\docs
    mkdir deploy\docs

    xcopy /e /i /y "docs\dist" "deploy\docs" >nul

    echo Documentation successfully moved to deploy/docs/
) else (
    echo Skipping documentation build.
)

echo.
echo =====================================
echo Build finished successfully!
echo Press any key to exit...
pause >nul
