# 📦 HorusSupplyDrops

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/Minecraft-1.20.5%20--%2026.3-blue.svg)](https://papermc.io/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Plugin moderno de **Supply Drops (cajas de suministros aéreos)** desarrollado en Kotlin para servidores **Paper / Purpur / Leaf (1.20.5 – 26.3)**. Incluye soporte para hologramas nativos (`TextDisplay`), áreas coordenadas o regiones de WorldGuard, sistema avanzado de loot ponderado con MiniMessage y notificaciones por chat y Discord Webhooks.

---

## ✨ Características

- 🕒 **Eventos Periódicos Automatizados:** Programa drops automáticos cada $X$ minutos.
- 🗺️ **Zonas Flexibles de Aparición:**
  - Modo por **Coordenadas** (`minX`, `maxX`, `minZ`, `maxZ`).
  - Modo por **WorldGuard** (genera los drops dentro de cualquier región definida).
  - Detección inteligente de superficie segura (evita spawn en lava, agua y hojas).
- 🎁 **Loot Configurable y Ponderado (`loot.yml`):**
  - Soporte de pesos (`weight`) y probabilidades independientes en porcentaje (`chance`).
  - Soporte de ítems garantizados (`guaranteed: true`).
  - Personalización de nombres y lore con formato enriquecido **Adventure MiniMessage**.
  - Soporte completo para encantamientos, `CustomModelData`, `unbreakable` y destello forzado (`glow`).
- 🔮 **Efectos Visuales Nativos (1.21+):**
  - Hologramas usando entidades nativas `TextDisplay` con cuenta atrás en tiempo real (¡sin dependencias externas tipo HolographicDisplays o DecentHolograms!).
  - Rayo visual al aparecer el cofre.
  - Columna de partículas en forma de baliza/faro hacia el cielo y partículas ambientales alrededor del contenedor.
- 🛡️ **Seguridad y Protección del Contenedor:**
  - Inmunidad contra explosiones (TNT, creepers).
  - Bloqueo de desplazamiento con pistones.
  - Bloqueo de extracción/robo automático mediante tolvas (hoppers).
  - Desaparición y limpieza automática al vaciar o expirar.
- 💬 **Comunidad y Alertas:**
  - Anuncios en el chat con coordenadas formateadas en MiniMessage.
  - Integración asíncrona con **Discord Webhooks** con embeds personalizables y protección SSRF.

---

## 📋 Requisitos y Dependencias

- **Java**: JDK / JRE 21 o superior.
- **Servidor**: Paper, Purpur o Leaf **1.20.5 – 26.3** (desde la primera versión de Minecraft con Java 21 hasta la 26.3).
- **Dependencias obligatorias**: **Ninguna.** El plugin es autónomo (incluye su propio sistema de hologramas nativos con `TextDisplay` y no requiere plugins externos ni librerías adicionales).
- **Dependencias opcionales (`softdepend`)**: **WorldGuard 7+** (junto a WorldEdit), únicamente necesario si configuras `area.mode: WORLDGUARD`.

---

## 📥 Instalación

1. Descarga el archivo compilado `HorusSupplyDrops-1.0.0.jar` desde [Releases](https://github.com/1Shad0w/HorusSupplyDrops/releases).
2. Coloca el `.jar` en la carpeta `plugins/` de tu servidor.
3. Inicia o reinicia el servidor para generar los archivos de configuración por defecto (`config.yml` y `loot.yml`).
4. Configura tus áreas, tiempos y recompensas a tu gusto.
5. Ejecuta `/supplydrop reload` para aplicar los cambios sin reiniciar.

---

## ⌨️ Comandos y Permisos

| Comando | Descripción | Permiso |
| :--- | :--- | :--- |
| `/supplydrop start` | Fuerza la aparición manual e inmediata de un Supply Drop | `horussupplydrops.admin` |
| `/supplydrop stop` | Elimina todos los Supply Drops activos y limpia sus contenedores | `horussupplydrops.admin` |
| `/supplydrop reload` | Recarga `config.yml` y `loot.yml` en caliente | `horussupplydrops.admin` |

> **Alias disponibles:** `/sd`, `/supplydrops`

---

## ⚙️ Configuración (`config.yml`)

```yaml
# Intervalo en minutos entre cada Supply Drop automático (0 para desactivar el auto-spawn)
interval-minutes: 30

# Duración en segundos antes de que el drop desaparezca si no se vacía
duration-seconds: 300

# Permitir múltiples Supply Drops activos simultáneamente
allow-multiple: false

# Desaparecer el contenedor al vaciarlo y cerrarlo
despawn-when-empty: true

# Contenedor a generar: CHEST o BARREL
container-type: CHEST

loot:
  min-items: 3
  max-items: 6
  allow-duplicates: false

area:
  # Modos disponibles: COORDINATES o WORLDGUARD
  mode: COORDINATES
  world: "world"

  # Coordenadas rectangulares (Modo COORDINATES)
  coordinates:
    min-x: -500
    max-x: 500
    min-z: -500
    max-z: 500

  # Nombre de la región (Modo WORLDGUARD)
  worldguard:
    region-name: "pvp_arena"

  max-location-attempts: 30
  avoid-liquids: true
  avoid-leaves: true

visuals:
  lightning-on-spawn: true
  particles:
    enabled: true
    type: "TOTEM_OF_UNDYING"
    count: 10
    interval-ticks: 10
    radius: 0.8
  beam:
    enabled: true
    particle: "END_ROD"
    height: 30
    step: 1.0
    interval-ticks: 10
  hologram:
    enabled: true
    height-offset: 1.65
    see-through: false
    shadow: true
    background-color: "#66000000" # ARGB Hexadecimal
    lines:
      - "<gradient:#ffaa00:#ff5500><bold>✦ SUPPLY DROP ✦</bold></gradient>"
      - "<gray>Estado: <green>¡DISPONIBLE!"
      - "<gray>Desaparece en: <yellow>{time}"

messages:
  prefix: "<gradient:#ffaa00:#ff5500><bold>[SupplyDrops]</bold></gradient> "
  spawn-broadcast:
    - ""
    - "<gradient:#ffaa00:#ff5500><bold>✦ ¡UN SUPPLY DROP HA CAÍDO! ✦</bold></gradient>"
    - "<gray>Mundo: <white>{world} <dark_gray>| <gray>Coordenadas: <yellow>X: {x}, Y: {y}, Z: {z}"
    - "<gray>Tiempo restante: <aqua>{time}"
    - ""
  claimed-broadcast: "{prefix}<green>¡<yellow>{player}</yellow> ha saqueado el Supply Drop en <white>{x}, {y}, {z}</white>!"
  expired-broadcast: "{prefix}<red>El Supply Drop en <white>{x}, {y}, {z}</white> ha expirado y desaparecido."

discord:
  enabled: false
  webhook-url: "https://discord.com/api/webhooks/..."
  username: "HorusSupplyDrops"
  embed:
    title: "✦ ¡Nuevo Supply Drop! ✦"
    description: "Un suministro aéreo ha aterrizado en **{world}**.\n\n**Coordenadas:** `X: {x}, Y: {y}, Z: {z}`\n**Tiempo disponible:** `{time}`"
    color: "#FFAA00"
    footer: "HorusSupplyDrops"
```

---

## 💎 Configuración de Recompensas (`loot.yml`)

El sistema de loot utiliza selección aleatoria ponderada combinada con probabilidad individual:

```yaml
items:
  espada_horus:
    material: NETHERITE_SWORD
    amount: 1
    weight: 10          # Peso relativo dentro de la ruleta ponderada
    chance: 45.0        # 45% de probabilidad de ser elegible
    name: "<gradient:#ffaa00:#ff5500><bold>Espada de Horus</bold></gradient>"
    lore:
      - "<gray>Forjada en el sol eterno."
      - "<dark_gray>Loot exclusivo de Supply Drop"
    custom-model-data: 1001
    unbreakable: true
    enchantments:
      sharpness: 5
      fire_aspect: 2
      unbreaking: 3

  manzana_encantada:
    material: ENCHANTED_GOLDEN_APPLE
    min-amount: 1
    max-amount: 2
    weight: 8
    chance: 35.0
    guaranteed: false   # Si es true y pasa el chance, se incluye prioritariamente
    glow: true
```

---

## 🛠️ Compilación desde el Código Fuente

Para compilar el proyecto manualmente, clona el repositorio y usa el wrapper de Gradle:

```bash
git clone https://github.com/1Shad0w/HorusSupplyDrops.git
cd HorusSupplyDrops
./gradlew build
```

El archivo compilado y optimizado con todas sus dependencias sombreadas (ShadowJar) se generará en:
```
build/libs/HorusSupplyDrops-1.0.0.jar
```

---

## 🤝 Reglas de Contribución

Consulta [AGENTS.md](AGENTS.md) para conocer las políticas de ramas y pull requests del proyecto:
- Prohibido hacer push directo a `main`.
- Toda aportación debe hacerse mediante ramas secundarias (`feat/...`, `fix/...`, `docs/...`).
- Siempre ejecuta `./gradlew build` y asegúrate de que todos los tests pasen antes de enviar un Pull Request.
