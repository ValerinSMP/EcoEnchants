# Incremento 5: utilidades y golpes iniciales (2026-10-20)

Fuente canónica personal. Candidatos únicamente: no instalar ni activar desde este chat. Paquete incremental sobre NATIVE-ADDITIVE-HANDOFF.md; conserva compra/XP, firmas anteriores y EcoSkills byte idéntico.

| ID | Encantador | Tiers | Mecánica |
|---|---:|---|---|
| experience | VIII | I–V | 12.5% por tier de duplicar XP de orbes del monstruo, una sola vez por evento/familia; no dinero ni XP de skill. |
| mana_steal | XX | I–III | Recupera .25% del máximo de maná por tier tras daño directo confirmado, nunca vida; las fracciones se acumulan exactamente dentro de la sesión. |
| triple_strike | XIX | I–V | +8/16/24/32/40% aditivo en los primeros tres intentos no rechazados por jugador/sesión/objetivo. Intercambiar arma no reinicia. |
| swarm | XXXV | I–V | +.5/1/1.5/2/2.5% aditivo por monstruo válido a diez bloques, máximo diez. |

Targets, conflictos, tipos, lore configurable y hash de origen/candidato: delivery/native-expansion-20261020/utility-fixes/manifest.json y cuatro YAML completos. No son fichas de compra; el chat dueño publica ofertas. No modificar el skills YAML ni sus nueve movimientos.

## Contrato técnico

Los cuatro efectos registrados son double_mob_xp, mana_recovery, first_hits_multiplier y swarm_multiplier. Solo los dos últimos contribuyen a ecoenchants_normal; no aplican otro multiplicador después del finalizador. Secundarios/comandos/protegidos no autorizan esas contribuciones. Triple golpe usa el mismo ticket de OriginalRuntime: rechazo explícito devuelve cuota; resultado incierto la consume, nunca repite a ciegas. Limpieza por sesión/objetivo, límites 32768 pares/256 por actor/1024 pendientes.

XP: EditableXpDropEvent.claimAdjustment(String) añade una reclamación transitoria acotada (256), sin alterar constructor, XP original/cancelación ni callbacks. El efecto modifica el XP vivo de entity_xp_drop y no inventa recibos durables. Se preserva el orden nativo de telequinesis.

Maná: resuelve una vez por instancia real de EcoSkills sus APIs públicas peekObservedMagic(UUID,MagicType), getMaxMagic(Player,MagicType), setMagic(Player,MagicType,int), y MagicTypes.getByID. Hay invocación reflectiva en cada recuperación, no búsquedas de métodos por golpe. Caché fría/ausencia no crean perfiles ni defaults. Fracción exacta tier/400, máximo 512 jugadores por defecto; se descarta en sesión/mundo/muerte/reload/pool lleno. Escritura única seguida de lectura nativa: excepción posterior a escritura confirmada no vuelve a conceder; incertidumbre descarta fracción. No persistencia nueva, curación ni cambio del pool Int de EcoSkills.

Enjambre: eco BoundedLivingQueries.nearby(Location,double,int), MAIN, API aditiva con disponibilidad AVAILABLE/UNSUPPORTED/UNAVAILABLE. La lista es copia estructural inmutable de referencias Bukkit vivas (no snapshot). Adaptador común nativo usa Level.getEntities(..., List, int), máximo+1 para detectar truncamiento; no carga chunks. Inicialización por CommonsProvider.setIfNeeded existente, en los cinco namespaces. Consulta por golpe con presupuesto default 8/tick configurable0–32; candidatos default128 configurable16–256, radio10. Diez testigos vivos se reutilizan solo tras revalidar cada uno, sesión/mundo/distancia/exclusiones/protección. Menos de diez con truncamiento es desconocido y no concede bonus. No polling, SQL ni tareas por tooltip. Fallo de adaptador se registra con causa completa y deshabilita hasta nuevo binding.

## Referencia y balance

Contrastado 2026-10-20: https://hypixelskyblock.minecraft.wiki/w/Triple-Strike (normal I–IV10–40%, V50 especial). Aquí cinco tiers normales llegan a40%; conflictos locales intactos, no se copia la exclusión FirstStrike de Hypixel. https://hypixelskyblock.minecraft.wiki/w/Swarm es ultimate de mazmorra, sin tier normal de mesa; aquí sigue normal y máximo25%, no100%. Nombres iguales no significan mecánicas iguales. La wiki oficial redirige a su anuncio de cierre de julio2026; referencias comunitarias, no prueba de runtime Hypixel.

Con misma base B y sin otras diferencias, cada contribución alpha se suma: daño bruto B*(1+sum(alpha)). Armadura posterior puede cambiar la razón final; no se promete ratio absoluto entre servidores. No cap global nuevo ni rebaja de items existentes.

## Verificación y límites

- eco78, LibreForge166, EcoEnchants96 pruebas JVM/mocks:340, cero fallos. Incluyen fracciones todos los tiers, lecturas/escritura única e incertidumbre, primeros tres, duplicados/cancelación/limpieza/límites, reclamo XP y API bounded.
- Composición selectiva:75 clases cambiadas/2762 referencias propias verificadas; fuera de los miembros seleccionados se preservan ZIP entries. Sin incluir fuentes ajenas AI/target/XP en shadowJars completos.
- Smoke JVM sobre JAR compuestos: enlace Kotlin sombreado, callbacks/procedencia/restauración, aritmética fraccional y firmas reales EcoSkills pasan. No servidor iniciado.
- Compilación API/core/common y adapters1.21.8/1.21.10/1.21.11/26.1.2 pasó. Compilación completa v26_2 no ejecutable offline por paper-api26.2.build.134-stable ausente: no actualizar dependencias para ocultarlo. No se cambió código específico26.2: común compilado se remapeó a los cinco namespaces sobre baseline; comprobadas estáticamente las cuatro referencias NMS contra los binarios disponibles, incluyendo26.2. No equivale a ejecución del adaptador en mundo.
- Pendiente: arranque real/instalación de binding NMS, invocación de bridge maná en servidor, ordende death/telequinesis, cancelaciones de otros plugins, equipar/quitar, reconexión/transferencias, MySQL/XP/compra y gameplay. No rendimiento medido: falta carga60–100 jugadores/30–40 activos.

## Artefactos finales de este lote

- `eco-2026.28-native-utility-all.jar` SHA256 `5ca36aa859cef009156cef5e821ad7907e62f11006c8cde7382130799542544f`.
- `LibreForge-2026.27-native-utility-all.jar` SHA256 `4c421008229464db550168b1edaee48ea5dae0a29e85c0dd8c940ebcb1ab0637`.
- `EcoEnchants-2026.27-native-utility-all.jar` SHA256 `b0f70932411a9322ba62853a356fbc766625f847533d3cb93b5a49e31be7f76c`.
- EcoSkills sin cambios: requisito `0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca`.
