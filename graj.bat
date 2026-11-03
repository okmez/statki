@echo off
rem Gra na dwoch komputerach. Uzycie: graj.bat [IP_przeciwnika]
rem (adres mozna tez zmienic w oknie gry przed kliknieciem Polacz)
cd /d "%~dp0"
call kompiluj.bat || exit /b 1
set FX=C:/Users/przem/Documents/JAVA/openjfx-21.0.13_windows-x64_bin-sdk/javafx-sdk-21.0.13/lib
set IP=%1
if "%IP%"=="" set IP=10.10.10.165
java --module-path "%FX%" --add-modules javafx.controls -cp bin Statki 6000 %IP% 6000
