@echo off
rem Compila e abre a demonstracao de ORM (Matricula escolar).
rem Uso:  executar.bat          abre a janela
rem       executar.bat --teste  roda o roteiro no console, usando H2
cd /d "%~dp0"
chcp 65001 >nul

if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -cp "lib/*" -d out src\*.java src\modelo\*.java src\persistencia\*.java src\negocio\*.java src\visao\*.java
if errorlevel 1 (
    echo.
    echo Erro de compilacao.
    pause
    exit /b 1
)
xcopy /e /i /q /y src\META-INF out\META-INF >nul

java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "out;lib/*" Principal %*
if errorlevel 1 pause
