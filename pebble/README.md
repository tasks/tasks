# Tasks.org Pebble Watch App

Companion app for [Pebble](https://repebble.com/) watches, built with
the Pebble C SDK. Communicates with the Tasks.org Android app over
Bluetooth via AppMessage (PebbleKit Android).

**Supported platforms:** All Pebble hardware — aplite (original Pebble),
basalt (Pebble Time), chalk (Pebble Time Round), diorite (Pebble 2),
emery (Pebble Time 2). Color displays show priority color bars;
B&W displays use text priority indicators.

## Setup

### 1. Install uv

macOS:

```sh
brew install uv
```

Ubuntu/Debian:

```sh
curl -LsSf https://astral.sh/uv/install.sh | sh
```

Windows: Use WSL with the Ubuntu instructions above.

### 2. Install Pebble SDK

```sh
uv tool install pebble-tool --python 3.13
pebble sdk install latest
```

Ubuntu/Debian also needs: `sudo apt install libsdl1.2debian libfdt1`

Verify with `pebble --version`.

## Project Structure

```
pebble/
  src/c/
    main.c                ← entry point, AppMessage router
    protocol.h            ← protocol constants (mirrors PebbleProtocol.kt)
    protocol.c            ← message sending, chunk parsing
    task_list_window.c/.h ← main task list (MenuLayer)
    menu_window.c/.h      ← filter/list picker (MenuLayer)
    task_view_window.c/.h ← task detail view (ScrollLayer)
  resources/              ← images, fonts
  wscript                 ← WAF build file
  package.json            ← Pebble app manifest
```

## Build

```sh
cd pebble
pebble build
```

The build produces a `.pbw` bundle in `build/` for all target platforms.

## Run on Emulator

```sh
pebble install --emulator basalt
```

View logs while running:

```sh
pebble logs
```

Useful emulator commands:

```sh
pebble emu-bt-connection --connected false   # simulate BT disconnect
pebble emu-bt-connection --connected true    # reconnect
pebble screenshot                            # take screenshot
pebble kill                                  # stop emulator
```

## Install on Watch

The watch must be paired to an Android phone running Tasks.org — the
phone bridges all communication between the watch and your task data.

### Via Pebble/Rebble app

1. Pair your Pebble to your phone via the Pebble app
2. On the phone, go to **Settings > Developer Connection** and enable it
3. Note the IP address shown
4. Run:

```sh
pebble install --phone <IP_ADDRESS>
```

### Via Gadgetbridge

1. Pair your Pebble to your phone via Gadgetbridge
2. Transfer `build/pebble.pbw` to your phone
3. Open the file — Gadgetbridge will install it on the watch

### Via Bluetooth serial (macOS)

If your Pebble is paired to your Mac via System Settings > Bluetooth:

```sh
pebble install --serial /dev/cu.PebbleTimeXXXX-SerialPo
```

Check `ls /dev/cu.Pebble*` to find the device name. Note: you still
need the phone paired for the app to communicate with Tasks.org.

## Watch App Controls

- **UP/DOWN** — scroll through task list
- **SELECT** — open task detail / toggle section header
- **Long SELECT** — complete task / open filter menu (on header)
- **BACK** — go back / exit app

In task detail view:
- **SELECT** — complete/uncomplete the task
- **UP/DOWN** — scroll description
- **BACK** — return to task list

## Phone-Side Setup

The Android side lives in the main Tasks.org app:

- `app/src/main/java/org/tasks/pebble/` — PebbleService (BroadcastReceiver),
  PebbleMessageHandler, PebbleProtocol, PebbleRefresher
- `app/src/main/java/org/tasks/watch/WatchServiceLogic.kt` — shared
  business logic (also used by Wear OS)

PebbleKit Android is added as a dependency in `app/build.gradle.kts`.
The `PebbleService` BroadcastReceiver is registered in
`AndroidManifest.xml` and handles communication automatically when
the Pebble app sends messages.

## Resources

- [Pebble Developer Portal](https://developer.repebble.com/)
- [C SDK Documentation](https://developer.rebble.io/developer.pebble.com/docs/c/)
- [SDK Examples](https://github.com/pebble/pebble-sdk-examples)
- [CloudPebble IDE](https://cloudpebble.repebble.com) (browser-based, no install)
- [VS Code Extension](https://marketplace.visualstudio.com/items?itemName=coredevices.pebble-vscode)
- [Rebble Discord #sdk-dev](https://discord.gg/rebble)
