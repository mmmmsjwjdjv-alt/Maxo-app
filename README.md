# MAXO — Offline Security & APK Processing Tool

MAXO is a specialized, offline-first Android security tool designed to perform Dalvik Executable (DEX) protection (**DPT**) and bytecode analysis/restoration (**ONLOCK**) directly on Android devices.

---

## Key Features

1. **100% Offline Operation**:
   - Zero internet permission (`INTERNET` permission is omitted from the manifest).
   - No external APIs, bots, cloud servers, or remote dependencies.
   - All processing routines run locally within isolated application sandbox storage.

2. **Android 11+ Scoped Storage (SAF)**:
   - Uses `ACTION_OPEN_DOCUMENT_TREE` on first run to designate a secure workspace directory.
   - Automatically maintains `DPT/` and `ONLOCK/` subfolders.
   - Uses `ACTION_OPEN_DOCUMENT` to select APK files from internal storage, Downloads, SD card, or external providers.
   - Employs streaming I/O to avoid out-of-memory errors on large APK packages.

3. **Local DPT Engine**:
   - Parses the Dalvik Executable headers (`classes*.dex`).
   - Injects DPT protection metadata tables and checksum signatures.
   - Adds DPT runtime rules and prepares protected DEX structures.
   - Automatically re-signs the APK container using on-device cryptographic key generation (RSA 2048 + SHA256withRSA).

4. **Local ONLOCK Engine**:
   - Inspects protected APK containers for DPT and obfuscated structures.
   - Extracts bytecode tables, truncates shell hooks, and normalizes Dalvik headers and Adler-32 / SHA-1 signatures.
   - Restores clean container structures and signs the output APK.

5. **Modern Glassmorphic UI**:
   - High-end dark theme (`#000000` / `#050505` palette).
   - Translucent glass cards with subtle white borders.
   - Ambient monochrome background particles.
   - Clear progress meters and diagnostic logs.

---

## Directory Structure

```text
Selected Folder/
├── DPT/        # Outputs processed by the DPT engine
└── ONLOCK/     # Outputs processed by the ONLOCK engine
```

---

## Building with GitHub Actions

The repository includes a ready-to-use GitHub Actions workflow (`.github/workflows/android.yml`).

1. Push or commit this repository to GitHub on `main` or `master`.
2. GitHub Actions will automatically trigger the `Build Android APK` job.
3. Once completed, download `MAXO-debug-apk` from the **Artifacts** section of the run.
