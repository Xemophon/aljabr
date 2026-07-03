# TUsCalc

TUsCalc is a modern, feature-rich calculator application for Android, built using **Jetpack Compose** and following **Material Design 3** principles. It provides a clean, responsive interface for both simple calculations and advanced scientific operations.

## 🚀 Features

- **Standard Operations:** Addition, subtraction, multiplication, and division.
- **Scientific Mode:** Toggle between basic and scientific layouts to access:
    - Trigonometry: `sin`, `cos`, `tan` and their inverses (`asin`, `acos`, `atan`).
    - Logarithms: Common log (`log`) and natural log (`ln`).
    - Math Constants: $\pi$ (Pi) and $e$ (Euler's number).
    - Power functions and Square root.
- **Advanced Math Engine:**
    - Supports nested parentheses with auto-closing logic.
    - Implicit multiplication (e.g., `2(3)` or `2pi`).
    - Intelligent percentage handling.
    - Factorial support (`!`).
- **User-Centric UI:**
    - Built with **Jetpack Compose** for a smooth, reactive experience.
    - **Material 3** theming for a modern look and feel.
    - Smart Backspace: Deletes entire function names (like `sin(`) in one tap.
    - Responsive Display: Text size adjusts automatically as the expression grows.
- **Precision:** Results are formatted to up to 4 decimal places with smart rounding.

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Design System:** [Material Design 3](https://m3.material.io/)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Dependency Management:** Gradle Version Catalog (libs.versions.toml) and Compose BOM.

## 📱 Screenshots

| Main Interface | Scientific Mode |
| :---: | :---: |
| ![Main Screen](https://raw.githubusercontent.com/user-attachments/assets/placeholder) | ![Scientific Mode](https://raw.githubusercontent.com/user-attachments/assets/placeholder) |
*(Note: Replace placeholders with actual screenshots from the `artifacts` folder if available)*

## 📥 Getting Started

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/yourusername/TUsCalc.git
    ```
2.  **Open in Android Studio:**
    Ensure you have the latest version of Android Studio (Ladybug or newer recommended).
3.  **Build & Run:**
    Click the **Run** button or use `./gradlew assembleDebug` to build the APK.

## 🏗️ Project Structure

- `app/src/main/java/com/example/tuscalc/`
    - `MainActivity.kt`: Entry point and main UI composition.
    - `CalcFuncs.kt`: The custom expression evaluation engine.
    - `ui/components/`: Reusable Compose UI elements (buttons, display box).
    - `ui/theme/`: Material 3 theme definitions.
    - `CalcBoxViewModel.kt`: State management and calculator logic.
