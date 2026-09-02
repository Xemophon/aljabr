package com.xemophon.aljabr.modules.calculus.differentiate

import com.xemophon.aljabr.data.SymjaUtils
import com.xemophon.aljabr.ui.components.CalculusStep
import org.matheclipse.core.eval.EvalEngine
import org.matheclipse.core.expression.F
import org.matheclipse.core.interfaces.IAST
import org.matheclipse.core.interfaces.IExpr
import org.matheclipse.core.interfaces.ISymbol

/**
 * Step-aware symbolic differentiation engine.
 *
 * The important architectural rule is:
 *
 *     RULE -> DERIVATION -> VERIFICATION -> UI STEPS
 *
 * Mathematical rules never mutate CalculusStep directly.
 *
 * This gives us:
 *  - clean recursive backtracking
 *  - reusable derivations
 *  - independent verification
 *  - multiple rendering styles later
 *  - honest failure for unsupported expressions
 *
 * Symja is used as a mathematical verifier, not as the explanation generator.
 */
class DerivativeSolver(
    private val engine: EvalEngine
) {

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Existing public API preserved.
     */
    fun solveDerivative(
        expr: IExpr,
        variable: ISymbol,
        steps: MutableList<CalculusStep>,
        addTrivialStep: Boolean = true
    ): IExpr {

        val derivation =
            differentiate(
                expr,
                variable
            )

        /*
         * Unsupported expressions are allowed to fall back to Symja only if
         * there is no way to produce a derivation.
         *
         * We do NOT pretend that the fallback was derived step-by-step.
         */
        if (derivation == null) {

            val fallback =
                engine.evaluate(
                    F.D(expr, variable)
                )

            if (addTrivialStep) {
                steps += CalculusStep(
                    "Direct Symbolic Evaluation",
                    derivativeNotation(expr, variable),
                    fallback.toLaTeX()
                )
            }

            return fallback
        }

        val rendered =
            render(
                derivation,
                addTrivialStep
            )

        steps.addAll(rendered)

        return derivation.result
    }

    /**
     * Internal mathematical API.
     *
     * Returns a structured derivation or null when this solver does not have
     * a rule for the expression.
     */
    fun differentiate(
        expr: IExpr,
        variable: ISymbol
    ): Derivation? {

        for (rule in rules) {
            val result =
                rule.tryApply(
                    expr,
                    variable
                )

            if (result != null) {
                return result
            }
        }

        return null
    }

    // =========================================================================
    // DERIVATION MODEL
    // =========================================================================

    sealed interface Derivation {

        val input: IExpr
        val result: IExpr
    }

    enum class RuleType {

        CONSTANT,
        VARIABLE,

        SUM,
        CONSTANT_MULTIPLE,
        PRODUCT,
        QUOTIENT,

        POWER,
        GENERAL_POWER,

        CHAIN,

        SIN,
        COS,
        TAN,
        SEC,
        CSC,
        COT,

        ARCSIN,
        ARCCOS,
        ARCTAN,

        SINH,
        COSH,
        TANH,

        ARCSINH,
        ARCCOSH,
        ARCTANH,

        LOG,
        EXPONENTIAL
    }

    data class RuleDerivation(
        val rule: RuleType,
        override val input: IExpr,
        override val result: IExpr,
        val details: Map<String, IExpr> = emptyMap()
    ) : Derivation

    data class CompositeDerivation(
        val rule: RuleType,
        override val input: IExpr,
        val children: List<Derivation>,
        override val result: IExpr,
        val details: Map<String, IExpr> = emptyMap()
    ) : Derivation

    // =========================================================================
    // RULE INTERFACE
    // =========================================================================

    private interface DerivativeRule {

        fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): Derivation?
    }

    /**
     * Specific rules first.
     *
     * Generic rules such as ProductRule and ChainRule are deliberately later.
     */
    private val rules: List<DerivativeRule> by lazy {
        listOf(

            ConstantRule(),
            VariableRule(),

            SumRule(),
            ConstantMultipleRule(),

            PowerRule(),
            GeneralPowerRule(),

            ProductRule(),
            QuotientRule(),

            ElementaryFunctionRule()
        )
    }

    // =========================================================================
    // CONSTANT
    // =========================================================================

    private inner class ConstantRule : DerivativeRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): Derivation? {

            if (!expr.isFree(variable, true)) {
                return null
            }

            return RuleDerivation(
                rule = RuleType.CONSTANT,
                input = expr,
                result = F.C0
            )
        }
    }

    // =========================================================================
    // VARIABLE
    // =========================================================================

    private inner class VariableRule : DerivativeRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): Derivation? {

            if (expr != variable) {
                return null
            }

            return RuleDerivation(
                rule = RuleType.VARIABLE,
                input = expr,
                result = F.C1
            )
        }
    }

    // =========================================================================
    // SUM
    // =========================================================================

    private inner class SumRule : DerivativeRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): Derivation? {

            if (expr !is IAST || !expr.isPlus) {
                return null
            }

            val children =
                mutableListOf<Derivation>()

            for (i in 1 until expr.size()) {

                val child =
                    differentiate(
                        expr.get(i),
                        variable
                    ) ?: return null

                children += child
            }

            val result =
                engine.evaluate(
                    F.Plus(
                        *children
                            .map { it.result }
                            .toTypedArray()
                    )
                )

            return CompositeDerivation(
                rule = RuleType.SUM,
                input = expr,
                children = children,
                result = result
            )
        }
    }

    // =========================================================================
    // CONSTANT MULTIPLE
    // =========================================================================

    private inner class ConstantMultipleRule :
        DerivativeRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): Derivation? {

            if (expr !is IAST ||
                !expr.isTimes
            ) {
                return null
            }

            val constants =
                mutableListOf<IExpr>()

            val variableParts =
                mutableListOf<IExpr>()

            for (i in 1 until expr.size()) {

                val term =
                    expr.get(i)

                if (term.isFree(variable, true)) {
                    constants += term
                } else {
                    variableParts += term
                }
            }

            if (constants.isEmpty() ||
                variableParts.isEmpty()
            ) {
                return null
            }

            val constant =
                engine.evaluate(
                    F.Times(
                        *constants.toTypedArray()
                    )
                )

            val innerExpression =
                if (variableParts.size == 1) {
                    variableParts.first()
                } else {
                    engine.evaluate(
                        F.Times(
                            *variableParts.toTypedArray()
                        )
                    )
                }

            val inner =
                differentiate(
                    innerExpression,
                    variable
                ) ?: return null

            val result =
                engine.evaluate(
                    F.Times(
                        constant,
                        inner.result
                    )
                )

            return CompositeDerivation(
                rule = RuleType.CONSTANT_MULTIPLE,
                input = expr,
                children = listOf(inner),
                result = result,
                details = mapOf(
                    "constant" to constant,
                    "inner" to innerExpression
                )
            )
        }
    }

    // =========================================================================
    // POWER
    // =========================================================================

    private inner class PowerRule :
        DerivativeRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): Derivation? {

            if (expr !is IAST ||
                !expr.isPower
            ) {
                return null
            }

            val base =
                expr.arg1()

            val exponent =
                expr.arg2()

            /*
             * u^n, where n is constant.
             */
            if (exponent.isFree(variable, true)) {

                val exponentMinusOne =
                    engine.evaluate(
                        F.Subtract(
                            exponent,
                            F.C1
                        )
                    )

                val outer =
                    engine.evaluate(
                        F.Times(
                            exponent,
                            F.Power(
                                base,
                                exponentMinusOne
                            )
                        )
                    )

                /*
                 * x^n
                 */
                if (base == variable) {

                    return RuleDerivation(
                        rule = RuleType.POWER,
                        input = expr,
                        result = outer,
                        details = mapOf(
                            "base" to base,
                            "exponent" to exponent
                        )
                    )
                }

                /*
                 * u(x)^n
                 *
                 * Need chain rule.
                 */
                val inner =
                    differentiate(
                        base,
                        variable
                    ) ?: return null

                val result =
                    engine.evaluate(
                        F.Times(
                            outer,
                            inner.result
                        )
                    )

                return CompositeDerivation(
                    rule = RuleType.POWER,
                    input = expr,
                    children = listOf(inner),
                    result = result,
                    details = mapOf(
                        "base" to base,
                        "exponent" to exponent,
                        "outerDerivative" to outer
                    )
                )
            }

            return null
        }
    }

    // =========================================================================
    // GENERAL POWER u(x)^v(x)
    // =========================================================================

    private inner class GeneralPowerRule :
        DerivativeRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): Derivation? {

            if (expr !is IAST ||
                !expr.isPower
            ) {
                return null
            }

            val u =
                expr.arg1()

            val v =
                expr.arg2()

            /*
             * This rule only applies when the exponent is actually variable.
             */
            if (v.isFree(variable, true)) {
                return null
            }

            val du =
                differentiate(
                    u,
                    variable
                ) ?: return null

            val dv =
                differentiate(
                    v,
                    variable
                ) ?: return null

            /*
             * d(u^v)
             *
             * = u^v * ( v/u * u' + ln(u) * v' )
             */
            val firstTerm =
                engine.evaluate(
                    F.Times(
                        F.Divide(v, u),
                        du.result
                    )
                )

            val secondTerm =
                engine.evaluate(
                    F.Times(
                        F.Log(u),
                        dv.result
                    )
                )

            val inner =
                engine.evaluate(
                    F.Plus(
                        firstTerm,
                        secondTerm
                    )
                )

            val result =
                engine.evaluate(
                    F.Times(
                        expr,
                        inner
                    )
                )

            return CompositeDerivation(
                rule = RuleType.GENERAL_POWER,
                input = expr,
                children = listOf(
                    du,
                    dv
                ),
                result = result,
                details = mapOf(
                    "u" to u,
                    "v" to v,
                    "du" to du.result,
                    "dv" to dv.result,
                    "inner" to inner
                )
            )
        }
    }

    // =========================================================================
    // PRODUCT
    // =========================================================================

    private inner class ProductRule :
        DerivativeRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): Derivation? {

            if (expr !is IAST ||
                !expr.isTimes ||
                expr.size() < 3
            ) {
                return null
            }

            val factors =
                (1 until expr.size())
                    .map { expr.get(it) }

            /*
             * For 2 factors:
             *
             * (uv)' = u'v + uv'
             */
            if (factors.size == 2) {

                val u = factors[0]
                val v = factors[1]

                val du =
                    differentiate(
                        u,
                        variable
                    ) ?: return null

                val dv =
                    differentiate(
                        v,
                        variable
                    ) ?: return null

                val first =
                    engine.evaluate(
                        F.Times(
                            du.result,
                            v
                        )
                    )

                val second =
                    engine.evaluate(
                        F.Times(
                            u,
                            dv.result
                        )
                    )

                val result =
                    engine.evaluate(
                        F.Plus(
                            first,
                            second
                        )
                    )

                return CompositeDerivation(
                    rule = RuleType.PRODUCT,
                    input = expr,
                    children = listOf(
                        du,
                        dv
                    ),
                    result = result,
                    details = mapOf(
                        "u" to u,
                        "v" to v,
                        "uPrime" to du.result,
                        "vPrime" to dv.result
                    )
                )
            }

            /*
             * For >2 factors, recursively split:
             *
             * f1 * (f2*f3*...)
             */
            val first =
                factors.first()

            val rest =
                if (factors.size == 2) {
                    factors[1]
                } else {
                    engine.evaluate(
                        F.Times(
                            *factors.drop(1).toTypedArray()
                        )
                    )
                }

            val dFirst =
                differentiate(
                    first,
                    variable
                ) ?: return null

            val dRest =
                differentiate(
                    rest,
                    variable
                ) ?: return null

            val term1 =
                engine.evaluate(
                    F.Times(
                        dFirst.result,
                        rest
                    )
                )

            val term2 =
                engine.evaluate(
                    F.Times(
                        first,
                        dRest.result
                    )
                )

            val result =
                engine.evaluate(
                    F.Plus(
                        term1,
                        term2
                    )
                )

            return CompositeDerivation(
                rule = RuleType.PRODUCT,
                input = expr,
                children = listOf(
                    dFirst,
                    dRest
                ),
                result = result,
                details = mapOf(
                    "u" to first,
                    "v" to rest,
                    "uPrime" to dFirst.result,
                    "vPrime" to dRest.result
                )
            )
        }
    }

    // =========================================================================
    // QUOTIENT
    // =========================================================================

    private inner class QuotientRule :
        DerivativeRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): Derivation? {

            if (expr !is IAST ||
                !expr.isDivide()
            ) {
                return null
            }

            val numerator =
                expr.arg1()

            val denominator =
                expr.arg2()

            val du =
                differentiate(
                    numerator,
                    variable
                ) ?: return null

            val dv =
                differentiate(
                    denominator,
                    variable
                ) ?: return null

            /*
             * (u/v)'
             * =
             * (v u' - u v') / v²
             */
            val numeratorResult =
                engine.evaluate(
                    F.Subtract(
                        F.Times(
                            denominator,
                            du.result
                        ),
                        F.Times(
                            numerator,
                            dv.result
                        )
                    )
                )

            val denominatorResult =
                engine.evaluate(
                    F.Power(
                        denominator,
                        F.C2
                    )
                )

            val result =
                engine.evaluate(
                    F.Divide(
                        numeratorResult,
                        denominatorResult
                    )
                )

            return CompositeDerivation(
                rule = RuleType.QUOTIENT,
                input = expr,
                children = listOf(
                    du,
                    dv
                ),
                result = result,
                details = mapOf(
                    "u" to numerator,
                    "v" to denominator,
                    "uPrime" to du.result,
                    "vPrime" to dv.result
                )
            )
        }
    }

    // =========================================================================
    // ELEMENTARY FUNCTIONS
    // =========================================================================

    private inner class ElementaryFunctionRule :
        DerivativeRule {

        override fun tryApply(
            expr: IExpr,
            variable: ISymbol
        ): Derivation? {

            if (expr !is IAST) {
                return null
            }

            val argument =
                when (expr.head()) {

                    F.Sin,
                    F.Cos,
                    F.Tan,
                    F.Sec,
                    F.Csc,
                    F.Cot,

                    F.ArcSin,
                    F.ArcCos,
                    F.ArcTan,

                    F.Sinh,
                    F.Cosh,
                    F.Tanh,

                    F.ArcSinh,
                    F.ArcCosh,
                    F.ArcTanh,

                    F.Log,
                    F.Exp ->
                        expr.arg1()

                    else ->
                        return null
                }

            val outerDerivative =
                elementaryOuterDerivative(
                    expr.head(),
                    argument
                ) ?: return null

            /*
             * f(x)
             *
             * If the argument is exactly x, there is no visible chain-rule
             * multiplication.
             */
            if (argument == variable) {

                return RuleDerivation(
                    rule = ruleTypeFor(expr.head()),
                    input = expr,
                    result = outerDerivative
                )
            }

            /*
             * f(g(x))
             *
             * Differentiate g(x).
             */
            val inner =
                differentiate(
                    argument,
                    variable
                ) ?: return null

            val result =
                engine.evaluate(
                    F.Times(
                        outerDerivative,
                        inner.result
                    )
                )

            return CompositeDerivation(
                rule = RuleType.CHAIN,
                input = expr,
                children = listOf(inner),
                result = result,
                details = mapOf(
                    "function" to expr.head(),
                    "argument" to argument,
                    "outerDerivative" to outerDerivative
                )
            )
        }
    }

    // =========================================================================
    // ELEMENTARY OUTER DERIVATIVES
    // =========================================================================

    private fun elementaryOuterDerivative(
        head: IExpr,
        u: IExpr
    ): IExpr? {

        return when (head) {

            F.Sin ->
                F.Cos(u)

            F.Cos ->
                engine.evaluate(
                    F.Times(
                        F.CN1,
                        F.Sin(u)
                    )
                )

            F.Tan ->
                engine.evaluate(
                    F.Power(
                        F.Cos(u),
                        F.CN2
                    )
                )

            F.Sec ->
                engine.evaluate(
                    F.Times(
                        F.Sec(u),
                        F.Tan(u)
                    )
                )

            F.Csc ->
                engine.evaluate(
                    F.Times(
                        F.CN1,
                        F.Csc(u),
                        F.Cot(u)
                    )
                )

            F.Cot ->
                engine.evaluate(
                    F.Times(
                        F.CN1,
                        F.Power(
                            F.Sin(u),
                            F.CN2
                        )
                    )
                )

            F.ArcSin ->
                engine.evaluate(
                    F.Power(
                        F.Subtract(
                            F.C1,
                            F.Power(u, F.C2)
                        ),
                        F.Rational(
                            F.CN1,
                            F.C2
                        )
                    )
                )

            F.ArcCos ->
                engine.evaluate(
                    F.Times(
                        F.CN1,
                        F.Power(
                            F.Subtract(
                                F.C1,
                                F.Power(u, F.C2)
                            ),
                            F.Rational(
                                F.CN1,
                                F.C2
                            )
                        )
                    )
                )

            F.ArcTan ->
                engine.evaluate(
                    F.Power(
                        F.Plus(
                            F.C1,
                            F.Power(u, F.C2)
                        ),
                        F.CN1
                    )
                )

            F.Sinh ->
                F.Cosh(u)

            F.Cosh ->
                F.Sinh(u)

            F.Tanh ->
                engine.evaluate(
                    F.Power(
                        F.Cosh(u),
                        F.CN2
                    )
                )

            F.ArcSinh ->
                engine.evaluate(
                    F.Power(
                        F.Plus(
                            F.Power(u, F.C2),
                            F.C1
                        ),
                        F.Rational(
                            F.CN1,
                            F.C2
                        )
                    )
                )

            F.ArcCosh ->
                engine.evaluate(
                    F.Power(
                        F.Subtract(
                            F.Power(u, F.C2),
                            F.C1
                        ),
                        F.Rational(
                            F.CN1,
                            F.C2
                        )
                    )
                )

            F.ArcTanh ->
                engine.evaluate(
                    F.Power(
                        F.Subtract(
                            F.C1,
                            F.Power(u, F.C2)
                        ),
                        F.CN1
                    )
                )

            F.Log ->
                engine.evaluate(
                    F.Power(
                        u,
                        F.CN1
                    )
                )

            F.Exp ->
                F.Exp(u)

            else ->
                null
        }
    }

    private fun ruleTypeFor(
        head: IExpr
    ): RuleType {

        return when (head) {

            F.Sin -> RuleType.SIN
            F.Cos -> RuleType.COS
            F.Tan -> RuleType.TAN
            F.Sec -> RuleType.SEC
            F.Csc -> RuleType.CSC
            F.Cot -> RuleType.COT

            F.ArcSin -> RuleType.ARCSIN
            F.ArcCos -> RuleType.ARCCOS
            F.ArcTan -> RuleType.ARCTAN

            F.Sinh -> RuleType.SINH
            F.Cosh -> RuleType.COSH
            F.Tanh -> RuleType.TANH

            F.ArcSinh -> RuleType.ARCSINH
            F.ArcCosh -> RuleType.ARCCOSH
            F.ArcTanh -> RuleType.ARCTANH

            F.Log -> RuleType.LOG
            F.Exp -> RuleType.EXPONENTIAL

            else -> RuleType.CHAIN
        }
    }

    // =========================================================================
    // RENDERING
    // =========================================================================

    private fun render(
        derivation: Derivation,
        addTrivialStep: Boolean
    ): List<CalculusStep> {

        return when (derivation) {

            is RuleDerivation ->
                renderRule(
                    derivation,
                    addTrivialStep
                )

            is CompositeDerivation ->
                renderComposite(
                    derivation,
                    addTrivialStep
                )
        }
    }

    // =========================================================================
    // BASIC RULE RENDERER
    // =========================================================================

    private fun renderRule(
        derivation: RuleDerivation,
        addTrivialStep: Boolean
    ): List<CalculusStep> {

        if (!addTrivialStep &&
            (
                    derivation.rule == RuleType.CONSTANT ||
                            derivation.rule == RuleType.VARIABLE
                    )
        ) {
            return emptyList()
        }

        return when (derivation.rule) {

            RuleType.CONSTANT ->
                listOf(
                    CalculusStep(
                        "Constant Rule",
                        derivativeNotation(
                            derivation.input,
                            null
                        ),
                        "0"
                    )
                )

            RuleType.VARIABLE ->
                listOf(
                    CalculusStep(
                        "Variable Rule",
                        derivativeNotation(
                            derivation.input,
                            null
                        ),
                        "1"
                    )
                )

            RuleType.POWER ->
                listOf(
                    CalculusStep(
                        "Power Rule",
                        derivativeNotation(
                            derivation.input,
                            null
                        ),
                        derivation.result.toLaTeX()
                    )
                )

            RuleType.SIN ->
                listOf(
                    CalculusStep(
                        "Derivative of Sine",
                        derivativeNotation(
                            derivation.input,
                            null
                        ),
                        derivation.result.toLaTeX()
                    )
                )

            RuleType.COS ->
                listOf(
                    CalculusStep(
                        "Derivative of Cosine",
                        derivativeNotation(
                            derivation.input,
                            null
                        ),
                        derivation.result.toLaTeX()
                    )
                )

            RuleType.TAN ->
                listOf(
                    CalculusStep(
                        "Derivative of Tangent",
                        derivativeNotation(
                            derivation.input,
                            null
                        ),
                        derivation.result.toLaTeX()
                    )
                )

            RuleType.SEC ->
                listOf(
                    CalculusStep(
                        "Derivative of Secant",
                        derivativeNotation(
                            derivation.input,
                            null
                        ),
                        derivation.result.toLaTeX()
                    )
                )

            RuleType.CSC ->
                listOf(
                    CalculusStep(
                        "Derivative of Cosecant",
                        derivativeNotation(
                            derivation.input,
                            null
                        ),
                        derivation.result.toLaTeX()
                    )
                )

            RuleType.COT ->
                listOf(
                    CalculusStep(
                        "Derivative of Cotangent",
                        derivativeNotation(
                            derivation.input,
                            null
                        ),
                        derivation.result.toLaTeX()
                    )
                )

            RuleType.ARCSIN,
            RuleType.ARCCOS,
            RuleType.ARCTAN,
            RuleType.SINH,
            RuleType.COSH,
            RuleType.TANH,
            RuleType.ARCSINH,
            RuleType.ARCCOSH,
            RuleType.ARCTANH,
            RuleType.LOG,
            RuleType.EXPONENTIAL ->
                listOf(
                    CalculusStep(
                        functionRuleTitle(
                            derivation.rule
                        ),
                        derivativeNotation(
                            derivation.input,
                            null
                        ),
                        derivation.result.toLaTeX()
                    )
                )

            else ->
                listOf(
                    CalculusStep(
                        ruleTitle(
                            derivation.rule
                        ),
                        derivativeNotation(
                            derivation.input,
                            null
                        ),
                        derivation.result.toLaTeX()
                    )
                )
        }
    }

    // =========================================================================
    // COMPOSITE RENDERER
    // =========================================================================

    private fun renderComposite(
        derivation: CompositeDerivation,
        addTrivialStep: Boolean
    ): List<CalculusStep> {

        return when (derivation.rule) {

            RuleType.SUM ->
                renderSum(
                    derivation,
                    addTrivialStep
                )

            RuleType.CONSTANT_MULTIPLE ->
                renderConstantMultiple(
                    derivation,
                    addTrivialStep
                )

            RuleType.PRODUCT ->
                renderProduct(
                    derivation,
                    addTrivialStep
                )

            RuleType.QUOTIENT ->
                renderQuotient(
                    derivation,
                    addTrivialStep
                )

            RuleType.POWER ->
                renderPowerChain(
                    derivation,
                    addTrivialStep
                )

            RuleType.GENERAL_POWER ->
                renderGeneralPower(
                    derivation,
                    addTrivialStep
                )

            RuleType.CHAIN ->
                renderChain(
                    derivation,
                    addTrivialStep
                )

            else ->
                mutableListOf()
        }
    }

    // =========================================================================
    // SUM RENDERING
    // =========================================================================

    private fun renderSum(
        derivation: CompositeDerivation,
        addTrivialStep: Boolean
    ): List<CalculusStep> {

        val steps =
            mutableListOf<CalculusStep>()

        steps += CalculusStep(
            "Sum Rule",
            derivativeNotation(
                derivation.input,
                null
            ),
            "Differentiate each term separately"
        )

        for (child in derivation.children) {
            steps += render(
                child,
                addTrivialStep
            )
        }

        steps += CalculusStep(
            "Combine Terms",
            derivation.children.joinToString(
                " + "
            ) {
                it.result.toLaTeX()
            },
            derivation.result.toLaTeX()
        )

        return steps
    }

    // =========================================================================
    // CONSTANT MULTIPLE RENDERING
    // =========================================================================

    private fun renderConstantMultiple(
        derivation: CompositeDerivation,
        addTrivialStep: Boolean
    ): List<CalculusStep> {

        val constant =
            derivation.details["constant"]

        val inner =
            derivation.details["inner"]

        val steps =
            mutableListOf<CalculusStep>()

        steps += CalculusStep(
            "Constant Multiple Rule",
            derivativeNotation(
                derivation.input,
                null
            ),
            if (constant != null &&
                inner != null
            ) {
                "${constant.toLaTeX()} \\cdot " +
                        "\\frac{d}{dx}(${inner.toLaTeX()})"
            } else {
                derivation.result.toLaTeX()
            }
        )

        for (child in derivation.children) {
            steps += render(
                child,
                addTrivialStep
            )
        }

        steps += CalculusStep(
            "Simplify",
            derivation.children
                .firstOrNull()
                ?.result
                ?.toLaTeX()
                ?: derivation.result.toLaTeX(),
            derivation.result.toLaTeX()
        )

        return steps
    }

    // =========================================================================
    // PRODUCT RENDERING
    // =========================================================================

    private fun renderProduct(
        derivation: CompositeDerivation,
        addTrivialStep: Boolean
    ): List<CalculusStep> {

        val u =
            derivation.details["u"]

        val v =
            derivation.details["v"]

        val uPrime =
            derivation.details["uPrime"]

        val vPrime =
            derivation.details["vPrime"]

        val steps =
            mutableListOf<CalculusStep>()

        steps += CalculusStep(
            "Product Rule",
            derivativeNotation(
                derivation.input,
                null
            ),
            if (u != null &&
                v != null
            ) {
                "\\frac{d}{dx}(uv) = u'v + uv'"
            } else {
                derivation.result.toLaTeX()
            }
        )

        if (uPrime != null &&
            vPrime != null
        ) {

            steps += CalculusStep(
                "Differentiate the Factors",
                "u = ${u?.toLaTeX()}, " +
                        "v = ${v?.toLaTeX()}",
                "u' = ${uPrime.toLaTeX()}, " +
                        "v' = ${vPrime.toLaTeX()}"
            )
        }

        for (child in derivation.children) {
            steps += render(
                child,
                addTrivialStep
            )
        }

        steps += CalculusStep(
            "Apply Product Rule",
            "u'v + uv'",
            derivation.result.toLaTeX()
        )

        return steps
    }

    // =========================================================================
    // QUOTIENT RENDERING
    // =========================================================================

    private fun renderQuotient(
        derivation: CompositeDerivation,
        addTrivialStep: Boolean
    ): List<CalculusStep> {

        val u =
            derivation.details["u"]

        val v =
            derivation.details["v"]

        val uPrime =
            derivation.details["uPrime"]

        val vPrime =
            derivation.details["vPrime"]

        val steps =
            mutableListOf<CalculusStep>()

        steps += CalculusStep(
            "Quotient Rule",
            derivativeNotation(
                derivation.input,
                null
            ),
            "\\frac{vu' - uv'}{v^2}"
        )

        steps += CalculusStep(
            "Differentiate Numerator and Denominator",
            "u = ${u?.toLaTeX()}, " +
                    "v = ${v?.toLaTeX()}",
            "u' = ${uPrime?.toLaTeX()}, " +
                    "v' = ${vPrime?.toLaTeX()}"
        )

        for (child in derivation.children) {
            steps += render(
                child,
                addTrivialStep
            )
        }

        steps += CalculusStep(
            "Apply Quotient Rule",
            "\\frac{vu' - uv'}{v^2}",
            derivation.result.toLaTeX()
        )

        return steps
    }

    // =========================================================================
    // POWER + CHAIN
    // =========================================================================

    private fun renderPowerChain(
        derivation: CompositeDerivation,
        addTrivialStep: Boolean
    ): List<CalculusStep> {

        val base =
            derivation.details["base"]

        val exponent =
            derivation.details["exponent"]

        val outerDerivative =
            derivation.details["outerDerivative"]

        val steps =
            mutableListOf<CalculusStep>()

        steps += CalculusStep(
            "Power Rule",
            derivativeNotation(
                derivation.input,
                null
            ),
            outerDerivative?.toLaTeX()
                ?: derivation.result.toLaTeX()
        )

        steps += CalculusStep(
            "Chain Rule",
            outerDerivative?.toLaTeX()
                ?: derivation.result.toLaTeX(),
            "${outerDerivative?.toLaTeX() ?: ""}" +
                    " \\cdot \\frac{d}{dx}(" +
                    "${base?.toLaTeX() ?: ""})"
        )

        for (child in derivation.children) {
            steps += render(
                child,
                addTrivialStep
            )
        }

        steps += CalculusStep(
            "Simplify",
            "...",
            derivation.result.toLaTeX()
        )

        return steps
    }

    // =========================================================================
    // GENERAL POWER RENDERING
    // =========================================================================

    private fun renderGeneralPower(
        derivation: CompositeDerivation,
        addTrivialStep: Boolean
    ): List<CalculusStep> {

        val u =
            derivation.details["u"]

        val v =
            derivation.details["v"]

        val du =
            derivation.details["du"]

        val dv =
            derivation.details["dv"]

        val inner =
            derivation.details["inner"]

        val steps =
            mutableListOf<CalculusStep>()

        steps += CalculusStep(
            "Logarithmic Differentiation",
            derivativeNotation(
                derivation.input,
                null
            ),
            "${derivation.input.toLaTeX()} " +
                    "\\left(" +
                    "\\frac{${v?.toLaTeX()}}{${u?.toLaTeX()}}" +
                    "u' + " +
                    "\\ln(${u?.toLaTeX()})v'" +
                    "\\right)"
        )

        steps += CalculusStep(
            "Differentiate Base and Exponent",
            "u = ${u?.toLaTeX()}, " +
                    "v = ${v?.toLaTeX()}",
            "u' = ${du?.toLaTeX()}, " +
                    "v' = ${dv?.toLaTeX()}"
        )

        for (child in derivation.children) {
            steps += render(
                child,
                addTrivialStep
            )
        }

        steps += CalculusStep(
            "Substitute",
            inner?.toLaTeX()
                ?: derivation.result.toLaTeX(),
            derivation.result.toLaTeX()
        )

        return steps
    }

    // =========================================================================
    // CHAIN RULE RENDERING
    // =========================================================================

    private fun renderChain(
        derivation: CompositeDerivation,
        addTrivialStep: Boolean
    ): List<CalculusStep> {

        val function =
            derivation.details["function"]

        val argument =
            derivation.details["argument"]

        val outerDerivative =
            derivation.details["outerDerivative"]

        val steps =
            mutableListOf<CalculusStep>()

        steps += CalculusStep(
            "Chain Rule",
            derivativeNotation(
                derivation.input,
                null
            ),
            "${outerDerivative?.toLaTeX() ?: ""}" +
                    " \\cdot \\frac{d}{dx}" +
                    "(${argument?.toLaTeX() ?: ""})"
        )

        for (child in derivation.children) {
            steps += render(
                child,
                addTrivialStep
            )
        }

        steps += CalculusStep(
            "Simplify",
            "${outerDerivative?.toLaTeX() ?: ""}" +
                    " \\cdot " +
                    derivation.children
                        .firstOrNull()
                        ?.result
                        ?.toLaTeX()
                        .orEmpty(),
            derivation.result.toLaTeX()
        )

        return steps
    }

    // =========================================================================
    // VERIFICATION
    // =========================================================================

    /**
     * Independent mathematical verification.
     *
     * This is intentionally NOT used to generate the explanation.
     */
    fun verify(
        original: IExpr,
        derivative: IExpr,
        variable: ISymbol
    ): Boolean {

        return try {

            val engineDerivative =
                engine.evaluate(
                    F.D(
                        original,
                        variable
                    )
                )

            mathematicallyEquivalent(
                derivative,
                engineDerivative
            )

        } catch (_: Exception) {
            false
        }
    }

    private fun mathematicallyEquivalent(
        left: IExpr,
        right: IExpr
    ): Boolean {

        if (left == right) {
            return true
        }

        return try {

            val difference =
                engine.evaluate(
                    F.Subtract(
                        left,
                        right
                    )
                )

            if (difference.isZero) {
                true
            } else {

                val simplified =
                    engine.evaluate(
                        F.FullSimplify(
                            difference
                        )
                    )

                simplified.isZero
            }

        } catch (_: Exception) {
            false
        }
    }

    // =========================================================================
    // DISPLAY HELPERS
    // =========================================================================

    private fun derivativeNotation(
        expr: IExpr,
        variable: ISymbol?
    ): String {

        val variableText =
            variable?.toLaTeX() ?: "x"

        return "\\frac{d}{d$variableText}" +
                "\\left(${expr.toLaTeX()}\\right)"
    }

    private fun ruleTitle(
        rule: RuleType
    ): String {

        return when (rule) {

            RuleType.CONSTANT ->
                "Constant Rule"

            RuleType.VARIABLE ->
                "Variable Rule"

            RuleType.SUM ->
                "Sum Rule"

            RuleType.CONSTANT_MULTIPLE ->
                "Constant Multiple Rule"

            RuleType.PRODUCT ->
                "Product Rule"

            RuleType.QUOTIENT ->
                "Quotient Rule"

            RuleType.POWER ->
                "Power Rule"

            RuleType.GENERAL_POWER ->
                "Logarithmic Differentiation"

            RuleType.CHAIN ->
                "Chain Rule"

            else ->
                "Derivative Rule"
        }
    }

    private fun functionRuleTitle(
        rule: RuleType
    ): String {

        return when (rule) {

            RuleType.SIN ->
                "Derivative of Sine"

            RuleType.COS ->
                "Derivative of Cosine"

            RuleType.TAN ->
                "Derivative of Tangent"

            RuleType.SEC ->
                "Derivative of Secant"

            RuleType.CSC ->
                "Derivative of Cosecant"

            RuleType.COT ->
                "Derivative of Cotangent"

            RuleType.ARCSIN ->
                "Derivative of Arcsine"

            RuleType.ARCCOS ->
                "Derivative of Arccosine"

            RuleType.ARCTAN ->
                "Derivative of Arctangent"

            RuleType.SINH ->
                "Derivative of Sinh"

            RuleType.COSH ->
                "Derivative of Cosh"

            RuleType.TANH ->
                "Derivative of Tanh"

            RuleType.ARCSINH ->
                "Derivative of Arcsinh"

            RuleType.ARCCOSH ->
                "Derivative of Arccosh"

            RuleType.ARCTANH ->
                "Derivative of Arctanh"

            RuleType.LOG ->
                "Derivative of Natural Logarithm"

            RuleType.EXPONENTIAL ->
                "Derivative of Exponential"

            else ->
                "Derivative Rule"
        }
    }

    // =========================================================================
    // SYMJA HELPERS
    // =========================================================================

    private fun IExpr.isDivide(): Boolean {

        return this is IAST &&
                this.head() == F.Times &&
                this.args().any { factor ->

                    factor is IAST &&
                            factor.isPower &&
                            factor.arg2().isNegative
                }
    }

    private fun IExpr.toLaTeX(): String =
        SymjaUtils.toLaTeX(
            this.toString()
        )
}