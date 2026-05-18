#!/usr/bin/env python3
from __future__ import annotations

import argparse
import shutil
from pathlib import Path


def plugin_xml_candidates(package_dir: Path, abi: str) -> list[Path]:
    roots = [
        package_dir / "runtime" / "lib",
        package_dir / "android-jni" / abi,
    ]
    candidates: list[Path] = []
    for root in roots:
        if root.is_dir():
            candidates.extend(path for path in root.rglob("plugins.xml") if path.is_file())
    return candidates


def plugin_dir_name(package_dir: Path, installed_plugin_xml: Path | None, package_name: str) -> str:
    runtime_lib_dir = package_dir / "runtime" / "lib"
    if installed_plugin_xml is not None and installed_plugin_xml.parent.name.startswith("openvino-"):
        return installed_plugin_xml.parent.name

    if runtime_lib_dir.is_dir():
        for child in runtime_lib_dir.iterdir():
            if child.is_dir() and child.name.startswith("openvino-"):
                return child.name

    return f"openvino-{package_name.rsplit('-', maxsplit=1)[-1]}"


def stage_runtime_assets(package_dir: Path, abi: str, package_name: str, output: Path) -> None:
    jni_dir = package_dir / "android-jni" / abi
    output.mkdir(parents=True, exist_ok=True)
    shutil.rmtree(output)
    output.mkdir(parents=True)

    installed_plugin_xml = next(
        (path for path in plugin_xml_candidates(package_dir, abi) if "<plugin " in path.read_text(encoding="utf-8")),
        None,
    )
    target_plugin_xml = output / plugin_dir_name(package_dir, installed_plugin_xml, package_name) / "plugins.xml"
    target_plugin_xml.parent.mkdir(parents=True, exist_ok=True)

    if installed_plugin_xml is not None:
        shutil.copy2(installed_plugin_xml, target_plugin_xml)
        return

    cpu_plugin = jni_dir / "libopenvino_arm_cpu_plugin.so"
    if not cpu_plugin.is_file():
        raise SystemExit(f"OpenVINO CPU plugin is missing: {cpu_plugin}")
    target_plugin_xml.write_text(
        "\n".join(
            [
                "<ie>",
                "    <plugins>",
                '        <plugin name="CPU" location="libopenvino_arm_cpu_plugin.so"/>',
                "    </plugins>",
                "</ie>",
                "",
            ],
        ),
        encoding="utf-8",
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--package-dir", required=True, type=Path)
    parser.add_argument("--abi", required=True)
    parser.add_argument("--package-name", required=True)
    parser.add_argument("--output", required=True, type=Path)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    stage_runtime_assets(
        package_dir=args.package_dir,
        abi=args.abi,
        package_name=args.package_name,
        output=args.output,
    )


if __name__ == "__main__":
    main()
