# Enchantment guide

The optional `enchant-guide.enabled` browser extends the existing `/ecoenchants gui` and `/enchantinfo` menus. Existing commands, permissions, enchantments, crafting/anvil mechanics and effects remain authoritative.

- Search button: private Bukkit conversation, 64-character literal name/ID search, case/accent insensitive. Left-click searches; right-click clears. `cancelar` or 30 seconds without input returns without replacing the search. Other inventory opens, quit, reload and disable cancel pending input.
- Equipment filter: left-click advances, right-click goes back. Targets are configurable. Combined native targets such as melee/armor are expanded once per reload using native matchers and example items; no repeated matcher work while browsing.
- Clear removes search/equipment/group selections. A captive item continues to restrict the native applicable-enchantment list.
- Book click opens level details. Previous/next stay within 1..maximumLevel; back preserves query/equipment/group/page. Native close returns captive items before a detail/search transition. Navigation is deferred beyond inventory click handling.
- Empty results show a configured message, with a valid page count of one.
- Obtaining information lists only globally enabled native table/trading/loot paths permitted by the enchantment. Custom systems and their prices are not inferred; the configurable fallback indicates missing information.

All new visible text, items, positions and filters use the native configuration/parser. The feature defaults to disabled for existing configurations; the Test candidate enables it. No purchase or enchant-application actions were added. Detail-menu cache is bounded to 256 entries and does not capture a Player. No periodic task, storage lookup or network request was added. Runtime performance is not measured.

## Build and controlled runtime

Canonical source: personal `/root/development/projects/eco-suite/EcoEnchants`. Kotlin 2.3.0, Gradle 9.3.1, toolchain 25 and JVM/release 21 retained. Build through `run-personal-build.sh` with one worker and Kotlin compilation in process.

The installed runtime differs from source. `tools/menu-runtime/package-runtime.py BASELINE CORE-SHADOW OUTPUT` overlays only the guide classes and appends disabled optional defaults onto the original embedded config. It verifies all other original entries byte-for-byte, retains every entry and checks existing nonsynthetic public GUI signatures. It must run in the limited personal executor. Do not substitute an unrestricted full rebuild for this candidate.

Baseline SHA256: `9f974541c99cdb6ef180be428dcd526472634c49434637612998f67b119de75e`.
Candidate SHA256: `243bf0dad95769df9918184a95b911868da0b347da9a9bed93c38c20fc8b22f0`.
Changed: 3 existing guide classes, embedded config, 5 added guide classes; 693 original entries unchanged, none removed. Mechanics, enchantment registrations, loader and NMS entries are byte-identical to the baseline.

## Validation and delivery — 2026-10-14

Implemented, compiled and installed only in Test `f2ea450e-02a0-416c-9838-e73458d37b1c`. No lifecycle performed; activation awaits the owner's restart. Custom/Sync JAR and EcoEnchants YAML hashes remained unchanged.

20 JVM tests passed (6 new guide tests plus 14 existing regressions), no failures/errors/skips. Search formatting/accents, literal query, bounded input, cyclic filters, level boundaries and universal/combined equipment selections covered. Native GUI/inventory/conversation gameplay has not been tested; JVM tests do not prove those interactions. Native YAML was checked for nonoverlapping controls and unchanged gameplay settings.

Backup: `/mnt/storage/backups/ai/survival-custom/ecoenchants-menu-20261014T160210Z/`.
Evidence/staging: `/home/mtyn/staging/survival-custom/enchants-menu-20261014/`.

After owner restart, verify `/ecoenchants gui`: searching with/without accents; cancel/timeout; both filter directions; unknown query and page counts; clicking a book, all level boundaries and returning; placing/removing an item and checking it returns exactly once on search/detail/close; full inventory; independent player sessions; quit/reconnect/reload while entering search. Startup/linkage and these player tests remain pending. Production delivery requires separate authorization.
