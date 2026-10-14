# Mesa seleccionable de encantamientos — propuesta y handoff

Fecha: 2026-10-14. Propuesta no implementada ni precios aprobados. Solo la guía existente fue entregada a Custom/Sync por instrucción explícita; no se bloqueó la mesa ni se habilitaron compras. El propietario realiza todos los reinicios/recargas.

## Entrega de la guía

Fuentes canónicas en personal: `/root/development/projects/eco-suite/EcoEnchants`, branch valerin. Commit de implementación 74ca6821. Cambios ajenos no versionados conservados. Runtime construido previamente mediante ejecutor limitado/toolchain nativa (25/release21); 32 tests JVM pasaron. Overlay de menú preserva mecánicas/NMS del baseline: 693 entradas originales idénticas, ABI GUI comprobada y dependencias de 11 clases verificadas. No nueva build en esta entrega.

Instalado en disco Custom y Sync: `EcoEnchants-2026.27-all.jar`, SHA256 `64213413c86fe6d247ea1e4081de50a654716326d935449a22c2549ab2a14c08`. Configuración SHA256 de ambos `182f867a33111d3c2e901eebe0baef07bcedcfeac4abf75268f146f333079532`. Se cambiaron únicamente enchant-gui/enchantinfo/enchant-guide conservando el resto de configuración de cada nodo. Retirados 13 YAML por nodo de 11 IDs explícitamente desactivados; las demás definiciones intactas. Backup verificado HDD `/mnt/storage/backups/ai/survival-custom/ecoenchants-guide-production-20261014T182127Z`; manifiesto de artefactos/retiradas y reversión allí y en staging `enchants-guide-production-20261014/installation.json`.

No lifecycle realizado. Arranque/registro/juego en producción pendientes. El propietario considera listo el menú de Test; el fallo de lectura de Encantador que muestra «No disponible» sigue sin causa exacta confirmada ni corrección, y se conserva explícitamente en esta entrega. No usar ese dato ausente para autorizar una futura compra.

## Evidencia local vigente

- Tras la retirada quedan 104 YAML en cada nodo: 103 elegibles únicos + ejemplo. Distribución de max-level: 12 nivel1, 18 nivel2, 21 nivel3, 12 nivel4, 32 nivel5, 5 nivel6, 2 nivel7 y 1 nivel8. Conteo de configuraciones, no registro activo.
- En Custom `enchanting-table.enabled: false`; se conservó. Es el interruptor de oferta personalizada de EcoEnchants, NO un bloqueo universal del inventario vanilla. 98 de los 103 YAML declaran enchantable:true: ese indicador local no significa que hoy sean obtenibles en mesa. No convertirlo solo en whitelist de compra.
- El requisito directo has_skill_level/enchanting pertenece a la activación de efectos. canEnchantItem valida compatibilidad/conflictos/encantamientos previos/límite, sin Player. Para la nueva compra se requiere comprobación adicional del nivel; no convertir automáticamente en restricciones nuevas los 19 anuncios manuales sin condición ni modificar umbrales actuales.
- Audit previo `ECOENCHANTS-ENCANTADOR-REQUISITOS-AUDIT-20261014.md` es una foto anterior a las retiradas. Discrepancias anunciadas: Apagón1 vs14, Venenoso1 vs17, Cazador de Sculk11 vs5, Estimulante22 retirado. No corregir recompensas o niveles por inferencia.
- Configuración real de Encantador Custom: solo xp-gain-methods `enchant_item`, multiplier3. El default upstream incluye otra ruta que NO está en ese runtime. LibreForge TriggerEnchantItem escucha EnchantItemEvent no cancelado y envía value=expLevelCost. No asumir que escribir ItemMeta dispara este evento. Conservar callbacks, modificadores y puente XP final de vPets.
- vStore ProductPricing tiene equivalencia nominal 100 gemas por 1 USD, decimal exacta; ejemplo de catálogo inspeccionado `price-usd: 6.39`. Es valoración de catálogo, no mercado ni conversión XP/gemas. Fuente canónica `/root/development/projects/survival-custom/vStore/src/main/java/com/valerinsmp/vstore/domain/ProductPricing.java`. Runtime/API de pagos deberán contrastarse nuevamente antes de cobros.
- vCurrency expone `mutate(UUID operationId, UUID uuid, String playerName, String currencyId, double delta)` asíncrono. Gemas pertenecen a vCurrency, no a Eco; ExcellentEconomy conserva autoridad money. No SQL directo/credenciales/reflejar saldos en un addon.
- Regla de monetización vigente: gemas aceleran progreso, esperas o capacidad, manteniendo funcionalidad básica accesible; no nuevos beneficios VIP por inferencia.

## Qué aporta Hypixel y qué no está demostrado

La publicación oficial histórica describe elección sin azar, requisito de habilidad y potencia de estanterías, retirada por XP, guía separada y cuidado con la progresión por repetición. No copiar sus cambios de daño/recompensas ni el límite de XP histórico como si fueran valores actuales. [Anuncio oficial, diciembre2020](https://hypixel.net/threads/skyblock-enchanting-mega-overhaul.3604521/).

La wiki comunitaria indexada describe poner el objeto, escoger encantamiento y ver sus niveles juntos con costes de XP; cada tier tiene precio/potencia propios y se admiten objetos ya encantados. La sustitución de conflictos existe en ese sistema. Para Valerin se propone confirmación explícita y conservar inicialmente la prohibición nativa, no borrar enchants especiales por copiar esa regla. [Enchantments](https://hypixel-skyblock.fandom.com/wiki/Enchantments), [Enchantment Table](https://hypixel-skyblock.fandom.com/wiki/Enchantment_Table). Acceso directo bloqueado; estos detalles se contrastaron mediante texto indexado, sin prueba dentro de Hypixel.

La wiki oficial cerró y hoy sus rutas redirigen al anuncio; no presentar esos enlaces como consulta de páginas vigentes. [Cierre oficial julio2026](https://hypixel.net/threads/end-of-the-official-hypixel-wiki-july-2026.6112020/). Las fuentes muestran pagos en experiencia para la mesa; la alternativa gemas sería diseño propio de Valerin, no función copiada y verificada de Hypixel. Ninguna fuente revela su implementación interna ni demuestra cuánto lag tendría nuestro addon.

## Flujo recomendado

1. Interactuar con cualquier mesa válida abre el nuevo menú en vez de la interfaz vanilla. Respetar cancelación/protecciones, mano duplicada y mundos excluidos. También validar aperturas por otros plugins: no hacer bucle al abrir nuestro menú. Habilitar sustitución solo cuando addon/catálogo/proveedores estén listos; no poner una mesa inútil si falta dependencia. No activar bloqueo global ahora.
2. Mesa con ranura de objeto a la izquierda, lista compatible a la derecha y botón de guía. La guía existente sigue sin ranura. Vanilla y Eco según registro y catálogo de mesa explícito. Libros/yunque/loot/especiales conservados; no fabricar tier extra ni saltar cap. Definir individualmente qué personalizados entran ahora, porque actualmente están desactivados globalmente en la mesa.
3. Pulsar un enchant abre TODOS sus tiers juntos, incluyendo los 1–8 existentes en una cuadrícula con hasta dos filas; nunca un nivel por página. Muestra efecto, estado actual, requisito únicamente cuando falte y coste. Nivel ya aplicado identificado, inferior no se compra accidentalmente.
4. Aplicar vanilla normal con XP; Eco/custom con shards o gemas; vanilla superior con XP más shards. Conservar Encantador/compatibilidad/cap. No tiers exclusivos gemas, no pagar para saltar requisitos, no beneficios VIP nuevos.
5. No sustituir conflictos/downgrade/retirar enchants en primer incremento. Esa función exige política para enchants especiales y objetos con libro, confirmación y costes separados. No crear XP por retirada/reaplicación ni devolver el precio original de un enchant de origen desconocido.
6. Potencia de estanterías es opcional configurable por tier, inicialmente sin añadir requisitos nuevos al balance. Si se adopta, área acotada y solo chunks cargados, cálculo al abrir/comprar y validación puntual; ninguna búsqueda global o por tick.

## Modelo de pago corregido por el propietario

Esta decisión sustituye los presets anteriores de XP/gemas por rareza: vanilla hasta su límite normal cuesta solo XP de Survival; Eco/custom cuesta shards O gemas, sin añadir XP como tercer coste; vanilla superior al límite normal cuesta XP + una cantidad pequeña de shards. Gemas no permiten saltar Encantador, compatibilidad, niveles permitidos, conflictos ni caps. La alternativa shards mantiene accesible la misma funcionalidad sin pago premium. No imponer el nuevo precio a enchants ya aplicados ni cambiar vías de obtención externas.

Referencia completa de shop: [ECOENCHANTS-MESA-PRECIOS-SHOP-20261014.csv](ECOENCHANTS-MESA-PRECIOS-SHOP-20261014.csv). Extraída de YAML reales de EconomyShopGUI-Premium (no defaults). 103 ofertas de libros por nodo; valores/precios/páginas/monedas equivalentes en Custom/Sync, aunque algunos hashes de archivos difieren. 96 ofertas Eco/custom y7 vanilla superiores. Moneda de libros: EXCELLENT_ECONOMY:shards. Los importes son precios configurados buy del libro, no cotizaciones efectivas con permisos/descuentos de un jugador ni precios de aplicación ya implementados. No usar sell como coste.

| Oferta de libro existente | Nivel | Buy shards |
|---|---:|---:|
| Abrasión | II | 5.000 |
| Robo de vida (lifesteal) | V | 4.500 |
| Apagón (blackout) | IV | 3.500 |
| Eficiencia | VI | 500 |
| Fortuna | IV | 500 |
| Saqueo | IV | 500 |
| Atracción | IV | 500 |
| Suerte marina | IV | 500 |
| Sigilo veloz | IV | 500 |
| Irrompibilidad | VI | 5.000 |

Vanilla normal permanece solo XP. Para los superiores +1 con oferta500, tomar500 shards como candidato adicional a XP; no hacer un precio único para todos los enchants. IrrompibilidadVI no es +1 sobre el máximo normalIII: conserva su oferta especial de5.000 como referencia, no habilitarVI ni inventar precioIV por extrapolación. XP exacta por tier aún debe proponerse y probarse con su política puntos/niveles; no reutilizar XP de skill como moneda.

Para Eco/custom, el tier vendido en shop ancla su tarifa shards al buy existente. Otros tiers y coste incremental entre niveles necesitan un catálogo derivado explícito revisable, no copiar ese precio a todos ni habilitar máximos adicionales. El CSV identifica el tier de cada ancla. Obtener nombres/targets/condiciones de EcoEnchants registrado, no texto de la shop. No fabricar libros revendibles baratos: inicialmente aplicación directa al objeto, sin retirar y devolver libro ni reembolso del precio histórico. Conservar valor y obtención de libros especiales, no borrarlos.

Gemas: propuestas con referencia explícita al catálogo actual de packs de shards de vStore. Valores inspeccionados:800shards/249gemas,1.350/499,2.200/899,3.500/1.299,5.000/1.899,7.500/2.499,11.000/3.499,14.000/3.999. No hay una tasa universal porque los paquetes difieren. Como candidato simple de aplicación directa, `ceil(shards × 249/800)` (pack800), calculado con decimal exacto y configurable; no implementado/aprobado como catálogo definitivo. Ejemplos:3.500shards→1.090gemas,4.500→1.401 y5.000→1.557. El nominal100gemas/USD continúa siendo solo valoración del catálogo. Revisar diferencias frente a todos los paquetes y descuentos antes de aprobar, no crear conversor bidireccional ni arbitrraje.

La shop aún anuncia rumbleIII, cuya definición fue retirada por desactivación. Se incluye como evidencia CSV con definition_present:false, pero se excluye del catálogo candidato de compras. Esta revisión NO retiró esa oferta de la shop. Ocho definiciones sin ancla Eco en el inventario inspeccionado: bastion, carve, crimson_slayer, indestructible, one_for_all, repairing, soulbound, ultimate_wise. Precio no disponible: no inventar oferta. Registro efectivo, caps y permisos a verificar en el nuevo chat.

Shards son autoridad de ExcellentEconomy; gemas de vCurrency. El addon necesita adapters separados, validación/cobro/reembolso seguro por sus APIs y journal durable/custodia del objeto. No usar vCurrency como autoridad de shards ni SQL directo por compartir BD. El resultado de compra debe ser coherente en Custom/Sync; doble click, resultado incierto o transferencia no justifican repetir el débito.

## Addon frente a fork

Recomendación: addon de mesa separado, nombre de trabajo vEnchanting (no creado), con autoridad de EcoEnchants sobre definiciones/compatibilidad/efectos, EcoSkills sobre nivel/XP, vCurrency sobre gemas y ExcellentEconomy sobre shards. Aísla transacciones y nueva UI del fork upstream, permite deshabilitar la mesa sin rehacer EcoSuite y facilita otro chat. Estándares Valerin aplican al addon; no imponerlos a repos EcoSuite.

Antes de implementar, verificar APIs exportadas exactas del JAR, no inventar EcoEnchantsAPI que no existe en la ruta esperada. Interfaces nativas EcoEnchant/EcoEnchantLike/registry/targets y LibreForge refreshHolders son los puntos iniciales. Evitar comandos /enchant para comprar: bypass de compatibilidad/tiers y no asegura XP final. Si falta una operación pública segura o metadatos/requisitos tipados, ese chat entrega solicitud acotada al chat EcoSuite; parche mínimo aquí y ABI conservada. No alterar eco/LibreForge ni meter JDBC/pagos en EcoEnchants por conveniencia.

## Corrección y rendimiento necesarios

- La lectura No disponible debe diagnosticarse con razón separada (capacidad, perfil frío, campo ausente, excepción) y verificarse en runtime antes de compras. Nivel ausente bloquea transacción claramente; no calcularlo desde texto/lore ni asumir0.
- Validación en main de UUID/sesión/slot/item/metadata/versión de catálogo/skill/objetivo/coste/protección. Repetir antes de aplicar después del pago asíncrono; un tooltip no autoriza compra.
- Un operationId durable por compra y protección frente a doble click entre Custom/Sync. Pago async por API vCurrency; nunca JDBC/HTTP en main. Custodia/estado transaccional del objeto y recibo para reconexión/transferencia/reinicio: MySQL pago y Bukkit inventario no son una transacción atómica. Falla incierta va a revisión, no retry/refund ciego. Devolución segura al cerrar/desconectar o inventario lleno sin pérdida/duplicado.
- XP skill solo tras éxito real y una vez usando dispatch/puente nativo verificado. No simular EnchantItemEvent completo si dispara mutadores aleatorios o multiplica XP; solicitar adaptador oficial acotado si necesario. Mantener XP final vPets y listeners existentes.
- No construir catálogo por combate ni cada tick. Precalcular definiciones/tiers estáticos en enable/reload; leer estado por apertura/clic; copiar solo objeto relevante. Sin refrescos continuos de inventario/título/tooltip. Operaciones DB/cola acotadas con timeout; fallo explícito.
- Objetivo de validación:60–100 jugadores,30–40 activos. Estas cifras son escenarios solicitados, no benchmark medido. Medir main-thread CPU/MSPT y profiler LibreForge/spark antes/después sin tráfico artificial en producción.
- Tests: todos los tiers; vanilla/Eco/Nexo y enchants especiales existentes; skill insuficiente/ausente/suficiente; coste exacto puntos vs niveles; click duplicado; pagos denegados/timeout/resultado incierto; inventario lleno; drag/shift/números/doble click; cierre/quit/transferencia/reinicio; protecciones y mano secundaria; renovación de catálogo; no cura/acumulación; XP y callbacks una vez. Separar mocks de runtime Test.

## Prompt para el otro chat

Implementa por incrementos un addon de mesa de encantamientos para Survival Custom/Sync siguiendo este documento. Primero lee AGENTS general/de ámbito, estándar visual, flujo de desarrollo y este handoff, y contrasta fuentes canónicas/Git/JAR actuales. No delegues automáticamente. No modifiques EcoSuite, ExcellentEconomy, vCurrency, vStore, vPets o vNetwork desde ese chat: solicita al chat dueño los contratos/parches que falten.

Primero entrega diseño de UI, catálogo candidato completo por ID/tier y análisis de APIs/pagos/XP; precios aquí son propuestas no aprobadas. Primer incremento funcional en Test, vanilla normal solo XP, Eco/custom shards y superiores XP+shards: mesa→objeto→enchant→todos los niveles en cuadrícula, guía separada, preservando requisitos, compatibilidad, libros especiales, eventos, callbacks y puente XP final. No generes XP de skill por acciones fallidas/duplicadas/retirada. Corrige o coordina diagnóstico de nivel No disponible antes de autorizar compras. No fabriques eventos nativos como atajo.

Preparar pago opcional gemas vía API durable operationId, solo como alternativa al coste shards de Eco/custom, mismos enchants/requisitos y sin exclusividad/VIP nuevo; mantenerlo deshabilitado hasta aprobar costes y pasar pruebas de entrega/custodia/reconexión/transferencia. Construir catálogo estático una vez, sin polling/SQL en main ni refresco continuo. Verifica protección de mesas e inventario y fallos seguros. Builds solo personal/ejecutor limitado. Entrega Test con HDD backup/hashes; no producción ni lifecycle. El propietario reinicia. Entrega código/documentación/STATE, artefactos/hashes, pruebas unitarias y reales separadas y handoff de cualquier cambio requerido a EcoSuite.
