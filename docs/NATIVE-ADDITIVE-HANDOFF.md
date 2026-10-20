# Ruta aditiva y muerte/XP — incremento 4, 2026-10-20

Candidato acumulativo, SIN instalar/activar. Requiere los TRES JAR de este incremento juntos; EcoSkills conserva requirement-lore. No usar el EcoEnchants aditivo con LibreForge de mining/five solamente. Se conservan los cinco encantamientos nuevos, requisitos/lore, procs originales (/summon/huevos) y APIs de compra/XP. No incorpora fichas, anuncios ni precios: el chat dueño define Test1shard O1gema cuando valide. Las130familias y todas las comparaciones todavía NO están terminadas.

## Contrato de la ruta nativa

```yaml
- id: damage_multiplier
  args:
    multiplier: '1 + 0.05 * %level%'
    damage_group: ecoenchants_normal
  triggers: [melee_attack]
```

No basta una etiqueta arbitraria: EcoEnchants registra AdditiveDamageGroups.bind(group, owner, Predicate<TriggerData>) y valida holder.holder como EcoEnchantLevel, tipo normal, jugador actual/vivo/online, víctima/evento y autor del ataque/proyectil. Procedencias secundarias/abilities y contexto de comandos quedan fuera. La atribución/selección de holders original de cada trigger no se sustituye; el snapshot del arco de Sello glacial sigue su ruta existente. Falta de guard nativo de comandos se registra con causa completa y deja estos bonos fuera; comprobar arranque Test antes de habilitar compras.

API de extensión de LibreForge: AdditiveDamageGroups.bind(String,Plugin,Predicate<TriggerData>), unbind(String,Plugin), reseatBoundary(), offer(String,TriggerData,double). vEnchanting no necesita invocarla: consume las definiciones nativas por las APIs ya existentes. No se modifica SDK ni recibos/outbox/awardId/progreso/cap/puente final de XP de vPets.

LOWEST abre scope solo para eventos reales del jugador/proyectil; MONITOR, recolocado después de registros reales de Effect/Trigger de LibreForge, lo retira y aplica UNA operación sobre event.damage. Ofertas fuera del scope o después de cerrarlo fallan, no abren otro collector. Evento cancelado: retirada sin cambiar daño ni cancelación. Duplicados mismo ProvidedHolder/grupo en varios triggers aportan el máximo de sus ramas, una vez; holders distintos se suman. Si un enchant futuro necesita dos términos legítimos en un mismo golpe, entregarlos como una expresión conjunta; no duplicar bloques esperando suma interna. No cambian conflictos, normal.limit ni anvil.enchant-limit.

Sin damage_group, damage_multiplier primario conserva la multiplicación legacy para skills/items/pets/abilities y demás consumidores. Se conserva también la rama MythicDamageEvent legacy; el opt-in de esta entrega corresponde a EntityDamageEvent, no a una promesa de compatibilidad con cualquier evento Mythic inventado por un tercero.

## Fórmula y comparación

Sea B el raw que llega a nuestra frontera nativa, tras fuerza/crítico/flats/defensas y otros consumidores ya procesados. Cada contribución normal es alpha=multiplier-1. Raw= B*(1+sum(alpha)). +50% y+40% sobre B20 producen38, no42. No se toca vida ni se emite un segundo evento para el golpe principal; Paper conserva su único cálculo de defensa. Extras de otros plugins que corran después son una transformación H; daño real=F(H(B*(1+sum(alpha)))).

Para comparar hay que mantener B, tier, crítico, armadura/toughness/absorción y H iguales: Dcon/Dsin=F(H(B*(1+sum(alpha))))/F(H(B)). No asumir siempre1+sum(alpha): la armadura vanilla puede ser no lineal con el raw. Tampoco comparar corazones absolutos contra el modelo de defensa de Hypixel. No hay un cap global de daño.

Candidatos YAML completos con hashes origen/candidato en delivery/native-expansion-20261020/additive-hp:

| ID / Encantador / tiers | alpha local | Diferencia |
|---|---|---|
| giant_killer / XV / I–V | min(max(Hv/Hp-1,0)*0.1*L,0.05*L) | Usa vidas máximas relativas, no salud faltante; topes5/10/15/20/25%. Conflicto titan_killer intacto. |
| goliath / XXV / I–V | min(max(Hv/Hp-1,0)*0.02*L,0.04*L) | Tope propio4/8/12/16/20%; factorV aislado máximo1.2 en vez4.6. No es cap global ni equivalente directo de Hypixel. |
| cubism / III / I–V | 0.05*L, filtros existentes | Mismos valores/targets/conflictos; opt-in solamente. |

GK+GoliathV: máximo45% sumado, raw1.45B; no4.6*1.25. No se retiran/rebajan niveles de objetos ni se reembolsan ítems. Estos tres archivos no habilitan por sí solos los demás pendientes ni sustituyen YAML de skills/movimientos aprobados.

Referencias comunitarias verificadas20/10/2026, distintas de certificación del servidor Hypixel:
- https://hypixelskyblock.minecraft.wiki/w/Giant_Killer : mesaI–V; VI/VII especiales. I–IV 0.1/0.2/0.3/0.4% por1% de vida adicional, topes5/10/15/20; V0.6%/30. NuestroV0.5%/25 es deliberadamente menor en esa contribución.
- https://hypixelskyblock.minecraft.wiki/w/First_Strike : mesaI–IV25/50/75/100%; V125% especial. LocalV existente no debe describirse como tierV normal de Hypixel.
- https://hypixelskyblock.minecraft.wiki/w/Triple-Strike : mesaI–IV10/20/30/40%, V50% especial; solo primeros3golpes. El contador local sigue pendiente: no habilitar su multiplicador plano como sustituto.
- https://hypixelskyblock.minecraft.wiki/w/Swarm : ultimate de dungeon, sin tier normal de mesa;2/4/6/8/10% por enemigo, máximo10. Localnormal actual suma plano y necesita adaptador/rebalance, no copiar el ultimate como normal.
La wiki oficial redirige al cierre de julio2026; no se presenta una página oficial vieja como estado actual. Cubism y resto aún requieren ficha/comparativa actual propia.

## Ferocidad, defensas y callbacks

Consumer real inspeccionado: EcoSkills/stats/ferocity.yml usa damage_twice. El scheduler nativo EcoScheduler.run llama Bukkit.runTask, por lo que el replay lee event.damage después de finalizar el original. RepeatedDamage lleva un Frame privado y provenance PASSIVE por el mecanismo existente; identifica exactamente el primer evento repetido del mismo autor/objetivo. No hay cadena recursiva ni metadata cuya retirada dependa de acertar otra chance. Cancelado, víctima muerta/retirada/otro mundo o emisor Player no válido descartan el callback.

Solo en ese replay se omite dispatch ofensivo del emisor y la aritmética ya incluida en el raw: damage_multiplier, total_damage_multiplier, pvp_damage_multiplier, crit_multiplier, add_damage y make_skill_crit. Receptor sigue recibiendo callbacks/protecciones/cancelaciones; no se reinterpreta isActive/isAbilityOnly (siguenfalse en el puente legacy). Armadura vanilla se procesa normalmente en cada golpe, sobre el raw ya calculado; no se aplica otra vez fuerza/crítico/bonos/defensa porcentual de LibreForge incluidos en él. Curación/cancelación/otros efectos del receptor no se suprimen por defecto; su comportamiento real puede cambiar el resultado del segundo golpe. Muerte/kill/drop tienen evento distinto y siguen su ruta, con XP/callback final una vez por muerte real, no un nuevo award sintético.

Reflejos/procs siguen sus mecanismos anteriores; no se agregan al grupo como ataques primarios ni se copian a raw principal. Último recurso continúa HIGHEST antes de esta frontera, con factor1-p; escalares anteriores combinan como1-(1-r)*(1-p), planos posteriores no reciben ese factor retroactivamente. No duplica defensa ni resucita.

## Reparación de muerte nativa

EntityDeathByEntityListeners.HIGH preparaba solo golpes ya letales, antes del daño MONITOR de LibreForge. Nueva frontera EntityDeathEvent.MONITOR usa la causa final aceptada para recuperar muerte real que faltaba; conserva fallback provisional5ticks de WildStacker, consume ambas rutas y publica un solo EntityDeathByEntityEvent. Guarda el daño provisional para descartar cancelado; muerte cancelada consume sin publicar. No nuevo candidato por cada golpe ni nueva tarea/polling. Se mantienen constructor/payload/drop/XP y callbacks de TriggerKill, TriggerEntityXpDrop, TriggerEntityItemDrop y Telekinesis; no se cambia fórmula/curva/writer de progreso. Cancelaciones posteriores hechas indebidamente en MONITOR no están garantizadas; validar orden real de terceros.

## Límites, monitor y pruebas

Máximos operativos:8políticas,1024eventos anidados,1024contribuciones por evento (supera130familias en6slots); saturación falla cerrada. Son límites de trabajo, no cap de daño ni un tope comercial de enchants. No polling por tick, SQL, perfiles o invalidación de holders por tooltip en esta ruta. Finalización instrumentada con el monitor existente: libreforge:damage | groups:normal:finalize, solo cuando hubo contribuciones; de otro modo la comparación CPU del onTrigger ocultaría el coste trasladado a la frontera.

Ejecutado en personal/ejecutor limitado: eco74, LibreForge164, EcoEnchants86 tests JVM/mocks,324totales sin fallos. Incluyen scope/cancelación/base común, duplicados,780contribuciones sin cap global, identidad de replay/restauración por excepción/receptor conservado y muerte real tras boost/una sola emisión/cancelación/prioridad. Cierre estático del compuesto42clases/1974referencias propias. Smoke JVM del JAR compuesto verifica Kotlin sombreado, llamada de procedencia/callback, restauración y calculador; no arranca Paper. APIs/entradas ajenas preservadas por injerto/composición contra hashes pinned. Se comprobó que el helper remapea también clases LF nuevas, no se distribuye common shadowJar entero.

Pendiente REAL Test: orden nativo/extensiones externas, daño normal/crítico/fuerza/flats/absorción/armadura, equipar/quitar y metadatos, varios enchants/conflictos, ferocidad y reflejos/procs sin repetición, golpe letal/vidaextra/no curación, supervivencia/cancelación, WildStacker/MythicMobs/telekinesis, skillXP/levelup/recompensas/recibos/vPets una vez, reconexión/mundo/transferencias/reload/disable y perfiles fríos. MySQL y gameplay NO ejecutados; no se mide ni promete mejora de MSPT/TPS. Escenario de carga objetivo60–100 conectados,30–40activos, sin resultado medido aún.

## Artefactos coordinados

- eco-2026.28-native-additive-all.jar — SHA256 `7c3f30330924719e89ff247f0d59c4063361f51f41d8830e9010aaafe8f2892e`.
- LibreForge-2026.27-native-additive-all.jar — SHA256 `ef6931f7bcc24df2a7f072e039a5f497f1c8e1d4c9999f4a4f2251f4ce7fb6bb`.
- EcoEnchants-2026.27-native-additive-all.jar — SHA256 `78c8612c4cb1538ce9d8a909a12620a9fd98ac5d33380b5047cc8df3e0718501`.

EcoSkills permanece SHA256 `0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca`. Manifest adjunto enumera baselines y entradas exactas. NO instalado, NO activo, NO verificado en juego. No instalar ni lifecycle desde EcoSuite; integración posterior en el chat dueño.
