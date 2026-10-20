# Handoff para vEnchanting — elegibilidad de originales, 2026-10-20

Sustituye la política NATURAL-only del handoff de 29c25061. No requiere cambios
ni excepciones en vEnchanting; la autoridad sigue siendo el filtro nativo de EcoEnchants.

- `eco_resonante` I–III, espada, Encantador XVI: `resonant_pulse`, trigger `confirmed_original_hit`, 3 golpes en 5 s, 4/6/8% de daño confirmado a dos objetivos en radio 2, CD8 s, conflicto cleave.
- `sello_glacial` I–III, arco, Encantador XVIII: `glacial_seal`, mismo trigger, 2 impactos en 4 s, −10/15/20% velocidad por 40 ticks, CD10 s, conflicto frost.

Ahora se admiten monstruos vanilla NATURAL, COMMAND (/summon), SPAWNER_EGG y
DISPENSE_EGG (huevos usados a mano o dispensados), siempre que pasen TODAS las
exclusiones. No habilita mobs de spawner/trial spawner, CUSTOM de plugins ni
orígenes desconocidos. Todos los MythicMobs y jefes quedan fuera aunque nazcan
mediante comandos; también jugadores, NPC, aliados, mascotas, PDC, tags, nombres,
invulnerables y protegidos. Sin bypass de owner.

Pruebas sugeridas tras integración: zombie natural, `/summon minecraft:zombie`
y huevo de zombie deben contar. Repetir contra spawner/trial spawner, jefe
invocado, Mythic invocado, objetivo con nombre/tag/PDC y protegido: no deben contar.
Respetar Encantador XVI/XVIII y la guarda de llegada 8/10 s; confirmar pulsos,
retirada del sello y arco original al cambiar de arma después del disparo.

Compra/mejora/XP: contratos previos SIN CAMBIOS, incluidas sus clases API. Mantener
la confirmación genuina de compra en vEnchanting y la autoridad nativa de requisitos,
conflictos y efectos. Toque Infernal requiere XII, no V; no sustituir skills por
un YAML histórico que revierta los nueve movimientos aprobados en 5b6ea99.

Entrega: runtime EcoEnchants-2026.27-original-procs-eligibility-all.jar y dos YAML
activos. Descripciones cambian únicamente «monstruo natural» → «monstruo»; valores
y colores intactos. Frente al JAR padre únicamente difieren el filtro compilado
y esos dos recursos. Ver runtime-manifest.json, integration-contract.json y SHA256SUMS.

74 pruebas JVM/Mockito aprobadas, comparación de todas las entradas y cierre
estático interno; no prueba real de arranque/gameplay ni rendimiento medido.
NO instalado ni lifecycle por esta entrega. La integración se realiza en el chat
vEnchanting con la autorización y verificación correspondientes.
