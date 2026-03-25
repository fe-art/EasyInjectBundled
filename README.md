# EasyInjectBundled

A variant of EasyInject that bundles DLLs inside the JAR at build time.

## Usage

1. **Add your DLLs** to the `custom-dlls` folder
2. **Build** by running `build.bat` (or `mvn clean package`)
3. **Use** `target\EasyInjectBundled-1.0.jar` as your injector

## What Gets Bundled

- `liblogger_x64.dll` (always included)
- All `.dll` files from the `custom-dlls` folder

## Injection Order

1. **liblogger_x64.dll** is always injected first
2. Other DLLs are injected after

## MultiMC / PrismLauncher Setup

Set **pre-launch command** in Settings → Custom Commands:
```
$INST_JAVA -jar EasyInjectBundled-1.0.jar
```

## Modrinth App Setup

Place the JAR in the instance profile folder and double-click it. The installer detects the Modrinth App and writes the pre-launch hook directly into its database — no manual configuration needed.

## How It Works

This spawns a background watcher process that waits for Minecraft to start, then injects the bundled DLLs.

No need to place DLLs next to the JAR - they're already embedded!
