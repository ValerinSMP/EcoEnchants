# Enchantment guide

The optional `enchant-guide.enabled` browser extends the existing `/ecoenchants gui` and `/enchantinfo` menus. Existing command signatures, permissions, enchantments, anvil mechanics, effects and obtaining paths remain authoritative.

## Current Test layout — 2026-10-14

The reference is the owner's Medal video. Main menu has six rows: information book at row 1/column 5; 28 book cells in rows 2–5/columns 2–8; bottom close at column 5, search sign at 6, equipment-filter eye at 7, previous/next at 1/9. Black filler has an empty name and no lore. Unavailable page/level arrows show the configured filler instead of gray dyes or inactive arrows. Empty content cells are empty; no object-input/captive slot exists in the enabled guide.

The information book resets query/equipment/group/page selections. The separate clear button is disabled in this layout. Search remains a private Bukkit conversation: left-click searches, right-click clears, `cancelar` (case-insensitive) or timeout (30 seconds) returns preserving query, filter, group and page. Return checks the actual default inventory identity captured after closing the guide; it does not require CRAFTING and therefore makes no survival/creative type assumption. Quit, another inventory opening, reload and disable cancel pending input. Literal search is case/accent insensitive and capped at 64 characters.

Equipment eye cycles forwards/backwards and shows an eight-entry sliding preview, highlighting the selected entry, plus the number of remaining options. Native combined targets such as melee/armor are expanded once per reload from native matchers/example items.

Guide books now only show information on hover, including available levels and the effect at the displayed level. Clicking does not open level details. The native /enchantinfo entrypoint is preserved, with guide level navigation disabled. Search/filter selections are prepared before native rendering, so the first refreshed view uses the new result list. Main title stays fixed: the guide never changes InventoryView titles during navigation. Page count is shown on the information book only when multiple pages exist. Legacy detail command titles preserve names but remove formatting. No periodic task was added.

Visual content follows `/home/mtyn/valerin-network/docs/ESTANDAR-VISUAL.md`: #FFDA29, #00FB9A, #FF3300, #FF9F0F and #5286FF; approved arrows, pencil, target, conflict and flag symbols. All visible text/items/positions are configured through native YAML/parser. Click glyphs U+E179/U+E17A were verified from Test's EconomyShopGUI Spanish configuration and Nexo `valerin_shop/font/clicks.json`, referenced by `minecraft/font/default.json`. No pack edits/publication occurred. Actual Java/Bedrock glyph rendering remains to be checked by the owner.

Obtaining metadata lists only enabled native table/trading/loot paths permitted by each enchantment. Custom-system paths/prices are not invented. The guide has no purchase or enchant-application action. Detail-menu cache is bounded to 256 entries and does not capture a Player. No storage/network read or gameplay mutation was introduced. Runtime performance is not measured.

The feature defaults to disabled in source, preserving upstream configuration/mechanics. The Test configuration enables it and supplies the corresponding native `enchant-gui` layout. Disabled legacy GUI retains its native captive behavior; the enabled guide does not.

## Build and preserved runtime

Canonical source is personal `/root/development/projects/eco-suite/EcoEnchants`; Gradle 9.3.1/Kotlin 2.3.0, toolchain 25 and JVM/release 21. Builds/packaging use the limited personal executor (2 CPU/4 GiB, one worker, Kotlin in process).

Installed runtime differs from source. `tools/menu-runtime/package-runtime.py BASELINE CORE-SHADOW OUTPUT` overlays only guide classes and appends disabled optional guide defaults to the original embedded config. It verifies unchanged entries, retained original entries and nonsynthetic public GUI signatures, and invokes `check-guide-linkage.py` for internal EcoEnchants class references. Do not deliver an unrestricted full rebuild.

Baseline SHA256: `9f974541c99cdb6ef180be428dcd526472634c49434637612998f67b119de75e`.
Current Test candidate SHA256: `76f479c25d2532d6beaf3a992195eb8c0dac5aea6993c240111eaa2becf6e2fa`.
693 original entries unchanged, 3 guide classes + embedded config changed, 5 guide classes added, no entries removed. Mechanics, registrations, loader and NMS entries remain byte-identical to the baseline. Internal class checks are not verification of all external method signatures or live integration.

The first candidate failed because the guide referenced DragAndDropPricingKt, which source contains but runtime does not. Those display calls were removed in commit 2218650a; drag/drop mechanics were not installed. The linkage check rejects the failed candidate and accepts the corrected/new layouts.

## Validation and delivery

24 JVM tests passed (10 guide tests plus 14 existing regressions), no failures/errors/skips. New checks cover formatted/plain titles, conditional page counts and bounded/current-selection-inclusive filter previews. Static YAML checks cover 28 cells, nonoverlapping controls, filler-based inactive pagination, verified glyph definitions and unchanged gameplay settings. This is not visual or inventory gameplay verification.

Installed only Test `f2ea450e-02a0-416c-9838-e73458d37b1c`. Backups verified on secondary disk `/mnt/storage/backups/ai/survival-custom/ecoenchants-guide-simple-20261014T171434Z/`. Staging/evidence `/home/mtyn/staging/survival-custom/enchants-guide-simple-20261014/`. No lifecycle; owner restart required. Custom/Sync EcoEnchants JAR/YAML hashes unchanged.

Pending after owner restart: startup and opening the main menu; title with single/multiple pages; no item can be deposited at the information slot; filler at first/last page and level limits; first-frame search/filter updates; both filter directions and preview highlighting; book levels/back/reset; private input/cancel/timeout; Java/Bedrock glyphs; independent users and quit/reconnect/reload during search. No in-game performance benchmark performed.

## Stable browsing and cancelled search — 2026-10-14

Compiled, packaged and installed only Test, activation awaits owner restart; no lifecycle occurred. Bytecode checks confirm GuideBrowser does not call setTitle and guide-book rendering does not reference openGuideInfo. These structural checks do not prove client cursor behavior. The two new JVM cases cover exact cancellation/timeout state retention and inventory identity-based return versus offline/reload/other view. Real survival/creative cancellation, timeout, independent sessions and cursor/tooltip checks remain pending.

SignGUI support for Leaf 26.2 was not verified: the library declares 1.8–1.21 and wrappers through 1_21_R5. No unsupported library or NMS adaptation was installed; private chat remains the authorized fallback. See docs/ENCHANTING-PROGRESSION-NEXT.md for sources and the separate proposal for skill unlock authority and the future table. No unlock requirements, balance or XP/reward/callback changes were implemented.

## Requisito real de Encantador — 2026-10-14

Una sola sección amarilla «Requisitos:», separada como «Obtención». El skill se presenta en una única línea roja `Requiere Encantador 14 (10)` únicamente cuando falta nivel; entre paréntesis está el nivel del jugador. Cumplir el requisito elimina esa línea. Sin requisito directo de Encantador ni encantamientos previos, desaparece la sección completa: no se muestra «Ninguno», «Cumplido» ni «Sin requisito». Los encantamientos previos no vacíos se agrupan dentro de la misma sección, y se omite su antiguo campo `%required%` del lore genérico de la guía para no duplicarlo. Configurable bajo `enchant-guide.requirements`; la posición en `enchant-guide.book-lore` se define con una línea `%requirements%`.

La guía lee las condiciones directas `has_skill_level` con `args.skill: enchanting` del enchant registrado. Varias condiciones constantes se combinan por máximo; expresiones/no enteros/valores negativos muestran una indicación dinámica sin inventar umbral. No usa anuncios manuales de desbloqueos ni interpreta la carpeta disabled como autoridad. No interpreta árboles OR/AND anidados. Cumplir Encantador no promete activar efectos: otras condiciones, mundos, permisos y mecánicas siguen nativos.

Tu nivel se lee una vez por preparación del menú mediante el método público estático opcional `EcoSkillsAPI.getCachedSnapshot(UUID)`, schemaVersion 1, en el hilo principal. Valida UUID/disponibilidad/entero del skill enchanting. Plugin/API/caché ausente produce `Requiere Encantador 14 (No disponible)` en advertencia, nunca un nivel cero ni falso permiso. No carga perfiles, consulta almacenamiento o evalúa condiciones/callbacks. Resuelve contra el plugin habilitado sin conservar loaders. No agrega tareas periódicas. Los libros reutilizan el valor del estado de la vista; `/enchantinfo` tiene su lectura propia con guía habilitada. Refresca al render/reabrir, no continuamente.

Retira la línea manual `Req: ... Encantador NvNN` o variante `Requisito(s): ... Nv NN` antes del word-wrap, solo si hay umbral soportado. Conservar descripción, formato, reset tags, placeholders y wrapping nativos. La descripción filtrada se cachea por enchant/nivel como la nativa y se invalida en el reload existente; el estado personal nunca se cachea allí. Lore de armas, efectos, condiciones, XP/recompensas y puente de vPets no cambian.

## Configuraciones desactivadas retiradas — solo Test

Retiradas 13 configuraciones correspondientes a 11 IDs con condición explícita Encantador 999 + mensaje «Temporalmente desactivado»: ascend, aura, brightness, charge, confusion, ninja, rumble, soul_storm, stimulating, transmission y vitality. Charge/rumble tenían dos copias. Wisdom/abrasion permanecen: su carpeta no demostraba desactivación. No se eliminó la carpeta entera ni se alteraron otros enchants. El loader nativo copia defaults cuando falta la carpeta completa, no por cada YAML ausente; `enchants/` permanece. Los defaults upstream del JAR/fuentes siguen conservados. Retirada del registro y comprobación real tras reinicio del propietario pendientes; no se intervino en inventarios. Anuncios de desbloqueo EcoSkills y referencias en otros plugins no se migran en esta tarea.

## Validación y entrega actual

32 tests JVM sin fallos: 28 anteriores +4 casos de sección única/ocultación/dos jugadores/caché fría/preenchants/filtrado antes de wrapping. ABI GUI conservada, 11 clases verificadas por dependencias, 693 entradas originales idénticas; NMS/mecánicas byte-idénticas al baseline preservado. No benchmark ni ejecución visual o del método dentro del servidor.

Instalado únicamente Test: `EcoEnchants-2026.27-all.jar` SHA256 `64213413c86fe6d247ea1e4081de50a654716326d935449a22c2549ab2a14c08`; config.yml `75655a24538eb754793c8b095fe910226ac29a3761ef2eecfe4d9588cb8391e0`. Respaldo HDD `/mnt/storage/backups/ai/survival-custom/ecoenchants-guide-compact-20261014T181009Z`, con copias verificadas de JAR/YAML y 13 retirados, manifest.json. Reversión: restaurar estos 15 archivos exactos preservando propietario/permisos antes de un reinicio autorizado. Staging HomeSV `enchants-guide-compact-20261014/installation.json` y `removals.json` registran hashes y destinos. 104 YAML permanecen, incluidos ejemplo y wisdom/abrasion; verificado que solo cambió el conjunto previsto. Hashes completos JAR/config/todos enchants de Custom/Sync intactos. No lifecycle realizado.

Pendiente tras reinicio del propietario: `/ecoenchants gui`, Apagón requerido14 con jugador inferior/suficiente; una sola línea roja con su nivel; desaparición de sección vacía y conservación de obtención; ausencia de los 11 desactivados; navegación/búsqueda/cancelar y dos jugadores/reconexión. Compilación/tests/instalación no prueban arranque, registro real ni apariencia.

## Entrega posterior a producción — 2026-10-14

Tras aceptación del menú por el propietario, mismo JAR instalado en Custom/Sync con respaldo; no lifecycle ni arranque/gameplay productivo verificado. Encantador No disponible sigue pendiente. Estado y manifiesto actuales en STATE.md y docs/ENCHANTING-TABLE-PROPOSAL-20261014.md. Mesa/cobros propuestos, no implementados.
