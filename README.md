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

Place the JAR in the instance folder and double-click it. The installer writes a `-agentpath:` JVM arg to the instance config; the JVM loads the DLL directly at startup. (The installer kills the launcher first and restarts it on dialog dismiss.)

## Modrinth App Setup

Place the JAR in the instance's profile folder and double-click it. The installer writes the pre-launch hook into Modrinth's `app.db` for that profile.

## How It Works

On MultiMC / PrismLauncher the launcher's JVM loads the bundled DLL directly via the documented `-agentpath:` JVMTI ABI — no watcher process, no cross-process injection. The Modrinth App path still uses a background pre-launch hook.

No need to place DLLs next to the JAR - they're already embedded!
