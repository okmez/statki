@echo off
rem Gra z botem na jednym komputerze, bez sieci
rem "graj_z_botem.bat --auto" = komputer gra sam z botem (test)
cd /d "%~dp0"
call kompiluj.bat || exit /b 1
set FX=C:/Users/przem/Documents/JAVA/openjfx-21.0.13_windows-x64_bin-sdk/javafx-sdk-21.0.13/lib
java --module-path "%FX%" --add-modules javafx.controls -cp bin Statki --bot %1
