@echo off
cd /d "%~dp0"
set FX=C:/Users/przem/Documents/JAVA/openjfx-21.0.13_windows-x64_bin-sdk/javafx-sdk-21.0.13/lib
javac --module-path "%FX%" --add-modules javafx.controls -d bin Komunikat.java Statki.java || (pause & exit /b 1)
