
# Emulation Engine

This directory contains the shared emulation engine.

## Main responsibilities

- CPU emulation
- RAM management
- Virtual display
- Input devices
- Audio devices
- Virtual storage
- Networking

## Initial architecture

QEMU will be evaluated as the system emulation backend.

The initial guest target is 32-bit ARM.

The emulator must use a compatible virtual machine
configuration for the selected Android system image.

## Important

A generic ARM virtual machine is not automatically
compatible with every Android device image.

The guest kernel and virtual hardware must match.
