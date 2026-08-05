# HonorX8Root

One-click temp root — **Honor X8 TFY-LX2 · SD680 · Android 13 · MagicOS 7.1**

---

## Upload to GitHub & build

```
1. Create new GitHub repo (any name)
2. Upload this folder's contents (drag & drop or git push)
3. Actions → "Build HonorX8Root APK" → Run workflow
4. Wait ~5 min → Artifacts → download app-debug.apk
5. Install APK → tap GRANT ROOT
```

---

## If exploit fails: update offsets

Android 13 zeroes `/proc/kallsyms` for non-root processes.
The exploit uses hardcoded symbol offsets for your exact build.

**Get real offsets for build `7.1.0.296(C185E7R2P4)`:**

```bash
# Option A: Honor kernel OSS source (GPL required release)
# Search: github.com/honor-oss  or  source.codeaurora.org
# Build kernel → nm vmlinux | grep -E "commit_creds|prepare_kernel_cred"

# Option B: via ADB if you have ADB root (some eng/debug builds)
adb shell cat /proc/kallsyms | grep -E "commit_creds|prepare_kernel_cred"
# Output example:
# ffffffc012345678 T commit_creds
# ffffffc0123478a0 T prepare_kernel_cred
# kernel base (from /proc/iomem "Kernel code"): ffffffc010000000
# offset_commit_creds        = 0x12345678 - 0x10000000 = 0x02345678
# offset_prepare_kernel_cred = 0x123478a0 - 0x10000000 = 0x023478a0
```

**Then edit `app/src/main/cpp/exploit.c` lines 38-39:**
```c
#define OFFSET_COMMIT_CREDS         0x02345678UL  ← your value
#define OFFSET_PREPARE_KERNEL_CRED  0x023478a0UL  ← your value
```
Re-push → workflow rebuilds → done.

---

## How it works

| Stage | Detail |
|---|---|
| 1 | CVE-2024-0044 run-as bypass (pre-escalation, Android 13) |
| 2 | CVE-2022-20421 binder UAF → kernel code exec |
| 3 | CVE-2021-0920 unix GC UAF (fallback) |
| 4 | commit_creds(prepare_kernel_cred(0)) → uid=0 |
| 5 | su bind-mounted over /system/bin/su (RAM only) |
| 6 | Module overlayfs from /data/adb/modules |
| Reboot | Root gone · Modules dir intact · Re-run app to restore |

## Device target

| Field | Value |
|---|---|
| Model | Honor X8 TFY-LX2 |
| SoC | Snapdragon 680 (SM6225) |
| Kernel | 4.19.157-perf+ |
| OS | Android 13 / MagicOS 7.1 |
| Build | 7.1.0.296(C185E7R2P4) |
| Patch | June 1 2024 |
