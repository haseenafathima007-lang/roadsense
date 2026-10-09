"""
convert_to_yolo.py
==================
Convert the RDD2022 PASCAL VOC XML annotations to YOLO format.

Two class schemes selectable via --scheme:

  rdd4 (default)
    0 D00 – longitudinal crack
    1 D10 – transverse crack
    2 D20 – alligator / mesh crack
    3 D40 – other damage (rutting, bump, separation, pothole)

  rdd3  (D10 mitigation: orientation decided later in Java)
    0 crack_linear  – D00 + D10 merged
    1 alligator     – D20
    2 pothole       – D40

Non-standard classes (D43, D44, D50, D01, D11, Block crack, D0w0, Repair)
are excluded (dropped).  Every dropped annotation is logged.
"""

import argparse
import logging
import shutil
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

import yaml

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
logger = logging.getLogger(__name__)

# ── Class maps ────────────────────────────────────────────────────────────────
SCHEMES: dict[str, dict[str, int]] = {
    "rdd4": {
        "D00": 0,
        "D10": 1,
        "D20": 2,
        "D40": 3,
    },
    "rdd3": {
        "D00": 0,  # crack_linear
        "D10": 0,  # crack_linear  ← D10 mitigation
        "D20": 1,  # alligator
        "D40": 2,  # pothole
    },
}

CLASS_NAMES: dict[str, list[str]] = {
    "rdd4": ["D00", "D10", "D20", "D40"],
    "rdd3": ["crack_linear", "alligator", "pothole"],
}


def convert_box(
    xmin: float,
    ymin: float,
    xmax: float,
    ymax: float,
    img_w: int,
    img_h: int,
) -> tuple[float, float, float, float] | None:
    """
    Convert PASCAL VOC absolute box to YOLO normalised (cx, cy, w, h).
    Clips to image boundaries.  Returns None if resulting box is degenerate.
    """
    xmin = max(0.0, min(float(xmin), img_w))
    ymin = max(0.0, min(float(ymin), img_h))
    xmax = max(0.0, min(float(xmax), img_w))
    ymax = max(0.0, min(float(ymax), img_h))

    bw = xmax - xmin
    bh = ymax - ymin
    if bw <= 0 or bh <= 0:
        return None  # degenerate

    cx = (xmin + xmax) / 2.0 / img_w
    cy = (ymin + ymax) / 2.0 / img_h
    nw = bw / img_w
    nh = bh / img_h

    # Clamp to [0, 1] (floating point edge cases)
    cx = max(0.0, min(1.0, cx))
    cy = max(0.0, min(1.0, cy))
    nw = max(0.0, min(1.0, nw))
    nh = max(0.0, min(1.0, nh))
    return cx, cy, nw, nh


def process_split(
    split_txt: Path,
    raw_dir: Path,
    out_images: Path,
    out_labels: Path,
    class_map: dict[str, int],
    drop_log: list[str],
    use_symlinks: bool = True,
) -> tuple[int, int]:
    """
    Process one split (train/val/test).
    Returns (images_processed, annotations_kept).
    """
    img_count = 0
    ann_kept = 0

    image_paths = [Path(p.strip()) for p in split_txt.read_text().splitlines() if p.strip()]

    for img_path in image_paths:
        if not img_path.exists():
            logger.warning("Image not found: %s", img_path)
            continue

        # Locate XML
        rel = img_path.relative_to(raw_dir)
        parts = rel.parts  # (country, 'train', 'images', filename)
        if len(parts) < 4:
            continue
        country, split_name = parts[0], parts[1]
        xml_path = (
            raw_dir / country / split_name / "annotations" / "xmls" / (img_path.stem + ".xml")
        )

        # Destination
        dest_img = out_images / img_path.name
        dest_lbl = out_labels / (img_path.stem + ".txt")

        # Link/copy image
        if dest_img.exists():
            dest_img.unlink()
        if use_symlinks:
            dest_img.symlink_to(img_path.resolve())
        else:
            shutil.copy2(img_path, dest_img)

        # Parse XML
        lines: list[str] = []
        if xml_path.exists():
            try:
                tree = ET.parse(xml_path)
                root_el = tree.getroot()
                size_el = root_el.find("size")
                img_w = int(size_el.find("width").text) if size_el is not None else None
                img_h = int(size_el.find("height").text) if size_el is not None else None

                for obj in root_el.findall("object"):
                    name = obj.find("name")
                    cls_name = name.text if name is not None else ""
                    cls_id = class_map.get(cls_name)
                    if cls_id is None:
                        drop_log.append(
                            f"DROP excluded_class  {img_path.name}  class={cls_name}"
                        )
                        continue

                    bndbox = obj.find("bndbox")
                    if bndbox is None:
                        drop_log.append(f"DROP no_bndbox  {img_path.name}  class={cls_name}")
                        continue

                    try:
                        xmin = float(bndbox.find("xmin").text)
                        ymin = float(bndbox.find("ymin").text)
                        xmax = float(bndbox.find("xmax").text)
                        ymax = float(bndbox.find("ymax").text)
                    except (TypeError, ValueError):
                        drop_log.append(
                            f"DROP bad_coords  {img_path.name}  class={cls_name}"
                        )
                        continue

                    if img_w is None or img_h is None:
                        drop_log.append(
                            f"DROP no_size_in_xml  {img_path.name}  class={cls_name}"
                        )
                        continue

                    box = convert_box(xmin, ymin, xmax, ymax, img_w, img_h)
                    if box is None:
                        drop_log.append(
                            f"DROP degenerate_box  {img_path.name}"
                            f"  class={cls_name}  coords=({xmin},{ymin},{xmax},{ymax})"
                        )
                        continue

                    cx, cy, bw, bh = box
                    lines.append(f"{cls_id} {cx:.6f} {cy:.6f} {bw:.6f} {bh:.6f}")
                    ann_kept += 1

            except ET.ParseError as exc:
                drop_log.append(f"DROP xml_parse_error  {img_path.name}  {exc}")

        dest_lbl.write_text("\n".join(lines) + ("\n" if lines else ""))
        img_count += 1

    return img_count, ann_kept


def main() -> None:
    parser = argparse.ArgumentParser(description="Convert RDD2022 → YOLO format")
    parser.add_argument(
        "--scheme",
        choices=["rdd3", "rdd4"],
        default="rdd4",
        help="Class mapping scheme (default: rdd4)",
    )
    parser.add_argument(
        "--raw-dir",
        type=Path,
        default=Path("data/raw/RDD2022"),
        help="Path to extracted RDD2022 root",
    )
    parser.add_argument(
        "--splits-dir",
        type=Path,
        default=Path("data/processed/splits"),
        help="Directory containing train/val/test.txt",
    )
    parser.add_argument(
        "--out-dir",
        type=Path,
        default=None,
        help="Output directory (default: data/processed/yolo_{scheme})",
    )
    parser.add_argument(
        "--no-symlinks",
        action="store_true",
        help="Copy images instead of symlinking",
    )
    args = parser.parse_args()

    out_dir = args.out_dir or Path(f"data/processed/yolo_{args.scheme}")
    class_map = SCHEMES[args.scheme]
    class_names = CLASS_NAMES[args.scheme]

    if not args.raw_dir.exists():
        logger.error("Raw dataset not found: %s", args.raw_dir)
        sys.exit(1)

    drop_log: list[str] = []
    summary: dict[str, dict] = {}

    for split_name in ("train", "val", "test"):
        split_txt = args.splits_dir / f"{split_name}.txt"
        if not split_txt.exists():
            logger.warning("Split file missing: %s — skipping", split_txt)
            continue

        out_images = out_dir / split_name / "images"
        out_labels = out_dir / split_name / "labels"
        out_images.mkdir(parents=True, exist_ok=True)
        out_labels.mkdir(parents=True, exist_ok=True)

        logger.info("Processing %s split...", split_name)
        img_count, ann_kept = process_split(
            split_txt=split_txt,
            raw_dir=args.raw_dir,
            out_images=out_images,
            out_labels=out_labels,
            class_map=class_map,
            drop_log=drop_log,
            use_symlinks=not args.no_symlinks,
        )
        label_files = list(out_labels.glob("*.txt"))
        summary[split_name] = {
            "images": img_count,
            "label_files": len(label_files),
            "annotations_kept": ann_kept,
        }
        logger.info(
            "  %s: %d images, %d label files, %d annotations kept",
            split_name,
            img_count,
            len(label_files),
            ann_kept,
        )

    # Write data.yaml
    data_yaml = {
        "path": str(out_dir.resolve()),
        "train": "train/images",
        "val": "val/images",
        "test": "test/images",
        "nc": len(class_names),
        "names": class_names,
    }
    (out_dir / "data.yaml").write_text(yaml.dump(data_yaml, sort_keys=False))

    # Write drop log
    drop_log_path = out_dir / "dropped_annotations.log"
    drop_log_path.write_text("\n".join(drop_log) + ("\n" if drop_log else ""))
    logger.info("Dropped %d annotations → %s", len(drop_log), drop_log_path)

    # Print data.yaml for verification
    print("\n--- data.yaml ---")
    print(yaml.dump(data_yaml, sort_keys=False))

    print("\n--- Summary ---")
    for split_name, info in summary.items():
        print(
            f"  {split_name}: {info['images']} images, "
            f"{info['label_files']} label files, "
            f"{info['annotations_kept']} annotations"
        )


if __name__ == "__main__":
    main()
