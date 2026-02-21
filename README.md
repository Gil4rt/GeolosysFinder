# Geolosys Scanner Addon

Addon-мод для [Geolosys](https://www.curseforge.com/minecraft/mc-mods/geolosys) (Minecraft Forge 1.16.5).
Добавляет систему геологической разведки — сканирование чанков на руды, HUD-радар с приближением и звуковой индикацией.

An addon mod for [Geolosys](https://www.curseforge.com/minecraft/mc-mods/geolosys) (Minecraft Forge 1.16.5).
Adds a geological prospecting system — chunk ore scanning, proximity HUD radar, and sound indicators.

---

## Возможности / Features

- Сканирование чанков на руды Geolosys правым кликом (ПКМ) с инструментом в руке
- HUD-панель справа по центру экрана с информацией о найденных рудах
- Радар приближения: шкала нагрева, расстояние, глубина, направление относительно камеры
- GUI выбора руды (Shift+ПКМ) с иконками блоков и оценкой плотности залежей
- Звуковая индикация приближения к руде (настраиваемая, с кнопкой мута в GUI)
- Полная локализация: русский и английский языки
- Серверный и клиентский конфиг

---

## Установка / Installation

1. Установите [Minecraft Forge 1.16.5](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.16.5.html)
2. Установите [Geolosys](https://www.curseforge.com/minecraft/mc-mods/geolosys) (версия 5.0.0+)
3. Скачайте `geolosys-scanner-addon-1.0.0.jar` из [Releases](../../releases)
4. Положите JAR-файл в папку `mods/`

**Мультиплеер:** мод нужно установить и на сервер, и на клиент.

---

## Использование / Usage

Возьмите в руку **Prospector's Pick** (Geolosys) или **палку** (Stick).

| Действие | Клавиша | Описание |
|---|---|---|
| Сканировать | ПКМ (RMB) | Сканирует чанки вокруг. На HUD появится список найденных руд |
| Выбрать руду | Shift + ПКМ | Открывает GUI со списком руд, иконками и оценкой плотности. Кликните на руду для отслеживания |
| Выключить сканер | ЛКМ (LMB) | Выключает HUD и радар. Нужно держать инструмент в руке |

После выбора руды HUD переключается в режим радара:
- **Шкала нагрева** — чем ближе к руде, тем больше заполнена (пульсирует на расстоянии <5 блоков)
- **Направление** — показывает куда идти относительно вашей камеры (Впереди, Сзади, Слева, Справа и т.д.)
- **Расстояние и глубина** — сколько блоков до руды и нужно ли копать вниз/вверх
- **Звук** — пинг-звук ускоряется при приближении. Можно отключить кнопкой в GUI выбора руды

HUD остаётся на экране даже если убрать инструмент — можно спокойно копать и строить.

---

## Конфигурация / Configuration

### Серверный конфиг (`geolosys_scanner-server.toml`)

| Параметр | По умолчанию | Описание |
|---|---|---|
| `allowedItems` | `["geolosys:prospectors_pick", "minecraft:stick"]` | Предметы, активирующие сканер |
| `scanRadius` | `1` | Радиус сканирования в чанках (1–5) |
| `minDepth` | `1` | Минимальный Y уровень сканирования |
| `maxDepth` | `80` | Максимальный Y уровень сканирования |
| `updateIntervalTicks` | `20` | Интервал обновления радара в тиках |

### Клиентский конфиг (`geolosys_scanner-client.toml`)

| Параметр | По умолчанию | Описание |
|---|---|---|
| `soundEnabled` | `true` | Включить звук радара |
| `soundVolume` | `0.5` | Громкость (0.1–1.0) |
| `hudEnabled` | `true` | Показывать HUD |

---

## Сборка из исходников / Building from Source

### Требования

- **Java Development Kit 8** (JDK 8) — [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=8)
- **Git**

### Шаги

```bash
git clone https://github.com/YOUR_USERNAME/GeolosysFinder.git
cd GeolosysFinder
```

Если ваша системная Java **не** версии 8, укажите путь к JDK 8 в `gradle.properties`:

```properties
org.gradle.java.home=C:/path/to/jdk8
```

Сборка:

```bash
./gradlew build
```

Готовый JAR будет в `build/libs/geolosys-scanner-addon-1.0.0.jar`.

---

## Структура проекта / Project Structure

```
src/main/java/com/geolosysscanner/
├── GeolosysScanner.java          # Точка входа мода
├── config/
│   ├── ScannerConfig.java        # Серверный конфиг
│   └── ClientConfig.java         # Клиентский конфиг
├── network/
│   ├── NetworkHandler.java       # Регистрация пакетов
│   ├── OreEntry.java             # DTO руды
│   ├── PacketScanRequest.java    # C→S: запрос сканирования
│   ├── PacketSelectTarget.java   # C→S: выбор руды
│   ├── PacketDeactivate.java     # C→S: выключение
│   ├── PacketScanResult.java     # S→C: результат сканирования
│   ├── PacketRadarUpdate.java    # S→C: обновление радара
│   └── PacketScannerDeactivated.java  # S→C: подтверждение выкл.
├── server/
│   ├── ServerScanHandler.java    # Логика сканирования
│   ├── ServerEventHandler.java   # Тик и логаут
│   ├── PlayerScanState.java      # Состояние игрока
│   └── ScanResult.java           # Аккумулятор блоков руды
└── client/
    ├── ClientEventHandler.java   # Ввод, звук
    ├── ClientScanData.java       # Клиентское состояние
    ├── ScannerHudRenderer.java   # HUD оверлей
    └── OreSelectionScreen.java   # GUI выбора руды
```

---

## Лицензия / License

MIT
