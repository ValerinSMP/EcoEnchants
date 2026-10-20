# Incremento9: ocho originales reactivos ejecutables —2026-10-20

Candidatos sin instalar/activar. Rutas native_reactive y native_reactive_action registradas junto a native_burst/confirmed_native_burst previas. Sigue la misma cola de OriginalRuntime, sesiones y OriginalProcEngine; reservas pre-acción explícitas, no hits confirmados inventados. No edita vEnchanting/vPets ni su SDK/XP/recibos. No sustituir skills YAML/Toque InfernalXII/nueve movimientos.

| ID | Encantador | Tiers | % | Mecánica |
|---|---:|---|---|---|
|ancla|22|I–III|5/8/10|Empuje nativo de monstruo estando en suelo, CD15s; evento Paper real, no modificar base knockback.|
|marca_celeste|21|I–III|4/6/8|Flecha cargada confirmada marca3s; siguiente melee al mismo monstruo consume bonus aditivo, CD10s.|
|guardia_cristal|25|I–III|8/10/12|3 golpes físicos confirmados de monstruos en4s (pueden ser distintos); siguiente golpe en3s reduce daño, CD20s.|
|velo_lunar|32|I–III|5/7/9|Daño físico de monstruo por la noche y salud estrictamente<40%; reduce ese golpe, CD20s.|
|vigilia|33|I–III|5/7/9|Tras4s sin perder salud por daño, reduce siguiente golpe físico de monstruo, CD20s.|
|aguja_aurora|30|I–III|4/6/8|Ballesta real del disparo, fuerza>=.99 y distancia origen-disparo/objetivo>15bloques: bonus aditivo en el mismo impacto, CD12s.|
|paso_ceniza|35|I–III|5/7/9|Salida real de lava confirmada al tick siguiente: reducción de fuego durante3s, CD20s.|
|eclipse|35|I–III|6/8/10|6 golpes/8s mismo arma y monstruo: bonus aditivo en siguiente melee en3s con esa espada, CD25s; puede cambiar de monstruo.|

Todos: YAML completos, valores/lore de una frase configurables, targets/conflictos/tipo/tiers y hash por ID en delivery/native-expansion-20261020/original-reactive/manifest.json. Requisitos levels.json actuales, no históricos. No mana ni tarifa productiva nuevos; compras/anuncios las publica su chat (Test1shard O1gema), no este proveedor.

## Semántica y compatibilidad

- Guardia cuenta por receptor+pechera/mundo, no exige mismo atacante. Reusa record de daño real de la cola existente; muerte/cancelación/falta de pérdida observable no cargan contador. Las tres familias de bonus usan ecoenchants_normal sobre la misma base; no un segundo golpe ni otro crítico/defensa. Marca conserva arco del disparo aunque cambie a espada, solo su dueño/sesión y monstruo marcado; Eclipse exige misma identidad de espada y no se pierde por morir el monstruo que dio el sexto hit.
- Guardia/Velo/Vigilia son defensa sobre daño bruto nativo y se multiplican como reducciones existentes, sin cap global. Velo usa condición nativa is_night (12301–23849) y umbral estricto40%; Vigilia registra daño confirmado de cualquier causa, no ausencia convertida en cero. LastTaken se inicia por sesión, MAIN, map acotado; no SQL ni datos persistentes nuevos. Todos los golpes de combate nuevos siguen exclusiones nativas de monstruos (natural/comando/huevo sí; spawner/MM/jefes/PvP/NPC/aliados/mascotas/protegidos/tags/PDC/nombre/invulnerabilidad fuera). Sin excepción por dueño.
- Paso conserva efecto para todos los ticks de fuego de sus3s y verifica botas originales. Usa FIRE/FIRE_TICK/LAVA/HOT_FLOOR; si DamageSource conserva causante, se valida el mismo filtro. Fuego ambiental sin causante no inventa un atacante ni historial de ignición; no promete atribuir quien inició un fuego que vanilla no retiene. Salida confirmada exige movimiento no cancelado, mundo/sesión/equipamiento actuales y estar fuera de lava. No polling.
- Ancla modifica solo vector del evento físico nativo (DAMAGE/ENTITY_ATTACK/SWEEP_ATTACK), estando en suelo. No puede esperar a salud al siguiente tick para modificar un impulso que se está aplicando: su frontera es el evento de impulso real. Veto/impulso final cero devuelven reserva al siguiente tick; no se fabrica daño para él. Vectores cero/no finitos se rechazan.
- Arcos/ballestas usan ShotLedger y copia de UUID/metadatos al disparar; Aguja mide desde ubicación capturada al disparar, no la posición a la que se teletransportó después. Contextos de armas/armadura se restringen por ruta/targets; primera identidad se escribe en slot real y refresca provider solo de ese jugador. No invalidaciones globales continuas ni curación.

## Cancelación, incertidumbre y retirada

ArmedProcLedger acotado incluye tokens consumidos pendientes en su capacidad. Cancelación explícita o daño final finito<=0 restaura solamente la misma revisión no expirada, con deadline y payload originales; no amplía3s. Nueva revisión, retiro/muerte/mundo/sesión/reload impiden escritura atrasada aunque el slot quede vacío. Resultado positivo no observable/no finito es incierto: consume y nunca repite a ciegas. FirstHits adopta la misma clasificación para no devolver cuota ante NaN/infinito incierto.

Reservas de cooldown en el mismo engine tienen token por intento; rollback viejo no elimina cooldown posterior. No se borran cooldowns vivos al salir; llegada/reload espera CD de familia. Guardia/Marca/Eclipse/Paso tienen expiración con identidad; movimientos/impulsos diferidos usan identidad antes de retirar un slot. También se corrigió el orden de comprobación del ticket OriginalRuntime para no retirar otro candidato si llega un callback antiguo. Ninguna retirada altera salud, niveles, recompensas o XP final.

Cap default512/máximo1024 de estados/buff y movimientos/impulsos,1024 eventos con reservas y8 por evento; cola de confirmación existente512/1024, consultas limitadas del lote8 sin cambio. Candidatos de enchant/material y estado se revisan antes de invocar Mythic/protección; no se hace consulta de integración por cada golpe de un jugador que no tiene ese efecto. Sin tareas repetitivas de tooltip. Diseño para60–100online/30–40activos, no benchmark ni TPS medidos. Selección previa al dispatch requiere Spark para medir cobertura total: monitor LibreForge cubre efectos, no cada línea del listener.

## Evidencia y pendientes

- eco78/LF166/EE114 JVM/mocks,358sin fallos. Nuevas pruebas del ledger: consumo único, rechazo, deadline, reemplazo nuevo incluso consumido, incertidumbre, expiración, capacidad incluyendo pendientes y retirada/reconexión; leases sin hits falsos, expiración/rollback tardío y familias independientes; vectores inválidos y resultados no finitos. Matriz del engine ahora20familias/todos los tiers y regresiones anteriores conservadas.
- Cierre112clases/4787referencias propias y smoke JVM sobre JAR compuesto pasan (API EcoSkills/shaded Kotlin/procedencia/maná/20familias/arribo). SDK compra/XP byte idéntico. No Paper iniciado ni MySQL/juego.
- Test filesystem targets.yml incluye todos los targets utilizados (SHAf028d94f6f380a93785374c6b136899ddad1fb242a93259a612b32058e58198d). Lectura no prueba registro activo. Leaf26.2 actual usa CraftEventFactory/com.destroystokyo knockback; API real confirma que es subclase de EntityPushedByEntityAttackEvent que escuchamos (API SHAae110b396d8bde3589bcba899ff64884f41a66392b476ba981e42d8b7928d15b); Entity también referencia el evento nuevo. Evidencia estática, no ejecución de Ancla en juego.
- Pendiente Test: hooks/prioridades reales del finalizador/reload, registro de ocho YAML y efecto real por tier, PVP/jefes/NPC/spawner/huevos/summon, cancelación tardía, protección, carga y rango de ballesta, swap de armas/slots, bajar/subir40%,4s de quietud, salida/reentrada lava, knockback en suelo/aire y retirada/reconexión/transferencia. No afirmar fidelidad visual nueva: se reutilizan visuales existentes de los procs cuando hay emisión; apariencia de marcas/guardias/aurora pendiente de aceptación.
- Completados candidatos de los18 originales restantes y primeros5; faltan39 familias existentes y balance/global full matrix. Esto no autoriza su instalación ni publicación automática.

## Runtime coordinado

- `EcoEnchants-2026.27-native-reactive-all.jar` SHA256 `3a5fd41791b48ce2039efe9ff52cb575302bce517c3fe52fccda82b3576d3b34`.
- LibreForge native-bursts `6616d2de528d906f7ce13a8709dfe42892afb86b9f893bd67e6f6543329204e2`.
- eco native-utility `5ca36aa859cef009156cef5e821ad7907e62f11006c8cde7382130799542544f`.
- EcoSkills requirement-views sin cambio `0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca`.

Requiere arranque con JAR coordinados y los YAML para validar clases nuevas; este chat no instala ni ejecuta lifecycle. No intentar activar YAML nuevos con JAR anteriores ni declarar listo por un hash.
