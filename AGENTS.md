# AGENTS.md — Reglas del proyecto HorusSupplyDrops

## Flujo de trabajo con Git (Obligatorio)

1. **Prohibido hacer push directo a `main`**.
2. Toda modificación (nuevas funcionalidades, correcciones o refactorizaciones) debe realizarse en una **rama nueva** con prefijo adecuado:
   - `feat/<nombre>` para nuevas funcionalidades
   - `fix/<nombre>` para correcciones de errores
   - `refactor/<nombre>` para refactorizaciones
   - `docs/<nombre>` / `chore/<nombre>` para documentación o mantenimiento
3. Los cambios deben integrarse en `main` exclusivamente mediante **Pull Request (PR)**.
4. **Verificar siempre `./gradlew build` antes de abrir el PR** (y asegurarse de que compila y pasa los tests sin errores).

## Stack técnico

- **Lenguaje**: Kotlin (JVM Toolchain 21)
- **JDK**: 21 (`/usr/lib/jvm/java-21-temurin`)
- **Build**: Gradle (Kotlin DSL) con ShadowJar
- **Plataforma objetivo**: Paper / Leaf 1.21.11 (`io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`)
- **Texto y formato**: Adventure MiniMessage
