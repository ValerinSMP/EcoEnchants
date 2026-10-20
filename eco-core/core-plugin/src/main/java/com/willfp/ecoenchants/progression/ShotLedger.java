package com.willfp.ecoenchants.progression;
import java.util.*;
import java.util.function.LongSupplier;
/** Immutable source snapshots, finite TTL and one accepted hit per projectile/target. MAIN only. */
public final class ShotLedger<T> {
 private record Entry<T>(UUID owner,T source,long expires,Set<UUID> targets){}
 private final LinkedHashMap<UUID,Entry<T>> entries=new LinkedHashMap<>();
 private final Map<UUID,Set<UUID>> owners=new HashMap<>();
 private final int capacity;private final long ttl;private final LongSupplier clock;
 public ShotLedger(int capacity,long ttl,LongSupplier clock){
  if(capacity<1||capacity>4096||ttl<1||ttl>60000)throw new IllegalArgumentException("shot bounds");
  this.capacity=capacity;this.ttl=ttl;this.clock=clock;
 }
 private void expire(){long now=clock.getAsLong();var it=entries.entrySet().iterator();while(it.hasNext()){var entry=it.next();if(entry.getValue().expires()>now)break;it.remove();unlink(entry.getValue().owner(),entry.getKey());}}
 private void unlink(UUID owner,UUID projectile){var ids=owners.get(owner);if(ids==null)return;ids.remove(projectile);if(ids.isEmpty())owners.remove(owner);}
 public boolean remember(UUID projectile,UUID owner,T source){
  expire();if(entries.containsKey(projectile)||entries.size()>=capacity)return false;
  entries.put(projectile,new Entry<>(owner,source,clock.getAsLong()+ttl,new HashSet<>()));owners.computeIfAbsent(owner,ignored->new HashSet<>()).add(projectile);return true;
 }
 public T source(UUID projectile){expire();var e=entries.get(projectile);return e==null?null:e.source();}
 public boolean claim(UUID projectile,UUID target){
  expire();var e=entries.get(projectile);return e!=null&&e.targets().size()<8&&e.targets().add(target);
 }
 public void removeOwner(UUID owner){var ids=owners.remove(owner);if(ids!=null)for(var id:ids)entries.remove(id);}
 public void clear(){entries.clear();owners.clear();}
 int ownerReferences(UUID owner){expire();var ids=owners.get(owner);return ids==null?0:ids.size();}
 public int size(){expire();return entries.size();}
}
