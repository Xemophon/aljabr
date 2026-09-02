# AlJabr

AlJabr is a modern, high-performance mathematical toolkit for Android, designed to bring university-level mathematics to your pocket. Built with **Jetpack Compose** and **Material Design 3**, it offers a seamless experience for everything from basic arithmetic to advanced symbolic calculus, linear algebra, series expansions, and graphing.

## 🚀 Features

### 🔢 Basic & Scientific Calculator
- **Standard Operations:** Full support for addition, subtraction, multiplication, and division.
- **Scientific Mode:** Advanced functions including:
    - Trigonometry: `sin`, `cos`, `tan` and their inverses.
    - Logarithms: Common (`log`) and natural (`ln`) logarithms.
    - Math Constants: $\pi$ (Pi) and $e$ (Euler's number).
    - Power functions, roots, and factorials.
- **Smart Engine:** Intelligent handling of nested parentheses, implicit multiplication (e.g., `2π`), and percentages.

### 📈 Graphing Engine
- **Function Plotting:** High-resolution visualization of functions in the form $f(x)$.
- **Implicit Equations:** Plot complex relations like $x^2 + y^2 = 9$.
- **Automated Analysis:** Automatically identifies local extrema (maxima/minima), inflection points, and vertical asymptotes.
- **Interactive Interface:** Smooth, responsive panning and zooming with companion analysis screens.

### 📐 Calculus Suite
- **Symbolic Differentiation:** Find exact $d/dx$ derivatives for complex expressions with **Step-by-Step** solution breakdowns.
- **Integration:** 
    - **Indefinite:** Exact symbolic integration powered by a robust CAS engine with step-by-step guidance.
    - **Definite:** Precise numerical integration using Simpson's 1/3 rule.
- **Limits:** Calculate finite and infinite ($\infty$) limits, including left/right-sided evaluations.
- **Laplace Transform:** Compute Laplace transforms and inverse transforms for differential equations and signal analysis.

### 🧩 Algebra
- **Matrices:** Comprehensive support for matrix operations including addition, multiplication, transpose, determinant, and inverse computation.
- **Polynomials:** Advanced tools for root finding, polynomial division, factorization, and simplification.

### 📉 Series
- **Power Series:** Generate Taylor and Maclaurin expansions for common mathematical functions.
- **Fourier Analysis:** Decompose periodic functions into their constituent sine and cosine harmonic components.

### 🔄 Unit Conversion & Reference Sheets
- **Versatile Converters:** Real-time conversion for length, weight, area, volume, temperature, and more.
- **Mathematical Reference Sheets:** Quick access to essential math formulas, identity tables, and constants.

### 📱 Premium User Experience
- **Material 3 Design:** A polished UI with dynamic color support and adaptive layouts.
- **LaTeX Rendering:** Beautifully typeset mathematical expressions for maximum clarity.
- **Step-by-Step Explanations:** Detailed step breakdowns for calculus operations.
- **Customizable & Responsive:** System theme integration (Light/Dark/Auto) and dynamically scaling text.

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
    - `basicCalc/`: Core logic and UI for the scientific calculator.
    - `calculus/`:
        - `differentiate/`: Symbolic derivatives and step-by-step solutions.
        - `integrate/`: Indefinite/definite integration and steps.
        - `limits/`: Limit evaluation engine.
        - `laplace/`: Laplace transform calculators.
    - `algebra/`:
        - `matrices/`: Matrix calculator and view model.
        - `polynomials/`: Polynomial operations and root finder.
    - `series/`:
        - `fourier/`: Fourier series expansion.
        - `taylor/`: Taylor and Maclaurin series.
    - `graphMaker/`: Interactive plotting and curve analysis engine.
    - `conversions/`: Unit convertors and reference utility tables.
    - `misc/`: App settings, About screen, and utilities.
- `navigation/`: Type-safe navigation routes and app scaffolding.
- `ui/`:
    - `components/`: Reusable math input handlers, button grids, and step display views.
    - `theme/`: Material 3 themes, colors, typography, and dimensions.

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
