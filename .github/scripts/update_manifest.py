"""Describe the verified universal APK attached to this immutable main build."""
import json
import os
from pathlib import Path

code = int(os.environ["APP_VERSION_CODE"])
name = os.environ["APP_VERSION_NAME"]
repository = os.environ["GITHUB_REPOSITORY"]
manifest = {
    "versionCode": code,
    "versionName": name,
    "commit": os.environ["GITHUB_SHA"],
    "notes": "新增课程下课倒计时与启动更新检测，优化课表导入、桌面小组件和导航体验。",
    "apkUrl": f"https://github.com/{repository}/releases/download/main-{code}/NJUPTer.apk",
}
Path("dist/update.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
