# Hetu (हेतु) - Private AI Journal 🌿

Hetu is a privacy-first, offline Android application designed to be your personal AI companion and journal. It combines the ease of a chat interface with structured tracking, empowering you to capture your life's moments, thoughts, and progress without your data ever leaving your device.

## Key Features

*   **🔒 100% Offline & Private:** Built with privacy as the core tenet. All data is stored locally using SQLCipher encryption. No cloud sync, no tracking.
*   **💬 AI Journal:** A chat-like interface for journaling. Talk to Hetu like a friend using text or voice.
*   **📸 My Feed:** A personal, private media journal. Save photos and videos of your journey, accessible only to you.
*   **📊 Insights & Timeline:** Visualize your habits and progress over time with automatically generated insights and a chronological timeline.
*   **🎯 Track Actions & Outcomes:** Quickly log specific actions and their outcomes to track your personal growth goals.
*   **🎨 Dynamic Theming:** Beautiful "Whispering Sands" design system with support for Light, Dark, and System themes.
*   **🤖 On-Device AI:** Powered by `SmolLM` (integrated as a module) for local intelligence.

## Tech Stack

*   **Language:** Kotlin
*   **UI:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM + Clean Architecture principles
*   **Dependency Injection:** Hilt
*   **Database:** Room with SQLCipher (Encrypted)
*   **Image Loading:** Coil
*   **Navigation:** Jetpack Navigation Compose

## Getting Started

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/hetu-android.git
    ```
2.  **Open in Android Studio:**
    *   Open the project `hetu-android`.
    *   Sync Gradle.
3.  **Build & Run:**
    *   Connect an Android device or start an emulator.
    *   Run the `app` configuration.

## Project Structure

*   `app/`: Main Android application code.
*   `smollm/`: Local LLM inference module.
*   `misc_logs/`: Build logs and non-essential files.

## Contributing

This is a personal project. Suggestions and feedback are welcome!
