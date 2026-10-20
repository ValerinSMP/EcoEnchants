# Incremento8: diez originales con adaptadores ejecutables —2026-10-20

Candidatos completos, NO instalados ni activos. Fuente personal; sin editar vEnchanting/vPets, skills YAML ni SDK/recibos/puente XP. Se amplía el mismo OriginalProcEngine y OriginalRuntime: no otro monitor de daño ni counters sin efecto. Registro native_burst, confirmed_native_burst, has_hunters_trail mediante OriginalProgression.register existente. YAML y hashes por ID en delivery/native-expansion-20261020/original-bursts/manifest.json; publicación de compras/anuncios corresponde al dueño.

| ID | Encantador | Tiers | Valores % | Efecto |
|---|---:|---|---|---|
|brasa_errante|21|1/2/3|4/6/8|Muerte melee confirmada: otro monstruo a3 bloques,12s.|
|estela_cazador|28|1/2/3|5/7/9|Muerte a distancia: velocidad3s,20s, atributo transitorio nativo.|
|pulso_abisal|28|1/2/3|4/6/8|Impacto de tridente con portador en agua en el impacto: hasta2 monstruos a2 bloques,12s.|
|rebote_prismatico|30|1/2/3|8/10/12|Flecha cargada: otro monstruo a3 bloques,15s; sin crear flecha recogible.|
|cicatriz_ignea|32|1/2/3|3/4/5|3 golpes/5s mismo arma/objetivo:3 pulsos de daño a20/40/60ticks,12s.|
|ruptura_sonica|33|1/2/3|6/8/10|4 golpes/6s: hasta3 monstruos a3bloques con línea de visión,15s.|
|tormenta_encadenada|35|1/2/3|6/8/10|5 golpes/7s: hasta3 saltos distintos con línea de visión y3bloques por salto,18s.|
|cometa|35|1/2/3|10/12/15|3 flechas cargadas/8s: un daño adicional al mismo objetivo,20s.|
|constelacion|35|1/2/3|8/10/12|3 impactos de tridente/8s: hasta2 monstruos a3bloques,25s.|
|legado_valerin|35|1|15|8 golpes/10s: un daño adicional al mismo objetivo,30s.|

Niveles exclusivamente lista130: no usar los históricos del plan ni sustituir Toque InfernalXII o los nueve movimientos. Targets/conflictos del plan preservados. Valores y lore de una frase en YAML; porcentaje positivo finito configurable mediante args.percent, sin cap global nuevo. Ventanas/cooldowns/targets máximos del mecanismo aprobado se mantienen en el core; defaults coinciden con plan. No referencia Hypixel inventada para originales. Ninguno cobra maná.

## Fuente y ejecución

Se conserva el ticket de daño real, salud antes/después y último daño; rechazo/cancelación/ausencia no conceden activaciones. Un evento confirmado puede alimentar una vez cada familia independiente, no quedarse con solo el primer enchant. Evento/familia deduplicado, contadores por jugador+arma+objetivo+mundo, cooldown por jugador/familia; cambiar arma/objetivo o retirar sesión no borra un cooldown vivo. Llegada/reload espera el cooldown completo de la familia, hasta30s. Tiers fuera de rango, incluido Legado>1, no cuentan. Cap4096 por mapa y cola de confirmación existente512 default/1024 máximo; no SQL ni polling.

Arcos/ballestas capturan metadatos/UUID nativo original_item_id y nivel al disparar, fuerza nativa >=.99 para flecha cargada; tridente usa ProjectileLaunchEvent y su ItemStack real. ShotLedger nativo4096/TTL30s/8objetivos por proyectil, una reclamación proyectil/objetivo para todas las familias. Swaps posteriores no atribuyen un arco distinto. Estela conserva botas del disparo y comprueba que siguen equipadas al activarse; sesión/mundo cercan los callbacks. Primera asignación de identidad refresca el provider solo de ese jugador, no invalidación global por golpe.

Daño secundario usa Bukkit damage/dealDamage con protección, procedencia PASSIVE y RepeatedDamage existente; no cambio directo de salud, daño sintético ni XP inventada. RepeatedDamage ahora es público también en metadatos Kotlin para reutilizar esa misma frontera, sin alterar sus firmas ni el cálculo nativo. Bloquea relanzamiento y aritmética ya calculada; callbacks de receptor y muerte/drops/XP permanecen. El viejo veto global cancelaba hasta kill/entity_xp_drop: se acotó a ataques ofensivos, con regresión para ambas rutas. No se vuelve a conceder XP manualmente.

El porcentaje toma el daño primario confirmado (limitado por pérdida real de salud, también sobreletal), y lo ofrece como daño bruto de un nuevo golpe nativo. Armadura/protecciones normales del golpe secundario pueden reducirlo: no prometer porcentaje final exacto ni comparar vida absoluta de Hypixel. Cometa/Legado y cada pulso ganado de Cicatriz abren la ventana nativa de daño extra, ejecutan una vez y restauran noDamageTicks en finally (también ante cancelación/excepción); no borran protección indefinidamente. Los otros objetivos respetan sus i-frames habituales.

Selección limitada desde el productor: BoundedLivingQueries,32 candidatos default/64máximo,8 consultas/tick default/32máximo compartidas para estos procs y Eco resonante. Ya no se obtiene toda la lista de criaturas para luego hacer take en Eco resonante. Ordena solamente muestra acotada; no promete enemigo más cercano global cuando está truncada. Saltos de Tormenta revalidan cada tramo y nunca repiten objetivo; presupuesto puede producir menos de tres (máximo, no garantía). Exclusiones mismas de OriginalRuntime: natural/comando/huevo permitidos, spawner/MM/jefes/jugadores/NPC/mascotas/aliados/protegidos/tags/PDC/nombre/invulnerabilidad fuera. Fallo Mythic/protección no abre bypass.

Cicatriz: máximo512 series/1024 configurable, una tarea pendiente por serie y3pulsos; comprueba sesión/mundo/objetivo/protección en cada ejecución, limpieza por dueño/objetivo. Estela: máximo512 buffs, expiración60ticks con fence de identidad y2emisiones visuales adicionales separadas10ticks; condición nativa y movement_speed_multiplier transitorio retiran atributo, sin curación/base stats. Cicatriz3partículas por pulso, Estela6por emisión (3); otros efectos reutilizan el visual acotado/configurable de OriginalRuntime. No entidades/explosiones/rayos destructivos ni pack nuevo. El visual actual es el anillo/chispa nativo compartido, no una animación distinta de trayectoria para cada nombre; apariencia final pendiente de aceptación, no se presenta fidelidad visual1:1 al plan.

## Verificación

- eco78/LF166/EE104 JVM/mocks,348 sin fallos; ocho pruebas nuevas (matriz todas las familias/tiers/ventanas/CD/llegada, coexistencia, fuente/carga/agua/muerte, scopes, callbacks XP/receptor e i-frames). Regresión previa de scopes conservada con fixture explícito de trigger ofensivo; no eliminación del test.
- Final EE build + shadow de fuente pasa; composición selectiva96clases/3748referencias propias pasa, con todos los demás ZIP entries conservados y SDK unchanged. Smoke JVM real de los JAR compuestos: shaded Kotlin/procedencia/fracciones de maná/APIs EcoSkills/doce familias/arribo pasa. No Paper iniciado.
- No MySQL ni juego, sin afirmación de mejora TPS medida. Pendientes en Test: orden real de registro, flechas/tridentes/fuerza/metadata, impactos cancelados/sobreletales/protección/line-of-sight, daños/buffs/retirada, reconstrucción de holders, ferocidad/curación/reflejos/XP/telequinesis, reload/reconexión/transferencia y carga60–100 jugadores/30–40activos. Los otros ocho originales y39familias existentes siguen pendientes de implementación/auditoría, no balance completo.

## Artefactos coordinados

- `LibreForge-2026.27-native-bursts-all.jar` SHA256 `6616d2de528d906f7ce13a8709dfe42892afb86b9f893bd67e6f6543329204e2`.
- `EcoEnchants-2026.27-native-bursts-all.jar` SHA256 `b98f62cf5266680c37d3fa493954d2e2711145f87fdfc59a415d061117f001f0`.
- eco mantiene utility `5ca36aa859cef009156cef5e821ad7907e62f11006c8cde7382130799542544f`; EcoSkills mantiene requirement-views `0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca`. No usar combinaciones de JAR elegidas por nombre sin contrastar estos hashes.
