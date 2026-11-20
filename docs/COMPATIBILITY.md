
# APK Compatibility

## Target

The primary compatibility target is Android 1.5 Cupcake.

Cupcake uses Android API Level 3.

## Compatibility goals

| Android version | API level | Priority |
|---|---:|---|
| Android 1.0 | 1 | High |
| Android 1.1 | 2 | High |
| Android 1.5 | 3 | Highest |
| Android 1.6 | 4 | Experimental |
| Android 2.0 | 5 | Experimental |
| Android 2.1 | 7 | Experimental |

## APK installation

Applications should be installed inside the guest system.

The host Android Package Installer must not be used
as a replacement for the guest Package Manager.

## Native libraries

The project will investigate support for legacy ARM
native libraries.

Unsupported architectures must produce a clear error.

## System dependencies

Applications may depend on:

- Android framework APIs
- Native libraries
- System permissions
- Telephony services
- Location services
- Network services
- Hardware features

These dependencies must be tested individually.

## Online services

Some applications may depend on discontinued servers.

The emulator cannot guarantee that discontinued
online services will become operational again.

## Testing

Every tested APK should have a compatibility report.

Reports should include:

- Application name
- APK version
- Minimum Android version
- CPU architecture
- Installation result
- Launch result
- Main features tested
- Known limitations
