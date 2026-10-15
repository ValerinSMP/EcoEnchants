# API de mejora nativa v1 — 2026-10-15
EnchantUpgradeAPI.validateUpgrade(Player,ItemStack,Enchantment,expectedFrom,target,LevelPolicy).
EnchantUpgradeAPI.applyUpgrade(Player,storageSlot,expectedItem,Enchantment,expectedFrom,target,LevelPolicy,operationId).
Ambas MAIN; unidad única en almacenamiento del jugador. CAS exacto de item/metadatos/cantidad; clone y revalidación tras evento preparatorio cancelable. Conserva PDC/metadatos/libros, refresca holders y emite evento aplicado con getters clonados.
Restricciones nativas de objetivos/conflictos/prerrequisitos/límites. has_skill_level requiere caché conocida; condiciones de activación permanecen holders. NATIVE_MAXIMUM o VANILLA_ONE_ABOVE solo vanilla max+1.
Status APPLIED/APPLIED_CALLBACK_REVIEW/UNKNOWN_APPLY_REVIEW/DENIED/ITEM_CHANGED/CANCELLED. Tras aplicar o incertidumbre reconciliar, no repetir.
operationId solo correlación; no recibo/deduplicación durable ni cobro/custodia, responsabilidad vEnchanting. No XP ni EnchantItemEvent sintético.
38 pruebas JVM pasan; inventario/PDC/Nexo/holders reales pendientes Test. Runtime/SDK no instalados ni activos.
