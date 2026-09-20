# AlJabr

AlJabr is a modern, high-performance mathematical toolkit for Android, designed to bring university-level mathematics to your pocket. Built with **Jetpack Compose** and **Material Design 3**, it offers a seamless experience for everything from basic arithmetic to advanced symbolic calculus, linear algebra, differential equations, probability distributions, series expansions, and interactive graphing.

## 🚀 Features

### 🔢 Basic & Scientific Calculator
- **Standard Operations:** Full support for addition, subtraction, multiplication, division, and modulo.
- **Scientific Mode:** Advanced functions including:
    - Trigonometry: `sin`, `cos`, `tan`, their hyperbolics (`sinh`, `cosh`, `tanh`), and their inverses.
    - Logarithms: Common (`log`), natural (`ln`), and custom base logarithms.
    - Mathematical Constants: $\pi$ (Pi) and $e$ (Euler's number).
    - Power functions, roots, factorials, and combinations/permutations.
    - Full support for **Complex Numbers** ($a + bi$).
- **Smart Engine:** Intelligent handling of nested parentheses, implicit multiplication (e.g., `2π`), and percentage calculations.

### 📈 Graphing Engine
- **Function Plotting:** High-resolution visualization of functions in the form $f(x)$.
- **Implicit Equations:** Plot complex 2D relations such as $x^2 + y^2 = 9$.
- **Automated Analysis:** Automatically computes local extrema (maxima/minima), inflection points, and vertical asymptotes.
- **Interactive Interface:** Responsive panning, zooming, and fullscreen inspection mode with companion analysis cards.

### 📐 Calculus Suite
- **Symbolic Differentiation:** Find exact $d/dx$ derivatives for single and multivariable expressions with **Step-by-Step** solution breakdowns.
- **Integration:** 
    - **Indefinite:** Exact symbolic integration powered by a robust CAS engine with step-by-step guidance.
    - **Definite:** Numerical integration using Simpson's 1/3 rule as well as symbolic evaluation.
    - **Multiple Integrals:** Support for double integrals and arc/surface/volume integration.
- **Limits:** Calculate finite and infinite ($\infty$) limits, including left/right-sided evaluations.
- **Laplace Transform:** Compute direct Laplace transforms and inverse Laplace transforms for differential equations and signal analysis.

### 🧩 Algebra & Differential Equations
- **Ordinary Differential Equations (ODE):** Solve first-order and higher-order differential equations with support for initial and boundary conditions.
- **Matrices:** Comprehensive matrix operations including addition, subtraction, multiplication, determinant, transpose, matrix inversion, and eigenvalues/eigenvectors.
- **Polynomials:** Advanced polynomial analysis including root finding, division, factorization, and simplification.

### 📉 Series
- **Taylor & Maclaurin Series:** Generate power series expansions around any point $x = a$ up to any specified order $n$.
- **Fourier Analysis:** Decompose periodic functions into their constituent sine and cosine harmonic components, supporting single and two-branch piecewise functions.

### 📊 Statistics & Probability
- **Probability Distributions:** Compute PDF/PMF and CDF with real-time interactive distribution curve shading and statistical metrics for:
    - Continuous: Normal, Student's $t$, Uniform, Exponential, Chi-Square ($\chi^2$), and $F$-Distribution.
    - Discrete: Binomial, Poisson, Hypergeometric, and Geometric.
- **Descriptive Statistics:** Calculate summary metrics including Mean, Median, Mode, Variance (sample & population), Standard Deviation, Range, Quartiles, Skewness, and Kurtosis.
- **Bivariate & Regression Analysis:** Compute Pearson's correlation coefficient ($r$) and simple linear regression models ($y = ax + b$).
- **Hypothesis Testing & Confidence Intervals:** Inferential statistics suite featuring:
    - **Hypothesis Tests:** Perform single-sample $Z$-Tests and Student's $t$-Tests with two-tailed, left-tailed, and right-tailed $p$-values, critical values, and null hypothesis rejection guidance.
    - **Confidence Intervals:** Evaluate $Z$ and $t$ confidence intervals with configurable confidence levels ($90\%$, $95\%$, $99\%$, etc.), margin of error, and lower/upper bounds.

### 🔄 Unit Conversion & Reference Sheets
- **Math Utility Converters:** Real-time conversion for polar/cartesian complex numbers, base conversions (hexadecimal, binary, decimal), and angle units (degrees/radians).
- **Mathematical Reference Sheets:** Quick access to essential math formulas, identity tables, and constants.

### 📱 Premium User Experience
- **Material 3 Design:** A polished UI with dynamic color support (Android 12+ Monet) and multiple themed color schemes (Default, Blue, Green, Red, Yellow, Orange, Teal, Pink, Brown).
- **LaTeX Rendering:** Beautifully typeset mathematical expressions for maximum clarity.
- **Step-by-Step Explanations:** Detailed step breakdowns for calculus operations.
- **Customizable & Responsive:** System theme integration (Light/Dark/Auto) and adaptive layouts.

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/) - Modern, safe, and expressive.
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) - Declarative UI for a responsive experience.
- **Math Engines:**
    - **[Matheclipse (Symja)](https://github.com/axkr/symja_android_library):** A powerful symbolic Computer Algebra System (CAS) for advanced mathematics.
    - **[LaTeX Rendering](https://github.com/huarangmeng/latex):** High-quality typesetting for mathematical notation.
- **Architecture:** Clean MVVM (Model-View-ViewModel) pattern with Jetpack Navigation Compose.

## 📥 Getting Started

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/xemophon/AlJabr.git
    ```
2.  **Open in Android Studio:**
    Requires Android Studio Ladybug or newer for the latest Compose and Kotlin support.
3.  **Build & Run:**
    Use the **Run** button in Android Studio or execute `./gradlew assembleDebug` from the terminal.

## 🏗️ Project Structure

The project is organized into modular packages under `com.xemophon.aljabr`:

- `modules/`
    - `basic/`: Core logic and UI for the scientific calculator.
    - `calculus/`:
        - `differentiate/`: Symbolic derivatives with analysis.
        - `integrate/`: 
            - `standard/`: Indefinite/definite integration & double integrals.
            - `geometric/`: Evaluate scalar, vector, arc, area and volume integrals.
        - `limits/`: Limit evaluation engine.
        - `laplace/`: Laplace and Inverse Laplace transform calculators.
    - `algebra/`:
        - `matrices/`: Matrix operations, linear solver, and eigenvalues.
        - `polynomials/`: Polynomial operations, roots, and factoring.
        - `bde/`: Ordinary differential equation (ODE) solver with boundary conditions.
    - `series/`:
        - `fourier/`: Fourier series expansion (single and two-branch).
        - `taylor/`: Taylor and Maclaurin series expansions.
    - `statistics/`:
        - `distributions/`: Probability distribution calculators and interactive distribution plots.
        - `descriptives/`: Summary statistics, correlation, and regression models.
        - `hypothesis/`: Hypothesis testing (Z/t-tests) and confidence interval calculators.
    - `graphMaker/`: Interactive plotting and curve analysis engine.
    - `conversions/`: Unit convertors and reference utility tables.
    - `misc/`: App settings, About screen, and utilities.
- `navigation/`: Type-safe navigation routes and app scaffolding.
- `ui/`:
    - `components/`: Reusable math input handlers, button grids, and step display views.
    - `theme/`: Material 3 themes, color schemes, typography, and dimensions.

## 📄 License

Aljabr - University-level mathematics toolkit
Copyright (C) 2026 [Maksim Trenev / Xemophon]

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
