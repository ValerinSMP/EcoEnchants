# Resolución de política del enchant registrado

## Causa demostrada

La captura interna Dev Custom02:12:33–02:12:35 reconoce espada/Eco resonanteIII,
pero eligibility=false impide capturar la fuente pese a confirmar daño positivo.
La categoría registra EcoEnchantsCraftEnchantment, que implementa EcoEnchant por
delegación y no hereda EcoEnchantBase. El instanceof/cast del helper rechazaba
ese objeto antes de evaluar la política. Además, los holders comparaban el objeto
registrado con su base interna por identidad, por lo que también fallaba su
segundo control al llegar a LibreForge. Fuente y bytecode instalado contrastados.

## Corrección mínima sin API nueva

OriginalRuntime.eligibleFor obtiene el objeto registrado como EcoEnchant y usa
getLevel(1).allowsCombatTarget. La API upstream ya ofrece ese nivel; la delegación
conserva el holder y su base propietaria. EcoEnchantBase mantiene su caché nativa
de niveles y su política ya compilada. No reflexión ni parseo YAML por golpe.

EcoEnchantLevel comprueba que el nivel actual del objeto registrado sea este
mismo holder (`registered.getLevel(level) === this`), en lugar de comparar wrapper
y base. Aplica a objetivos vivos y efectos post mortem. Reemplazo/reload/eliminación
de la categoría deja rechazados los holders anteriores. Sin registro paralelo ni
caché adicional que invalidar. El cast de la base interna del holder se conserva:
es el propietario que crea el nivel, no el objeto envoltorio registrado.

Consumidores revisados: OriginalRuntime.modeAllows/eligible/eligibleFor, captura
original, confirmación y selección de área; NativeCombat/NativeBurst/NativeReactive,
NativeSwarm y los efectos de LibreForge que usan EcoEnchantLevel como
CombatTargetHolder. Todos reutilizan los dos puntos corregidos; no se cambia
CombatTargetPolicy ni los filtros mecánicos específicos.

Sin firmas nuevas o eliminadas en EcoEnchant/EcoEnchantLevel/API de compra/XP.
Sin cambios en YAML, requisitos, cooldowns, balance, precios, XP, vPets o vEnchanting.
Diagnóstico de211d27c6 conservado byte por byte fuera de OriginalRuntime: manual,
30s/8impactos, sin polling/logs continuos, stop y cleanup nativos existentes.

## Regresiones y alcance de la prueba

DelegatedCombatPolicyTest registra un EcoEnchant real por delegación de Kotlin
que NO hereda EcoEnchantBase. Usa los holders EcoEnchantLevel reales y la política
real para aceptación/rechazo, NPC/invulnerabilidad, plugin deshabilitado,
reemplazo/eliminación y rechazo de holders antiguos, incluidos post mortem.

DelegatedOriginalFlowTest utiliza observe→confirm→ConfirmedOriginalHit.dispatch
con forceHolders del objeto delegado, ConditionList, pipeline ElementLike,
OriginalProcEffect y OriginalProcEngine reales. Confirma tres impactos y una
activación; requisito inferior a16 impide ejecutar/contar. Comprueba el placeholder
%level%=3 que llega a la expresión original2+2*%level%.

Límites explícitos: Bukkit/registradores de Minecraft, autoridad de nivel de skill
y servicio matemático NumberUtils.evaluateExpression están simulados. El servicio de expresión verifica texto
YAML y placeholder recibido y devuelve8; no valida el compilador matemático real.
El hash nativo de ItemStack está simulado; el placeholder ajeno victim_max_health
se retira temporalmente solo en el fixture porque requiere registros Paper, y se
restaura al terminar. El placeholder de nivel del holder sí se genera realmente.
El EffectBlock externo se simula para enrutar al ChainElement real; no se ejecuta
la carga completa de categorías ni todos sus argumentos de bloque. Se comprueban
confirmación/dispatch/condición/placeholder/contador; no daño de área real, partículas,
sonido, TPS ni aceptación en cliente. Es regresión JVM, no prueba de servidor.

## Ejecución JVM en el entorno canónico

La dependencia de tests del Maven aislado conserva clases compiladas de LF pero
omite11clases de diagnostics usadas por ElementLike. No se modificó el Maven
compartido ni se recompiló LF: test-fixture.json registra las clases ausentes que
se copiaron de sus clases de build existentes a missing-test-diagnostics.jar.
Solo se agrega al classpath de tests con test-fixture.init.gradle; nunca al JAR
de entrega. Ambos y la lista exacta quedan en staging de esta entrega.

Ejecutar con el ejecutor personal, el Maven aislado y el mismo comando Gradle
core-plugin:test/shadowJar, añadiendo:
`--init-script /root/development/staging/ecosuite-policy-wrapper-fix-20261021/test-fixture.init.gradle`.
No instalar missing-test-diagnostics.jar ni el shadowJar completo del checkout.

## Entrega y aceptación pendiente

Candidato compuesto únicamente sobre211d27c6/SHA193174602f9f66ec361a591ac9a2e140380cc653f0bbe2e016259979fb4e7a52.
Solo raíces OriginalRuntime y EcoEnchantLevel; resto del runtime, SDK, YAML y
recursos idénticos. Composición reproducible con el modo policy-fix del script
existente; ABI y cierre de referencias del JAR final verificados por separado.

No instalado ni lifecycle en ningún destino. Chat vEnchanting recibe JAR/hash y
coordina integración Dev: misma espadaIII, objetivo y vecino a<=2bloques, tres
golpes válidos en5s tras8s de llegada. Con diagnóstico confirmar eligibility=true,
source captured=true, accepted_hit1/2/3 y activation. Comprobar daño real al vecino
y partículas/sonido, después probar requisito insuficiente, objetivo protegido,
cooldown y sustitución/reload. Preservar PvP y aliados/mascotas/NPC. No autorizar
producción por pasar JVM o por tener el JAR instalado.

## Resultado verificado de esta entrega

159tests JVM,0fallos/errores/omitidas. ABI existente y cierre de referencias del
JAR compuesto pasan; solo cambian dos entradas .class. SHA256 runtime:
0b17315c911f87d5695a7c754f258dd11096f3ea7cb56f163666ec998c84d677.
Ambos Dev conservan193174602f9f66ec361a591ac9a2e140380cc653f0bbe2e016259979fb4e7a52.
Sin instalación ni lifecycle. Fuera de JVM, todo lo listado en aceptación sigue
pendiente. No se midieron mejoras de rendimiento.
