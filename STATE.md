# EcoEnchants — estado

## Guía entregada a Custom/Sync y propuesta de mesa — 2026-10-14

- Por autorización actual del propietario se instaló el mismo JAR de Test en Custom y Sync: SHA256 64213413c86fe6d247ea1e4081de50a654716326d935449a22c2549ab2a14c08. YAML de ambos 182f867a33111d3c2e901eebe0baef07bcedcfeac4abf75268f146f333079532; solo secciones GUI/guide/enchantinfo. 13 definiciones explícitamente desactivadas retiradas por nodo, otros enchants/config intactos. Respaldo HDD /mnt/storage/backups/ai/survival-custom/ecoenchants-guide-production-20261014T182127Z con manifiesto verificado.
- No nueva build: artefacto previamente probado con 32 tests/ABI/dependencias. Sin lifecycle; activación/arranque/juego productivo pendientes del propietario. No disponible al consultar Encantador sigue pendiente de causa/corrección; no afirmar que se solucionó ni permitir compras con ausencia.
- Propuesta investigada y documentada, sin implementar ni bloquear mesa: docs/ENCHANTING-TABLE-PROPOSAL-20261014.md. Recomienda addon separado, precios candidatos no aprobados, XP inicialmente/gemas opcionales deshabilitadas y autoridad nativa EcoSuite. Runtime Custom personalizado en mesa actualmente desactivado; XP solo enchant_item ×3. Entrega de mesa futura solo Test y chat separado, sin delegación automática.

## Requisito único y retirada de desactivados — 2026-10-14

- Implementado: sección amarilla única; línea roja «Requiere Encantador X (tu nivel)» solo si falta nivel; si cumple se oculta, y sin otros requisitos se oculta sección. Filtrado manual antes de wrapping, metadatos/descripción cacheados y estado personal por vista, sin tareas periódicas. Lore de ítems/mecánicas/callbacks intactos. Contrato actual docs/ENCHANT-GUIDE.md.
- Compilado en personal/ejecutor limitado/toolchains nativos. 32 tests JVM sin fallos; 11 clases verificadas por dependencias, ABI GUI preservada y 693 entradas originales idénticas. Sin medición ni verificación real de menú.
- Instalado solo Test: JAR 64213413c86fe6d247ea1e4081de50a654716326d935449a22c2549ab2a14c08; YAML 75655a24538eb754793c8b095fe910226ac29a3761ef2eecfe4d9588cb8391e0. Retirados 13 YAML de 11 IDs explícitamente desactivados (duplicados charge/rumble); wisdom/abrasion y otros YAML intactos. Defaults upstream conservados, carpeta enchants permanece.
- Respaldo HDD /mnt/storage/backups/ai/survival-custom/ecoenchants-guide-compact-20261014T181009Z. Custom/Sync verificados sin cambios (JAR/config/todos enchants). No reinicio/reload. Activación y comprobación visual/registro/reconexión pendientes del propietario.

## Requisitos observacionales de Encantador — 2026-10-14

- Implementado: umbral de condiciones nativas directas, nivel personal desde snapshot público de caché de EcoSkills, requerido/faltante/cumplido/ausencia y desactivación explícita 999. Fuera de la caché compartida de descripciones; no cambia niveles, balance, callbacks, lore de ítems ni progresión. Contrato/limitaciones en docs/ENCHANT-GUIDE.md.
- Compilado en personal con ejecutor limitado y toolchains existentes; 28 tests JVM pasaron (24 previos +4 requisitos). ABI pública GUI conservada, 11 clases verificadas por dependencias y 693 entradas originales idénticas. API getCachedSnapshot(UUID) confirmada en JAR Test; sin benchmark ni prueba en juego.
- Instalado solo Test: JAR f7c200c4e48365058ea3438df830209ac80ed20003633a3c0089e9ec70d6efe0; YAML 502c3825c7a97772123583ab39b0462733bef60b88e7351e6135b490613e4bd4. Respaldo HDD /mnt/storage/backups/ai/survival-custom/ecoenchants-guide-requirements-20261014T173959Z. Custom/Sync JAR/YAML sin cambios.
- Activo: pendiente reinicio del propietario, no lifecycle realizado. Pendientes apertura, valores personales/diferencias entre jugadores, caché fría, reconexión y validación visual de estados. No presentar como integración validada.

## Guía de consulta estable y cancelar — 2026-10-14

- Implementado: opción A sin abrir submenú al pulsar libros; niveles disponibles y efecto mostrado en lore. Título fijo y contador en libro informativo; GuideBrowser ya no llama setTitle. Retorno de cancelar/timeout conserva query/filtro/grupo/página y verifica identidad de vista posterior al cierre en lugar de exigir CRAFTING. Comando /enchantinfo y firmas existentes conservados.
- SignGUI investigado por documentación/VersionMatcher: no soporte 26.2 verificado; sigue chat privado autorizado, sin nueva dependencia/NMS. Propuesta de desbloqueos/mesa en docs/ENCHANTING-PROGRESSION-NEXT.md, no implementada. No cambiar balance/XP/recompensas/puente de vPets.
- Compilado en personal, ejecutor limitado/toolchains originales. 24 tests JVM pasaron, dependencias de 9 clases GUI verificadas, 693 entradas originales idénticas y ABI GUI conservada. Checks bytecode de no setTitle/no apertura por libro. Sin medición ni prueba en juego.
- Instalado solo Test: JAR SHA256 76f479c25d2532d6beaf3a992195eb8c0dac5aea6993c240111eaa2becf6e2fa; YAML e775319fb9a9b1fcefae55d5fb5ebc949b184a0fea83721a72aeedb0fb96385d. Backup HDD /mnt/storage/backups/ai/survival-custom/ecoenchants-guide-simple-20261014T171434Z/. Custom/Sync JAR/YAML intactos.
- Activo: pendiente reinicio del propietario; no lifecycle realizado. Pendiente arranque, cursor/tooltip al navegar y cancelar/timeout en supervivencia/creativo y tras abrir otra vista/desconectar/reload.

## Distribución y estilo de la guía — 2026-10-14

- Implementado en personal: guía sin ranura cautiva, 28 libros (4×7), libro de ayuda/restablecer arriba, cerrar/buscar/filtro abajo como el video, relleno para flechas de página/nivel inactivas, lista de filtros con selección visible y contador solo para varias páginas. Colores/símbolos del estándar; glifos E179/E17A verificados en archivos reales de Test. No editar/publicar pack.
- Preparar selección antes del render evita mostrar la lista previa al buscar/filtrar. Títulos sin formato; actualización diferida/coalescida solo ante render, sin tarea periódica. Comandos, mecánicas y runtime ajeno conservados.
- Compilado con ejecutor personal y toolchains originales. 22 tests JVM (8 guía +14 existentes) pasaron; 9 clases verificadas por dependencias, firmas GUI conservadas, 693 entradas originales idénticas. Sin benchmark ni validación visual/gameplay.
- Instalado solo Test: JAR SHA256 30611b39a78de4f38bc4d7c6b315c6251d54c42bcfd67e2b54148a5b5c65f8de; YAML SHA256 f187299223f6bd97c942e8662e96c8b60fcc2fed72b617a4871f277fc8c62687. Backup HDD /mnt/storage/backups/ai/survival-custom/ecoenchants-guide-layout-20261014T164819Z/. Custom/Sync JAR/YAML intactos.
- Activo nuevo código: pendiente reinicio del propietario, no lifecycle realizado. Pendientes apertura, páginas/niveles, primera actualización de búsquedas, imposibilidad de depositar objetos y glifos Java/Bedrock. Guía actual docs/ENCHANT-GUIDE.md; evidencia staging enchants-menu-layout-20261014.

## Corrección de dependencia de la guía — 2026-10-14

- Causa confirmada por ticket y JAR: la guía compilada invocaba DragAndDropPricingKt, ausente en el runtime conservado. Se retiraron únicamente importaciones/indicadores drag/drop de la guía; no se añadió esa mecánica.
- Compilado en personal con ejecutor limitado y toolchains existentes; 20 tests JVM pasaron nuevamente. Nueva comprobación de dependencias de clases: rechaza el candidato fallido y verifica las 9 clases de guía del corregido. No sustituye prueba de integración/juego.
- Instalado únicamente Test: SHA256 e744da64f39d093688bb9d773a78c0687221f41138f355bd1d6bdde2d88d10e0. YAML Test y JAR/YAML Custom/Sync sin cambios. Respaldo del fallido: /mnt/storage/backups/ai/survival-custom/ecoenchants-menu-linkage-fix-20261014T162357Z/.
- Activo corregido: pendiente reinicio del propietario; no reinicio/reload realizado. Pendientes arranque y apertura/navegación real del menú. Guía y empaquetador actualizados; cambios ajenos preservados.

## Guía de encantamientos — 2026-10-14

- Modificado en personal: EnchantGUI, GuideBrowser, GuideQuery, defaults opcionales, 6 tests de guía y empaquetador de runtime; sin editar mecánicas/NMS/eco/LibreForge ni cambios ajenos bajo target/DEVELOPMENT_HANDOFF.md.
- Compilado: ejecutor limitado 2 CPU/4 GiB, Gradle 9.3.1/Kotlin 2.3.0, toolchain 25 y JVM/release 21. Probado: 20 tests JVM, 0 fallos/errores/omitidos; 6 nuevos y 14 existentes. Sin benchmark de rendimiento.
- Runtime conservado: candidato sobre JAR instalado 9f974541…, 693 entradas originales idénticas, 3 clases GUI y config modificados, 5 clases agregadas, ninguna retirada; firmas públicas GUI conservadas. Mecánicas/loader/NMS idénticos.
- Instalado solo Test: EcoEnchants-2026.27-all.jar SHA256 243bf0dad95769df9918184a95b911868da0b347da9a9bed93c38c20fc8b22f0 y config guía habilitada. Custom/Sync JAR+YAML sin cambios.
- Respaldo HDD: /mnt/storage/backups/ai/survival-custom/ecoenchants-menu-20261014T160210Z/. Staging: /home/mtyn/staging/survival-custom/enchants-menu-20261014/.
- Activo nuevo código: pendiente reinicio del propietario; no lifecycle realizado. Arranque y gameplay pendientes. Pruebas manuales: búsqueda/cancelación/timeout, filtros, niveles, devolución única del objeto, inventario lleno, usuarios separados y desconexión/reload.
- Contrato/guía y reproducibilidad: docs/ENCHANT-GUIDE.md, tools/menu-runtime/package-runtime.py.

## Estado previo conservado como referencia

- Fuente canónica: personal `/root/development/projects/eco-suite/EcoEnchants`. HomeSV conserva la referencia sin editar. Original comparado: `upstream-review/main`, SHA en `upstream-differences-final.json`; sin merge, checkout o actualización completa.
- Tarea autorizada: las ocho prioridades pendientes (3–10) y las dos funciones de EcoArmor, solo Test.
- Modificado: Guard de conversión de lore, corrección de doble coste/combinar durabilidad en su yunque actual y libros drag/drop con precio/GUI nativos. Se conserva Indestructible, costes vanilla y controles existentes. Drag/drop ausente = deshabilitado; no se habilitó en configuración viva.
- Archivos: código/configuración de fuente y tests nativos del cambio; inventario exacto en git-final.json. DEVELOPMENT_HANDOFF.md ajeno/no versionado preservado.
- Compilado: sí, ejecutor personal 2 CPU/4 GiB/sin swap, una build, Gradle Wrapper original, un worker y Kotlin en proceso. Toolchains/release por módulo conservados. Eco/EcoEnchants/EcoSkills utilizan la opción nativa `paperweight.experimental.genSources=false`, que evita generar fuentes auxiliares y conserva los adaptadores.
- Probado: 14 pruebas JVM, 0 fallos, 0 errores, 0 omitidas. Suite total 200. EcoItems no tiene tests propios. No es medición de rendimiento ni prueba en Minecraft de las funciones nuevas.
- Dependencias: eco/LibreForge propios en Maven aislado; versiones/hashes resueltos en los manifests de la tarea. La recompilación final de LibreForge conserva todas las clases usadas por consumidores; solo cambia CRLF del mismo config.yml (libreforge-final-abi.json). Loader y bibliotecas empaquetadas preservados.
- Instalado: sí, únicamente Test `/var/lib/pterodactyl/volumes/f2ea450e-02a0-416c-9838-e73458d37b1c/plugins/EcoEnchants-2026.27-all.jar`, SHA256 `727e5840a1e5ada2d0b09164cb5186677f43b450d8164f3abad34c9b6c0997cc`. Hash en disco verificado; dueño/modo preservados.
- Respaldo verificado: `/mnt/storage/backups/ai/survival-custom/ecosuite-upstream-remaining-20261010-test/`. Anterior SHA256 `9f974541c99cdb6ef180be428dcd526472634c49434637612998f67b119de75e`. Manifest y reversión junto al respaldo; staging/evidencia en `/home/mtyn/staging/survival-custom/eco-upstream-remaining-20261010/`.
- Activo Test: inicio de los siete plugins observado en latest.log y hashes instalados coincidentes. Gameplay: el propietario informa «funciona todo»; aceptación básica de la entrega, sin registro individual de todas las pruebas dirigidas. El agente nunca reinicia ni recarga, incluido Test. No se ejecutó lifecycle, BD ni pack; no se editaron YAML vivos. Custom/Sync no instalados/modificados; los 14 JAR productivos conservaron sus hashes al verificar la entrega.
- Siguiente paso: entrega aceptada de forma básica en Test. Candidato siguiente: revisión acotada de homing usado por el Arco de Enderita, todavía sin aplicar. Custom/Sync y transferencia requieren autorización futura; Test 1.21.11 observado, Minecraft 26.2 no probado en juego.
- Git: cierre de cambios propios autorizado tras aceptación en Test. Resultado de commit/push normal en git-closeout.json; restauraciones preexistentes bajo target y DEVELOPMENT_HANDOFF.md se mantienen fuera del commit.

- Restauración de fuentes preexistentes: 19 archivos legítimos bajo paquetes target de eco y 5 de EcoEnchants habían quedado fuera de la copia personal. Se recuperaron desde HomeSV con hash coincidente, sin sobrescribir archivos; no son funciones nuevas ni cambios propios de esta entrega.
