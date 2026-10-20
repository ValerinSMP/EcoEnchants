package com.willfp.ecoenchants.progression
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
class SwarmQueryBudgetTest {
 @Test fun `global query budget is per real game tick and supports disable reset and tick wrap`(){
  val budget=SwarmQueryBudget()
  repeat(8){assertTrue(budget.take(100,8))};assertFalse(budget.take(100,8));assertTrue(budget.take(101,8))
  assertFalse(budget.take(102,0));assertTrue(budget.take(103,1));assertFalse(budget.take(103,1))
  assertTrue(budget.take(Int.MAX_VALUE,1));assertTrue(budget.take(Int.MIN_VALUE,1))
  budget.reset();assertTrue(budget.take(Int.MIN_VALUE,1))
 }
}
