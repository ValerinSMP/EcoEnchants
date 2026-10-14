# Encantador y futura mesa — propuesta, no implementada

Referencia: Hypixel separa guía, habilidad de Encantamiento y mesa. La mesa permite seleccionar el encantamiento y después muestra sus niveles/costes. El nivel de habilidad, el nivel del encantamiento, la potencia de estanterías y la experiencia gastada son requisitos diferentes.

Fuentes consultadas el 2026-10-14:
- https://hypixel.net/threads/skyblock-enchanting-mega-overhaul.3604521/ (explicación oficial histórica, 2020).
- https://hypixel-skyblock.fandom.com/wiki/Enchantments (wiki comunitaria, resultados indexados; el acceso directo devolvió bloqueo).
- https://hypixel-skyblock.fandom.com/wiki/Enchanting (wiki comunitaria, resultados indexados; el acceso directo devolvió bloqueo).
- https://hypixel.net/threads/end-of-the-official-hypixel-wiki-july-2026.6112020/ (cierre de wiki oficial).

En Test se observaron listas de desbloqueos escritas manualmente en reward-messages de enchanting.yml. No equivalen a una autoridad funcional. Las referencias ecoskills de algunas definiciones de enchants inspeccionadas correspondían a maná/descripciones; no prueban una restricción de Encantador. No se concluye que todas las vías estén abiertas: falta inventariar las restricciones efectivas de cada consumidor.

Siguiente incremento propuesto:
1. Auditar las restricciones reales, consumidores y entradas de aplicación/activación de los encantamientos y preservar la distribución actual del balance.
2. Definir un requisito tipado único por encantamiento en su configuración nativa de EcoEnchants. EcoSkills continúa siendo autoridad del nivel de Encantador; no duplicar perfiles, progreso o tablas. Ausencia de requisito no inventa una nueva prohibición.
3. Publicar datos propios mediante API de EcoEnchants para que la guía, los desbloqueos mostrados por EcoSkills y la futura mesa consuman la misma definición. El índice de desbloqueos se prepara al cargar/reload, sin recorridos globales continuos en combate.
4. Generar los desbloqueos mostrados por nivel de habilidad desde esa autoridad, sustituyendo solamente las listas de nombres manuales; preservar las demás recompensas, XP y callbacks, incluido el puente final de vPets.
5. En la futura mesa: objeto compatible -> encantamiento -> cuadrícula de niveles disponibles juntos, con coste/requisito/estado. No habilitar tiers especiales por copiar límites de Hypixel. Sin precios ni estanterías inventadas.
6. Validar requisitos y cobro al aplicar, con devolución segura, repetición de clic, desconexión y pago fallido. El lore de bloqueo no sustituye la validación funcional. La XP de skill usa callbacks nativos exactamente una vez.

Bloquear la aplicación y bloquear el uso de encantamientos ya existentes son decisiones distintas. Hypixel contempla ambas, pero desactivar efectos actuales en Survival requiere aprobación de balance; no está incluido en el incremento de guía. No se modificaron mecánicas, experiencia, recompensas ni vPets en esta tarea.

SignGUI: se consultaron README y VersionMatcher documentado de Rapha149/SignGUI. Declara 1.8–1.21 y wrappers hasta 1_21_R5; no se verificó un adaptador 26.2. Se conserva la búsqueda privada por chat autorizada. Integrar cartel exige verificar un proveedor/adaptador real contra Leaf 26.2 antes de entregar; no modificar NMS de eco por conveniencia.
