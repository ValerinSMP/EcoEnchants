set -euo pipefail
out=/root/development/artifacts/eco-suite/combat-targets-20261021/final
old=/root/development/artifacts/eco-suite/native-expansion-20261020
mkdir -p "$out/verification"
asm=$(find /root/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm/9.8 /root/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm-tree/9.8 /root/.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm-commons/9.8 -name '*.jar' -print | paste -sd:)
python3 tools/compose-combat-targets.py "$out" "$asm" > "$out/verification/composition.json"
python3 - "$out" <<'PATCH'
import json,sys
from pathlib import Path
w=Path(sys.argv[1]);d=json.loads((w/'runtime-manifest.json').read_text())
(w/'verification/changed-classes.txt').write_text('\n'.join(n for a in d['artifacts'] for n in a['changes'])+'\n')
PATCH
paper=$(find /root/.gradle/caches/modules-2/files-2.1/io.papermc.paper/paper-api/26.2.build.134-stable -name '*.jar' -print -quit)
kyori=$(find /root/.gradle/caches/modules-2/files-2.1/net.kyori -name '*.jar' -print | paste -sd:)
extra=$(find /root/.gradle/caches/modules-2/files-2.1/com.google.guava/guava /root/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson /root/.gradle/caches/modules-2/files-2.1/org.slf4j/slf4j-api -name '*.jar' -print | paste -sd:)
eco="$old/eco-2026.28-native-utility-all.jar"
lf="$out/LibreForge-2026.27-combat-targets.jar"
skills=/root/development/artifacts/eco-suite/requirement-lore-20261020/EcoSkills-2026.27-all-requirement-views.jar
ee="$out/EcoEnchants-2026.27-combat-targets-all.jar"
base=/var/lib/pterodactyl/volumes/48d37562-51f7-48c7-812b-b295df2e8aeb/plugins
javac --release 21 -cp "$asm" -d "$out/verification" tools/VerifyBootstrapClosure.java tools/VerifyCombatTargets.java
java -Xmx256m -cp "$out/verification:$asm" VerifyCombatTargets "$eco" "$lf" "$skills" "$ee" "$base/libreforge-2026.27.jar" "$base/EcoEnchants-2026.27-all.jar" "$out/verification/changed-classes.txt" | tee "$out/verification/abi-linkage.log"
bungee=$(find /root/.gradle/caches/modules-2/files-2.1/net.md-5/bungeecord-chat -name '*.jar' -print | paste -sd:)
cp="$ee:$lf:$eco:$skills:$paper:$kyori:$extra:$bungee"
javac --release 21 -cp "$cp" -d "$out/verification" tools/VerifyCombatPolicyRuntime.java
java -Xmx256m -cp "$out/verification:$cp" VerifyCombatPolicyRuntime | tee "$out/verification/policy-runtime.log"
# Reuse the exact previously rejected artifact and actual handleLoad execution regression.
mkdir -p "$out/bootstrap"
ln -sfn "$eco" "$out/bootstrap/eco-2026.28-native-utility-all.jar"
ln -sfn "$lf" "$out/bootstrap/LibreForge-2026.27-native-consolidated-all.jar"
bash tools/verify-native-bootstrap.sh "$out/bootstrap" "$old/final/EcoEnchants-2026.27-native-consolidated-all.jar" "$ee" | tee "$out/verification/bootstrap.log"
