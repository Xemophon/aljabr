package com.xemophon.aljabr.calculus.integrate

import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.CalculusStep
import org.matheclipse.core.eval.EvalEngine
import org.matheclipse.core.expression.F
import org.matheclipse.core.interfaces.IAST
import org.matheclipse.core.interfaces.IExpr
import org.matheclipse.core.interfaces.ISymbol

/**
 * Step-by-step symbolic integration solver.
 *
 * Design:
 *   expression
 *       ↓
 *   rule matching
 *       ↓
 *   derivation tree
 *       ↓
 *   verification
 *       ↓
 *   CalculusStep renderer
 *
 * Symja is used to:
 *   - differentiate
 *   - simplify algebra
 *   - verify results
 *   - integrate small sub-problems when explicitly requested
 *
 * Symja's Integrate[...] is NOT used to invent the explanation.
 */
class IntegrationSolver(
    private val engine: EvalEngine
) {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Existing API preserved for compatibility with the rest of the project.
     */
    fun solveIntegral(
        expr: IExpr,
        variable: ISymbol,
        steps: MutableList<CalculusStep>
    ): IExpr? {

        val derivation = integrate(expr, variable) ?: return null

        val rendered = render(derivation)
        steps.addAll(rendered)

        return derivation.result
    }

    /**
     * Produces a mathematical derivation without touching the UI layer.
     */
    internal fun integrate(
        expr: IExpr,
        variable: ISymbol
    ): IntegrationDerivation? {

        for (rule in rules) {
            val result = rule.tryApply(expr, variable)

            if (result != null) {
                return result
            }
        }

        return null
    }

    // -------------------------------------------------------------------------
    // Derivation model
    // -------------------------------------------------------------------------

    internal sealed interface IntegrationDerivation {
        val input: IExpr
        val result: IExpr
    }

    private enum class RuleType {
        CONSTANT,
        VARIABLE,
        SUM,
        CONSTANT_MULTIPLE,
        POWER,
        EXPONENTIAL,
        TRIGONOMETRIC,
        LOGARITHMIC,
        U_SUBSTITUTION,
        INTEGRATION_BY_PARTS
    }

    private data class RuleDerivation(
        val rule: RuleType,
        override val input: IExpr,
        override val result: IExpr,
        val details: Map<String, IExpr> = emptyMap()
    ) : IntegrationDerivation

    private data class SumDerivation(
        override val input: IExpr,
        val terms: List<IntegrationDerivation>,
        override val result: IExpr
    ) : IntegrationDerivation

    private data class ConstantMultipleDerivation(
        override val input: IExpr,
        val constant: IExpr,
        val inner: IntegrationDerivation,
        override val result: IExpr
    ) : IntegrationDerivation

    private data class SubstitutionDerivation(
        override val input: IExpr,
        val variable: ISymbol,
        val u: IExpr,
        val duDx: IExpr,
        val transformedIntegrand: IExpr,
        val inner: IntegrationDerivation,
        override val result: IExpr
    ) : IntegrationDerivation

    private data class PartsDerivation(
        override val input: IExpr,
        val u: IExpr,
        val dv: IExpr,
        val du: IExpr,
        val v: IExpr,
        val remainingIntegrand: IExpr,
        val remaining: IntegrationDerivation,
        override val result: IExpr
    ) : IntegrationDerivation

    // -------------------------------------------------------------------------
    // Rule interface
    // -------------------------------------------------------------------------

    private interface IntegrationRule {
        fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): IntegrationDerivation?
    }

    // -------------------------------------------------------------------------
    // Rule ordering
    // -------------------------------------------------------------------------

    /**
     * Ordered from simple/local rules to more complicated transformations.
     *
     * The important part is that every rule must either:
     *   - produce a complete derivation
     *   - or return null
     *
     * No rule is allowed to partially modify the UI step list.
     */
    private val rules: List<IntegrationRule> by lazy {
        listOf(
            ConstantRule(),
            VariableRule(),
            SumRule(),
            ConstantMultipleRule(),
            PowerRule(),
            ExponentialRule(),
            TrigonometricRule(),
            LogarithmicRule(),
            USubstitutionRule(),
            IntegrationByPartsRule()
        )
    }

    // -------------------------------------------------------------------------
    // 1. Constant Rule
    // -------------------------------------------------------------------------

    private inner class ConstantRule : IntegrationRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): IntegrationDerivation? {

            if (!expr.isFree(variable, true)) {
                return null
            }

            val result = engine.evaluate(
                F.Times(expr, variable)
            )

            if (!verify(expr, result, variable)) {
                return null
            }

            return RuleDerivation(
                rule = RuleType.CONSTANT,
                input = expr,
                result = result
            )
        }
    }

    // -------------------------------------------------------------------------
    // 2. Variable Rule
    // -------------------------------------------------------------------------

    private inner class VariableRule : IntegrationRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): IntegrationDerivation? {

            if (expr != variable) {
                return null
            }

            // ∫ x dx = x²/2
            val result = engine.evaluate(
                F.Divide(
                    F.Power(variable, F.C2),
                    F.C2
                )
            )

            if (!verify(expr, result, variable)) {
                return null
            }

            return RuleDerivation(
                rule = RuleType.VARIABLE,
                input = expr,
                result = result
            )
        }
    }

    // -------------------------------------------------------------------------
    // 3. Sum Rule
    // -------------------------------------------------------------------------

    private inner class SumRule : IntegrationRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): IntegrationDerivation? {

            if (expr !is IAST || !expr.isPlus) {
                return null
            }

            val terms = mutableListOf<IntegrationDerivation>()

            for (i in 1 until expr.size()) {

                val term =
                    integrate(expr.get(i), variable)
                        ?: return null

                terms += term
            }

            val result = engine.evaluate(
                F.Plus(
                    *terms.map { it.result }.toTypedArray()
                )
            )

            if (!verify(expr, result, variable)) {
                return null
            }

            return SumDerivation(
                input = expr,
                terms = terms,
                result = result
            )
        }
    }

    // -------------------------------------------------------------------------
    // 4. Constant Multiple Rule
    // -------------------------------------------------------------------------

    private inner class ConstantMultipleRule : IntegrationRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): IntegrationDerivation? {

            if (expr !is IAST || !expr.isTimes) {
                return null
            }

            val constants = mutableListOf<IExpr>()
            val variableParts = mutableListOf<IExpr>()

            for (i in 1 until expr.size()) {
                val part = expr.get(i)

                if (part.isFree(variable, true)) {
                    constants += part
                } else {
                    variableParts += part
                }
            }

            if (constants.isEmpty() || variableParts.isEmpty()) {
                return null
            }

            val constant = engine.evaluate(
                F.Times(*constants.toTypedArray())
            )

            val innerExpr =
                if (variableParts.size == 1) {
                    variableParts[0]
                } else {
                    engine.evaluate(
                        F.Times(*variableParts.toTypedArray())
                    )
                }

            val inner =
                integrate(innerExpr, variable)
                    ?: return null

            val result = engine.evaluate(
                F.Times(constant, inner.result)
            )

            if (!verify(expr, result, variable)) {
                return null
            }

            return ConstantMultipleDerivation(
                input = expr,
                constant = constant,
                inner = inner,
                result = result
            )
        }
    }

    // -------------------------------------------------------------------------
    // 5. Power Rule
    // -------------------------------------------------------------------------

    private inner class PowerRule : IntegrationRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): IntegrationDerivation? {

            if (expr !is IAST || !expr.isPower) {
                return null
            }

            val base = expr.get(1)
            val exponent = expr.get(2)

            if (base != variable) {
                return null
            }

            // n = -1 is the logarithmic case.
            if (isNegativeOne(exponent)) {
                return null
            }

            val nPlusOne = engine.evaluate(
                F.Plus(exponent, F.C1)
            )

            val result = engine.evaluate(
                F.Divide(
                    F.Power(variable, nPlusOne),
                    nPlusOne
                )
            )

            if (!verify(expr, result, variable)) {
                return null
            }

            return RuleDerivation(
                rule = RuleType.POWER,
                input = expr,
                result = result,
                details = mapOf(
                    "n" to exponent,
                    "nPlusOne" to nPlusOne
                )
            )
        }
    }

    // -------------------------------------------------------------------------
    // 6. Exponential Rule
    // -------------------------------------------------------------------------

    private inner class ExponentialRule : IntegrationRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): IntegrationDerivation? {

            /*
             * ∫ e^x dx = e^x
             */
            if (expr is IAST &&
                expr.isPower &&
                expr.get(1) == F.E &&
                expr.get(2) == variable
            ) {

                val result = expr

                if (!verify(expr, result, variable)) {
                    return null
                }

                return RuleDerivation(
                    rule = RuleType.EXPONENTIAL,
                    input = expr,
                    result = result
                )
            }

            /*
             * ∫ Exp(x) dx = Exp(x)
             */
            if (expr is IAST &&
                expr.head() == F.Exp &&
                expr.get(1) == variable
            ) {

                val result = expr

                if (!verify(expr, result, variable)) {
                    return null
                }

                return RuleDerivation(
                    rule = RuleType.EXPONENTIAL,
                    input = expr,
                    result = result
                )
            }

            return null
        }
    }

    // -------------------------------------------------------------------------
    // 7. Trigonometric Rules
    // -------------------------------------------------------------------------

    private inner class TrigonometricRule : IntegrationRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): IntegrationDerivation? {

            val result = when {

                expr.isASTHead(F.Sin) &&
                        expr.get(1) == variable -> {

                    engine.evaluate(
                        F.Times(
                            F.CN1,
                            F.Cos(variable)
                        )
                    )
                }

                expr.isASTHead(F.Cos) &&
                        expr.get(1) == variable -> {

                    F.Sin(variable)
                }

                expr.isASTHead(F.Tan) &&
                        expr.get(1) == variable -> {

                    engine.evaluate(
                        F.Times(
                            F.CN1,
                            F.Log(
                                F.Cos(variable)
                            )
                        )
                    )
                }

                else -> null
            } ?: return null

            if (!verify(expr, result, variable)) {
                return null
            }

            return RuleDerivation(
                rule = RuleType.TRIGONOMETRIC,
                input = expr,
                result = result
            )
        }
    }

    // -------------------------------------------------------------------------
    // 8. Logarithmic Rule
    // -------------------------------------------------------------------------

    private inner class LogarithmicRule : IntegrationRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): IntegrationDerivation? {

            /*
             * ∫ 1/x dx = log(x)
             *
             * This is represented structurally as x^-1.
             */
            if (expr is IAST &&
                expr.isPower &&
                expr.get(1) == variable &&
                isNegativeOne(expr.get(2))
            ) {

                val result = F.Log(variable)

                if (!verify(expr, result, variable)) {
                    return null
                }

                return RuleDerivation(
                    rule = RuleType.LOGARITHMIC,
                    input = expr,
                    result = result
                )
            }

            /*
             * ∫ Log(x) dx
             *
             * ∫ log(x) dx = x log(x) - x
             */
            if (expr.isASTHead(F.Log) &&
                expr.get(1) == variable
            ) {

                val result = engine.evaluate(
                    F.Subtract(
                        F.Times(
                            variable,
                            F.Log(variable)
                        ),
                        variable
                    )
                )

                if (!verify(expr, result, variable)) {
                    return null
                }

                return RuleDerivation(
                    rule = RuleType.LOGARITHMIC,
                    input = expr,
                    result = result
                )
            }

            return null
        }
    }

    // -------------------------------------------------------------------------
    // 9. U-Substitution
    // -------------------------------------------------------------------------

    private inner class USubstitutionRule : IntegrationRule {

        private val uSymbol: ISymbol by lazy {
            engine.parse("u") as ISymbol
        }

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): IntegrationDerivation? {

            if (expr !is IAST) {
                return null
            }

            val candidates =
                getUSubCandidates(expr, variable)

            for (u in candidates) {

                // du/dx
                val duDx = engine.evaluate(
                    F.D(u, variable)
                )

                if (duDx.isZero) {
                    continue
                }

                /*
                 * We want:
                 *
                 * original integrand
                 * ------------------
                 *       du/dx
                 *
                 * to become a function of u only.
                 */
                val ratio = engine.evaluate(
                    F.Divide(expr, duDx)
                )

                val substituted = engine.evaluate(
                    F.ReplaceAll(
                        ratio,
                        F.Rule(u, uSymbol)
                    )
                )

                /*
                 * If x remains, this is not a valid clean substitution.
                 */
                if (!substituted.isFree(variable, true)) {

                    continue
                }

                val inner =
                    integrate(substituted, uSymbol)
                        ?: continue

                if (!verify(
                        substituted,
                        inner.result,
                        uSymbol
                    )) {
                    continue
                }

                val finalResult =
                    engine.evaluate(
                        F.ReplaceAll(
                            inner.result,
                            F.Rule(uSymbol, u)
                        )
                    )

                /*
                 * Most important verification:
                 *
                 * d/dx [final answer]
                 * ==
                 * original integrand
                 */
                if (!verify(
                        expr,
                        finalResult,
                        variable
                    )) {
                    continue
                }

                return SubstitutionDerivation(
                    input = expr,
                    variable = variable,
                    u = u,
                    duDx = duDx,
                    transformedIntegrand = substituted,
                    inner = inner,
                    result = finalResult
                )
            }

            return null
        }

        /**
         * Finds plausible inner functions.
         *
         * Examples:
         *
         *   sin(x²)      -> x²
         *   e^(x² + 1)   -> x² + 1
         *   log(x³ + 2)  -> x³ + 2
         *
         * Candidates are only hypotheses.
         * The ratio/verification logic decides whether they actually work.
         */
        private fun getUSubCandidates(
            expr: IAST,
            variable: ISymbol
        ): List<IExpr> {

            val candidates = LinkedHashSet<IExpr>()

            fun scan(e: IExpr) {

                if (e == variable) {
                    return
                }

                if (e.isFree(variable, true)) {
                    return
                }

                if (e is IAST) {

                    when (e.head()) {

                        F.Power -> {
                            candidates += e.get(1)

                            // Useful for e^f(x)
                            if (!e.get(2).isFree(variable, true)) {
                                candidates += e.get(2)
                            }
                        }

                        F.Exp,
                        F.Sin,
                        F.Cos,
                        F.Tan,
                        F.Sec,
                        F.Csc,
                        F.Cot,
                        F.Log,
                        F.ArcSin,
                        F.ArcCos,
                        F.ArcTan -> {
                            candidates += e.get(1)
                        }
                    }

                    e.forEach(::scan)
                }
            }

            scan(expr)

            return candidates
                .filter { it != variable }
                .toList()
        }
    }

    // -------------------------------------------------------------------------
    // 10. Integration by Parts
    // -------------------------------------------------------------------------

    private inner class IntegrationByPartsRule : IntegrationRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): IntegrationDerivation? {

            if (expr !is IAST ||
                !expr.isTimes ||
                expr.size() != 3
            ) {
                return null
            }

            val first = expr.get(1)
            val second = expr.get(2)

            val choice =
                selectUAndDv(first, second, variable)
                    ?: return null

            val u = choice.first
            val dv = choice.second

            val du = engine.evaluate(
                F.D(u, variable)
            )

            /*
             * v = ∫ dv
             *
             * Deliberately ask the solver first.
             * This gives us a real derivation when possible.
             */
            val vDerivation =
                integrate(dv, variable)

            val v =
                vDerivation?.result
                    ?: return null

            val remainingIntegrand =
                engine.evaluate(
                    F.Times(v, du)
                )

            val remaining =
                integrate(
                    remainingIntegrand,
                    variable
                ) ?: return null

            val uv =
                engine.evaluate(
                    F.Times(u, v)
                )

            val result =
                engine.evaluate(
                    F.Subtract(
                        uv,
                        remaining.result
                    )
                )

            if (!verify(expr, result, variable)) {
                return null
            }

            return PartsDerivation(
                input = expr,
                u = u,
                dv = dv,
                du = du,
                v = v,
                remainingIntegrand = remainingIntegrand,
                remaining = remaining,
                result = result
            )
        }

        /**
         * LIATE-ish heuristic.
         *
         * Lower number = better candidate for u.
         */
        private fun selectUAndDv(
            first: IExpr,
            second: IExpr,
            variable: ISymbol
        ): Pair<IExpr, IExpr>? {

            val p1 = getLIATEPriority(first, variable)
            val p2 = getLIATEPriority(second, variable)

            return if (p1 <= p2) {
                first to second
            } else {
                second to first
            }
        }

        private fun getLIATEPriority(
            expr: IExpr,
            variable: ISymbol
        ): Int {

            if (expr.isFree(variable, true)) {
                return 5
            }

            if (expr is IAST) {

                when (expr.head()) {

                    F.Log ->
                        return 0

                    F.ArcSin,
                    F.ArcCos,
                    F.ArcTan ->
                        return 1

                    F.Sin,
                    F.Cos,
                    F.Tan ->
                        return 3

                    F.Exp ->
                        return 4
                }

                if (expr.isPower &&
                    expr.arg1() == F.E
                ) {
                    return 4
                }
            }

            return 2
        }
    }


    // -------------------------------------------------------------------------
    // Verification
    // -------------------------------------------------------------------------

    /**
     * Verify:
     *
     *     d/dx(result) == original integrand
     *
     * This is the final safety check for a successful integration strategy.
     */
    private fun verify(
        originalIntegrand: IExpr,
        antiderivative: IExpr,
        variable: ISymbol
    ): Boolean {

        return try {

            val derivative =
                engine.evaluate(
                    F.D(antiderivative, variable)
                )

            mathematicallyEquivalent(
                originalIntegrand,
                derivative
            )

        } catch (_: Exception) {
            false
        }
    }

    /**
     * Uses Symja simplification to test whether two expressions are equivalent.
     */
    private fun mathematicallyEquivalent(
        a: IExpr,
        b: IExpr
    ): Boolean {

        return try {

            val difference =
                engine.evaluate(
                    F.Subtract(a, b)
                )

            if (difference.isZero) {
                true
            } else {
                /*
                 * Sometimes Together/FullSimplify is useful when the raw
                 * difference does not immediately reduce to zero.
                 */
                val simplified =
                    engine.evaluate(
                        F.FullSimplify(difference)
                    )

                simplified.isZero
            }

        } catch (_: Exception) {
            false
        }
    }

    // -------------------------------------------------------------------------
    // Renderer
    // -------------------------------------------------------------------------

    private fun render(
        derivation: IntegrationDerivation
    ): List<CalculusStep> {

        return when (derivation) {

            is RuleDerivation ->
                renderRule(derivation)

            is SumDerivation ->
                renderSum(derivation)

            is ConstantMultipleDerivation ->
                renderConstantMultiple(derivation)

            is SubstitutionDerivation ->
                renderSubstitution(derivation)

            is PartsDerivation ->
                renderParts(derivation)
        }
    }

    private fun renderRule(
        derivation: RuleDerivation
    ): List<CalculusStep> {

        val variable =
            inferVariableFromDerivation(derivation)

        val inputLatex =
            "\\int ${derivation.input.toLaTeX()} \\, d$variable"

        return when (derivation.rule) {

            RuleType.CONSTANT -> {
                listOf(
                    CalculusStep(
                        "Constant Rule",
                        inputLatex,
                        derivation.result.toLaTeX()
                    )
                )
            }

            RuleType.VARIABLE -> {
                listOf(
                    CalculusStep(
                        "Power Rule",
                        inputLatex,
                        derivation.result.toLaTeX()
                    )
                )
            }

            RuleType.POWER -> {

                val n =
                    derivation.details["n"]

                listOf(
                    CalculusStep(
                        "Power Rule",
                        inputLatex,
                        if (n != null) {
                            "\\frac{" +
                                    "${inferVariableFromExpression(derivation.input, n)}^{" +
                                    "${engine.evaluate(F.Plus(n, F.C1)).toLaTeX()}" +
                                    "}}{" +
                                    "${engine.evaluate(F.Plus(n, F.C1)).toLaTeX()}" +
                                    "}"
                        } else {
                            derivation.result.toLaTeX()
                        }
                    ),
                    CalculusStep(
                        "Simplify",
                        derivation.result.toLaTeX(),
                        derivation.result.toLaTeX()
                    )
                )
            }

            RuleType.EXPONENTIAL -> {
                listOf(
                    CalculusStep(
                        "Exponential Rule",
                        inputLatex,
                        derivation.result.toLaTeX()
                    )
                )
            }

            RuleType.TRIGONOMETRIC -> {
                listOf(
                    CalculusStep(
                        "Trigonometric Rule",
                        inputLatex,
                        derivation.result.toLaTeX()
                    )
                )
            }

            RuleType.LOGARITHMIC -> {
                listOf(
                    CalculusStep(
                        "Logarithmic Rule",
                        inputLatex,
                        derivation.result.toLaTeX()
                    )
                )
            }

            else -> {
                listOf(
                    CalculusStep(
                        "Apply Rule",
                        inputLatex,
                        derivation.result.toLaTeX()
                    )
                )
            }
        }
    }

    /**
     * Simple fallback used only for renderer text.
     *
     * It intentionally does not alter mathematics.
     */
    private fun inferVariableFromDerivation(
        derivation: RuleDerivation
    ): String {

        return when {
            derivation.input.isASTHead(F.Sin) &&
                    derivation.input.get(1) is ISymbol ->
                (derivation.input.get(1) as ISymbol).toLaTeX()

            derivation.input.isASTHead(F.Cos) &&
                    derivation.input.get(1) is ISymbol ->
                (derivation.input.get(1) as ISymbol).toLaTeX()

            derivation.input.isASTHead(F.Log) &&
                    derivation.input.get(1) is ISymbol ->
                (derivation.input.get(1) as ISymbol).toLaTeX()

            derivation.input is ISymbol ->
                derivation.input.toLaTeX()

            derivation.input is IAST &&
                    derivation.input.isPower &&
                    derivation.input.get(1) is ISymbol ->
                (derivation.input.get(1) as ISymbol).toLaTeX()

            else ->
                "x"
        }
    }

    private fun inferVariableFromExpression(
        expr: IExpr,
        ignored: IExpr
    ): String {

        return when {
            expr is IAST &&
                    expr.isPower &&
                    expr.get(1) is ISymbol ->
                (expr.get(1) as ISymbol).toLaTeX()

            else ->
                "x"
        }
    }

    private fun renderSum(
        derivation: SumDerivation
    ): List<CalculusStep> {

        val steps = mutableListOf<CalculusStep>()

        steps += CalculusStep(
            "Sum Rule",
            "\\int (${derivation.input.toLaTeX()}) \\, dx",
            "Integrate each term separately"
        )

        for (term in derivation.terms) {
            steps += render(term)
        }

        steps += CalculusStep(
            "Combine Terms",
            derivation.terms.joinToString(
                separator = " + "
            ) {
                it.result.toLaTeX()
            },
            derivation.result.toLaTeX()
        )

        return steps
    }

    private fun renderConstantMultiple(
        derivation: ConstantMultipleDerivation
    ): List<CalculusStep> {

        val steps = mutableListOf<CalculusStep>()

        steps += CalculusStep(
            "Constant Multiple Rule",
            "\\int (${derivation.input.toLaTeX()}) \\, dx",
            "${derivation.constant.toLaTeX()} " +
                    "\\int (${removeConstantFromExpression(
                        derivation.input,
                        derivation.constant
                    ).toLaTeX()}) \\, dx"
        )

        steps += render(derivation.inner)

        steps += CalculusStep(
            "Multiply by Constant",
            "${derivation.constant.toLaTeX()} " +
                    "${derivation.inner.result.toLaTeX()}",
            derivation.result.toLaTeX()
        )

        return steps
    }

    private fun renderSubstitution(
        derivation: SubstitutionDerivation
    ): List<CalculusStep> {

        val steps = mutableListOf<CalculusStep>()

        steps += CalculusStep(
            "U-Substitution",
            "\\int (${derivation.input.toLaTeX()}) \\, " +
                    "d${derivation.variable.toLaTeX()}",
            "Let \\(u = ${derivation.u.toLaTeX()}\\)"
        )

        steps += CalculusStep(
            "Differentiate",
            "u = ${derivation.u.toLaTeX()}",
            "du = ${derivation.duDx.toLaTeX()} \\, " +
                    "d${derivation.variable.toLaTeX()}"
        )

        steps += CalculusStep(
            "Substitute",
            "\\int (${derivation.input.toLaTeX()}) \\, " +
                    "d${derivation.variable.toLaTeX()}",
            "\\int (${derivation.transformedIntegrand.toLaTeX()}) \\, du"
        )

        steps += render(derivation.inner)

        steps += CalculusStep(
            "Back-Substitute",
            "u = ${derivation.u.toLaTeX()}",
            derivation.result.toLaTeX()
        )

        return steps
    }

    private fun renderParts(
        derivation: PartsDerivation
    ): List<CalculusStep> {

        val steps = mutableListOf<CalculusStep>()

        steps += CalculusStep(
            "Integration by Parts",
            "\\int (${derivation.input.toLaTeX()}) \\, dx",
            "Let " +
                    "\\(u = ${derivation.u.toLaTeX()}\\), " +
                    "\\(dv = ${derivation.dv.toLaTeX()} \\, dx\\)"
        )

        steps += CalculusStep(
            "Differentiate and Integrate",
            "u = ${derivation.u.toLaTeX()}, " +
                    "dv = ${derivation.dv.toLaTeX()} \\, dx",
            "du = ${derivation.du.toLaTeX()} \\, dx, " +
                    "v = ${derivation.v.toLaTeX()}"
        )

        val uv =
            engine.evaluate(
                F.Times(
                    derivation.u,
                    derivation.v
                )
            )

        val vdu =
            engine.evaluate(
                F.Times(
                    derivation.v,
                    derivation.du
                )
            )

        steps += CalculusStep(
            "Apply Integration by Parts",
            "\\int u\\,dv = uv - \\int v\\,du",
            "${uv.toLaTeX()} - " +
                    "\\int (${vdu.toLaTeX()}) \\, dx"
        )

        steps += render(
            derivation.remaining
        )

        steps += CalculusStep(
            "Simplify",
            "...",
            derivation.result.toLaTeX()
        )

        return steps
    }

    // -------------------------------------------------------------------------
    // Expression helpers
    // -------------------------------------------------------------------------

    private fun removeConstantFromExpression(
        expr: IExpr,
        constant: IExpr
    ): IExpr {

        return engine.evaluate(
            F.Divide(
                expr,
                constant
            )
        )
    }

    private fun isNegativeOne(
        expr: IExpr
    ): Boolean {

        return engine.evaluate(
            F.Equal(
                expr,
                F.CN1
            )
        ) == F.True
    }

    private fun IExpr.isASTHead(
        head: IExpr
    ): Boolean {

        return this is IAST &&
                this.head() == head
    }

    private fun IExpr.toLaTeX(): String =
        SymjaUtils.toLaTeX(this.toString())
}