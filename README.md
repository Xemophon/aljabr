# TUsCalc

TUsCalc is a modern, feature-rich calculator and mathematical toolkit for Android. Built using **Jetpack Compose** and **Material Design 3**, it provides a clean, responsive interface for everything from simple arithmetic to advanced calculus and graphing.

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

### 📱 User Experience
- **Navigation Drawer:** Easily switch between different calculator variants.
- **Material 3 Theming:** Modern, clean UI with support for dynamic colors.
- **Responsive Display:** Expression text scales dynamically as you type.
- **Smart Backspace:** Deletes entire function blocks (like `asin(`) in a single tap.

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Math Engines:**
    - [Symja / Matheclipse](https://github.com/axkr/symja_android_library): Powers symbolic calculus.
    - Custom expression evaluator for high-performance numerical tasks.
- **Design System:** [Material Design 3](https://m3.material.io/)
- **Architecture:** MVVM (Model-View-ViewModel)

## 📥 Getting Started

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/TUsCalc.git
    ```
2.  **Open in Android Studio:**
    Requires Android Studio Ladybug or newer for Compose support.
3.  **Build & Run:**
    Use the **Run** button or execute `./gradlew assembleDebug`.

## 🏗️ Project Structure

The project is organized into modular packages based on functionality:

- `com.example.tuscalc/`
    - `basicCalc/`: Logic and UI for the standard scientific calculator.
    - `graphMaker/`: Graphing engine and coordinate system visualization.
    - `differentiate/`: Symbolic differentiation interface.
    - `integrate/`: Numerical and symbolic integration tools.
    - `limits/`: Numerical limit calculation logic.
    - `navigation/`: Type-safe navigation using Jetpack Navigation Compose.
    - `ui/theme/`: Material 3 theme and color definitions.
