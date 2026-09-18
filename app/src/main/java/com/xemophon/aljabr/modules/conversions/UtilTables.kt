package com.xemophon.aljabr.modules.conversions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.xemophon.aljabr.ui.components.screens.CalculatorScaffold
import com.xemophon.aljabr.ui.components.buttons.HorizontalSeparator

val derivativesSheet = listOf(
    // Basic Rules & Power Rule
    """\frac{d}{dx}(c) = 0""",
    """\frac{d}{dx}(x) = 1""",
    """\frac{d}{dx}(x^n) = n x^{n-1}""",
    """\frac{d}{dx}(c \cdot f(x)) = c \cdot f'(x)""",
    """\frac{d}{dx}[f(x) \pm g(x)] = f'(x) \pm g'(x)""",

    // Product, Quotient & Chain Rules
    """\frac{d}{dx}[f(x) \cdot g(x)] = f'(x)g(x) + f(x)g'(x)""",
    """\frac{d}{dx}\left[\frac{f(x)}{g(x)}\right] = \frac{f'(x)g(x) - f(x)g'(x)}{[g(x)]^2}""",
    """\frac{d}{dx}[f(g(x))] = f'(g(x)) \cdot g'(x)""",
    """\frac{d}{dx}[f(x)^(g(x))] = f(x)^(g(x)) \cdot \left(g'(x) \cdot f(x) + f'(x) \cdot \ln(g(x))\right)""",

    // Exponential & Logarithmic Functions
    """\frac{d}{dx}(e^x) = e^x""",
    """\frac{d}{dx}(a^x) = a^x \ln(a)""",
    """\frac{d}{dx}(\ln(x)) = \frac{1}{x}""",
    """\frac{d}{dx}(\log_a(x)) = \frac{1}{x \ln(a)}""",

    // Trigonometric Functions
    """\frac{d}{dx}(\sin(x)) = \cos(x)""",
    """\frac{d}{dx}(\cos(x)) = -\sin(x)""",
    """\frac{d}{dx}(\tan(x)) = \sec^2(x)""",
    """\frac{d}{dx}(\csc(x)) = -\csc(x)\cot(x)""",
    """\frac{d}{dx}(\sec(x)) = \sec(x)\tan(x)""",
    """\frac{d}{dx}(\cot(x)) = -\csc^2(x)""",

    // Inverse Trigonometric Functions
    """\frac{d}{dx}(\arcsin(x)) = \frac{1}{\sqrt{1 - x^2}}""",
    """\frac{d}{dx}(\arccos(x)) = -\frac{1}{\sqrt{1 - x^2}}""",
    """\frac{d}{dx}(\arctan(x)) = \frac{1}{1 + x^2}""",

    // Hyperbolic Functions
    """\frac{d}{dx}(\sinh(x)) = \cosh(x)""",
    """\frac{d}{dx}(\cosh(x)) = \sinh(x)""",
    """\frac{d}{dx}(\tanh(x)) = \text{sech}^2(x)"""
)

val integralsSheet = listOf(
    // Basic Rules
    """\int 0 \, dx = C""",
    """\int k \, dx = kx + C""",
    """\int x^n \, dx = \frac{x^{n+1}}{n+1} + C \quad (n \neq -1)""",
    """\int \frac{1}{x} \, dx = \ln|x| + C""",
    """\int e^x \, dx = e^x + C""",
    """\int a^x \, dx = \frac{a^x}{\ln(a)} + C""",

    // Trigonometric Integrals
    """\int \sin(x) \, dx = -\cos(x) + C""",
    """\int \cos(x) \, dx = \sin(x) + C""",
    """\int \sec^2(x) \, dx = \tan(x) + C""",
    """\int \csc^2(x) \, dx = -\cot(x) + C""",
    """\int \sec(x)\tan(x) \, dx = \sec(x) + C""",
    """\int \csc(x)\cot(x) \, dx = -\csc(x) + C""",

    // Inverse Trigonometric Integrals
    """\int \frac{1}{\sqrt{1-x^2}} \, dx = \arcsin(x) + C""",
    """\int \frac{1}{1+x^2} \, dx = \arctan(x) + C""",
    """\int \frac{1}{x\sqrt{x^2-1}} \, dx = \text{arcsec}|x| + C""",

    // Hyperbolic Integrals
    """\int \sinh(x) \, dx = \cosh(x) + C""",
    """\int \cosh(x) \, dx = \sinh(x) + C"""
)

val trigonometrySheet = listOf(
    // Fundamental Identities
    """\sin^2(\theta) + \cos^2(\theta) = 1""",
    """1 + \tan^2(\theta) = \sec^2(\theta)""",
    """1 + \cot^2(\theta) = \csc^2(\theta)""",

    // Angle Sum and Difference
    """\sin(\alpha \pm \beta) = \sin(\alpha)\cos(\beta) \pm \cos(\alpha)\sin(\beta)""",
    """\cos(\alpha \pm \beta) = \cos(\alpha)\cos(\beta) \mp \sin(\alpha)\sin(\beta)""",
    """\tan(\alpha \pm \beta) = \frac{\tan(\alpha) \pm \tan(\beta)}{1 \mp \tan(\alpha)\tan(\beta)}""",

    // Double Angle Formulas
    """\sin(2\theta) = 2\sin(\theta)\cos(\theta)""",
    """\cos(2\theta) = \cos^2(\theta) - \sin^2(\theta)""",
    """\cos(2\theta) = 2\cos^2(\theta) - 1 = 1 - 2\sin^2(\theta)""",
    """\tan(2\theta) = \frac{2\tan(\theta)}{1 - \tan^2(\theta)}""",

    // Half Angle Formulas
    """\sin^2(\theta/2) = \frac{1 - \cos(\theta)}{2}""",
    """\cos^2(\theta/2) = \frac{1 + \cos(\theta)}{2}"""
)

val laplaceSheet = listOf(
    // Definition & Fundamental Properties
    """\mathcal{L}\{f(t)\} = \int_{0}^{\infty} e^{-st} f(t) \, dt = F(s)""",
    """\mathcal{L}\{a f(t) + b g(t)\} = a F(s) + b G(s)""",
    """\mathcal{L}\{e^{at} f(t)\} = F(s - a)""",
    """\mathcal{L}\{f(t - a) u(t - a)\} = e^{-as} F(s)""",
    """\mathcal{L}\{f'(t)\} = s F(s) - f(0)""",
    """\mathcal{L}\{f''(t)\} = s^2 F(s) - s f(0) - f'(0)""",
    """\mathcal{L}\{t \cdot f(t)\} = -F'(s)""",
    """\mathcal{L}\left\{\int_{0}^{t} f(\tau) \, d\tau\right\} = \frac{F(s)}{s}""",

    // Common Laplace Transforms
    """\mathcal{L}\{1\} = \frac{1}{s}""",
    """\mathcal{L}\{t^n\} = \frac{n!}{s^{n+1}}""",
    """\mathcal{L}\{e^{at}\} = \frac{1}{s - a}""",
    """\mathcal{L}\{\sin(at)\} = \frac{a}{s^2 + a^2}""",
    """\mathcal{L}\{\cos(at)\} = \frac{s}{s^2 + a^2}""",
    """\mathcal{L}\{\sinh(at)\} = \frac{a}{s^2 - a^2}""",
    """\mathcal{L}\{\cosh(at)\} = \frac{s}{s^2 - a^2}""",
    """\mathcal{L}\{e^{at}\sin(bt)\} = \frac{b}{(s-a)^2 + b^2}""",
    """\mathcal{L}\{e^{at}\cos(bt)\} = \frac{s-a}{(s-a)^2 + b^2}""",
    """\mathcal{L}\{u(t - a)\} = \frac{e^{-as}}{s}""",
    """\mathcal{L}\{\delta(t - a)\} = e^{-as}"""
)

val inverseLaplaceSheet = listOf(
    // Fundamental Properties
    """\mathcal{L}^{-1}\{a F(s) + b G(s)\} = a f(t) + b g(t)""",
    """\mathcal{L}^{-1}\{F(s - a)\} = e^{at} f(t)""",
    """\mathcal{L}^{-1}\{e^{-as} F(s)\} = f(t - a) u(t - a)""",
    """\mathcal{L}^{-1}\{F(s) G(s)\} = \int_{0}^{t} f(\tau) g(t - \tau) \, d\tau""",

    // Common Inverse Laplace Transforms
    """\mathcal{L}^{-1}\left\{\frac{1}{s}\right\} = 1""",
    """\mathcal{L}^{-1}\left\{\frac{1}{s^n}\right\} = \frac{t^{n-1}}{(n-1)!}""",
    """\mathcal{L}^{-1}\left\{\frac{1}{s - a}\right\} = e^{at}""",
    """\mathcal{L}^{-1}\left\{\frac{1}{s^2 + a^2}\right\} = \frac{1}{a} \sin(at)""",
    """\mathcal{L}^{-1}\left\{\frac{s}{s^2 + a^2}\right\} = \cos(at)""",
    """\mathcal{L}^{-1}\left\{\frac{1}{s^2 - a^2}\right\} = \frac{1}{a} \sinh(at)""",
    """\mathcal{L}^{-1}\left\{\frac{s}{s^2 - a^2}\right\} = \cosh(at)""",
    """\mathcal{L}^{-1}\left\{\frac{1}{(s-a)^2 + b^2}\right\} = \frac{1}{b} e^{at} \sin(bt)""",
    """\mathcal{L}^{-1}\left\{\frac{s-a}{(s-a)^2 + b^2}\right\} = e^{at} \cos(bt)""",
    """\mathcal{L}^{-1}\left\{\frac{e^{-as}}{s}\right\} = u(t - a)""",
    """\mathcal{L}^{-1}\{e^{-as}\} = \delta(t - a)"""
)

@Composable
fun UtilitiesScreen(
    onOpenDrawer: () -> Unit
) {
    val sheets = listOf(
        "Derivatives" to derivativesSheet,
        "Integrals" to integralsSheet,
        "Trigonometry" to trigonometrySheet,
        "Laplace Transform" to laplaceSheet,
        "Inverse Laplace" to inverseLaplaceSheet
    )
    val pagerState = rememberPagerState { sheets.size }

    val boxModifier = Modifier
        .padding(start = 8.dp,end = 8.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.primaryContainer)
        .border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(12.dp)
        )

    CalculatorScaffold(
        title = { Text("Reference sheets") },
        onOpenDrawer = onOpenDrawer
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                HorizontalSeparator(
                    text = sheets[pagerState.currentPage].first
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { page ->
                    ExampleSheet(
                        modifier = boxModifier,
                        content = sheets[page].second
                    )
                }

                // Page Indicator
                Row(
                    Modifier
                        .height(50.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(sheets.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExampleSheet(
    modifier: Modifier = Modifier,
    content: List<String>
) {
    val listState = rememberLazyListState()
    
    Column(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(content) { index, formula ->
                Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    Latex(
                        latex = formula,
                        config = LatexConfig(
                            fontSize = when {
                                formula.length <= 20 -> 20.sp
                                formula.length <= 40 -> 16.sp
                                else -> 12.sp
                            },
                            theme = LatexTheme.light(color = MaterialTheme.colorScheme.secondary),
                        ),
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .widthIn(max = 2000.dp)
                    )
                }
                if (index < content.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun TablesPreview(){
    UtilitiesScreen(onOpenDrawer = {})
}