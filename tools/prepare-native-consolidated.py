"""Prepare owner-reviewable definitions from pinned Test snapshots and the already-reviewed overlays. Never installs."""
from pathlib import Path
import copy,hashlib,json,yaml
r=Path(__file__).resolve().parents[1]
w=Path('/root/development/artifacts/eco-suite/native-expansion-20261020')
out=r/'delivery/native-expansion-20261020/consolidated';out.mkdir(exist_ok=True)
coverage=json.loads((r/'delivery/native-expansion-20261020/coverage.json').read_text())
rows={x['id']:x for x in coverage['rows']}
reserved={x['id'] for x in rows.values() if x['status']=='reserved_excluded'}
audit={x['id']:x for x in json.loads((w/'test-enchant-audit.json').read_text())}
levels={x['id']:x for x in json.loads((w/'levels.json').read_text())}
source=r/'eco-core/core-plugin/src/main/resources/enchants'
defs={}
for id in rows:
 if id in reserved:continue
 if id in audit:defs[id]=copy.deepcopy(audit[id]['definition'])
 else:
  paths=list(source.rglob(id+'.yml')) or list((r/'delivery/native-expansion-20261020').glob('original-*/'+id+'.yml'));assert len(paths)==1,(id,paths)
  defs[id]=yaml.safe_load(paths[0].read_text())
for folder in ['config-fixes','additive-hp','utility-fixes','offensive-common','passive-stats','original-bursts','original-reactive']:
 for path in (r/'delivery/native-expansion-20261020'/folder).rglob('*.yml'):
  if path.stem in defs:defs[path.stem]=yaml.safe_load(path.read_text())
notes={}
if 'reposo' not in defs['rejuvenate'].setdefault('conflicts',[]):defs['rejuvenate']['conflicts'].append('reposo')
def change(id,note):notes[id]=note

def effect(id,args,triggers,**extra):return dict(id=id,args=args,triggers=triggers,**extra)
def native(id,args):return effect('native_burst',dict(family=id,**args),['confirmed_native_burst'])
# Correct observed units and conditions; preserve established max levels, item targets and conflicts.
d=defs['ender_slayer'];d['placeholder']='5 * %level% + 5 * max(0, %level% - 4)';d['effects'][0]['id']='damage_multiplier';d['effects'][0]['args']={'multiplier':'1 + (5 * %level% + 5 * max(0, %level% - 4)) / 100','damage_group':'ecoenchants_normal'};change('ender_slayer','5/10/15/20/30% aditivo, filtros originales; antes sumaba daño plano pese a anunciar porcentaje.')
d=defs['smoldering'];d['effects'][0]['id']='damage_multiplier';d['effects'][0]['args']={'multiplier':'1 + 0.03 * %level%','damage_group':'ecoenchants_normal'};change('smoldering','3% por tier aditivo contra blaze; corrige plano frente al porcentaje anunciado.')
d=defs['jumpshot'];d.setdefault('conditions',[]).append({'id':'in_air'});d['effects'][0]['args']['damage_group']='ecoenchants_normal';change('jumpshot','10% por tier aditivo solo cuando la condición nativa in_air se cumple; no simula tiempo de vuelo.')
d=defs['shura'];d.setdefault('conditions',[]).append({'id':'below_health_percent','args':{'percent':50}});d['description']='&7Multiplica por &a%placeholder%x&7 el crítico nativo al caer con menos del 50% de vida.';change('shura','Conserva crítico nativo por caída y multiplicador; añade umbral de vida anunciado, no EcoSkills crit chance.')
d=defs['titan_killer'];d['combat-mode']='pvp';d['effects'][0].setdefault('filters',{})['entities']=['player'];d['effects'][0]['args']={'multiplier':'1 + min(max(0, %victim_ecoskills_defense%) / 100 * (0.02 * %level% + 0.02 * max(0, %level% - 4)), 0.06 * %level% + 0.10 * max(0, %level% - 4))','damage_group':'ecoenchants_normal'};d['description']='&7Añade &a%placeholder%%&7 de daño por cada 100 de Defensa del jugador, hasta &a%cap%%&7.';d['placeholder']='2 * %level% + 2 * max(0, %level% - 4)';d.setdefault('placeholders',{})['cap']='6 * %level% + 10 * max(0, %level% - 4)';change('titan_killer','Solo PvP: Defensa EcoSkills pertenece a jugadores. Tasas2/4/6/8/12 y caps6/12/18/24/40%; sin inventar stats de criaturas.')
d=defs['thunderlord'];d['effects']=[effect('third_hit_multiplier',{'multiplier':'1 + 0.08 * %level%'},['melee_attack'])];d['placeholder']='8 * %level%';d['description']='&7Cada tercer golpe al mismo objetivo añade &a%placeholder%%&7 de daño, con 10 s de recarga.';change('thunderlord','8–40% aditivo al tercer intento tras dos golpes confirmados; sin puntos persistentes/NBT por golpe, CD10s por jugador.')
d=defs['thunderbolt'];d['effects']=[native('thunderbolt',{'percent':'4 * %level%'})];d['placeholder']='4 * %level%';d['description']='&7Cada tres golpes confirmados reparte &a%placeholder%%&7 del daño a hasta 10 objetivos en 2 bloques, cada 10 s.';change('thunderbolt','4–20% del daño final confirmado, hasta10 objetivos/radio2 y CD10s; sustituye rayos aleatorios con daño vanilla ajeno al porcentaje.')
d=defs['combo'];d['effects']=[native('combo',{'percent':'%level%'}),effect('recent_kills_multiplier',{},['melee_attack'])];d['description']='&7Añade &a%dmg%%&7 por muerte reciente, hasta &a%max_kills%&7 muertes durante &a%time_window% s&7.';change('combo','Ventana real2+2L segundos, máximo2+L muertes, +L% por muerte; memoria de sesión, sin puntos permanentes.')
for id in ['lifesteal','drain']:
 d=defs[id];heal=d['effects'][0];heal['triggers']=['confirmed_native_burst'];heal['args']['trigger_heal']=True
 if id=='lifesteal':
  heal['args']['amount']='min(%v% * %level% * 0.1, %player_max_health% * 0.005 * %level%)'
  d['description']='&7Cura &a%placeholder%%&7 del daño real, hasta &a%heal_cap%%&7 de tu vida máxima por golpe.';d.setdefault('placeholders',{})['heal_cap']='0.5 * %level%'
  d['effects'][1]['triggers']=['confirmed_native_burst'];d['effects'][1].pop('filters',None);d['effects'][1].setdefault('args',{})['chance']=50
 else:
  heal.pop('cooldown',None);heal['args'].update(amount='min(%v% * 0.001 * %level% * %player_max_health%, %player_max_health% * 0.01 * %level%)',cooldown=4,send_cooldown_message=False)
  d['description']='&7Cura &a%placeholder%%&7 de vida máxima por cada 100 de daño real, hasta &a%heal_cap%%&7 cada 4 s.';d.setdefault('placeholders',{})['heal_cap']='%level%'
 change(id,'Solo daño confirmado y no sobreletal; evento de curación cancelable. Límite por familia local explícito, no equivalencia de HP con Hypixel.')
d=defs['vampirism'];d['effects'][0]['args']={'amount':'max(0, %player_max_health% - %player_health%) * 0.01 * %level%','trigger_heal':True};d['effects'][0].setdefault('filters',{})['genuine_kill']=True;change('vampirism','1–5% de vida faltante tras muerte nativa real y curación cancelable; ignora force sin testigo, evita doble aviso Mythic/bleed.')
for id in ['carve','cleave']:
 d=defs[id];d['effects']=[native(id,{'damage':'2 * %level%'} if id=='carve' else {'percent':'5 * %level%'})]
 if id=='carve':d['description']='&7Inflige &a%damage%&7 de daño a hasta 12 objetivos adicionales en &a%radius%&7 bloques tras un golpe real.'
 else:d['description']='&7Inflige &a%placeholder%%&7 del daño real a hasta 12 objetivos adicionales en &a%radius%&7 bloques, cada 3 s.'
 change(id,'Cola de daño confirmado y consulta nativa acotada; excluye golpeado principal/portador/aliados/protegidos y no repite crítico/fuerza; Carve plano, Cleave porcentual.')
for id,interval in [('bleed',20),('wound',15)]:
 d=defs[id];first=d['effects'][0];args=first['args'];d['effects'][0]=native(id,dict(damage=args['damage'],amount=args['amount'],interval=interval,chance=args['chance']))
 if id=='bleed':
  d['effects'][0]['args'].update(cooldown=4,send_cooldown_message=False);d['effects'][1]['triggers']=['confirmed_native_burst']
  d['description']='&7Tiene &a%chance%%&7 de causar &a%ticks%&7 pulsos de sangrado y &c15%&7 de lentitud durante &a%ticks% s&7.'
 else:d['description']='&7Tiene &a%chance%%&7 de causar &a%damage%&7 de daño cada 0,75 s durante &a%duration% s&7.'
 change(id,'Pulsos reales con fuente y protecciones, en scheduler de procs existente; nunca fuerza kill antes de aplicar daño; conserva todos los tiers (Wound VII).')
d=defs['contagion'];old=d['effects'];d['effects']=[{'triggers':['projectile_hit'],'args':{'chance':'5 * %level%'},'effects':[{k:v for k,v in x.items() if k!='triggers'} for x in old]}];change('contagion','Restaura5L% de probabilidad compartida entre nube y sonido; cadena exterior sin id es nativa válida. Nube vanilla afecta ambos modos, no se promete filtro por víctima.')
d=defs['sparks'];d.setdefault('conditions',[]).append({'id':'is_storm'});change('sparks','Añade tormenta nativa requerida; cadena exterior conservada.')
d=defs['slaughter'];d['effects'][0]['filters']['entities']=['ocelot' if x=='ocelots' else x for x in d['effects'][0]['filters']['entities']];change('slaughter','Corrige ocelot; conserva daño plano y lista de criaturas.')
d=defs['rapid'];d['effects'][0]['args']['normalized_force']=True;change('rapid','Fuerza de EntityShootBowEvent ya es0–1: normalización opt-in, mantiene umbral100−15L y velocidad máxima3; no declara crítico/carga completa para procs nuevos.')
d=defs['rebounding'];d['effects'][0]['args'].update(use_source=True,damage_origin='PASSIVE',check_antigrief=True,prevent_recursive=True);d['description']='&7Devuelve al atacante &a%placeholder%%&7 del daño realmente bloqueado por el escudo.';change('rebounding','Cantidad realmente bloqueada, fuente nativa/protección y guardia de recursión; no refleja todo ataque por tener levantado el escudo.')
d=defs['foraging'];d['placeholder']='50 * %level%';change('foraging','Fortuna nativa1+uniforme[0,L], media1+L/2; corrige porcentaje medio de manzanas, sin cambiar drops.')
d=defs['shulker_harvest'];d['placeholder']='50 * (2 + %level%)';change('shulker_harvest','Media del multiplicador nativo de fortuna3..5: +150/200/250%, conserva distribución.')
d=defs['nether_prospector'];d['placeholder']='33 + 42 * (%level% - 1)';d['description']='&7Tiene &a%placeholder%%&7 de duplicar escombros ancestrales no colocados por jugadores.';change('nether_prospector','Descripción de chance real33/75%; no altera economía ni multiplicadores.')
d=defs['frost'];d.setdefault('placeholders',{})['seconds']='3 + %level%';change('frost','Corrige duración mostrada4/5/6 segundos de congelación; conserva efecto nativo.')
d=defs['escape'];d['placeholder']='20 * %level%';d['description']='&7Obtienes &a%placeholder%%&7 de velocidad durante 1,5 s tras recibir un ataque.';change('escape','VelocidadI/II significa20/40%, no1/2%; conserva duración y efectos.')
d=defs['vanish'];d.setdefault('placeholders',{})['seconds']='(10 + floor(%level% * 5)) / 20';change('vanish','Duración real0,75/1 s y chance2/4%, sin cambiar poción.')
d=defs['replenish'];d['replant-per-tick']=64;change('replenish','Semillas tras permiso/madurez, lote64/tick y cola1024; cancelación/sesión/chunk/bloque/reload protegidos y rollback de colocación vetada.')
d=defs['stun'];d['combat-mode']='pve';d['description']='&7Tiene &a%placeholder%%&7 de desactivar la IA de criaturas atacadas durante 2 s.';change('stun','IA solo existe en criaturas; PvE explícito, no promete aturdir jugadores.')
# Common migration: cooldown belongs to args; all passive failures stay silent.
legacy_combat={'melee_attack','bow_attack','trident_attack','take_entity_damage','shield_block','headshot','catch_entity','kill'}
combat=legacy_combat|{'confirmed_original_hit','confirmed_native_combat','confirmed_native_burst','native_reactive_action'}
def walk(effects):
 for x in effects or []:
  yield x
  yield from walk(x.get('effects',[]))
for id,d in defs.items():
 d['enabled']=True
 assert d.get('max-level')==levels[id]['max_level'],(id,d.get('max-level'),levels[id]['max_level'])
 for cond in d.get('conditions',[]):
  if cond.get('id')=='has_skill_level' and cond.get('args',{}).get('skill')=='enchanting':
   previous=cond['args']['level'];cond['args']['level']=levels[id]['proposed_level']
   if previous!=cond['args']['level']:
    val=cond['args']['level'];roman=''
    for number,letters in [(50,'L'),(40,'XL'),(10,'X'),(9,'IX'),(5,'V'),(4,'IV'),(1,'I')]:
     while val>=number:roman+=letters;val-=number
    cond['args']['not-met-lines']=['&cRequiere Nivel de Encantador &l'+roman]
    notes[id]=notes.get(id,'')+' Requisito del catálogo aprobado actual.'
 for i,x in enumerate(walk(d.get('effects',[]))):
  if 'cooldown' in x:
   x.setdefault('args',{})['cooldown']=x.pop('cooldown');x['args']['send_cooldown_message']=False
   notes[id]=notes.get(id,'Efectos nativos conservados;')+' cooldown reconocido dentro de args, silencioso.'
  if set(x.get('triggers',[]))&combat:
   filters=x.setdefault('filters',{})
   if set(x.get('triggers',[]))&legacy_combat:filters['enchant_combat_mode']=True
   d.setdefault('combat-mode','pve' if 'entities' in filters and 'player' not in filters['entities'] else 'both')
   if not filters:x.pop('filters',None)
 if id=='introversion':d['combat-mode']='pvp'
 if d.get('combat-mode')=='both':
  if isinstance(d.get('description'),str):d['description']=d['description'].replace('monstruos','objetivos').replace('monstruo','objetivo')
  elif isinstance(d.get('description'),list):d['description']=[x.replace('monstruos','objetivos').replace('monstruo','objetivo') for x in d['description']]
 # Missing specialized source effect details are not invented from names.
 if id not in notes:notes[id]='Ruta nativa revisada y conservada; ver contrato por lote o definición incluida, sin equivalencia Hypixel inventada.'
 path=out/(id+'.yml');path.write_text(yaml.safe_dump(d,allow_unicode=True,sort_keys=False,width=120))
manifest=[]
for id,d in sorted(defs.items()):
 manifest.append(dict(id=id,enchanting=levels[id]['proposed_level'],tiers=list(range(1,d['max-level']+1)),targets=d['targets'],conflicts=d.get('conflicts',[]),type=d['type'],combatMode=d.get('combat-mode','not_applicable_or_passive_both'),definitionPath=id+'.yml',candidateSha256=hashlib.sha256((out/(id+'.yml')).read_bytes()).hexdigest(),sourceTestPath=audit.get(id,{}).get('path'),sourceTestSha256=audit.get(id,{}).get('sha256'),effects=d.get('effects',[]),note=notes[id],installed=False,active=False,gameplayVerified=False))
(out/'manifest.json').write_text(json.dumps(dict(schema=1,definitions=manifest,reserved=sorted(reserved),validation='Candidate definitions and reviewed native sources; build/JVM report separate. Paper registration, gameplay, transfer and measured performance pending.'),ensure_ascii=False,indent=2)+'\n')
assert len(manifest)==117
print('Prepared',len(manifest),'definitions; no installation. Reserved untouched:',len(reserved))
