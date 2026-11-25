# MicroPlan Android — Microplanificación Académica, Enfoque y Protección del Descanso

Aplicación Android nativa construida en **Kotlin + Jetpack Compose (Material 3)** con arquitectura **Offline-First + Clean Architecture + MVVM**, diseñada para ayudar a estudiantes universitarios a dividir entregas académicas en micro-metas diarias, mantener sesiones Pomodoro libres de distracciones y proteger al menos 7 horas de sueño mediante bloques de ocio diurno blindado y alertas de desconexión nocturna.

---

## Stack Tecnológico

- **Lenguaje y UI:** Kotlin `2.3.20`, Jetpack Compose (Material 3), Navigation Compose `2.9.8`
- **Build & Tooling:** Android Gradle Plugin `9.0.1`, Gradle `9.1.0`, KSP `2.3.14` (`compileSdk = 36`, `minSdk = 26`, `targetSdk = 36`)
- **Persistencia Local:** Room `2.8.5` (7 tablas SQLite con campos de sincronización `remoteId`, `syncStatus`, `updatedAt`) + Jetpack DataStore Preferences `1.2.1`
- **Notificaciones Locales:** WorkManager `2.11.2` + `NotificationManager`
- **DI:** Contenedor manual `AppContainer` + `SimpleViewModelFactory` (sin dependencias de Gradle pesadas)

---

## Credenciales Demo Precargadas

Al primer arranque se precargan proyectos académicos de ejemplo, bloques protegidos (`Ocio Diurno Protegido` 05:00 PM – 06:00 PM y `Horario de Clases` 08:00 AM – 02:00 PM) y una racha inicial de 5 días:

- **Correo:** `estudiante@correo.edu.mx`
- **Contraseña:** `contraseña123`

*(También puedes crear una cuenta nueva en la pestaña **Registrarse** eligiendo tu meta inicial de sueño y hora de desconexión).*

---

## Comandos CLI (Compilación, Pruebas y Emulador)

### Requisitos previos
- JDK 21 (`JAVA_HOME` apuntando a JDK 21)
- Android SDK (`ANDROID_HOME` configurado con plataforma API 36, `platform-tools` y `emulator`)
- `android` CLI / `adb` en el `PATH`

### 1. Compilar APK de depuración
En Windows (PowerShell):
```powershell
.\gradlew.bat assembleDebug
```
En macOS / Linux:
```bash
./gradlew assembleDebug
```
El APK generado queda en `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Ejecutar pruebas unitarias (JVM)
Verifica `RebalanceScheduleUseCaseTest` y `StreakEvaluatorUseCaseTest`:
```powershell
.\gradlew.bat testDebugUnitTest
```
En macOS / Linux:
```bash
./gradlew testDebugUnitTest
```

### 3. Iniciar el emulador, instalar y lanzar la app
```powershell
# Arrancar el emulador configurado (ej. medium_phone)
android emulator start medium_phone

# Verificar dispositivo conectado
adb devices

# Instalar o actualizar el APK debug
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Abrir la aplicación
adb shell am start -n mx.buap.fcc.microplan/.MainActivity
```

### 4. Inspeccionar logs en tiempo real
```powershell
adb logcat -s AndroidRuntime:E MicroPlan:*
```

---

## Modo Debug del Temporizador Pomodoro

En compilaciones `debug` (`BuildConfig.DEBUG == true`), la pantalla **Focus** muestra el botón **`Simular fin de Pomodoro (Debug)`** debajo de la tarjeta del *Escudo Anti-Redes*. Al pulsarlo:
1. Detiene el conteo regresivo en `00:00`.
2. Abre inmediatamente el `ModalBottomSheet` **`¡Pomodoro Terminado!`** para probar las acciones de completar tarea (`+25 XP`), extender `+25 min` o pausar perdiendo la racha sin esperar 25 minutos reales en el emulador.

---

## Documentación Adicional

- [Arquitectura del Sistema y Contratos Cloud Fase 2](docs/ARQUITECTURA_SISTEMA.md)
- [Manual de Usuario Estudiantil](docs/MANUAL_USUARIO_ESTUDIANTIL.md)
