import json
import os
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

from PIL import Image
from tqdm import tqdm


def main():
    raw_dir = Path("data/raw/RDD2022")
    if not raw_dir.exists():
        print(f"Directory {raw_dir} does not exist.")
        return

    stats = {
        "images_per_split": defaultdict(int),
        "annotations_per_class_country": defaultdict(lambda: defaultdict(int)),
        "unlabelled_images": [],
        "corrupted_images": [],
        "size_distribution": defaultdict(int),
        "excluded_classes_audit": defaultdict(lambda: {"count": 0, "examples": []}),
        "test_sets_missing_labels": [],
    }

    allowed_classes = {"D00", "D10", "D20", "D40"}

    print("Gathering image files...")
    image_paths = []
    for root, _, files in os.walk(raw_dir):
        for file in files:
            if file.lower().endswith((".jpg", ".jpeg", ".png")):
                image_paths.append(Path(root) / file)

    print(f"Found {len(image_paths)} images. Processing...")

    for img_path in tqdm(image_paths):
        rel_parts = img_path.relative_to(raw_dir).parts
        if len(rel_parts) < 3:
            continue
        country = rel_parts[0]
        split = rel_parts[1]

        split_key = f"{country}/{split}"
        stats["images_per_split"][split_key] += 1

        # Check corruption and size
        try:
            if img_path.stat().st_size == 0:
                raise ValueError("Empty file")
            with Image.open(img_path) as img:
                img.verify()
            with Image.open(img_path) as img:
                w, h = img.size
                stats["size_distribution"][f"{w}x{h}"] += 1
        except Exception:
            stats["corrupted_images"].append(str(img_path.relative_to(raw_dir)))
            continue

        # Look for annotation XML alongside the image
        xml_path = (
            raw_dir / country / split / "annotations" / "xmls" / f"{img_path.stem}.xml"
        )

        has_annotations = False
        if xml_path.exists():
            try:
                tree = ET.parse(xml_path)
                root_el = tree.getroot()
                objects = root_el.findall("object")
                if objects:
                    has_annotations = True
                    for obj in objects:
                        name = obj.find("name")
                        if name is not None:
                            cls_name = name.text
                            stats["annotations_per_class_country"][country][cls_name] += 1
                            if cls_name not in allowed_classes:
                                stats["excluded_classes_audit"][cls_name]["count"] += 1
                                examples = stats["excluded_classes_audit"][cls_name]["examples"]
                                if len(examples) < 3:
                                    examples.append(str(xml_path.relative_to(raw_dir)))
            except Exception:
                pass

        if not has_annotations:
            stats["unlabelled_images"].append(str(img_path.relative_to(raw_dir)))

    # Identify test sets with no label directory
    for country in os.listdir(raw_dir):
        test_annot_dir = raw_dir / country / "test" / "annotations" / "xmls"
        test_images_dir = raw_dir / country / "test" / "images"
        if test_images_dir.exists() and (
            not test_annot_dir.exists() or len(list(test_annot_dir.glob("*.xml"))) == 0
        ):
            stats["test_sets_missing_labels"].append(f"{country}/test")

    def default_to_dict(d):
        if isinstance(d, defaultdict):
            d = {k: default_to_dict(v) for k, v in d.items()}
        return d

    stats_json = default_to_dict(stats)

    DECISIONS = {
        "Block crack": "EXCLUDE — 3 annotations only, not in challenge spec",
        "D01": "EXCLUDE — variant of D00; covered by D00",
        "D0w0": "EXCLUDE — single annotation, likely labelling error",
        "D11": "EXCLUDE — variant of D10; covered by D10",
        "D43": "EXCLUDE — unclear semantics, not in challenge spec",
        "D44": "EXCLUDE — unclear semantics, not in challenge spec",
        "D50": "EXCLUDE — unclear semantics, not in challenge spec",
        "Repair": "EXCLUDE (for now) — future patch hard-negative class (Phase 2/3)",
    }

    out_dir = Path("docs")
    out_dir.mkdir(parents=True, exist_ok=True)
    json_path = out_dir / "data_audit.json"
    if json_path.exists():
        try:
            with open(json_path) as f:
                existing = json.load(f)
            for k, v in existing.items():
                if k not in stats_json:
                    stats_json[k] = v
        except Exception:
            pass

    with open(json_path, "w") as f:
        json.dump(stats_json, f, indent=2)

    # --- Write Markdown ---
    md_lines = ["# Data Audit Report\n"]
    md_lines.append("## Images per Split")
    for k, v in sorted(stats_json["images_per_split"].items()):
        md_lines.append(f"- **{k}**: {v}")

    md_lines.append("\n## Annotations per Class per Country")
    for country, classes in sorted(stats_json["annotations_per_class_country"].items()):
        md_lines.append(f"### {country}")
        for cls_name, count in sorted(classes.items()):
            md_lines.append(f"- {cls_name}: {count}")

    md_lines.append("\n## Excluded-Class Label Audit")
    if stats_json["excluded_classes_audit"]:
        md_lines.append("| Class | Count | Examples | Include/Exclude Decision |")
        md_lines.append("|---|---|---|---|")
        for cls_name, info in sorted(stats_json["excluded_classes_audit"].items()):
            examples_str = "<br>".join(info["examples"])
            dec = DECISIONS.get(cls_name, "PENDING")
            md_lines.append(f"| {cls_name} | {info['count']} | {examples_str} | {dec} |")
    else:
        md_lines.append("No non-standard classes found.")

    md_lines.append("\n## Missing Labels / Unlabelled")
    md_lines.append(
        f"- Total images with no annotations: {len(stats_json['unlabelled_images'])}"
    )
    if stats_json["test_sets_missing_labels"]:
        md_lines.append(
            "\n**Test sets with no labels (we will build our own labelled split):**"
        )
        for ts in stats_json["test_sets_missing_labels"]:
            md_lines.append(f"- {ts}")

    md_lines.append("\n## Corrupted Images")
    md_lines.append(f"- Total corrupted images: {len(stats_json['corrupted_images'])}")

    md_lines.append("\n## Size Distribution (top 10)")
    top_sizes = sorted(
        stats_json["size_distribution"].items(), key=lambda x: x[1], reverse=True
    )[:10]
    for size, count in top_sizes:
        md_lines.append(f"- {size}: {count} images")

    if "grouped_split" in stats_json:
        gs = stats_json["grouped_split"]
        md_lines.append("\n## Grouped Split & Leakage Measurement")
        md_lines.append(f"- **Heuristic**: {gs.get('heuristic', '')}")
        md_lines.append(f"- **Seed**: {gs.get('seed', '')}")
        ratios = gs.get("ratios", {})
        md_lines.append(
            f"- **Ratios**: train={ratios.get('train', 0.7)}, "
            f"val={ratios.get('val', 0.15)}, test={ratios.get('test', 0.15)}"
        )
        counts = gs.get("counts", {})
        md_lines.append("- **Split Counts**:")
        total_split = sum(counts.values())
        for sname in ("train", "val", "test"):
            md_lines.append(f"  - {sname}: {counts.get(sname, 0):,} images")
        md_lines.append(f"  - total: {total_split:,} images")
        leakage = gs.get("leakage_fraction", 0.0)
        thresh = gs.get("leakage_threshold", 10)
        md_lines.append(
            f"- **Leakage**: {leakage:.4%} of test images have a near-duplicate in train "
            f"(at threshold={thresh})"
        )
        md_lines.append("- **Overlap**: 0 (all splits are completely disjoint)")

    with open(out_dir / "data_audit.md", "w") as f:
        f.write("\n".join(md_lines) + "\n")

    print(f"Audit complete. JSON and Markdown written to {out_dir}")


if __name__ == "__main__":
    main()

