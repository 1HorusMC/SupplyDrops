# Documentación Técnica y de Arquitectura — HorusSupplyDrops

Esta documentación detalla la arquitectura interna, el flujo de ejecución y las consideraciones de diseño del plugin **HorusSupplyDrops**.

---

## 1. Arquitectura de Módulos

El código fuente se encuentra organizado bajo el paquete base `com.horus.supplydrops`:

```
com.horus.supplydrops/
├── HorusSupplyDropsPlugin.kt   # Clase principal del plugin (JavaPlugin lifecycle)
├── area/                       # Cálculo de áreas y límites de spawn
│   ├── AreaBounds.kt           # Bounding box 2D y generador de coordenadas X/Z
│   ├── LocationFinder.kt       # Algoritmo de búsqueda de superficie segura en el mundo
│   └── WorldGuardHook.kt       # Integración opcional (softdepend) con WorldGuard 7
├── command/
│   └── SupplyDropCommand.kt    # Manejador de comandos y autocompletado en tabulación
├── config/
│   └── PluginConfig.kt         # Modelos de datos inmutables y parser tipado de config.yml
├── discord/
│   └── DiscordWebhookClient.kt # Cliente HTTP asíncrono para notificaciones Discord
├── drop/
│   ├── ActiveSupplyDrop.kt     # Representación en memoria de un drop activo
│   └── SupplyDropManager.kt    # Orquestador del ciclo de vida de los drops
├── listener/
│   └── SupplyDropListener.kt   # Eventos de inventario, protección de bloques y limpieza
├── loot/
│   ├── LootEntry.kt            # Definición de ítem de loot y mapeo a ItemStack Bukkit
│   ├── LootManager.kt          # Parser de loot.yml y despachador de inventarios
│   └── LootSelector.kt         # Algoritmo puro de selección ponderada y probabilidades
├── util/
│   └── TimeFormatter.kt        # Utilidad para formateo de tiempos (mm:ss / hh:mm:ss)
└── visual/
    ├── HologramDisplay.kt      # Manejador del TextDisplay nativo (Minecraft 1.21+)
    └── VisualEffectsTask.kt    # Tareas de partículas y baliza vertical
```

---

## 2. Ciclo de Vida de un Supply Drop

```
            [ Scheduler / /supplydrop start ]
                           │
                           ▼
               [ LocationFinder.kt ]
      ¿Mundo cargado y bloque seguro encontrado?
                ├── No  ──► Aborta y registra aviso en consola
                └── Sí
                           │
                           ▼
          [ Generación en SupplyDropManager ]
   1. Coloca Bloque Contenedor (Cofre / Barril)
   2. Puebla inventario según pesos en loot.yml
   3. Spawnea TextDisplay con PDC tag 'supply_drop_hologram'
   4. Dispara efecto visual de rayo (strikeLightningEffect)
   5. Inicia tarea de partículas / columna de baliza
   6. Difunde broadcast en chat + Webhook Discord
                           │
                           ▼
              [ Tarea de Cuenta Atrás ]
      Actualiza texto del holograma cada segundo
                           │
           ┌───────────────┴───────────────┐
           ▼                               ▼
    [ Jugador vacía cofre ]         [ Tiempo expira ]
           │                               │
           └───────────────┬───────────────┘
                           │
                           ▼
                  [ Limpieza / Despawn ]
   1. Cancela tareas programadas (cuenta atrás, partículas)
   2. Remueve TextDisplay nativo
   3. Limpia inventario y transforma bloque en AIR
   4. Anuncia fin del evento por chat (saqueado o expirado)
```

---

## 3. Sistema de Selección de Loot (`LootSelector.kt`)

El selector de loot resuelve dos factores para cada entrada en `loot.yml`:
1. **Filtro de Probabilidad (`chance`):** Determina si el ítem supera una tirada porcentual entre `0.0` y `100.0`.
2. **Prioridad Garantizada (`guaranteed: true`):** Los ítems que superen su probabilidad y estén marcados como garantizados entran directamente a la selección final hasta copar el límite de ítems.
3. **Selección Ponderada (`weight`):** Los espacios restantes se rellenan mediante selección ponderada basada en ruleta acumulativa sobre los candidatos elegibles restantes.
4. **Duplicados (`allow-duplicates`):** Si está deshabilitado, cada entrada del pool solo puede seleccionarse como máximo una vez por cofre.

---

## 4. Hologramas Nativos (`TextDisplay`)

A diferencia de soluciones tradicionales que requieren plugins externos (HolographicDisplays, DecentHolograms):
- **Entidades Nativas:** Se utiliza `org.bukkit.entity.TextDisplay`, introducido en Minecraft 1.19.4+ y completamente optimizado en 1.21.
- **Sin Persistencia Residual:** Las entidades se marcan con `isPersistent = false` y llevan un tag en su `PersistentDataContainer` (`supply_drop_hologram`).
- **Limpieza de Huérfanos:** Al cargar un chunk (`ChunkLoadEvent`), si existe un `TextDisplay` con el tag del plugin que no figure en la lista de drops activos en memoria, es eliminado automáticamente para evitar hologramas flotantes residuales tras caídas inesperadas del servidor.

---

## 5. Medidas de Seguridad y Endurecimiento

- **Prevención de SSRF en Webhooks:** `DiscordWebhookClient` valida que la URL pertenezca exclusivamente a dominios oficiales de Discord (`discord.com`, `discordapp.com`, etc.) bajo protocolo HTTPS y sin credenciales en la URL.
- **No Fuga de Secretos en Logs:** Los mensajes de error o excepciones de red nunca exponen la URL completa del Webhook en la consola del servidor.
- **Desactivación de Menciones Discord:** El payload JSON enviado a Discord define explícitamente `"allowed_mentions": {"parse": []}` evitando que atacantes utilicen nombres de mundos o coordenadas para generar menciones `@everyone` o `@here`.
- **Escape en MiniMessage:** Las cadenas dinámicas provenientes de jugadores (como el nombre del jugador que saquea el cofre) son sanitizadas con `MiniMessage.miniMessage().escapeTags(playerName)` para evitar inyecciones de formato en el chat.
- **Protección de Bloques:** Listeners activos cancelan la absorción por tolvas (`InventoryMoveItemEvent`), movimiento por pistones (`BlockPistonExtendEvent`, `BlockPistonRetractEvent`) y destrucción por explosiones (`EntityExplodeEvent`, `BlockExplodeEvent`).
