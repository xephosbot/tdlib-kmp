# tdlib-kmp

Минимальный Kotlin Multiplatform каркас для обертки над TDLib.

## Структура

- `src/commonMain` — `expect` API (`TdLib`)
- `src/androidMain` — `actual` через загрузку Android JNI (`tdjsonjava`)
- `src/jvmMain` — `actual` через загрузку JVM JNI (`tdjni`)
- `src/nativeMain` — `actual` для Kotlin/Native (место для cinterop)
- `libs/` — директории и плейсхолдеры предсобранных бинарников

## Проверка сборки

```powershell
./gradlew.bat tasks
./gradlew.bat compileKotlinJvm
```

