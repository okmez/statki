@echo off
rem Dwie kopie gry na jednym komputerze (localhost): porty 6000 i 6001
rem "graj_lokalnie.bat --auto" = obie kopie graja same (test)
cd /d "%~dp0"
call kompiluj.bat || exit /b 1
set FX=C:/Users/przem/Documents/JAVA/openjfx-21.0.13_windows-x64_bin-sdk/javafx-sdk-21.0.13/lib
start "Gracz 1" java --module-path "%FX%" --add-modules javafx.controls -cp bin Statki 6000 127.0.0.1 6001 %1
start "Gracz 2" java --module-path "%FX%" --add-modules javafx.controls -cp bin Statki 6001 127.0.0.1 6000 %1
