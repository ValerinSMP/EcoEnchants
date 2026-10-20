# Entrega consolidada EcoSuite — 20/10/2026

Candidato de código/YAML para integración por el chat de vEnchanting. **No instalado, no activo ni probado en juego.** Este documento sustituye la elección de JAR por lotes anteriores, no sus evidencias. No ejecutar instrucciones históricas de instalación. Producción queda intacta.

## Contrato para integrar

- `delivery/native-expansion-20261020/consolidated/manifest.json`:117 definiciones completas, ID, nivel Encantador aprobado, todos los tiers, targets/conflictos/tipo, fórmulas de effects, modo, hash candidato y ruta/hash Test de origen. `BALANCE-POR-ID.md` permite revisión humana.13 reservados excluidos; no habilitarlos por inferencia. No incluye ofertas, skills.yml ni precios productivos. Test mantiene1 shard **O**1 gema, compras/avisos los prepara el propietario del consumidor.
- Mantener normal.limit y anvil.enchant-limit en−1, conflictos y límites especiales existentes. No retirar encantamientos ni reducir niveles de objetos existentes. Toque Infernal XII y nueve movimientos aprobados se conservan: no sustituir skills.yml. Reposo y Rejuvenate declaran conflicto mutuo.
- Comprobar hashes de origen antes de aplicar cada YAML: `null` significa que no existía en la captura, NO permiso para sobrescribir un archivo que ahora exista. Si hay diferencias, reconciliar con la configuración actual del chat dueño. Las definiciones se entregan separadas del JAR; los recursos viejos dentro de este no sustituyen este conjunto.
- Usar los cuatro JAR coordinados y reinicio cuando lo coordine el chat dueño. No activar estas definiciones con runtimes anteriores. Este chat no ha instalado, reiniciado, recargado ni abierto servidores para probar.
- API/SDK de compra y XP, recibos/outbox/awardId, puente final vPets, fórmulas/BD/progresión de EcoSkills no se modifican. EcoSkills conserva byte por byte el candidato requirement-views. vEnchanting/vPets no editados. No SQL nuevo.

## PvP/PvE solicitado por el propietario

`combat-mode: both|pvp|pve` en la raíz de cada enchant de combate dirigido. Ausente equivale a ambos en las rutas que lo admiten; valor explícito inválido desactiva el efecto de combate.61 definiciones con ambos,5 PvE específicos y2 PvP.49 son actividades/pasivos/nubes nativas sin selector dirigido: sus estadísticas siguen funcionando en ambos contextos, las actividades mantienen su dominio y las nubes vanilla afectan ambos. No prometer que un selector por golpe filtra retrospectivamente una nube ni divide una estadística pasiva global.

Se reutilizan el filtro de elegibilidad y los adaptadores existentes; el filtro `enchant_combat_mode` solo entra en triggers legacy que necesitan la selección. Los originales y sus callbacks verifican su familia en la frontera existente, sin duplicar ese filtro en cada efecto.

- Originales, Letalidad, consultas de objetivos y Enjambre admiten jugadores reales cuando corresponde. Enjambre cuenta oponentes válidos, hasta10, según el modo; no cambia su coeficiente. Swarm mantiene el nombre de argumento compatible `percent_per_monster` aunque ahora admita oponentes PvP.
- Monstruos vanilla natural, /summon, huevos y dispensador permitidos. Spawner/trial-spawner, Mythic, jefes/warden/elder guardian, NPC, mascotas, aliados, tags/PDC/nombres e invulnerabilidad conservan sus exclusiones para originales/consultas. Modificadores legacy con objetivos específicos conservan esos filtros; no se transforma Boss Hunter en un ataque contra jugadores.
- PvP comprueba identidad distinta, sesión/mundo, jugador real/online, world.pvp, creativo/espectador, NPC/tags, equipo por nombre y protección. Defensa usa permiso atacante→portador. PDC ordinario de un jugador no se interpreta como marca de monstruo personalizado; esto no omite integraciones de protección.
- Titán usa Defensa EcoSkills de jugadores: PvP explícito y filtro de player, sin inventar estadísticas de mobs. Stun quita IA y permanece PvE. Cambiar un modo no transforma un efecto que requiere un tipo de criatura en otro efecto diferente.

## Cambios funcionales del cierre

Se conservaron los25 originales/incorporaciones ejecutables ya entregados y se revisaron las39 familias existentes restantes. Normalización selectiva aditiva y correcciones anteriores permanecen. Las definiciones conservan targets, tipos, conflictos, costes existentes y tiers; Cuchilla VI y Herida VII incluidos.

- Thunderlord: bonus8–40% aditivo al tercer golpe tras dos confirmados, mismo oponente/sesión, cooldown10s por jugador. Usa el ledger existente en modo cíclico; sin puntos/NBT por golpe. Una operación simultánea pendiente no adelanta el contador. Incertidumbre consume cuota y desautoriza bonus de ese ciclo; rechazo devuelve posición; retirada impide escritura atrasada.
- Thunderbolt: tres golpes confirmados,4–20% del daño confirmado hacia hasta10 otros objetivos/radio2, cooldown10s. No rayos vanilla de daño ajeno al porcentaje. Ambos conservan conflictos existentes.
- Combo: memoria de sesión con hasta7 vencimientos, ventana2+2L segundos/máximo2+L muertes; +L% por muerte, aditivo. Sin puntos persistentes; no contar un aviso kill adelantado.
- Robavida y Drenaje: `%v%` es salud realmente perdida, limitada por sobreletal, en `confirmed_native_burst`; usan give_health nativo con evento de curación. Propuesta local explícita: Robavida cura10L% del daño, hasta0,5L% de vida máxima por golpe; Drenaje conserva10L% de máximo por100daño, con techoL% de máximo cada4s. No confundir estos límites por familia con un cap global de daño ni con HP equivalentes a Hypixel.
- Vampirismo:1–5% de vida faltante, no puntos planos. `genuine_kill` requiere evento nativo con muerte no cancelada y atribución; force legacy sin testigo no concede esta curación. TriggerKill conserva firma/force/callbacks y añade EVENT al dispatch real, no a force. Evita concederla dos veces por avisos de Mythic y por el kill anticipado de bleed.
- Carve/Cleave: confirmación, consulta nativa ya acotada y hasta12 objetivos adicionales, sin repetir principal/portador ni proc del emisor. Carve conserva2L plano/radio0,5L; Cleave5L% de daño confirmado/radio3,3+0,2L/CD3s. El12 es límite local, no afirmación del servidor actual de Hypixel.
- Bleed/Wound usan el scheduler de pulsos existente de Cicatriz: fuente/sesión/mundo/protección/retirada/cancelación y límites, sin TriggerKill.force antes de que muera. Conservan daño1, intervalos20/15 ticks, pulsos2+L y chances10L/3L. No se cambia globalmente bleed de otros consumidores.
- Ender Slayer y Smoldering pasan del plano al porcentaje anunciado y al grupo aditivo; Shura añade umbral de salud; Jumpshot añade condición in_air nativa. Titán deja de ser multiplicador sin tope y solo consulta Defensa de jugadores.
- Cooldowns antes fuera de args pasan a args silenciosos. Contagion recupera probabilidad5L% común para nube/sonido; Sparks requiere tormenta. Cadenas exteriores sin id siguen siendo válidas, no se eliminan.
- Descripciones corregidas para duración de Frost/Vanish, velocidad de Escape, promedio de fortuna/drops y chance33/75% de Prospector del Nether. Tripleshot conserva chain-args.arrows1: la cadena real de Test emite2 flechas adicionales; no se duplica otra vez.
- Rápido opta por fuerza Paper0–1, conserva umbral100−15L y velocidad máxima3. No cambia crítico ni el valor force del evento: no declara carga completa para procs que requieren disparo realmente cargado. Otros consumidores conservan por defecto la fórmula anterior; retirada no deja buckets vacíos ni duplica identificadores al habilitar otra vez.
- Escudo mide BLOCKING real, no todo event.damage por tener el escudo levantado. Rebounding atribuye el golpe y valida protección, con guardia opt-in contra recursión; DamageVictim legacy sin esos args conserva su ruta. Cancelación tardía y orden real entre plugins siguen en la matriz Test.
- Replantación conserva implementación upstream y callback BlockPlaceEvent, ahora con madurez antes de semillas, cola1024/lote64 por tick (configurable1–256), sesión/generación/chunk/aire/suelo/herramienta actuales y rollback exacto si se veta la colocación. No carga chunks ni consume semilla anticipadamente. Se limita a cultivos con semilla reconocida; no reconstruye bloques colocados por otro. Falta prueba real de suelo/cancelación/otros plugins y carga.

## Daño, callbacks y rendimiento

Grupo `ecoenchants_normal` exclusivamente para holders normales autorizados: B×(1+Σα), no producto de cada bonus. B incluye la matemática nativa que corresponde a ese mismo golpe (crítico/fuerza/planos); no un segundo cálculo de stats. Las familias condicionales suman cuando se cumplen, sus propios caps permanecen. No cap global inventado ni modificación por defecto de damage_multiplier para items/pets/skills ajenos.

Ferocidad repite el raw resuelto y suprime volver a aplicar matemática/procs del emisor; receptor, muerte, drops y XP nativos continúan. Secundarios usan eventos de daño reales, sin salud directa ni premios manuales. Cuando una familia usa porcentaje de daño confirmado, su base ya refleja defensa del objetivo inicial: el nuevo objetivo aplica su propia protección al nuevo golpe. No prometer ese mismo porcentaje de salud final contra otra armadura. El pulso al mismo objetivo es otro golpe físico, no cambiar retrospectivamente el primero.

Comparaciones sobre B igual: WyvernII30% + FortalezaII4% + PrimerGolpeV80% ⇒2,14B, frente a2,4336B como producto. No afirma DPS absoluto ni efecto final tras armadura diferente. Curación, estados, debuffs, crítico, fortaleza, ferocidad, planos, reflejos y área son ejes separados; sus fórmulas y límites por ID están en manifiesto/tabla. Casos de PvP sostenido y combinación real requieren Test.

Una cola de confirmación existente, fuente al disparar, UUID de objeto, sesiones y mecanismos nativos de refresco. Limpieza por índices UUID, no barridos de todos los estados por cada muerte. Consultas8/tick y hasta32 candidatos por defecto, caps efectivos documentados en lotes previos; estados/pulsos512 (configurable hasta1024), partículas y tareas con límites. Saturación rechaza/no inventa objetivos ni concede XP. No polling/SQL por tooltip. Diseño pensado para60–100online y30–40activos; **no TPS/MSPT/ahorro medido**.

## Evidencia y aceptación pendiente

- EE127 y LF168 pruebas JVM/mocks pasan,0 fallos/errores/omitidas. eco78 del incremento inalterado previo:373 acumuladas, no373 pruebas de juego. La matriz del engine cubre27 familias/todos sus tiers; no equivale a ejecutar cada YAML en Paper. Nuevas pruebas: modos/monstruos natural-comando-huevo/spawner/PvP/aliados/protección, ciclos/solapamiento/incertidumbre/retiro, kill genuino, fuerza del arco y bloqueo real. Índices y regresiones anteriores conservados.
-117 YAML parseados,128 efectos y130 condiciones contrastados con declaraciones nativas actuales. Wrappers/herencia resueltos. No prueba de registro activo.131 clases modificadas acumuladas/5676 referencias propias verificadas; tres smoke sobre JAR compuesto pasan con Kotlin sombreado y firmas reales EcoSkills. SDK compra/XP byte idéntico; otros cambios ajenos fuera del compuesto.
- Common NMS y4adapters compilados antes; módulo completo26_2 offline sigue pendiente por paper-api134 ausente. Común remapeado sobre5adapters conservados y referencias reales26.2 verificadas estáticamente; no ocultar esta limitación ni afirmar NMS ejecutado.
- Pendiente exclusivamente integración/aceptación real: registro117/tiers/condiciones/menú, suelos/replantación y semillas ante veto, equipar/quitar/atributos sin acumulación ni curar, swap/proyectiles/armas/cancelación tardía, PvP/protecciones/MM/NPC/spawner, muerte única/drops/XP, reconexión/reload/transferencia, saturación y Spark con carga objetivo. MySQL/producción no tocados. Visuales genéricos existentes no prueban fidelidad de animaciones nuevas.

## Artefactos vigentes (SHA256)

- EcoEnchants-2026.27-native-consolidated-all.jar `e8ca5859f41e79056a2c29e686f26cd09d7dbdd770c1cad60e42d11e3377ab6b`
- LibreForge-2026.27-native-consolidated-all.jar `cc3a4c063e8f286aba82cf7469756d079c18b3ac245bd50e912840516bb25426`
- eco-2026.28-native-utility-all.jar `5ca36aa859cef009156cef5e821ad7907e62f11006c8cde7382130799542544f`
- EcoSkills-2026.27-all-requirement-views.jar `0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca`

Personal: `/root/development/artifacts/eco-suite/native-expansion-20261020/`; HomeSV: `/home/mtyn/staging/survival-custom/ecosuite-native-expansion-20261020/final/`. Entrega con manifiesto/hashes, no instalación. Test conserva requirement-views inicialmente contrastado; hashes rechecados sin cambios. Producción EE conserva64213413… y tampoco se reemplazó. Compilación siempre personal/ejecutor2CPU4GiB/toolchains upstream, sin clean ni upstream refresh.

Incidencia nueva y separada: desaparición de inventario al castigo por desconexión, incluidos Soulbound. Investigación en `PVPMANAGER-ITEM-LOSS-20261020.md`; no es corrección implementada ni incluida en estos JAR. No habilitar reservados para investigarla.
