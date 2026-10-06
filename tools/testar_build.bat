@echo off
rem Compila o kn8 e roda os testes automaticos, salvando tudo em kn8-teste-log.txt (para mandar ao Claude).
rem Uso: copie este arquivo para a pasta do projeto (onde fica o gradlew.bat) e de dois cliques.
setlocal
cd /d "%~dp0"
if not exist gradlew.bat (
    if exist ..\gradlew.bat cd ..
)
if not exist gradlew.bat (
    echo Nao achei o gradlew.bat. Coloque este arquivo na pasta do projeto kn8-main.
    pause
    exit /b 1
)
rem O java do PATH desta maquina e o 8: usa o JDK 21 que o Gradle baixou.
set "JAVA_HOME=%USERPROFILE%\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2"
set "LOG=%CD%\kn8-teste-log.txt"
echo ==== kn8: build + JUnit ==== > "%LOG%"
call gradlew.bat build --console=plain >> "%LOG%" 2>&1
set BUILD=%ERRORLEVEL%
echo. >> "%LOG%"
echo ==== kn8: GameTests ==== >> "%LOG%"
call gradlew.bat runGameTestServer --console=plain >> "%LOG%" 2>&1
set GAMETEST=%ERRORLEVEL%
echo. >> "%LOG%"
echo ==== resultado: build=%BUILD% gametests=%GAMETEST% (0 = ok) ==== >> "%LOG%"
echo.
echo Pronto. build=%BUILD% gametests=%GAMETEST% (0 = ok)
echo Log salvo em: %LOG%
echo Mande esse arquivo para o Claude se algo falhar.
pause
