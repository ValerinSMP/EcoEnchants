package com.willfp.ecoenchants.progression;

import java.util.*;
import java.util.function.LongSupplier;

/**
 * MAIN-only, bounded mechanic core. The adapter must supply a confirmed, final direct hit,
 * stable item identity and authoritative exclusions. This class never damages entities.
 */
public final class OriginalProcEngine {
    public enum Family {
        ECO_RESONANTE(3,5_000,8_000,new double[]{4,6,8}),
        SELLO_GLACIAL(2,4_000,10_000,new double[]{10,15,20}),
        BRASA_ERRANTE(1,1_000,12_000,new double[]{4,6,8}),
        ESTELA_CAZADOR(1,1_000,20_000,new double[]{5,7,9}),
        PULSO_ABISAL(1,1_000,12_000,new double[]{4,6,8}),
        REBOTE_PRISMATICO(1,1_000,15_000,new double[]{8,10,12}),
        CICATRIZ_IGNEA(3,5_000,12_000,new double[]{3,4,5}),
        RUPTURA_SONICA(4,6_000,15_000,new double[]{6,8,10}),
        TORMENTA_ENCADENADA(5,7_000,18_000,new double[]{6,8,10}),
        COMETA(3,8_000,20_000,new double[]{10,12,15}),
        CONSTELACION(3,8_000,25_000,new double[]{8,10,12}),
        LEGADO_VALERIN(8,10_000,30_000,new double[]{15}),
        ANCLA(1,1_000,15_000,new double[]{5,8,10}),
        MARCA_CELESTE(1,1_000,10_000,new double[]{4,6,8}),
        GUARDIA_CRISTAL(3,4_000,20_000,new double[]{8,10,12}),
        VELO_LUNAR(1,1_000,20_000,new double[]{5,7,9}),
        VIGILIA(1,1_000,20_000,new double[]{5,7,9}),
        AGUJA_AURORA(1,1_000,12_000,new double[]{4,6,8}),
        PASO_CENIZA(1,1_000,20_000,new double[]{5,7,9}),
        ECLIPSE(6,8_000,25_000,new double[]{6,8,10}),
        THUNDERLORD(3,10_000,10_000,new double[]{8,16,24,32,40}),
        THUNDERBOLT(3,10_000,10_000,new double[]{4,8,12,16,20}),
        COMBO(1,1_000,0,new double[]{1,2,3,4,5}),
        CLEAVE(1,1_000,3_000,new double[]{5,10,15,20,25,30}),
        CARVE(1,1_000,0,new double[]{2,4,6,8,10}),
        BLEED(1,1_000,4_000,new double[]{1,1,1,1,1}),
        WOUND(1,1_000,0,new double[]{1,1,1,1,1,1,1});
        final int hits;final long window,cooldown;final double[] values;
        Family(int hits,long window,long cooldown,double[]values){this.hits=hits;this.window=window;this.cooldown=cooldown;this.values=values;}
    }
    public record Hit(UUID event,UUID player,UUID item,UUID target,UUID world,int tier,
                      double finalDamage,boolean damageConfirmed,boolean direct,
                      boolean monster,boolean cancelled,boolean protectedTarget,boolean npc,
                      boolean pet,boolean ally,boolean bossExcluded,boolean spawner,
                      boolean invulnerable,boolean secondary) {}
    /** Pure mechanic intent; not evidence that an in-game effect was applied. */
    public record Activation(Family family,double value,double sourceFinalDamage,int maxTargets,
                             double radius,int durationTicks,int maxParticles) {}
    private record Key(UUID player,UUID item,UUID target,UUID world) {}
    private record Window(int hits,long expires) {}
    private record WindowRef(Family family,Key key) {}
    record Lease(UUID id,long until) {}
    private final Thread owner=Thread.currentThread();
    private final LongSupplier millis;
    private final int capacity;
    private final EnumMap<Family,LinkedHashMap<Key,Window>> windows=new EnumMap<>(Family.class);
    private final EnumMap<Family,LinkedHashMap<UUID,Lease>> cooldowns=new EnumMap<>(Family.class);
    private record EventKey(Family family,UUID event) {}
    private final LinkedHashMap<EventKey,Long> events=new LinkedHashMap<>();
    private final Map<UUID,Set<WindowRef>> windowEntities=new HashMap<>();
    public OriginalProcEngine(int capacity,LongSupplier millis){
        if(capacity<1||capacity>4096)throw new IllegalArgumentException("capacity must be 1..4096");
        this.capacity=capacity;this.millis=Objects.requireNonNull(millis);
        for(var f:Family.values()){windows.put(f,new LinkedHashMap<>());cooldowns.put(f,new LinkedHashMap<>());}
    }
    private void main(){if(Thread.currentThread()!=owner)throw new IllegalStateException("MAIN required");}
    private boolean valid(Hit h){
        return h!=null&&h.event()!=null&&h.player()!=null&&h.item()!=null&&h.target()!=null&&h.world()!=null
            &&h.tier()>=1&&Double.isFinite(h.finalDamage())&&h.finalDamage()>0
            &&h.damageConfirmed()&&h.direct()&&h.monster()&&!h.cancelled()&&!h.protectedTarget()
            &&!h.npc()&&!h.pet()&&!h.ally()&&h.bossExcluded()&&!h.spawner()&&!h.invulnerable()&&!h.secondary();
    }
    public Optional<Activation> record(Family family,Hit h){
        main();Objects.requireNonNull(family);if(!valid(h)||h.tier()>family.values.length)return Optional.empty();
        long now=millis.getAsLong();expire(now);
        var eventKey=new EventKey(family,h.event());
        if(events.containsKey(eventKey)||events.size()>=capacity)return Optional.empty();
        // One event may carry several independent families; each family claims it once.
        // Rejected events never enter the bounded deduplication map.
        var cd=cooldowns.get(family);
        if(cd.containsKey(h.player()))return Optional.empty();
        var map=windows.get(family);var key=new Key(h.player(),h.item(),h.target(),h.world());
        var old=map.get(key);
        if(old==null&&(map.size()>=capacity||cd.size()>=capacity))return Optional.empty();
        events.put(eventKey,now+1000);
        int hits=old==null?1:old.hits()+1;
        if(hits<family.hits){if(old==null)linkWindow(family,key);map.put(key,new Window(hits,old==null?now+family.window:old.expires()));return Optional.empty();}
        // At capacity, deny instead of evicting an active cooldown and permitting a bypass.
        if(cd.size()>=capacity)return Optional.empty();
        map.remove(key);if(old!=null)unlinkWindow(family,key);cd.put(h.player(),new Lease(h.event(),now+family.cooldown));
        return Optional.of(new Activation(family,family.values[h.tier()-1],h.finalDamage(),
            switch(family){case ECO_RESONANTE,PULSO_ABISAL,CONSTELACION->2;case RUPTURA_SONICA,TORMENTA_ENCADENADA->3;case THUNDERBOLT->10;case CLEAVE,CARVE->12;default->1;},
            switch(family){case CLEAVE->3.3+0.2*h.tier();case CARVE->0.5*h.tier();case ECO_RESONANTE,PULSO_ABISAL,THUNDERBOLT->2;case BRASA_ERRANTE,REBOTE_PRISMATICO,RUPTURA_SONICA,TORMENTA_ENCADENADA,CONSTELACION->3;default->0;},
            switch(family){case SELLO_GLACIAL->40;case ESTELA_CAZADOR,CICATRIZ_IGNEA->60;default->0;},family==Family.ECO_RESONANTE||family==Family.LEGADO_VALERIN?16:12));
    }
    /** Explicit reversible cooldown reservation for a native pre-damage/impulse boundary. No confirmed hit is fabricated. */
    Optional<Lease> reserve(Family family,UUID player,UUID attempt){
        main();Objects.requireNonNull(family);Objects.requireNonNull(player);Objects.requireNonNull(attempt);
        long now=millis.getAsLong();expire(now);var map=cooldowns.get(family);
        if(map.containsKey(player)||map.size()>=capacity)return Optional.empty();
        var lease=new Lease(attempt,now+family.cooldown);map.put(player,lease);return Optional.of(lease);
    }
    boolean rollback(Family family,UUID player,Lease lease){
        main();return cooldowns.get(Objects.requireNonNull(family)).remove(player,Objects.requireNonNull(lease));
    }
    /** No timer. Fixed deadlines keep insertion order, so expiry only visits expired heads. */
    public void expire(long now){
        main();expireLong(events,now);
        for(var f:Family.values()){
            var cds=cooldowns.get(f).entrySet().iterator();
            while(cds.hasNext()){if(cds.next().getValue().until()>now)break;cds.remove();}
            var it=windows.get(f).entrySet().iterator();
            while(it.hasNext()){var entry=it.next();if(entry.getValue().expires()>now)break;it.remove();unlinkWindow(f,entry.getKey());}
        }
    }
    private static <K>void expireLong(LinkedHashMap<K,Long>map,long now){
        var it=map.entrySet().iterator();while(it.hasNext()){if(it.next().getValue()>now)break;it.remove();}
    }
    /** Called on quit/death/world change/transfer; local cooldown remains until its bounded TTL. */
    public void clearEntity(UUID id){
        main();var refs=windowEntities.remove(id);if(refs==null)return;
        for(var ref:refs){windows.get(ref.family()).remove(ref.key());unlinkWindow(ref.family(),ref.key());}
    }
    public int retainedEntries(){main();return events.size()+windows.values().stream().mapToInt(Map::size).sum()+cooldowns.values().stream().mapToInt(Map::size).sum();}
    public void clear(){main();events.clear();windows.values().forEach(Map::clear);cooldowns.values().forEach(Map::clear);windowEntities.clear();}
    private void linkWindow(Family family,Key key){
        var ref=new WindowRef(family,key);
        windowEntities.computeIfAbsent(key.player(),ignored->new HashSet<>()).add(ref);
        windowEntities.computeIfAbsent(key.target(),ignored->new HashSet<>()).add(ref);
    }
    private void unlinkWindow(Family family,Key key){
        var ref=new WindowRef(family,key);
        for(var id:List.of(key.player(),key.target())){
            var refs=windowEntities.get(id);if(refs==null)continue;refs.remove(ref);if(refs.isEmpty())windowEntities.remove(id);
        }
    }
    int windowReferences(UUID entity){main();var refs=windowEntities.get(entity);return refs==null?0:refs.size();}
}
