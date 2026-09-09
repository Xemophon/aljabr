package com.xemophon.aljabr.ui.components.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.xemophon.aljabr.modules.algebra.bde.BDEResult

interface OdeStateHolder {
    var BDEResult: BDEResult?
    val odeConditions: SnapshotStateList<String>
    var odeConditionFocusIndex: Int

    fun isOdeConditionFocused(calculatorMode: CalculatorMode): Boolean
    fun setOdeConditionFocus(index: Int)
    fun addOdeCondition()
    fun removeOdeCondition(index: Int)
    fun clearOdeState()
}

class DefaultOdeStateHolder : OdeStateHolder {
    override var BDEResult by mutableStateOf<BDEResult?>(null)
    override val odeConditions = mutableStateListOf<String>()
    override var odeConditionFocusIndex by mutableIntStateOf(-1)

    override fun isOdeConditionFocused(calculatorMode: CalculatorMode): Boolean {
        return calculatorMode == CalculatorMode.ODE && 
                odeConditionFocusIndex >= 0 && 
                odeConditionFocusIndex in odeConditions.indices
    }

    override fun setOdeConditionFocus(index: Int) {
        odeConditionFocusIndex = index
    }

    override fun addOdeCondition() {
        odeConditions.add("")
        odeConditionFocusIndex = odeConditions.size - 1
    }

    override fun removeOdeCondition(index: Int) {
        if (index in odeConditions.indices) {
            odeConditions.removeAt(index)
            if (odeConditionFocusIndex == index) {
                odeConditionFocusIndex = -1
            } else if (odeConditionFocusIndex > index) {
                odeConditionFocusIndex -= 1
            }
        }
    }

    override fun clearOdeState() {
        BDEResult = null
        odeConditions.clear()
        odeConditionFocusIndex = -1
    }
}
