# Eco resonante y Sello glacial — contrato runtime, 2026-10-20

## Registro e integración
EcoEnchants registra el trigger `confirmed_original_hit` y los efectos nativos
`resonant_pulse` y `glacial_seal` en handleLoad. El listener se registra una sola
vez en handleEnable; reload limpia estado y disable cancela tareas y atributos.
Los YAML activos son `enchants/eco_resonante.yml` y `enchants/sello_glacial.yml`.
No necesita otro plugin de efectos. MythicMobs es softdepend, compileOnly; no se empaqueta.
Las APIs de mejora/compra y la concesión durable de XP permanecen intactas.

| Familia | Arma | Encantador | Tiers | Efecto | Ventana / cooldown |
|---|---|---|---|---|---|
| eco_resonante | espada | XVI | I–III | 4/6/8% del daño confirmado a otros dos monstruos, radio 2 | 3 golpes en 5 s / 8 s |
| sello_glacial | arco | XVIII | I–III | −10/15/20% velocidad de movimiento, 40 ticks | 2 impactos en 4 s / 10 s |

Los conflictos `cleave` y `frost` pasan por la autoridad nativa simétrica de
EcoEnchant. Requisitos, filtros y holders se evalúan mediante LibreForge; no se
reimplementa la compra. Los argumentos hits/window/proc_cooldown y los porcentajes
se validan contra este balance aprobado: editar esos números no crea otro balance.

## Confirmación, identidad y exclusiones
Todo acceso Bukkit sucede en MAIN. Se captura el daño real en MONITOR y se
confirma al tick siguiente comparando cancelación, finalDamage, pérdida real de
vida y la identidad de lastDamageCause. La muerte real también permite confirmar
el golpe letal. Un golpe ambiguo, reemplazado o curado completamente antes de la
confirmación se ignora; el daño usado no supera la vida efectivamente perdida.
No se crean eventos de golpe/muerte/XP para simular confirmación.

La identidad UUID del arma usa únicamente PDC propio `ecoenchants:original_item_id`.
Se conserva una copia del arco y del tier al disparar, con jugador/mundo/sesión;
cambiar de arma después no cambia el origen. Cada proyectil/objetivo cuenta una
vez, hasta ocho objetivos por proyectil. Flechas no registradas no cuentan.

Política conservadora: únicamente Enemy vanilla de aparición NATURAL, COMMAND (/summon),
SPAWNER_EGG o DISPENSE_EGG (huevos, también dispensados). Quedan
excluidos jugadores, aliados de scoreboard personal o principal, NPC, domesticados,
Boss/Wither/Dragon/Warden/ElderGuardian, TODOS los mobs MythicMobs, entidades con
PDC/tags/nombre personalizado, invulnerables y objetivos no autorizados por
AntigriefManager. Spawner/trial spawner, CUSTOM de plugins y orígenes desconocidos siguen excluidos.
/summon y huevos sí sirven como prueba positiva si no existe otra exclusión.
Si Mythic está presente y deshabilitado o su API falla, se bloquean estos efectos
y se registra la causa completa una vez hasta reload. No existe un modo inseguro
que permita adivinar cuáles de sus mobs son jefes.

El pulso conserva el antigrief nativo y la procedencia PASSIVE; su ámbito síncrono
suprime despachos secundarios de LibreForge para evitar cadenas recursivas.
Solo toma dos objetivos visibles, próximos y elegibles; no atraviesa paredes.
El sello usa un modificador TRANSITORIO MULTIPLY_SCALAR_1 propio, reemplazando
el sello anterior sin sumarse. No modifica bases ni vida ni atributos ajenos.
Se retira por timeout, muerte, salida/cambio de mundo, reload y disable.

## Sesiones y límites
Contadores volátiles por jugador/arma/objetivo/mundo; cooldown por jugador/familia.
Salida o cambio de mundo elimina pruebas/contadores; llegada/reconexión/transferencia,
respawn y reload requieren 8 s (Eco) o 10 s (Sello) antes de empezar a contar,
evitando saltarse el cooldown cambiando de nodo sin crear persistencia nueva.
No se promete conservar progreso parcial entre sesiones ni servidores.

Sin tarea por tick ni escaneo global: una confirmación por golpe admitido y una
retirada por sello. Capacidades: 4096 contadores/cooldowns/proyectiles, 1024 sesiones,
512 confirmaciones y sellos por defecto (máximo configurable 1024), proyectiles
30 s (máximo 60), ocho impactos distintos por flecha. Saturación falla cerrado.
Búsqueda local de radio 2: se inspeccionan hasta 32 candidatos (máximo 64) y se
aplican como máximo dos daños; la consulta espacial Bukkit también tiene coste.
Estas cotas no constituyen un benchmark de rendimiento con jugadores.

Partículas: hasta 16 por pulso / 12 por sello, emisión única, observadores próximos
24 bloques (máximo 32), hasta 16 observadores (máximo 32), 64 partículas/observador/s
y 256 globales/s por defecto; presupuesto independiente, agotarlo solo reduce FX.
Configuración opcional `original-procs` con defaults seguros, sin reemplazar el
config.yml existente. No explosiones, rayos, fuego, drops ni bucles cosméticos.

## Progresión y entrega
Toque Infernal requiere XII, aprobado en 5b6ea99. Se preservan nueve movimientos:
feather_step7, infernal_touch12, lifesteal15, adrenaline18, timber20, thunderlord20,
veinminer28, first_strike16, criticals14. Timber/thunderlord son definiciones locales
sin default upstream; no se recrean. Solo se entrega un parche ADITIVO de los dos
anuncios sobre el skills YAML actual. No aplicar el YAML completo del incremento
histórico: desharía movimientos aprobados.

El JAR se compone sobre el SHA del runtime Test contrastado, incorporando solo clases
progression, hooks lifecycle y recursos enumerados. Las clases API existentes y el
resto de entradas quedan idénticos; código ajeno no confirmado del checkout no se
incorpora al JAR. Ver runtime-manifest.json y SHA256SUMS de la entrega.

## Validación y límites de evidencia
Pruebas JVM: núcleo, ventanas, cooldown, duplicados, saturación/expiración, snapshots
inmutables de flecha, sobreletal, cancelación y daño ambiguo, guardas de sesión,
exclusiones y presupuestos. Mockito sobre APIs Bukkit reales: item/PDC, evento,
atributo transitorio y supresión/restitución del ámbito secundario. No equivalen
a ejecutar el servidor. El cierre de referencias nativas y la comparación binaria
de API son comprobaciones estáticas, no una prueba de gameplay.

Pendiente en Test, mediante activación separada: arranque y registro, requisitos
XVI/XVIII, tres golpes a monstruos y dos flechas con cambio de arma, muerte/daño
cancelado, PvP/jefes/Mythic/aliados, antigrief, retirada exacta sin acumulación,
logout/reconexión/cambio de nodo, reload/disable y compra con vEnchanting.
No instalación, lifecycle ni verificación en juego en este incremento. No SQL ni
cambios en EcoSkills, vPets o vEnchanting.

## Ajuste de elegibilidad sobre 29c25061
Se amplían solo los orígenes permitidos en ProcGuards; todas las exclusiones se
aplican igual, incluso a jefes/Mythic creados por comandos. Sin bypass de owner
ni excepciones en vEnchanting. YAML solo sustituye «monstruo natural» por
«monstruo», conservando valores y códigos de color.
Referencia API: https://jd.papermc.io/paper/26.2/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html
