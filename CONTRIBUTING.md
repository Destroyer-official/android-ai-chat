# Contributing

## Setup
1. Fork and clone the repository
2. Open in Android Studio
3. Get an OpenRouter API key for testing
4. Create a feature branch: `git checkout -b feature/your-feature`

## Requirements
- Android Studio 2023.1.1+
- Android SDK API 24+
- Kotlin 1.9+

## Code Style
- Follow Kotlin conventions
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable names

## Pull Requests
1. Create feature branch
2. Make your changes
3. Add tests if needed
4. Update documentation
5. Submit PR with clear description

## Testing
```bash
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # UI tests
./gradlew lint                    # Code analysis
```

## Project Structure
```
app/src/main/
├── java/com/example/androidimageapp/
│   ├── fragments/           # UI fragments
│   ├── utils/              # Utility classes
│   ├── MainActivity.kt     # Main activity
│   └── ApiService.kt       # Network layer
├── res/
│   ├── layout/             # XML layouts
│   ├── drawable/           # Graphics and animations
│   ├── values/             # Resources (colors, strings)
│   └── anim/               # Animations
└── AndroidManifest.xml
```