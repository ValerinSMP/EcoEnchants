set -euo pipefail
w=/root/development/artifacts/eco-suite/native-expansion-20261020
java -Xmx64m -Dmaven.repo.local="$w/maven" -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :core:common:test :core:common:shadowJar --offline --no-daemon --no-parallel --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process
python3 - <<'PATCH'
from pathlib import Path
import zipfile
classes=Path('core/common/build/classes/kotlin/main')
for p in Path('/root/development/artifacts/eco-suite/native-expansion-20261020/maven/com/willfp/libreforge/2026.27').glob('*.jar'):
 if p.name.endswith('-sources.jar'):continue
 with zipfile.ZipFile(p) as z:before={n:z.read(n) for n in z.namelist()}
 for pattern in ['CombatTargets*.class','CombatTargetHolder*.class']:
  for f in (classes/'com/willfp/libreforge').glob(pattern):before[str(f.relative_to(classes))]=f.read_bytes()
 roots={'CombatTargets','CombatTargetHolder','DamageUtilsKt','Effect','ElementLike','TriggerData','EffectAOE','EffectBleed','EffectChainLightning','EffectCreateExplosion','EffectDamageNearbyEntities','EffectFrostNova','EffectSetVelocity','EffectSetVictimVelocity','EffectShockwave','EffectSoulRip','EffectSpawnPotionCloud','EffectStrikeLightning','EffectStun','EffectSwarm','EffectTimeBomb','EffectVortex'}
 for f in (classes/'com/willfp/libreforge').rglob('*.class'):
  if f.name.split('$')[0].removesuffix('.class') in roots:before[str(f.relative_to(classes))]=f.read_bytes()
 with zipfile.ZipFile(p,'w',zipfile.ZIP_DEFLATED) as z:
  for n,b in before.items():z.writestr(n,b)
PATCH
