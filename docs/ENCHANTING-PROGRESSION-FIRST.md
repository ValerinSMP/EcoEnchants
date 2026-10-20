> HISTÓRICO: sustituido por [ORIGINAL-PROCS-RUNTIME.md](ORIGINAL-PROCS-RUNTIME.md).
> Toque Infernal requiere XII (5b6ea99), no V. No aplicar el skills YAML antiguo.
> Los bloqueos descritos abajo corresponden al primer incremento.

# Progresión de Encantador — primer incremento, 2026-10-20

## Estado exacto
Entrega parcial: dos familias existentes auditadas/configuradas y núcleo puro de reglas para las dos primeras originales. NO están implementados ni habilitados sus efectos completos. No anunciar Eco resonante/Sello glacial como disponibles ni incorporarlos a compras.
No instalación, lifecycle, SQL real, cambios productivos ni modificación de otros plugins.

## Familias entregadas
- angler/Pescador I–II, requisito EncantadorIV para todos sus tiers:15/30% más XP de la skill fishing. No es XP vanilla. skill_xp_multiplier aplica el multiplicador real a PlayerSkillXPGainEvent; no se concede XP nueva ni se altera el puente final. Defaults anteriores del repositorio decían8% pero calculaban5%; candidato adopta15% por nivel de la configuración Test actual.
- infernal_touch/Toque Infernal I, requisitoV: autosmelt sobre block_item_drop; pico; conflicto silk_touch. drop_xp:true conserva la XP nativa de fundición existente, no añade otra concesión. No cooldown inventado en un efecto pasivo. No cambiar fortune/parser ni recetas upstream.
- eco_resonante I–III: núcleo de tres golpes confirmados por jugador/objeto/objetivo/mundo en5s; valores4/6/8%, origen daño final, CD8s por jugador/familia, límite2 objetivos/radio2/16 partículas como especificación del intent.
- sello_glacial I–III: núcleo de dos golpes confirmados en4s;10/15/20%, duración40ticks y CD10s por jugador/familia. No se simula con SlownessI porque no representaría porcentajes exactos.

## Bloqueos de los originales y límite de implementación
Faltan adaptación de golpe realmente aplicado tras cancelaciones/invulnerabilidad, clasificación autoritativa de TODOS los jefes propios y origen/identidad estable del objeto disparado. TriggerBowAttack no aporta ITEM: no es seguro atribuir la flecha al arma sostenida en el impacto.
OriginalProcEngine emite un intent puro, no daño ni modificadores ni partículas. Sus entradas de confianza deben provenir de esos adaptadores antes de conectarlo; sus booleanos no son un mecanismo configurable de disponibilidad.
No se registra ningún efecto ficticio, no hay onTrigger vacío que aparente una mecánica implementada ni efectos inexistentes en YAML. El diagnóstico de bloqueo se emite una vez al habilitar EcoEnchants.
Los archivos _eco_resonante.yml y _sello_glacial.yml están excluidos por el prefijo NATIVO _. El loader inspeccionado NO filtra enabled:false; se conserva esa bandera descriptiva además de las flags de descubrimiento/comercio. Contienen effects:[] y no pueden tratarse como definiciones ejecutables. No quitar el prefijo para activarlos.
Pendientes21 familias originales en ejecución,19 todavía sin núcleo específico. Brasa errante/Marca celeste/Guardia de cristal son el siguiente grupo tras resolver adaptadores. No se ocultó su estado detrás de una opción true/ready ficticia.

## Núcleo de estado y límites
Sin polling ni tareas por tick. Ventanas desde el primer golpe confirmado, con TTL fijo y expiración perezosa. Límite máximo4096 por mapa: hasta5 mapas (dedup global, ventanas y cooldown de2 familias),20480 entradas como techo. Cooldown lleno rechaza nuevas admisiones en vez de expulsar uno vigente. Dedup1s; los eventos rechazados no consumen estado.
Identidad de item obligatoria (UUID), todavía no vinculada al runtime. Cooldown no depende de arma/objetivo. clearEntity limpia contadores por muerte/quit/mundo/transferencia; conserva cooldown local hastaTTL. clear elimina todo al cerrar. La transferencia real entre nodos y preservación de cooldown entre procesos siguen pendientes; no hay perfil persistente paralelo.
No hay ejecutor secundario: no procs recursivos ni daño/PvP, drops, premios, XP, rayos, fuego, entidades persistentes o tareas visuales nuevas. Presupuestos de partículas por observador/global, LoS y daño secundario marcado no están implementados; caps del intent no equivalen a presupuestos reales.
No se activan defensivos ni existe stacking nuevo. Antes del próximo incremento defensivo debe concretarse prioridad y cap combinado.

## Arquitectura y reutilización comprobadas
Solo se edita EcoEnchants. Se reutilizan skill_xp_multiplier/EcoSkills y autosmelt/LibreForge sin editar sus repos. El contador nativo %hits% no separa objeto ni tiene TTL acotado del diseño; damage_nearby_entities no aporta por sí solo límite2+LoS+exclusiones exigidas. No se sustituyeron por efectos parecidos.
EcoEnchant.conflictsWith ya verifica ambos sentidos; validación/aplicación pública existente conserva esa ruta. Los conflictos planificados pertenecen a YAML EcoEnchants, nunca a un catálogo paralelo del addon. Referencias a futuros IDs solo están en ejemplos excluidos; requieren resolución al implementar esas familias.
Runtime instalado contrastado: EcoEnchants7dd5c987…, LibreForge93d6f805…, EcoSkills0f5d7c18…. Se guardó bytecode de los dos efectos instalados: angler multiplica gainedXP; autosmelt usa FurnaceRecipe y EditableDropEvent.addModifier/drop_xp. Los hashes de compilación local son diferentes por shading; no se asumió identidad binaria de clase sin contrastar.
API pública de mejora y SDK permanecen byte a byte idénticos. No se toca contrato durable de compra/XP, vPets ni configuración monetaria.

## Pruebas y entrega
52 pruebas JVM,14 nuevas;0 fallos/errores/omisiones. Confirman thresholds/porcentajes/límites del intent, item/objetivo, TTL exacto, cooldown ante cambio de arma/limpieza, dobles eventos, rechazo de estados inseguros, capacidad y MAIN. YAML de originales no anuncia disponibilidad ni referencia efectos no implementados.
No son golpes reales, confirmación Paper de daño, protección de plugins, LoS, partículas, NPC/mascotas/jefes reales, transferencia ni MySQL/Test/gameplay. Esas pruebas están pendientes con sus adaptadores, no aprobadas por los tests puros.
Build personal con ejecutor2CPU/4GiB/JDK25, release21 y toolchain nativa. Runtime compuesto sobre el JAR instalado, sin reconstruir entradas NMS/loaders/cambios ajenos. Script tools/compose-first-progression.py comprueba hash de base y conserva API.
Artefacto, hashes, YAML externo EcoSkills y manifiesto vEnchanting están en la entrega staging. El YAML de EcoSkills filtra únicamente anuncios: conserva SustentoI, II sin custom, CubismoIII y permite PescadorIV/ToqueInfernalV; no modifica curva, stats, moneda, multiplicadores ni recompensas.
