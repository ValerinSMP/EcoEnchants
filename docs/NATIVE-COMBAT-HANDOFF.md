# Cinco familias nativas — incremento 2, 2026-10-20

Entrega acumulativa del primer par más Letalidad, Reposo y Último recurso. Fuente personal; no instala, no activa, no modifica vEnchanting/vPets/economía/XP, no crea ofertas ni anuncios. No constituye la entrega completa de las 130 familias ni el balance ofensivo global solicitado, que siguen en curso.

| ID | Encantador | Tiers | Tipo | Target | Conflictos | Valor |
|---|---|---|---|---|---|---|
| cuidado | VII | I | normal | hoe | ninguno | Protección de cultivos inmaduros/tallos; contrato NATIVE-MINING-HANDOFF.md. |
| ritmo_minero | XV | I–III | normal | pickaxe | ninguno | +5/10/15% tras10 roturas, pausa10s; contrato anterior. |
| letalidad | XIV | I–V | normal | sword | ninguno | 1/2/3/4/5% armadura por golpe confirmado, hasta4 stacks,4s. |
| reposo | XXIII | I–V | normal | chestplate | rejuvenate (simétrico nativo) | +3/6/9/12/15% exclusivamente regen SATIATED después10s sin combate confirmado. |
| ultimo_recurso | XXX | I–V | ultimate | chestplate | ninguno; límite ultimate nativo intacto | 3/6/9/12/15% sobre daño base de golpes posteriores durante5s; CD30s. |

Cinco definiciones ejecutables en recursos enchants/*.yml, lores de una frase y requisitos has_skill_level aprobados. Sin maná nuevo. El otro chat genera fichas y precio temporal Test 1 shard O 1 gema; sin tarifa productiva. Último recurso sustituye solo baluarte_estelar pendiente: bastion sigue reservado.

## Confirmación y lifecycle compartidos

OriginalRuntime conserva hit(EntityDamageByEntityEvent) y las firmas existentes; su listener observe(EntityDamageEvent) usa UNA cola de confirmación para los procs anteriores y estos tres. NativeProcAdapters conserva el overload anterior y añade el general. Solo salud perdida realmente, causa final idéntica, daño final positivo y evento no cancelado. Un tick MAIN de confirmación o frontera real EntityDeathEvent; no eventos sintéticos ni concesiones de XP. Si otro golpe reemplaza lastDamageCause, no se atribuye daño incierto ni se reintenta. Puede descartar golpes solapados antes de confirmarlos; no promete contarlos todos sin pérdida.

Mismas sesiones y tokens de OriginalRuntime; mundo/quit/muerte/respawn/reload/disable limpian estado y cercan callbacks. No cambia el snapshot de arco ni la elegibilidad de Eco resonante/Sello glacial (NATURAL/COMMAND/huevos permitidos, spawners/MythicMobs/jefes y restantes guardas excluidos). Letalidad reutiliza ese filtro exacto y solo ataques directos ENTITY_ATTACK con espada identificada y requisito válido; no proyectiles, secundarios ni daño sobreletal reinterpretado como más salud perdida.

Letalidad modifica solo su key ecoenchants:letalidad en Attribute.ARMOR, transitoriamente y con MULTIPLY_SCALAR_1 negativo. No toca base, toughness, durabilidad o modifiers ajenos ni cura. Solo armadura positiva. Stacks por atacante+tier, máximo4; cada golpe refresca4s. Se aplica el mayor debuff vivo (máximo20%), nunca suma jugadores. Una tarea por objetivo vence/recalcula; salida del atacante retira su contribución. La reducción afecta golpes posteriores al confirmado.

Reposo usa regen_multiplier nativo con reason [SATIATED] y condición out_of_confirmed_combat, invalidación explícita en transiciones. No se programa curación ni se amplifica MAGIC/REGEN/CUSTOM. Se registran golpes físicos directos/proyectiles aceptados dados o recibidos de entidades vivas; daños ambientales no cuentan como combate. Guarda10s de sesión; estado ausente no autoriza. No consulta SQL ni crea perfiles. Un timer de quietud por jugador; actividad solo cambia su fecha, sin reprogramarlo por golpe. La clasificación SATIATED la aporta el evento; un tercero que falsifique esa razón resulta indistinguible de la regeneración natural.

Último recurso exige cruce confirmado >=40% a<40%, vida restante positiva y máximo de salud estable durante la confirmación. No modifica retroactivamente el golpe activador, no salva sobreletal, no reanima ni cura. Una buff por UUID y pechera/tier; no se apila. Guarda de llegada30s y cooldown UUID30s retenido al retirar/cambiar sesión/objeto, con poda por vencimiento; reload/reinicio quedan cercados además por guarda de llegada. Buff se valida contra la pechera actual en cada golpe; cambios de inventario se comprueban coalescidos al tick siguiente. Permanecer por debajo40% no activa de nuevo sin otro cruce real.

## Daño administrativo y orden de defensas

Los tres nuevos adaptadores clasifican combate físico por ENTITY_ATTACK/ENTITY_SWEEP_ATTACK/PROJECTILE. Último recurso solo opera en ataques de entidades vivas o sus proyectiles; no VOID/KILL/SUICIDE, ambiental, CUSTOM/MAGIC, cancelado, invulnerabilidad/protecciones ni origen secundario/ability de LibreForge. Letalidad/Último recurso verifican además el contexto nativo de comandos; el tipo de daño por sí solo no identifica /damage ... player_attack by ....

CommandDamageGuard resuelve UNA vez net.minecraft.commands.Commands.CURRENT_EXECUTION_CONTEXT como ThreadLocal y luego solo consulta get(), sin stackwalking/reflexión por golpe. Es un límite interno necesario para identificar ejecución de comandos nativos, no una API pública nueva ni un cambio en el ejecutor de comandos. Si no existe/es inaccesible, se registra causa completa y no se ejecutan Letalidad/Último recurso. No afirmar disponibilidad solo por el YAML: comprobar arranque Test y /damage estándar, con tipo físico y /execute. Un plugin que programe daño futuro fuera del contexto de comandos debe conservar procedencia explícita: no se infiere retrospectivamente que una tarea vino de un comando.

La key/ThreadLocal fue contrastada en las clases de los bundles personales1.21.8/1.21.10/1.21.11/26.1.2/26.2 y en el Leaf26.2 real de Test SHA256 b0e5b2c75f3376cc451cb76d4488bcf61350c18448888fdadd00a904c9c6a00a. Es inspección estática, NO prueba de resolución activa del guard.

Último recurso aplica un factor (1-0.03*tier) UNA vez a event.damage en HIGHEST. Después continúan los triggers nativos MONITOR de LibreForge y modificadores externos según su orden; finalmente Paper procesa su defensa normal. No calcula defensa/crítico otra vez ni emite daño nuevo. Las defensas porcentuales puras combinan como 1-(1-r)*(1-p); daño plano que se añada después de este punto no recibe retroactivamente este factor. No se impone cap global. No prometer que el porcentaje final de corazones siempre coincide en presencia de planos o defensas no lineales. El agrupamiento ofensivo solicitado para las restantes familias todavía no está implementado.

## Límites y evidencia

Native-combat.max-players default512, rango1–1024; máximo de objetivos con debuff igual,128 contribuciones por objetivo. Cola común original-procs.max-pending-hits default512/máximo1024. Saturación descarta nuevas confirmaciones y elimina autorización de Reposo de los afectados hasta evidencia válida nueva, sin inventar combate confirmado. Cooldowns/buffs, tareas e inventario están acotados. MAIN exclusivamente, sin polling por tick ni SQL.

Ejecutado con ejecutor personal limitado/JDK25, target21: EcoEnchants86 tests JVM/mocks (cero fallos/errores/omitidos); LF155 del primer incremento. Incluye cinco tiers/stacks/expiración, mayor contribución, cruce/sobreletal/NaN, causas excluidas, confirmación general/cancelación/causa reemplazada, modifier transitorio sin base/modifiers ajenos, cooldown UUID/cap/guarda de sesión y contexto de comandos. Conserva regresiones previas. Cierre del runtime compuesto:29 clases/1497 referencias propias, sin faltantes. APIs EcoEnchants y restantes entradas ajenas al cambio preservadas por composición selectiva.

Pendiente REAL: arranque y guard de comandos, todos los tiers en Paper, condición/holders al equipar/retirar y requisito, trigger nativo/limpieza exacta del atributo, combate simultáneo/letal/protegido/secundario, natural regen vs pociones y custom, CD con reconexión/mundo/transferencias, reload/disable y rendimiento con60–100 jugadores/30–40 activos. No se ha ejecutado MySQL/Test/gameplay ni medido mejoras de MSPT. No instalar sin coordinación del chat dueño.

## Artefactos

- EcoEnchants-2026.27-native-five-all.jar SHA256 180e9bd974dd676602a6203895b07cd73d49b4a8249dd3b6715203692f26ec21
- Requiere LibreForge-2026.27-native-mining-all.jar SHA256 b0a0ab399620a3d37b37c16a0ae33f5d20a3a533378a9bb68984b56435971d77.
- eco/EcoSkills requirement-lore conservados, hashes en five-runtime-manifest.json. No cambios a sus APIs XP/compra.

No instalado, no activo, no verificado en juego. Referencias conceptuales Hypixel: Lethality/Respite/Last_Stand/Flowstate/Delicate, https://hypixelskyblock.minecraft.wiki/ ; valores locales aprobados en specs.json, NO copias de máximos de mesa. Comparativa actual por tier normal y balance de otras familias siguen pendientes; no afirmar equivalencia con el servidor Hypixel.
