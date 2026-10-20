# Incremento7: pasivos de atributos y estadísticas —2026-10-20

Once YAML completos y contrato por ID/tier en delivery/native-expansion-20261020/passive-stats/manifest.json. No JAR adicional, instalación ni lifecycle. Requiere paquete coordinado incremento5 + EcoSkills requirement-views idéntico. Preserva targets/conflictos/tipo/tiers/efectos, no tablas de skills, recompensas ni XP durable.

| ID | Efecto nativo conservado por tier L |
|---|---|
|crit_luck|crit_chance +4L|
|criticals|crit_chance +5L|
|forceful_crit|crit_damage +3+2L|
|might|strength -1+4L|
|overload|crit_chance y crit_damage +L cada uno|
|steel_string|strength +3+2L|
|strongarm|strength +2+4L|
|vicious|ferocity +5+5L|
|dexterity|attack_speed_multiplier 1+.1L|
|wisdom|xp_multiplier 1+.2L; lore corregido: bonus20L%, no120L%|
|caffeinated|attack_speed_multiplier 1+.05L, ahora condición nativa is_sprinting requerida por su lore|

Los add_stat declaran EcoSkills; no ficticia disponibilidad si no está. Native EffectAddStat usa UUID por lookupKey+stat, retira antes de añadir y limpia al deshabilitar holder; estadísticas Double/actualización nativa siguen intactas. Sin snapshots/SQL/refresh por tooltip ni cambios al puente final. Estos stats forman la base nativa de golpe; no se vuelven a sumar como alpha en el grupo ofensivo. CritChance/CritDamage/Strength/Ferocity mantienen las reglas del propio EcoSkills. Vicious aumenta frecuencia potencial de replay con ruta RepeatedDamage del lote4; no se promete ahorro de CPU ni una cadencia medida.

No existe equivalencia Hypixel normal verificada para estos once efectos locales. Critical de Hypixel no es la probabilidad de crítico local, ni Overload local implementa automáticamente sus mecánicas por llamarse igual. No se presentan tiers de tienda/mazmorra de Hypixel como máximos normales nuestros. Valores locales preservados para no inventar una referencia; balance combinado de estas familias y las pendientes requiere evaluación completa, no se declara terminado.

Verificación: parse/hash, niveles actuales, estructura original de efectos/targets/conflictos/tipo/tiers, registros nativos y camino de adición/retirada consultados en fuente. Sin build extra de código sin cambios ni nuevas pruebas que reproduzcan YAML. Validación en Paper/Test de cooldown/holders/correr/equipar/quitar/reload y velocidad de ataque sigue pendiente; no hacer pruebas o prometer un arreglo del ticket de espada sin captura real. Wisdom actúa sobre XP de Minecraft natural/telequinesis (ceil nativo), no XP de skill, fórmula/recibo de vEnchanting o vPets. La interacción Experience/Wisdom/Telequinesis requiere observar el orden real de callbacks en Test.
