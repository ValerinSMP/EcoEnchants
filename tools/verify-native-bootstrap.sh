set -euo pipefail
w=${1:?artifact directory required}
bad=${2:?previous broken JAR required}
work="$w/bootstrap-verification"
mkdir -p "$work/classes" "$work/fixtures"
asm=$(find /root/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.8 /root/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm-tree/9.8 -name '*.jar' -print | paste -sd:)
paper=$(find /root/.gradle/caches/modules-2/files-2.1/io.papermc.paper/paper-api/26.2.build.134-stable -name '*.jar' -print -quit)
kyori=$(find /root/.gradle/caches/modules-2/files-2.1/net.kyori -name '*.jar' -print | paste -sd:)
extra=$(find /root/.gradle/caches/modules-2/files-2.1/com.google.guava/guava /root/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson /root/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api -name '*.jar' -print | paste -sd:)
eco="$w/eco-2026.28-native-utility-all.jar"
lf="$w/LibreForge-2026.27-native-consolidated-all.jar"
skills=/root/development/artifacts/eco-suite/requirement-lore-20261020/EcoSkills-2026.27-all-requirement-views.jar
ee="$w/EcoEnchants-2026.27-native-consolidated-all.jar"
cp="$eco:$lf:$skills:$paper:$kyori:$extra"
javac --release 21 -cp "$asm" -d "$work/classes" tools/VerifyBootstrapClosure.java
# This must reject the actual previously delivered artifact, not a synthetic fixture.
if java -Xmx256m -cp "$work/classes:$asm" VerifyBootstrapClosure "$eco" "$lf" "$skills" "$bad" > "$work/previous-closure.log" 2>&1; then
 echo 'Regression failed: audit accepted previous broken artifact';exit 1
fi
grep -q 'Missing bootstrap class: com/willfp/ecoenchants/progression/NativeUtilityProgression' "$work/previous-closure.log"
java -Xmx256m -cp "$work/classes:$asm" VerifyBootstrapClosure "$eco" "$lf" "$skills" "$ee"
# Only registry storage is simulated. The actual plugin handleLoad, progression classes,
# Kotlin initializers and constructors come exclusively from delivered JARs.
# No registry onRegister/server enable callbacks run: those remain Dev verification.
python3 - "$work" <<'PY'
from pathlib import Path
import sys
p=Path(sys.argv[1])/'fixtures'
for package,name in [('effects','Effects'),('triggers','Triggers'),('conditions','Conditions'),('filters','Filters')]:
 d=p/'com/willfp/libreforge'/package;d.mkdir(parents=True,exist_ok=True)
 (d/(name+'.java')).write_text('package com.willfp.libreforge.'+package+';\npublic final class '+name+' extends com.willfp.eco.core.registry.Registry<com.willfp.eco.core.registry.Registrable> {\npublic static final '+name+' INSTANCE=new '+name+'();\npublic static final java.util.Set<String> ids=new java.util.LinkedHashSet<>();\n@Override public com.willfp.eco.core.registry.Registrable register(com.willfp.eco.core.registry.Registrable v){ids.add(v.getID());return v;}\n}\n')
PY
javac --release 21 -cp "$cp" -d "$work/classes" $(find "$work/fixtures" -name '*.java') tools/VerifyBootstrapExecution.java
if java -Xmx256m -cp "$work/classes:$bad:$cp" VerifyBootstrapExecution > "$work/previous-execution.log" 2>&1; then
 echo 'Regression failed: bootstrap accepted previous broken artifact';exit 1
fi
grep -q 'NoClassDefFoundError: com/willfp/ecoenchants/progression/NativeUtilityProgression' "$work/previous-execution.log"
java -Xmx256m -cp "$work/classes:$ee:$cp" VerifyBootstrapExecution
printf '%s\n' 'Original artifact rejected by closure AND executed bootstrap; corrected artifact passed both.'
