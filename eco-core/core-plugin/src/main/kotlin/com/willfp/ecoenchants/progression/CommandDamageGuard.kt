package com.willfp.ecoenchants.progression

import java.lang.reflect.Modifier

/** Native command execution identity, resolved once. No stack walking or reflection per hit. */
internal class CommandDamageGuard(private val context:ThreadLocal<*>) {
 fun gameplay()=context.get()==null
 companion object {
  fun resolve():CommandDamageGuard {
   val field=Class.forName("net.minecraft.commands.Commands").getDeclaredField("CURRENT_EXECUTION_CONTEXT")
   check(Modifier.isStatic(field.modifiers)&&ThreadLocal::class.java.isAssignableFrom(field.type)&&field.trySetAccessible()){"Native command execution context unavailable"}
   return CommandDamageGuard(field.get(null) as ThreadLocal<*>)
  }
 }
}
