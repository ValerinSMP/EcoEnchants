package com.willfp.ecoenchants.progression

import java.util.IdentityHashMap
import java.util.UUID

/** Explicit single-player observation only. No polling or gameplay decisions; the runtime schedules one expiry cleanup. */
internal class OriginalHitDiagnostic(private val millis: () -> Long) {
    private var player: UUID? = null
    private var until = 0L
    private var sink: ((String) -> Unit)? = null
    private class Ticket(val id: Int, var rows: Int = 0)
    private val hits = IdentityHashMap<Any, Ticket>()
    fun start(player: UUID, sink: (String) -> Unit) {
        clear(); this.player = player; until = millis() + 30_000; this.sink = sink
    }
    fun observe(player: UUID?, event: Any) {
        if (!active() || player != this.player || hits.containsKey(event) || hits.size >= 8) return
        hits[event] = Ticket(hits.size + 1)
    }
    fun note(event: Any, stage: String, detail: () -> String = { "" }) {
        if (!active()) return
        val ticket = hits[event] ?: return
        if (ticket.rows >= 24) return
        ticket.rows++
        try { sink?.invoke("Original hit diagnostic player=$player hit=${ticket.id} stage=$stage ${detail()}") }
        catch (_: RuntimeException) { clear() }
        catch (_: LinkageError) { clear() }
    }
    private fun active(): Boolean {
        if (player == null) return false
        if (millis() >= until) { clear(); return false }
        return true
    }
    fun clear() { player = null; until = 0; sink = null; hits.clear() }
}
