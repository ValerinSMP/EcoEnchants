# EcoSuite VFX piloto — 21/10/2026

Solo Eco resonante y Brasa errante. Otros30 diseños siguen como backlog; ningún VFX nuevo para ellos. No copia Filo Orbital. Candidato sin instalar/activar. Apariencia y rendimiento con jugadores NO verificados.

## Artefacto y contrato

EcoEnchants-2026.27-vfx-pilot-all.jar SHA256 `78eff168d845d1bf6ab2fb1709f9da33da2564833388ca82da6b187e9c9d3e50`.
Baseline real contrastado en ambos Dev: EcoEnchants1d7a0ca87822f38be5dbb9e2aae528cdb4e7895a301d179394e25ba34e64152c.
Conservar sin recompilar ni sustituir:
- eco5ca36aa859cef009156cef5e821ad7907e62f11006c8cde7382130799542544f
- LibreForgecc3a4c063e8f286aba82cf7469756d079c18b3ac245bd50e912840516bb25426
- EcoSkills0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca

La composición permite solamente OriginalRuntime, OriginalProcEngine, NativeBurstProgression, NativeProcAdapters, VfxProfiles y sus clases anidadas, y añadir las nuevas opciones al config por defecto preservando cada byte previo. Retira una clase anidada obsoleta de esas raíces sin referencias actuales. Enchants y API/SDK existentes byte idénticos. Cambios ajenos targets/contadores excluidos. XP, compra, costes, requisitos, tiers, conflictos, modos, selección, daño y cooldowns se conservan. No vEnchanting/vPets/SQL/pack nuevo.

## Enlace a resultados reales

- Eco resonante: el método record existente observa aceptación nativa mediante un IntConsumer opcional. No replica contadores ni validación. La firma anterior sigue delegando sin observador. Solo golpes aceptados1/2 generan marcas; duplicados/cancelados/cooldown no lo hacen. Excepciones cosméticas del observador no alteran el resultado del engine. Al resolver el pulso, el efecto recoge únicamente receptores con daño nativo positivo confirmado; solo entonces emite contracción/onda con radio<=2. Si no hubo receptor confirmado, no inventa impacto.
- Brasa errante: conserva kill confirmado y selección/radio/cooldown nativos. Después del mismo daño ya existente, el adaptador resolvedBy verifica evento nuevo, autor original, no cancelación/inmunidad y pérdida real de salud. El visual enlaza el origen muerto congelado con ese receptor real. Sigue su ubicación en sync, sin proyectil dañino ni demorar el daño. El chasquido se reproduce en el receptor al destello, no en el muerto. Si el receptor ya murió/desapareció o el presupuesto no permite, la cosmética puede descartarse.
- NativeProcAdapters.resolvedBy comparte ese testigo entre ambos enlaces; es solo lectura. No usa triggers genéricos para inventar impactos. Los dos pilotos sustituyen sus visuales genéricos anteriores; otros efectos mantienen los existentes.

## Configuración de los perfiles

`visuals.yml` es un fragmento para MERGE de claves bajo original-procs.visuals, NO para reemplazar config.yml entero. Conservar preferencias existentes, límites, textos, colores y configuración privada. Solo dos perfiles: eco_resonante (progress/resolved) y brasa_errante (link). El recurso config.yml del candidato trae los mismos nuevos defaults. No copiar VFX-PERFILES.csv/native-evidence.json como definiciones de enchants.

Renderer dentro de OriginalRuntime.visual, mismo ParticleBudget de los visuales legacy y una sola tarea síncrona para escenas activas. VfxProfiles solo prepara datos y geometría pura compartida; no contiene nombres de enchants ni scripts. Añadir otro perfil a estas primitivas requiere YAML y enlace al resultado nativo, no otra geometría por ID.

Opciones de perfil: enabled; phases.progress/resolved/link; steps con primitive MARK/RING/ARC/FLASH, motion STATIC/EXPAND/CONTRACT (radial), particle vanilla, color #RRGGBB y size para DUST, material para ITEM/BLOCK, at, duration, scale, height, quantity. Otros tipos de datos de partículas no soportados desactivan ese perfil con aviso. Phase: sound (clave vanilla validada en Registry.SOUNDS), sound-tick, sound-anchor origin/recipient, volume, pitch. Sonido máximo una vez por fase/observador, y solo con partículas realmente concedidas. Colores configurables, sin pack ni modelo externo.

Preparación al enable/reload. Perfil inválido se desactiva con advertencia; no cambia mecánicas. enabled global/perfil desactiva cosmética. Máximo16 perfiles,4 fases examinadas y4 pasos/fase; fases válidas progress/resolved/link. Los dos perfiles entregados se validan como YAML real en tests.

## Límites y retirada

- Reutiliza observer-radius24 (hard32), max-observers16 (hard32), per-activation16 (hard16) AHORA por escena/observador completa, no por frame; per-observer-per-second64 (hard256), global-per-second256 (hard4096), compartidos con el genérico.
- max-scenes32 (hard64), per-source-scenes4 (hard8), duración total<=40ticks, cantidad1..16/paso, escala<=2, altura±2, tamaño DUST<=1,5, volumen0..1, pitch0,5..2. El radio de anillo se limita también al radio real del resultado. Topes pueden descartar fases finales si el perfil exige más partículas que su presupuesto. No hay garantía de emitir todas bajo saturación.
- Observadores se capturan solo al crear la escena; sin búsqueda global por frame. Cada frame comprueba solo referencias/UUID acotados, chunks cargados, mundos, sesión, vida y visibilidad. Geometría calculada una vez por paso/frame y reutilizada entre observadores. No se cargan chunks ni crean entidades, ítems persistentes, puntos, SQL o tareas fuera de escenas.
- Índice por actor/receptores retira escenas en quit, muerte y cambio de mundo mediante retire existente. WorldUnload/ChunkUnload, entidad inválida y sesión caducada cancelan. Reload/disable vacían escenas, índices y tarea; terminar o agotar el presupuesto de la escena también la retira. No se borran partículas vanilla ya enviadas al cliente: solo futuras emisiones.

## Evidencia real y límites

Build solo core-plugin EcoEnchants en personal con ejecutor2CPU/4GiB/sin swap/un worker y toolchain/release existentes.138 pruebas JVM pasan:127 previas +11 nuevas. Nuevas prueban YAML de los dos perfiles, preparación/desactivación, primitivas/radio/extremos, inválidos, cuotas, aceptación nativa y fallos cosméticos, cancelación/inmunidad/testigo obsoleto, renderer real con Bukkit/scheduler simulados, fase única, timer, saturación/reload, quit/chunk unload y receptor muerto. No son compras ni escenas vistas por cliente.

Compuesto auditado:1291 métodos/9374 referencias del bootstrap, clases propias ausentes son error; handleLoad REAL ejecutado con almacenamiento de registros simulado. Rechaza el antiguo JAR e8ca5859 con la clase omitida y pasa este candidato. No ejecuta constructor JavaPlugin, callbacks de registro de Paper, enable ni SQL: no es un arranque completo de Leaf.

Pendientes: arranque real y observación visual en ambos Dev, materiales opcionales sobre el servidor real, PvP/PvE/protecciones y otros plugins, muerte/reconexión/transferencia con escenas, y Spark bajo carga60–100online/30–40activos. No se afirma mejora medida de MSPT/TPS o fidelidad visual aceptada.

## Integración por el chat dueño de Dev

Destino: Dev Custom48d37562-51f7-48c7-812b-b295df2e8aeb y Dev Sync90db2686-d703-4467-9b3a-7dcc0add36cf. No HomeSV Test ni producción. No instalación/lifecycle desde este chat.

1. Contrastar runtime/configuración actuales y cambios posteriores. Respaldar con hashes fuera de contenedores según reglas locales. Sustituir únicamente EcoEnchants por este candidato con el nombre canónico, sin JAR duplicado; no sustituir los otros tres.
2. Fusionar las claves del fragmento y verificar global enabled según la prueba acordada; no cambiar enchants, skills, precios, datos/journals ni catálogo. Coordinar jugadores y reinicio Dev para activar código nuevo. No hot-swap de clases con reload.
3. Después, comprobar arranque, bootstrap, validate/status de vEnchanting y compra/XP exactamente una vez. Para ajustes posteriores solo de perfiles sirve la recarga nativa de EcoEnchants; cancela escenas y revalida.
4. Eco resonante: respetar su espera nativa al entrar y cooldown. Tres golpes aceptados al mismo objetivo/arma dentro de ventana; marcas1/2, onda solo si el pulso dañó un receptor válido. No animación extra de golpe genérico. Probar veto, inmunidad, objetivo ausente y PvP/PvE.
5. Brasa: muerte nativa válida y receptor elegible cercano con daño positivo; brasa al receptor correcto, destello/sonido allí. Sin otro receptor, daño cancelado o muerto antes de la escena: no inventar vínculo.
6. Verificar global/perfil off, reload, cambio de mundo, desconexión, muerte, unload, saturación y ausencia de efectos/tareas acumulados. Comparar daño, cooldown, requisitos y XP con baseline. Documentar qué confirmó el jugador y qué queda pendiente.

Personal: /root/development/artifacts/eco-suite/vfx-pilot-20261021/.
HomeSV: /home/mtyn/staging/survival-custom/ecosuite-vfx-pilot-20261021/.
