"""Describe the verified universal APK attached to this immutable main build."""
import json
import os
from pathlib import Path

code = int(os.environ["APP_VERSION_CODE"])
name = os.environ["APP_VERSION_NAME"]
repository = os.environ["GITHUB_REPOSITORY"]

# 摘要取 docs/release-notes.md 的首行标题，保持应用内更新提示与发版说明一致。
notes_path = Path("docs/release-notes.md")
notes = "应用更新。"
if notes_path.exists():
    title = next(
        (line[2:].strip() for line in notes_path.read_text(encoding="utf-8").splitlines()
         if line.startswith("# ")),
        "",
    )
    notes = title or notes

manifest = {
    "versionCode": code,
    "versionName": name,
    "commit": os.environ["GITHUB_SHA"],
    "notes": notes,
    "apkUrl": f"https://github.com/{repository}/releases/download/main-{code}/NJUPTer.apk",
}
Path("dist/update.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
