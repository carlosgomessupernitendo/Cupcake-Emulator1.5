
# Cupcake Emulator Architecture

## 1. General architecture

Cupcake Emulator uses a host-and-guest architecture.

The host is the application running on Android or desktop.

The guest is the original Android 1.5 operating system.

## 2. Components

### Android frontend

Responsible for:

- Starting the emulator
- Displaying the guest screen
- Touch input
- Keyboard input
- APK file selection
- Virtual storage management

### Desktop frontend

Responsible for:

- Starting the emulator
- Displaying the guest screen
- Mouse input
- Keyboard input
- Window management
- Virtual storage management

### Emulation engine

Responsible for:

- CPU emulation
- Memory management
- Virtual devices
- Display output
- Audio output
- Storage devices

### Cupcake system

Contains the Android 1.5 guest operating system.

### Compatibility layer

Responsible for testing:

- Android API compatibility
- Legacy APK installation
- ARM native libraries
- System services
- Application permissions

## 3. CPU architecture

The initial target is 32-bit ARM.

Additional architectures will be evaluated separately.

## 4. Display

The guest should reproduce the original Android 1.5
screen and interface.

The host interface must not replace the guest launcher.

## 5. Application installation

APK installation must happen inside the guest Android
system, using its Package Manager.

## 6. Storage

The emulator must provide persistent virtual storage.

User data must survive emulator restarts.

## 7. Networking

Network access will be implemented through a virtual
network interface.

Legacy services may require separate compatibility work.
