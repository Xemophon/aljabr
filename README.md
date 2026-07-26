# AlJabr

AlJabr is a modern, feature-rich calculator and mathematical toolkit for Android. Built using **Jetpack Compose** and **Material Design 3**, it provides a clean, responsive interface for everything from simple arithmetic to advanced calculus and graphing.

## 🚀 Features

### 🔢 Basic & Scientific Calculator
- **Standard Operations:** Addition, subtraction, multiplication, and division.
- **Scientific Mode:** Access advanced functions including:
    - Trigonometry: `sin`, `cos`, `tan` and their inverses.
    - Logarithms: Common log (`log`) and natural log (`ln`).
    - Math Constants: $\pi$ (Pi) and $e$ (Euler's number).
    - Power functions, Square root, and Factorials (`!`).
- **Smart Engine:** Supports nested parentheses, implicit multiplication (e.g., `2π`), and intelligent percentage handling.

### 📈 Graph Maker
- **Function Plotting:** Visualize functions of the form $f(x)$.
- **Implicit Equations:** Plot complex equations like $x^2 + y^2 = 9$.
- **Graph Analysis:** Automatically identifies and highlights:
    - Local Maxima and Minima.
    - Inflection Points.
    - Vertical Asymptotes.
- **Interactive View:** Smoothly pan and zoom to explore functions.

### 📐 Calculus Suite
- **Differentiation:** Perform symbolic differentiation to find $d/dx$ of complex expressions.
- **Integration:** 
    - **Indefinite:** Symbolic integration using an embedded CAS engine.
    - **Definite:** Numerical integration using Simpson's 1/3 rule for high precision.
- **Limits:** Calculate finite, infinite ($\infty$), and one-sided (left/right) limits numerically.

### 🔄 Unit Conversions
- **Versatile Converters:** Easily convert between various units for length, weight, area, and more.
- **Real-time Results:** View converted values instantly as you type.

### 📱 User Experience
- **Navigation Drawer:** Easily switch between different calculator variants and tools.
- **Material 3 Theming:** Modern, clean UI with support for dynamic colors and adaptive layouts.
- **Settings:** Customizable app experience including Light, Dark, and System theme options.
- **Responsive Display:** Expression text scales dynamically as you type.
- **LaTeX Rendering:** Beautifully rendered mathematical expressions for clarity.

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Math Engines:**
    - [Matheclipse (Symja)](https://github.com/axkr/symja_android_library): Powers symbolic calculus and complex evaluations.
    - [LaTeX Rendering Library](https://github.com/huarangmeng/latex-renderer): Used for high-quality mathematical typesetting.
- **Design System:** [Material Design 3](https://m3.material.io/)
- **Data Storage:** [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for user settings.
- **Architecture:** MVVM (Model-View-ViewModel)

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

- `basicCalc/`: Logic and UI for the standard scientific calculator.
- `calculus/`:
    - `differentiate/`: Symbolic differentiation interface.
    - `integrate/`: Numerical and symbolic integration tools.
    - `limits/`: Numerical limit calculation logic.
    - `graphMaker/`: Graphing engine and coordinate system visualization.
- `conversions/`: Unit conversion tools and tables.
- `navigation/`: Type-safe navigation logic using Jetpack Navigation Compose.
- `ui/theme/`: Material 3 theme, color definitions, and layout constants.
- `misc/`: App settings, About screen, and general utility views.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
