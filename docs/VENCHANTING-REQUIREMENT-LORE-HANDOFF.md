# Handoff vEnchanting: actualización de requisitos en piezas — 2026-10-20

## Qué integra
Cuatro candidatos coordinados eco/LibreForge/EcoSkills/EcoEnchants. No cambiar
vEnchanting ni vPets, precios, requirements YAML, conflictos, SDK ni recibos/XP.
Este arreglo conserva los enchants y la elegibilidad de b0954d4e (incluye /summon
/huevos); Toque Infernal XII y los nueve movimientos aprobados siguen intactos.

El nivel nativo es autoritativo. La línea roja era susceptible de reutilización
del frame del objeto y del resultado off-thread del holder aunque cambiara el
nivel del observador. Ahora los requisitos literales consultan caché actual y
las publicaciones de nivel solicitan un refresco nativo de slots afectados.
No se hace permanente el lore por jugador ni se fuerza un level-up de premios.

## Disparadores y límites
- Progreso, set/reset/admin, carga normal y publicación durable: notificación
  posterior a la publicación de claves de nivel propias; misma clave/valor no repite.
- Join/cambio de mundo/reload: revisión de esa sesión; una carga demorada vuelve
  a solicitar refresco al publicar. Caché fría sigue desconocida/no autoriza.
- Cola: un batch por UUID, skills agrupadas, identidad retirada al desconectar,
  2048 pendientes máximos; no admite solicitudes de sesiones offline. No guarda niveles.
- MAIN: updateEffects existente una vez por batch, selección por ConditionBlock
  real del tier y skill, recorrido de la ventana actual hasta 256 slots y solo
  SetSlot afectados. Cursor solo si contiene una pieza afectada; envío de copia,
  sin cambiar el cursor ni inventario real. Sin updateInventory ni reapertura.
- Sin timer por tick ni SQL/HTTP por tooltip/clic/paquete. Expresiones dinámicas
  mantienen evaluación MAIN/fallback nativo. No se añade otro cargador de perfiles.

## Paquete
Ver runtime-manifest.json y SHA256SUMS. Requiere los cuatro JAR candidatos juntos;
no usar el shadowJar completo de los checkouts porque contiene cambios ajenos
excluidos en la composición. Las clases API de compra/XP/observación y el puente
XP se conservan; los YAML y demás recursos son byte-idénticos a Test contrastado.
Los cambios fuente y hashes de los commits se entregan en commits.json.
Comparar primero los hashes instalados con baseline en el manifest: si difieren,
reconciliar esos cambios antes de reemplazar archivos.

No hay nuevo SDK ni llamada que tenga que implementar el consumidor vEnchanting.
La integración/activación la coordina ese chat; esta entrega NO instala ni hace
lifecycle. No usar reload de producción como prueba de compatibilidad.

## Evidencia y pendientes
399 pruebas JVM/mocks/H2 sin fallos en las cuatro suites: eco40, LibreForge155,
EcoSkills130, EcoEnchants74. Incluyen regresiones y tests ajenos ya presentes.
18 clases de runtime cambiadas y 871 referencias propias comprobadas; firmas
previas conservadas. Esto no es prueba visual en Paper/MySQL ni benchmark.

Pendiente en Test tras integración autorizada: pieza requisito XIV SIN moverla,
Encantador13/14/50 y descenso/reset; cruce por XP y /ecoskills set. Misma pieza
vista por dos observadores; menú abierto/cursor con y sin requisito; PDC/Nexo/lore
y custodia intactos. Reconexión, transferencia y publicación demorada deben
mostrar el nivel nuevo; ausencia de caché nunca debe autorizar. Verificar que
no hay premios/level-ups ni transacciones extra al refrescar.
