package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.service.fcfb.animation.choreography.Choreography
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayScript

class PassPlayScript : PlayScript {
    private val completed = CompletedPassScript()
    private val incomplete = IncompletePassScript()
    private val interception = InterceptionScript()
    private val sack = SackScript()

    override fun choreograph(context: PlayContext): Choreography {
        val result = context.play.actualResult
        val script =
            when {
                context.play.result == Scenario.INCOMPLETE -> incomplete
                result == ActualResult.LOSS || result == ActualResult.SAFETY -> sack
                result == ActualResult.TURNOVER || result == ActualResult.TURNOVER_TOUCHDOWN -> interception
                else -> completed
            }
        return script.choreograph(context)
    }
}
