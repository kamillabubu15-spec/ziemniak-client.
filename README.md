# Ziemniak Client (Fabric 1.21.4)

Fioletowy mod client-side: legalny PvP-HUD + kilka przelacznikow FPS.
Menu otwierasz w grze klawiszem **RIGHT SHIFT**.

## Co jest w srodku
- Hit Indicator - fioletowy "flash" przy trafieniu (bez zmiany hitboxow/zasiegu)
- Combo Counter - licznik trafien pod rzad
- Custom Crosshair - fioletowa obwodka celownika
- Enemy HP HUD - pasek HP celu, na ktory patrzysz
- FPS Booster - mniej czasteczek, wylaczone chmury, fast graphics (przelaczniki wbudowanych opcji gry)
- Config zapisuje sie do `.minecraft/config/ziemniak-client.json`

## Jak zbudowac (potrzebujesz internetu - ja go tu nie mam)
1. Zainstaluj **JDK 21**.
2. Wypakuj ten folder, wejdz w niego w terminalu.
3. `./gradlew build` (Windows: `gradlew.bat build`) - Gradle sam sciagnie
   Minecraft 1.21.4, mapping i Fabric API z internetu przy pierwszym buildzie.
4. Gotowy jar bedzie w `build/libs/ziemniak-client-1.0.0.jar`.
5. Wrzuc go do folderu `.minecraft/mods` (wymaga zainstalowanego Fabric Loadera dla 1.21.4).

Uwaga: sprawdz w `gradle.properties`, czy `fabric_version` odpowiada
najnowszej wersji Fabric API pod 1.21.4 (lista: modrinth.com/mod/fabric-api/versions),
bo numerki wersji zmieniaja sie co jakis czas.

## Czego CELOWO nie ma (i dlaczego)
- **Logowanie/zapisywanie kont, wybor wersji MC** - to robi launcher
  (oficjalny Minecraft Launcher albo Prism/MultiMC), nie mod. Mod dziala
  na tej wersji, na ktora go zbudowales (tu: 1.21.4) - jeden jar = jedna wersja.
  Zeby "przelaczac wersje" jak w launcherach, musialbys zbudowac osobny
  launcher od zera (autoryzacja Microsoft OAuth, pobieranie assetow itd.)
  - to osobny, wielotygodniowy projekt, nie mod.
- **Instalowanie modow z poziomu clienta (jak w Feather/Modrinth App)** -
  to tez osobna aplikacja (menedzer modow), nie cos co mod moze zrobic
  sam sobie. Do tego polecam po prostu **Modrinth App** albo **Prism Launcher**
  - oba juz to robia i obsluguja Fabric 1.21.4 od reki.
- **Killaura / reach / velocity / autoclicker** - swiadomie pominiete,
  bo to czity dajace nieuczciwa przewage i w 99% przypadkow lamie
  regulamin serwera (ryzyko bana). Zamiast tego dodalem legalny PvP-feedback
  (hit indicator, combo, HP HUD).

## Rozwoj
Kolejne moduly HUD dodajesz jako nowa klasa w `features/`, rejestrujesz
w `ZiemniakClient.onInitializeClient()` i dodajesz przelacznik w
`ZiemniakMenuScreen` + pole w `ZiemniakConfig`.
