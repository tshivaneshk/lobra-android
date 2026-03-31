# Lobra

<p align="center">
  <b>A dark-mode-first, location-aware reminder application for Android.</b>
</p>

---

## Overview

Lobra is a reminder application designed to handle both time-based and location-based alerts with reliability and clarity. It combines a focused dark-mode interface with practical features such as full-screen reminders, background event handling, and local data persistence.

The project demonstrates modern Android development using Kotlin, Jetpack Compose, and an MVVM-based architecture.

---

## Demo

A complete walkthrough of the application, including permission handling, reminder creation, and notification triggering:

https://youtu.be/xe9Rmslnn4U

---

## Features

* Dark-mode-first interface designed for clarity and reduced eye strain
* Time-based and location-based reminders using Google Play Services
* Full-screen alerts for high-priority reminders
* Biometric authentication for securing sensitive entries
* Home screen widget built with Jetpack Glance
* Offline-first storage using Room Database
* Network layer prepared for backend integration

---

## Screenshots

### Dashboard

![Dashboard](screenshots/dashboard.png)

### Add Reminder

![Add Reminder](screenshots/addreminder.png)

### Reminder Details

![Details](screenshots/reminderscreen.png)

### Location Suggestions

![Location](screenshots/locationsuggestions.png)

### Notification

![Notification](screenshots/notifications.png)

### Settings

![Settings](screenshots/settings.png)

### Widget

![Widget](screenshots/widget.png)

### Recycle Bin

![Recycle Bin](screenshots/recyclebin.png)

---

## Tech Stack

* **Language**: Kotlin
* **UI**: Jetpack Compose (Material 3)
* **Architecture**: MVVM
* **Database**: Room (KSP)
* **Networking**: Retrofit, Gson
* **Widgets**: Jetpack Glance
* **Location Services**: Google Play Services Location
* **Security**: AndroidX Biometric
* **Image Loading**: Coil
* **Navigation**: Navigation Compose

---

## Getting Started

### Prerequisites

* Android Studio (latest stable version recommended)
* Android SDK installed

### Installation

```bash
git clone https://github.com/tshivaneshk/lobra.git
```

1. Open the project in Android Studio
2. Sync Gradle
3. Run on an emulator or physical device

---

## Project Structure

```
app/src/main/java/com/example/lobra

├── MainActivity.kt
├── FullScreenReminderActivity.kt
├── NotificationReceiver.kt
├── ui/
├── data/
├── network/
├── viewmodel/
├── theme/
├── widget/
```

---

## Design Notes

The application follows a consistent dark color palette focused on readability and minimal visual noise. The interface prioritizes quick interaction and clear presentation of reminder data.

---

## Future Improvements

* Cloud synchronization and user accounts
* Enhanced customization for reminders
* Background performance optimizations
* Expanded widget capabilities

---

## Author

T Shivanesh Kumar

---

## License

This project is licensed under the MIT License.
