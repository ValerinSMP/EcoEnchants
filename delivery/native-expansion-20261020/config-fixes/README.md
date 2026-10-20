# Correcciones de configuración — incremento 3

Candidatos completos sobre el YAML real de Test inspeccionado el20/10; no instalados. manifest.json incluye hash origen/candidato, ruta relativa, nivel aprobado de levels.json, tiers, targets, conflictos y explicación. No sustituir archivos si el hash origen cambió: revisar la diferencia. No instalar carpetas de entrega enteras ni retirar otros encantamientos.

- metabolism I–II, EncantadorX: food_multiplier1.125/1.25 en vez0.125/0.25; ceil del efecto nativo conservado.
- neptune I–III, XXXIII: crit_chance por0.65/0.70/0.75, con crit_damage30/40/50 y restantes contratos intactos.
- getaway I–II, VII: movement_speed_multiplier1.1/1.2 solo con below_health_percent20. El comparador nativo es <=20%; lore refleja eso.
- poison_ivy I–II, XI: take_entity_damage proporciona atacante en victim. Se retira victim_as_player y apply_to_player:false apunta a cualquier atacante vivo admitido por ese trigger; no al portador. Duración min(5*level*daño,80+20*level) ticks, PoisonI: 0.5/1s por corazón, máximos5/6s. El trigger nativo excluye THORNS y atacantes MythicMobs como antes; no se amplía a take_mythic_damage por inferencia. Conflicto thorns intacto.

Contraste de fuentes: EffectFoodMultiplier, EffectPotionEffect, ConditionBelowHealthPercent, MutatorVictimAsPlayer y TriggerTakeEntityDamage. Config SeparatorAmbivalent admite apply-to-player; no es error de separador. La configuración anterior de poison_ivy no envenenaba al portador en esta ruta: sí dejaba fuera atacantes no Player y tenía una duración máxima mal formulada. blast_mining/flashbang/sparks sin id exterior siguen siendo cadenas válidas, no se eliminan.

Ejecutado: parse YAML, preservación de targets/conflictos/max-level, contraste de argumentos/operaciones con el código nativo y cálculos de tiers. No requiere nuevo JAR para estas cuatro correcciones; no es una prueba de compilación de efectos en Paper. Pendiente: equipar/quitar/requisitos, alimento/redondeo/saturación, críticos reales, cruce20%/curación y veneno en Player/monstruo/proyectil/protección en Test. Sin pruebas MySQL ni juego, sin instalación/reload.

Aún pendientes: experience requiere editar XP del evento nativo entity_xp_drop (give_money es incorrecto; kill ofrece salud máxima, no cantidad de XP), y mana_steal debe usar el límite real y la granularidad Int del sistema mágico existente, sin inventar mínimos ni conceder salud. No se sustituyen por fórmulas que parezcan correctas sin respetar esos contratos. Agrupación ofensiva, restantes correcciones y familias faltantes siguen pendientes.
