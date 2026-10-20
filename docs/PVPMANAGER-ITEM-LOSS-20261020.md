# Incidencia separada: inventario perdido al desconectarse en combate

Reporte del propietario: PvPManager etiqueta, el jugador sale, recibe kill inmediato y desaparece TODO el inventario sin drops, incluidos Soulbound. Es general/reproducible según el propietario; no exige un caso nominal para investigar. **No reproducido por este chat, causa aún no confirmada. No incluido como fix en la entrega nativa.**

## Evidencia viva leída, sin cambios

- Custom/Sync: PvPManager4.1.68, mismo SHA256 `575b763ac15aa0809c76817490ca0dd5459bef5209ecb6a965bba6ce3ec5adb7`.
- Ambos: Kill on Logout=true; Inventory/Armor/Experience=true; Spawn NPC=false; Other Settings.Player Drop Mode=ALWAYS; sin comandos de combat log. No se configuró CLEAR ni TRANSFER. El comportamiento descrito no se explica por esos valores. No se ha cambiado el castigo ni recuperado/concedido objetos.
- Ambos EE64213413c86fe6d247ea1e4081de50a654716326d935449a22c2549ab2a14c08; handler Soulbound SHA256 `d49fb84b8a5286a1c6ebdf0aa4ab105bb2f96bed0256fa3bed02179f2daf223c`, igual al handler conservado en Test. Config Soulbound single-use=false, sin condiciones; no habilitado ni modificado por esta tarea.
- La integración AntigriefPvPManager de eco solo responde permisos de combate. No guarda/borra inventarios. Soulbound sí retira de drops los ítems con el enchant y usa itemsToKeep en Paper; el fallback profile.soulbound_items es de la ruta no-Paper. También quita drops tardíos de los ítems retenidos. Esto podría ser relevante para los encantados, pero no demuestra por qué desaparecerían los objetos normales.
- En latest.log no se encontraron errores de despacho PlayerDeathEvent/PlayerQuitEvent con la consulta dirigida; ausencia de excepción no prueba funcionamiento.

## Fuentes y límite

Fuente pública/API: https://github.com/ChanceSD/PvPManager y `_autodocs/api-reference/05-events.md`, `06-managers.md`. Context7 `/chancesd/pvpmanager` confirma PlayerCombatLogEvent y ruta de DeathHandler. Master público no se presenta como fuente exacta de este4.1.68 hasta contrastarla. No actualizar el plugin a ciegas.

La ruta pública handleCombatLogDrops solo fuerza keep/borra inventario en combinaciones parciales Inventory/Armor, que aquí no se usan. Se debe contrastar con binario4.1.68 y el orden real, no concluir desde master. Paper itemsToKeep es retención dentro del proceso de muerte, no recibo durable independiente de logout/sincronización.

Siguiente: seguir binario exacto y callbacks de quit→kill→drops/itemsToKeep→save, junto con sincronización de inventario; comparar muerte normal y salida en combate con objetos vanilla/Eco/Soulbound en Test cuando se autorice/coordine la reproducción. No usar jugadores productivos ni emitir muertes simuladas que disparen recompensas. No compensar ni repetir entregas sin evidencia de custodia. No editar PvPManager/vNetwork/vPets por inferencia.
