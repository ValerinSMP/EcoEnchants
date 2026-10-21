# Objetivos de combate — estado 21/10/2026

Implementado y compilado en personal con ejecutor limitado. EcoEnchants: 149 pruebas JVM, cero fallos/errores/omitidas, LibreForge: 174 pruebas JVM, cero fallos/errores/omitidas.

- LibreForge-2026.27-combat-targets.jar: `1035c2610a8dd91c4fd6425a809ed2f64d0369c2c9c8b100bebcbc22082c2368`
- EcoEnchants-2026.27-combat-targets-all.jar: `5c56242290c5683c9baf2853908fa90accde19f1418fa117f007130819629ca5`

ABI existente, cierre de clases revisadas, política/scope sobre JAR compuesto y handleLoad real con registro simulado: PASS. SDK, assets, configuraciones, eco/EcoSkills y cambios ajenos preservados. La composición aplica las mismas relocaciones de Kotlin y kotlin-math del runtime; no se entrega el shadowJar completo.

129 IDs del YAML real de ambos Dev examinados: véase delivery/combat-targets-20261021. Bonos permanentes al portador y reglas de recompensas/XP conservados, no inventados como efectos sobre rival. Contrato en docs/COMBAT-TARGETS-HANDOFF.md.

No instalado, activo ni verificado en juego; sin lifecycle/SQL. Pendientes arranque real de Dev, validación en juego/protecciones/sesiones por ID y Spark representativo. La prueba aislada no ejecutó handleEnable ni simuló MySQL ni compras reales. Fuente canónica personal.
