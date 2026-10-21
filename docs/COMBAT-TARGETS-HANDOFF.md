# Objetivos de combate EcoSuite — contrato para vEnchanting

## Entrega y límite

Candidato de EcoEnchants + LibreForge. Instalar ambos juntos solamente cuando el chat dueño de Dev coordine la integración. Esta entrega NO instala, activa, recarga, reinicia ni consulta/escribe SQL. No sustituye eco/EcoSkills, no modifica vEnchanting/vPets y no cambia SDK/contratos de compra/XP. Los hashes exactos están en runtime-manifest.json y SHA256SUMS de la entrega.

Baseline comprobado en disco en ambos Dev: EE78eff168… y LFcc3a4c06…. No prueba por sí solo qué clases tiene cargadas la JVM. El compuesto conserva literalmente todos los assets/configs y clases ajenos al conjunto revisado, incluidos targets de objetos y BoundCounter. No se utiliza el shadowJar completo con trabajo ajeno sin entregar.

## YAML por enchant

```yaml
combat-mode: both
combat-targets:
  pvp: true
  pve: true
  # Sin include-types: cualquier especie viva atacable.
  # include-types: [COW, SHEEP, ZOMBIE, PLAYER]
  exclude-types: [VILLAGER]
```

`combat-targets` es opcional y no reemplaza `targets`, que sigue siendo el objeto compatible. Tipos Bukkit EntityType sin distinguir mayúsculas; se admite prefijo minecraft:. Las listas son especies/tipos concretos; sin listas no se introduce una allowlist. `pvp`/`pve` heredan true. `combat-mode` conserva both/pvp/pve y se combina por intersección: ninguna opción amplía un veto anterior. Excluir prevalece sobre incluir. Claves, tipos y estructuras inválidos, selección explícita vacía o contradicción sin objetivos se diagnostican y cierran el combate de ese enchant. Un flag true incompatible con combat-mode se diagnostica y queda restringido por el modo.

La política se prepara al construir la categoría; reload crea otra instancia. El camino por golpe no lee/parsea esa sección. Se conserva la firma del constructor de EcoEnchantLevel. La extensión opt-in de LibreForge es CombatTargetHolder; los holders de otros plugins no implementan esa interfaz y conservan sus rutas.

## Alcance efectivo

Para efectos con un rival: se aplica la misma política en el camino común de efectos, en los procs nativos y en cada receptor secundario. El origen de aparición, nombre, scoreboard tags, PDC arbitrario, MythicMobs o condición de jefe no son vetos de esta política. No se sustituyen inmunidades ni se fuerzan resultados de otros plugins.

Siempre se conservan cancelaciones de combate, protección Antigrief, mundo PvP, equipo/aliados, propiedad Tameable, NPC, marcador propietario verificado vpets:pet-owner, invulnerabilidad, autoataque, mundo/sesión y Creativo/Espectador como destinatarios. No son objetivos decoraciones/ArmorStand/Mannequin ni entidades no vivas. Las restricciones de la mecánica y sus filtros YAML siguen estrechando la selección: Cubismo no se vuelve universal; strip_ai exige Mob, Titan Killer mantiene armadura, especies/dimensiones/condiciones de otros efectos no se borran.

Los bonos permanentes de estadísticas/atributos al portador NO reciben un enemigo como argumento y se conservan como bonos del portador. La sección no convierte esos atributos en estadísticas diferentes por enemigo. Los triggers y filtros antigranja de recompensas, como Sustento y XP, permanecen: esta entrega amplía elegibilidad de combate, no las reglas de recompensas. El manifiesto distingue estas familias; no declara que un bono pasivo o un trigger de XP esté sujeto a un rival inexistente.

## Ejecución, sesión y efectos pendientes

- La atribución original usa originalPlayer, holder y el contexto que ya copia TriggerData; el contexto se prepara antes de mutadores/delay, se conserva al copiar y se retira cuando cambia el proveedor. No se deduce el dueño desde use_source, que puede ser false.
- Se captura el token de sesión existente de OriginalRuntime mediante una lectura sin crear sesiones. Delays, repeticiones, áreas, cadenas, DoT y control revalidan al ejecutar; reload/cambio de categoría impide usar el holder anterior. El scope se restaura también ante excepciones.
- Las rutas nativas siguen capturando arma/arco/sesión y daño real con sus mecanismos anteriores. No se toca su ledger, porcentajes, cooldowns, requisitos, XP ni presupuesto VFX. Eco resonante y Brasa siguen mostrando únicamente sus resultados confirmados.
- Nubes registradas filtran la lista real de receptores de AreaEffectCloudApplyEvent. Los rayos mantienen el contexto de la llamada diferida y se filtra su daño real a entidades. Explosiones, controles y daño de rutas legacy comunes conservan el contexto necesario, sin cambiar sus cantidades.
- La nube de Espectralidad procede de un evento de muerte: conserva esa atribución y su duración comprometida aunque el dueño reaparezca en el mismo mundo. Es una acción póstuma existente, no un ataque nuevo ni un bypass de protecciones. Las acciones vivas antiguas se invalidan al cambiar de sesión. Quit/mundo/removal/disable retiran las entidades registradas; el respawn retira las de acciones vivas. La nube póstuma sigue pasando por las mismas protecciones y categorías actuales.
- Registro acotado a 512 nubes/rayos propios. Si está lleno, el nuevo efímero se retira; nunca se deja activo sin política. Se limpia por eventos y no añade un escaneo de jugadores/entidades por tick. No hay medición Spark de la nueva política: no afirmar impacto cero ni mejora de ms.

## Validación y aceptación de Dev

Las pruebas de módulo, del compuesto, hashes y ABI constan en VALIDATION.json. La ejecución aislada del compuesto usa configuración/entidades simuladas; el bootstrap llama handleLoad real con almacenamiento de registros simulado, NO handleEnable ni un servidor Paper. No equivale a prueba en juego.

El manifiesto JSON/Markdown recorre 129 IDs de YAML actual de ambos Dev (coincidían por hash al leerlos), e incluye sus efectos, filtros, condiciones, modo y vía. Es inventario de configuración, no prueba de categorías cargadas en memoria. No se sobrescriben sus colores, tiers, importes o YAML de skills.

Pendiente exclusivamente de la integración real: arranque de ambos Dev; vaca/oveja/zombie/jugador como ancla y secundarios, spawner/crianza/huevo/comando, nombre/tags, Mythic/jefes atacables e inmunes; región/PvP/equipo/NPC/mascota/invulnerable; cambios de YAML/reload, muerte/nube/respawn, transferencia/reconexión y cancelación tardía. Probar Eco resonante/Brasa/Sello, cadenas, periódicos, Cubismo/IA y beneficios al dueño. Confirmar compras y XP sin repetición por sus contratos existentes. Medir con Spark bajo carga representativa (60–100 usuarios, 30–40 activos), sin presentar la propuesta como rendimiento medido.
