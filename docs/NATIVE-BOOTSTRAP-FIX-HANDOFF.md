# Corrección de bootstrap EcoSuite — 20/10/2026

Sustituye exclusivamente el candidato EcoEnchants rechazado, no una actualización de mecánicas.
El JAR anterior e8ca5859f41e79056a2c29e686f26cd09d7dbdd770c1cad60e42d11e3377ab6b NO debe instalarse otra vez.

## Causa y corrección

La lista selectiva de composición omitía el objeto Kotlin NativeUtilityProgression y solo incluía el archivo NativeUtilityProgressionKt. OriginalProgression.register ya llamaba al objeto existente en fuente. Se corrigieron los selectores de utilidad y consolidación; no se cambió esa mecánica ni se importó un shadowJar completo.
Comparación ZIP anterior/corregido: una clase añadida, cero entradas existentes modificadas y cero retiradas. La clase viene del mismo core-plugin sombreado utilizado por el compuesto; sus referencias, INSTANCE, register, inicializador y dependencia EffectDoubleMobXp están presentes. Todos los SDK y APIs existentes son byte idénticos. Las117 definiciones, tiers, requisitos, targets/conflictos y modos quedan intactos. No se modificaron vEnchanting, vPets, precios, skills.yml ni datos.

## Regresión ejecutada en personal

Ejecutar dentro del ejecutor limitado desde EcoEnchants:
`bash tools/verify-native-bootstrap.sh <directorio-de-artefactos> <JAR-anterior-rechazado>`.

VerifyBootstrapClosure rechaza ahora una clase propia ausente en vez de tratarla como dependencia externa. Audita1155 métodos/8155 referencias, descriptores, tipos, clases anidadas y handles. Las APIs externas del servidor no se certifican por esta auditoría.
VerifyBootstrapExecution ejecuta el handleLoad REAL del JAR, que invoca los registros reales y construye sus objetos Kotlin. Solo se sustituye la frontera de almacenamiento de registros para no necesitar servidor: no se ejecutan onRegister/postRegister, constructor de JavaPlugin, enable, tareas ni SQL. Exige exactamente14 efectos,5 triggers,3 condiciones,2 filtros. El JAR anterior reproduce NoClassDefFoundError de NativeUtilityProgression; el corregido pasa ambas comprobaciones. Esto NO es una prueba de arranque completo de Leaf ni de los efectos en juego.
Las pruebas JVM127EE/168LF previas no se volvieron a ejecutar porque no cambió código funcional; no se suman artificialmente a estas dos regresiones de artefacto.

## Conjunto coordinado SHA256

- EcoEnchants-2026.27-native-bootstrap-fixed-all.jar: 1d7a0ca87822f38be5dbb9e2aae528cdb4e7895a301d179394e25ba34e64152c
- LibreForge-2026.27-native-consolidated-all.jar: cc3a4c063e8f286aba82cf7469756d079c18b3ac245bd50e912840516bb25426
- eco-2026.28-native-utility-all.jar: 5ca36aa859cef009156cef5e821ad7907e62f11006c8cde7382130799542544f
- EcoSkills-2026.27-all-requirement-views.jar: 0c936eecdc0557244ea0546d5e4e8d3f84b6d37fe2b0743e64f876a21999f4ca

Los otros tres JAR no cambiaron. SHA256SUMS y consolidated-runtime-manifest.json identifican el conjunto vigente. definitions/ conserva117 archivos y su manifiesto original sin cambios. No tocar el catálogo vEnchanting0.9.5 preparado por su chat ni revertir su exclusión de13 reservados; Cosecha sigue fuera de mesa conforme a esa integración.

## Integración pendiente, solo por el chat dueño

Destino vigente: Dev Custom48d37562-51f7-48c7-812b-b295df2e8aeb y Dev Sync90db2686-d703-4467-9b3a-7dcc0add36cf. No Test HomeSV ni producción.
Respetar reversión y cambios posteriores registrados en /root/development/staging/venchanting-native-consolidated-dev-20261020/. No restaurar datos, journals ni inventarios. Respaldar/contrastar hashes antes del reintento; no duplicar JAR ni reintentar compras inciertas. El chat de vEnchanting coordina instalación y activación.
Verificar después ambos arranques, ausencia del error, registro117custom y status/validate, XP y compras exactamente una vez, luego gameplay de efectos. Arranque no prueba combate ni la integración de compra. Este chat NO instaló, ejecutó lifecycle ni SQL en este incremento.
