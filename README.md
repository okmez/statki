# Statki przez sieć (UDP)

Gra w statki w Javie (JavaFX) dla dwóch graczy przez sieć lokalną albo dla jednego gracza z botem.

## Wymagania
- Java 17+ (JDK) i JavaFX SDK 21. Ścieżka do JavaFX jest ustawiona w plikach `.bat` (zmienna `FX`) –
  jeśli masz SDK gdzie indziej, popraw ją w `kompiluj.bat`, `graj.bat`, `graj_lokalnie.bat` i `graj_z_botem.bat`.

## Pliki
- `Komunikat.java` – własna klasa komunikacyjna: stałe (`PORT_DOMYSLNY = 6000`, `MAX_ROZMIAR`, typy `HALO/GOTOWY/STRZAL/WYNIK`, wyniki `PUDLO/TRAFIONY/ZATOPIONY`),
  pakowanie do `byte[]` (`do_bajtow()`) i rozpakowanie (`z_bajtow()`). Przy odbiorze konstruktor **nie** jest uruchamiany –
  widać to w logu: „konstruktor przy odbiorze: NIE”, a `licznik_konstruktorow` rośnie tylko przy wysyłaniu.
- `Statki.java` – gra (JavaFX). Odbiór pakietów działa w tle w `Service`/`Task`, tak jak w przykładzie z zajęć.
  Jest tu też klasa `Bot` – przeciwnik komputerowy.

## Uruchomienie
- **Z botem:** `graj_z_botem.bat` albo w oknie gry przycisk „Graj z botem” zamiast „Polacz”. Sieć nie jest potrzebna.
  Bot dostaje te same `Komunikat`y co przeciwnik w sieci (też pakowane do `byte[]` i rozpakowywane), więc gra działa tak samo.
  Bot szuka statków tam, gdzie zmieści się ich najwięcej, a po trafieniu dobija statek.
  `graj_z_botem.bat --auto` – komputer gra sam z botem (test).
- **Jeden komputer:** `graj_lokalnie.bat` – otwiera dwa okna (porty 6000 i 6001 na 127.0.0.1).
  `graj_lokalnie.bat --auto` – obie kopie grają same ze sobą (test).
- **Dwa komputery:** na każdym `graj.bat IP_DRUGIEGO_KOMPUTERA`, np. na 10.10.10.125: `graj.bat 10.10.10.165`,
  a na 10.10.10.165: `graj.bat 10.10.10.125`. Obie strony używają portu 6000. Adres można też poprawić w oknie przed „Polacz”.
- Przy pierwszym uruchomieniu Windows zapyta o zaporę – trzeba zezwolić Javie na dostęp (sieć prywatna), inaczej pakiety nie dojdą.

## Zasady
Flota: 4, 3, 3, 2, 2, 2, 1, 1, 1, 1; statki nie mogą się stykać. Lewy klik stawia statek, prawy klik / R obraca, albo „Losuj statki”.
Po „Gotowy!” obu graczy losuje się, kto zaczyna. Trafienie = strzelasz dalej, pudło = ruch przeciwnika.
UDP może zgubić pakiet, więc gra co sekundę ponawia wiadomość, na którą nie przyszła odpowiedź.
