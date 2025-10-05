# Android AI Chat App

A modern Android application for chatting with AI models and analyzing images.

## Features

- **Multiple AI Models**: Support for OpenAI, Anthropic, Google, Meta, and other providers
- **Image Analysis**: Upload and analyze images with vision-capable models
- **Chat History**: Persistent conversation storage
- **Custom Backgrounds**: Personalize your chat interface
- **Material Design**: Modern UI with smooth animations

## Quick Start

### Prerequisites

- Android 7.0+ (API level 24)
- OpenRouter API key ([get one free](https://openrouter.ai))

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/yourusername/android-ai-chat.git
   cd android-ai-chat
   ```

2. **Open in Android Studio**

   - Import the project
   - Sync Gradle files
   - Build and run

3. **Setup API Key**
   - Launch the app
   - Go to Settings → API Key
   - Enter your OpenRouter API key

## Build

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Project Structure

```
app/src/main/
├── java/com/example/androidimageapp/
│   ├── fragments/
│   │   ├── ChatFragment.kt      # Main chat interface
│   │   └── SettingsFragment.kt  # App settings
│   ├── utils/
│   │   └── PreferenceManager.kt # Settings storage
│   ├── MainActivity.kt          # Navigation and model management
│   ├── ApiService.kt           # Network layer
│   ├── ChatAdapter.kt          # Message display
│   └── ChatHistoryManager.kt   # Conversation persistence
├── res/
│   ├── layout/                 # UI layouts
│   ├── drawable/               # Icons and backgrounds
│   └── values/                 # Colors, strings, styles
└── AndroidManifest.xml
```

## Configuration

### Adding AI Models

1. **Through UI**: Settings → Model Management → Add Model
2. **Programmatically**: Edit `getDefaultModels()` in `MainActivity.kt`

### Customizing Themes

Edit `res/values/colors.xml`:

```xml
<color name="primary_color">#6366F1</color>
<color name="accent_color">#F59E0B</color>
<color name="background_color">#0F172A</color>
```

### API Configuration

The app uses OpenRouter API. Configure in `ApiService.kt`:

```kotlin
companion object {
    private const val BASE_URL = "https://openrouter.ai/api/v1/"
}
```

## Dependencies

- **Retrofit**: HTTP client
- **Gson**: JSON parsing
- **Markwon**: Markdown rendering
- **Glide**: Image loading
- **Material Components**: UI components

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if needed
5. Submit a pull request

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

## License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/android-ai-chat/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/android-ai-chat/discussions)

## Acknowledgments

- [OpenRouter](https://openrouter.ai) for AI model access
- [Material Design](https://material.io) for design guidelines
- [Android Jetpack](https://developer.android.com/jetpack) for modern Android development
