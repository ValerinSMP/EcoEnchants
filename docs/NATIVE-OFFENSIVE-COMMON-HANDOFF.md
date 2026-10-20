# Incremento6: multiplicadores ofensivos normales —2026-10-20

Once candidatos completos en delivery/native-expansion-20261020/offensive-common. Solo YAML, sin JAR adicional; requiere los tres JAR coordinados del incremento5 y EcoSkills pinned. No instalación/lifecycle.

Se preservan exactamente fórmulas/targets/conflictos/tipo/max-level/triggers/filtros y todas las condiciones existentes (incluidos mundos). Añadido solo args.damage_group:ecoenchants_normal y enabled:true, y corregidos textos que describían otra escala. Niveles autoritativos contrastados contra levels.json, sin escribir skills YAML ni fichas de compra.

| ID | Tiers | alpha local (se suma, no producto) |
|---|---|---|
|boss_hunter|I–VIII|.1L solo Boss/Warden/ElderGuardian según filtro nativo; no promete identificar todo MythicBoss|
|end_affinity|I–IV|.2+.1L solo world_the_end|
|nether_affinity|I–IV|.2+.1L solo world_nether|
|finishing|I–V|.1L*(1-Hv/Hmax)|
|first_strike|I–V|.2+.12L solo víctima a salud máxima; no es contador de primer impacto|
|fortitude|I–II|.02L por holder elegible de armadura|
|sculk_slayer|I–V|.2+.1L solo world_warden|
|skull_puncture|I–III|.1L solo headshot nativo|
|snipe|I–IV|distance/(11-ceil(L/2))*.01L, distancia actual atacante-víctima; no trayectoria acumulada|
|wyvern|I–II|.15L cuerpo a cuerpo; lore antes decía10L incorrectamente|
|prosecute|I–V|.05L*Hv/Hmax; lore antes confundía puntos porcentuales con proporción|

Cada tier, requisito Encantador, hash fuente/candidato y fórmula está en manifest.json. Sin cap global nuevo. Sniper conserva escalado por distancia sin nuevo cap inventado: debe medirse junto a Rapid y los procs pendientes, no declararlo balance global terminado.

Referencia comunitaria consultada2026-10-20: https://hypixelskyblock.minecraft.wiki/w/Execute (normal I–V, coeficiente0.2–1% por punto porcentual faltante, VI especial); https://hypixelskyblock.minecraft.wiki/w/Prosecute (normal I–V0.1/0.2/0.3/0.4/0.7 por punto porcentual restante, VI especial); https://hypixelskyblock.minecraft.wiki/w/First_Strike (normal I–IV25–100%, V125 especial). Local finishing .1L sobre fracción es coeficiente0.1–0.5% por punto; prosecute .05L equivale0.05–0.25%, con la misma fracción. FirstStrike local cinco tiers32–80%, diferente activación; no comparación exacta tier-a-tier para los cinco. Otros IDs: equivalencia Hypixel no establecida, no se atribuyen cifras de otro efecto por compartir palabra. La wiki oficial cerró; no se presenta fuente comunitaria como oficial ni prueba de runtime.

Ejemplo común: B100, WyvernII(+30%), FortitudeII una pieza(+4%), FirstStrikeV(+80%) sobre objetivo lleno =>214 bruto en vez de243.36 del producto. Si son cuatro holders de armadura distintos válidos =>226 bruto; la política de deduplicación por holder se conserva. Fuerza/crítico/planos forman la base nativa procesada; armadura/reducciones posteriores pueden cambiar la razón final, no se aplica defensa dos veces. Ferocidad reutiliza daño final bruto con ruta RepeatedDamage sin contar bonus de nuevo, según incremento4. Grupos no autorizados/procs/comandos/cancelados no contribuyen. Curaciones/reflejos/procs y Combo/Thunderlord/TitanKiller continúan auditoría: no afirmar que el balance de todas las familias está listo.

Validación realizada: parse/hash y preservación estructural exacta de todos los filtros/condiciones/targets/conflictos/tipo/tiers; once niveles contra lista130; efecto/filtros/condiciones/placeholders consultados en fuente real. No es compilación del YAML en servidor ni gameplay. Ruta de grupo compilada/probada en incremento5, sin cambios de código aquí. Pendiente Test y combinaciones reales/carga60–100 jugadores,30–40 activos. No instalación/activación/medición de TPS.
