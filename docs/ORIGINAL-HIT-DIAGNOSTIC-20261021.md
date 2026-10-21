# Eco resonante: observación interna limitada

## Estado real

Investigación sobre18afd5dc y runtime Dev SHA256
5c56242290c5683c9baf2853908fa90accde19f1418fa117f007130819629ca5.
La captura externa01:41:14–01:41:21 demuestra seis impactos sin cancelar,
WOODEN_SWORD/Eco resonanteIII, daño final positivo y pérdida real de vida.
No demuestra qué vio la confirmación interna ni si se ejecutó resonant_pulse.
CombatTargetPolicy no rechaza al atacante por creativo. NoAI/NoGravity no
justifican por sí solos el fallo. No se diagnostica una causa por inferencia.

**Causa raíz todavía no demostrada. No hay corrección funcional implementada.**
Esta entrega es el diagnóstico temporal autorizado por el prompt, necesario
para encontrar el primer tramo que rechaza el impacto sin saltarse sus controles.
No se afirma haber reproducido el recorrido completo con holders/condiciones/
expresiones; esa regresión del fallo sigue pendiente de localizarlo.

## Uso para el chat de vEnchanting

Candidato exclusivo EcoEnchants, no instalado ni activo. No precisa nuevos JAR
de eco/LibreForge/EcoSkills ni cambios en vEnchanting/vPets. Conserva YAML y API
pública existentes byte por byte frente al JAR Dev fijado.

Después de integrar y activar el candidato únicamente desde el chat responsable:

```
ecoenchants diagnosticoriginal Mtynnn
```

Reutiliza ecoenchants.command.reload; solo jugador conectado, sin cargar perfiles,
permisos nuevos ni escritura de XP. Observación de un jugador a la vez; máximo
30 segundos/8 identidades de impacto/24 líneas por impacto. Reiniciar la captura
sustituye la anterior. Detener antes con:

```
ecoenchants diagnosticoriginal stop
```

Sin polling: un único cleanup a600ticks, además de caducidad monotónica comprobada
al observar. Reset/reload/disable cancela ese cleanup y borra la captura. Los
fallos de texto/log apagan el diagnóstico sin cambiar la resolución del golpe.
No hay logs por todos los jugadores ni se registran inventarios completos.

Etapas: observe (duplicado/procedencia), weapon, eligibility, source,
confirm (before/after/cancelación/lastDamageCause), confirmed_source,
dispatch (triggerEnabled/holder), native_dispatch (cancelación del dispatch),
effect_entry (hits/window/cooldown), execute (amount/proof), arrival,
accepted_hit, activation, area_candidates, visual_entry y visual_frame.

Lectura: sin source, revisar captura/elegibilidad; confirm con0, revisar la
prueba interna; dispatch sin native_dispatch, revisar filtrado nativo/condiciones/
habilitación/deduplicación; native_dispatch sin effect_entry, revisar cancelación
posterior/pipeline; effect_entry sin execute, parámetros; amount inesperado,
expresión/placeholder; llegada no lista, conservar cooldown de sesión; sin
accepted_hit después de arrival lista, contador/cooldown; sin targets, selección
real; visual_entry/prepared=false, perfil; visual_frame/sent>0 demuestra llamada
al emisor, nunca aceptación visual por el cliente. No confundir ausencia de una
etapa con diagnóstico definitivo de una condición concreta.

Solicitar en Dev tres impactos válidos dentro de5s sobre el mismo objetivo/arma,
tras8s desde llegada; objetivo vecino dentro de2bloques y protecciones intactas.
Conservar captura externa y nueva interna del mismo intento; no repetir el mismo
ensayo sin la nueva observación. No usar comandos para falsear daño confirmado.

## Validación y límites

Build únicamente personal con ejecutor limitado, JDK25 y target21 originales,
Maven aislado existente, Gradle offline/core-plugin:test+shadowJar. Tests JVM de
límites/expiración/aislamiento/fallos del diagnóstico y suite existente; no son
una regresión del fallo original ni un arranque Paper.
Composición sobre runtime fijado, solo cinco raíces de clases revisadas, incluidas
clases anidadas; API, enchants, recursos y todo lo demás idénticos. Ver manifiesto
y chequeo ABI/linkage. No instalar el shadowJar completo del checkout, que puede
incluir otros cambios ajenos a este incremento.

No instalación, lifecycle, modificaciones en otros plugins, precios, XP, compras,
protecciones, cooldowns o condiciones. Pendiente: integración del candidato por
chat vEnchanting, captura interna real, causa demostrada, corrección mínima y
regresión del recorrido completo, luego aceptación visual en Dev.
