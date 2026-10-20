# Cuidado y Ritmo minero — incremento 1, 2026-10-20

Fuente canónica personal. Candidatos para el chat propietario de vEnchanting: no crear fichas/ofertas automáticamente, no instalar ni ejecutar lifecycle desde este incremento. El alcance completo de 130 familias sigue pendiente; esto entrega solamente dos familias ejecutables.

| ID | Nivel Encantador | Tiers | Tipo / targets / conflictos | Mecánica |
|---|---|---|---|---|
| cuidado | VII (7) | I | normal / hoe / ninguno | Bloquea rotura manual de los cinco cultivos inmaduros vanilla y cuatro tallos, incluidos unidos; permite frutos/cultivos maduros. |
| ritmo_minero | XV (15) | I–III | normal / pickaxe / ninguno | Tras 10 bloques minerales manuales confirmados, +5/10/15% al atributo nativo de velocidad minera; desaparece al parar 10 s o cambiar herramienta/sesión. |

YAML nativos en `eco-core/core-plugin/src/main/resources/enchants/`. Lore configurable de una frase. Requisitos has_skill_level, holders, conflictos y límites nativos siguen vigentes. Precio temporal de Test 1 shard O 1 gema lo publica el otro chat; no se añade tarifa productiva, maná, XP o recompensas aquí.

## Contrato y límites

Cuidado usa manual_crop_guard / guard_immature_crops al LOWEST, antes del procesamiento normal de rotura/drop de LibreForge. Solo cancela el evento exacto acreditado por el listener; no protege right-click ni redefine replantación.

Ritmo usa has_mining_rhythm con invalidación explícita de transiciones y mining_speed_multiplier existente. LibreForge admite args.transient:true en AttributeEffect; default y firmas existentes se conservan. No se cambia el atributo base ni se añade Haste. Primer sello UUID de herramienta refresca el provider nativo. Retirada usa el lifecycle nativo, sin sumar modifiers paralelos.

Atribución manual: BlockDamageEvent más BlockBreakEvent no cancelado y bloque efectivamente retirado al siguiente tick. Se excluyen metadatos secundarios blockbreakevent-ignore de las rutas mineras de LibreForge y custom blocks registrados en eco; no se promete detectar plugins que falsifiquen eventos manuales ni minerales colocados. Whitelist: stone/deepslate/netherrack/end_stone, *_ORE y ancient_debris. Otros bloques no cuentan ni extienden la ventana; el bonus activo corresponde al pico mediante el atributo nativo.

MAIN exclusivamente. Máximo 512 jugadores/estados por defecto (config native-mining.max-players, rango 1–2048), 8 confirmaciones pendientes por jugador y máximo global igual al límite; al saturar no se concede progreso. Un timer de vencimiento por secuencia y checks de inventario coalescidos; no polling por tick ni SQL. Callbacks viejos quedan cercados al salir, morir, cambiar mundo o reload/disable. Identidad y tier del pico se comprueban antes de admitir cada confirmación.

## Artefactos

- LibreForge-2026.27-native-mining-all.jar SHA256 b0a0ab399620a3d37b37c16a0ae33f5d20a3a533378a9bb68984b56435971d77
- EcoEnchants-2026.27-native-mining-all.jar SHA256 2125248d8ba1b186fe5e9204e7a059d9515d687ccdd453de0f009783e269f715

Composición selectiva sobre los baselines requirement-lore instalados en Test. Manifest adjunto enumera entradas sustituidas; no llevar shadowJar completo con cambios ajenos. eco y EcoSkills se conservan en los hashes declarados en mining-runtime-manifest.json.

## Evidencia y aceptación pendiente

Ejecutado en personal con el ejecutor limitado: LF155 y EcoEnchants78 tests JVM/mocks sin fallos; nuevos casos de cinco edades, cuatro tallos/frutos, tiers1–3/umbral10/expiración y scope de evento restaurado incluso al fallar. Cierre estático del compuesto: 9 clases y 425 referencias propias, sin referencias faltantes. No se simuló una prueba MySQL, no hay nuevo almacenamiento.

Pendiente REAL en Test: paquete cliente/manual instant-break, protección externa/cancelación, herramientas intercambiadas/equipadas/retiradas y metadatos, 3x3/dynamite/replantación sin progreso secundario, exactamente un modifier y retirada, requisito insuficiente, conflictos/varios enchants sin límite general, muerte/reconexión/mundo/reload/disable y varios jugadores. Medir MSPT con jugadores; los límites propuestos no son rendimiento medido. No instalado, no activo, no verificado en juego.
