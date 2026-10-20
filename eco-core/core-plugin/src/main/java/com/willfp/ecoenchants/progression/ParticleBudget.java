package com.willfp.ecoenchants.progression;
import java.util.*;
import java.util.function.LongSupplier;
/** MAIN-only, fixed one-second windows, finite observer table; aesthetics only. */
public final class ParticleBudget {
 private final LongSupplier clock;private final Map<UUID,Integer> used=new HashMap<>();
 private long start=Long.MIN_VALUE;private int global;
 public ParticleBudget(LongSupplier clock){this.clock=clock;}
 public int take(UUID observer,int request,int perActivation,int perObserver,int globalLimit){
  long now=clock.getAsLong();if(start==Long.MIN_VALUE||now-start>=1000){start=now;global=0;used.clear();}
  if(!used.containsKey(observer)&&used.size()>=256)return 0;
  int grant=Math.max(0,Math.min(Math.min(request,perActivation),Math.min(perObserver-used.getOrDefault(observer,0),globalLimit-global)));
  if(grant>0){used.merge(observer,grant,Integer::sum);global+=grant;}return grant;
 }
 public void clear(){used.clear();global=0;start=Long.MIN_VALUE;}
 public int observers(){return used.size();}
}
