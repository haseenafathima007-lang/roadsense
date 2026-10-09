import hashlib
import json
import logging
import random
import sys
from pathlib import Path

import imagehash
from PIL import Image
from tqdm import tqdm

logger = logging.getLogger(__name__)

# Hashing algorithm — phash is robust to minor compression artefacts
HASH_FUNC = imagehash.phash
HASH_SIZE = 8  # 64-bit hash
NEAR_DUPLICATE_THRESHOLD = 10  # hamming distance; images closer than this are same block

SPLIT_RATIOS = {"train": 0.70, "val": 0.15, "test": 0.15}
SEED = 42


def _phash(img_path: Path) -> imagehash.ImageHash:
    with Image.open(img_path) as img:
        return HASH_FUNC(img, hash_size=HASH_SIZE)


def build_sequence_blocks(image_paths: list[Path]) -> list[list[Path]]:
    """
    Group images into sequence blocks.

    Heuristic:
      1. Sort images within each country by filename (numeric suffix order mirrors
         camera-roll order in the CRDDC collection protocol).
      2. Compute perceptual hash (phash) for each image.
      3. If the hamming distance between consecutive images is <= NEAR_DUPLICATE_THRESHOLD,
         they belong to the same block.

    Limits:
      - Filename order is a proxy for capture order; it fails when filenames are
        non-sequential or re-numbered.
      - The threshold is heuristic and not validated on RDD2022 specifically.
      - Drone images (China_Drone) may group differently due to nadir viewpoint.
    """
    if not image_paths:
        return []

    blocks: list[list[Path]] = []
    current_block: list[Path] = [image_paths[0]]
    prev_hash = _phash(image_paths[0])

    for img_path in tqdm(image_paths[1:], desc="Hashing images"):
        try:
            h = _phash(img_path)
        except Exception:
            logger.warning("Cannot hash %s — placed in its own block", img_path)
            blocks.append(current_block)
            current_block = [img_path]
            prev_hash = None
            continue

        if prev_hash is not None and (h - prev_hash) <= NEAR_DUPLICATE_THRESHOLD:
            current_block.append(img_path)
        else:
            blocks.append(current_block)
            current_block = [img_path]
        prev_hash = h

    blocks.append(current_block)
    return blocks


def assign_blocks(blocks: list[list[Path]], seed: int = SEED) -> dict[str, list[Path]]:
    """Assign whole blocks to train / val / test maintaining ratio."""
    rng = random.Random(seed)
    shuffled = blocks[:]
    rng.shuffle(shuffled)

    n = len(shuffled)
    n_val = max(1, round(n * SPLIT_RATIOS["val"]))
    n_test = max(1, round(n * SPLIT_RATIOS["test"]))
    n_train = n - n_val - n_test

    splits: dict[str, list[Path]] = {"train": [], "val": [], "test": []}
    for block in shuffled[:n_train]:
        splits["train"].extend(block)
    for block in shuffled[n_train : n_train + n_val]:
        splits["val"].extend(block)
    for block in shuffled[n_train + n_val :]:
        splits["test"].extend(block)
    return splits


def measure_leakage(
    train_paths: list[Path],
    test_paths: list[Path],
    threshold: int = NEAR_DUPLICATE_THRESHOLD,
) -> float:
    """
    Compute the fraction of test images whose nearest phash neighbour in train
    is within `threshold` hamming distance.
    """
    print("Computing perceptual hashes for train set (leakage check)...")
    train_hashes = []
    for p in tqdm(train_paths, desc="Hashing train"):
        try:
            train_hashes.append(_phash(p))
        except Exception:
            pass

    leaky = 0
    print("Checking test images against train hashes...")
    for p in tqdm(test_paths, desc="Checking test"):
        try:
            h = _phash(p)
        except Exception:
            continue
        if any((h - th) <= threshold for th in train_hashes):
            leaky += 1

    return leaky / len(test_paths) if test_paths else 0.0


def main(raw_dir: Path | None = None, out_dir: Path | None = None) -> None:
    raw_dir = raw_dir or Path("data/raw/RDD2022")
    out_dir = out_dir or Path("data/processed/splits")

    if not raw_dir.exists():
        print(f"Dataset not found at {raw_dir}", file=sys.stderr)
        sys.exit(1)

    out_dir.mkdir(parents=True, exist_ok=True)

    # Collect labelled train images per country (test sets have no labels)
    print("Collecting labelled images...")
    labelled_by_country: dict[str, list[Path]] = {}
    for country_dir in sorted(raw_dir.iterdir()):
        if not country_dir.is_dir():
            continue
        train_images_dir = country_dir / "train" / "images"
        if not train_images_dir.exists():
            continue
        imgs = sorted(
            p
            for p in train_images_dir.iterdir()
            if p.suffix.lower() in {".jpg", ".jpeg", ".png"}
        )
        labelled_by_country[country_dir.name] = imgs
        print(f"  {country_dir.name}: {len(imgs)} labelled images")

    # Build blocks and assign splits per country to avoid cross-country leakage
    all_splits: dict[str, list[Path]] = {"train": [], "val": [], "test": []}
    for country, images in labelled_by_country.items():
        print(f"\nBuilding sequence blocks for {country}...")
        blocks = build_sequence_blocks(images)
        print(f"  {len(images)} images -> {len(blocks)} blocks")
        country_splits = assign_blocks(blocks, seed=SEED)
        for split_name, paths in country_splits.items():
            all_splits[split_name].extend(paths)

    # Write split lists
    hashes: dict[str, str] = {}
    for split_name, paths in all_splits.items():
        lines = sorted(str(p) for p in paths)
        txt = "\n".join(lines) + "\n"
        (out_dir / f"{split_name}.txt").write_text(txt)
        hashes[split_name] = hashlib.sha256(txt.encode()).hexdigest()
        print(f"\n{split_name}: {len(paths)} images  sha256={hashes[split_name][:16]}…")

    # Verify no overlap
    train_set = set(str(p) for p in all_splits["train"])
    val_set = set(str(p) for p in all_splits["val"])
    test_set = set(str(p) for p in all_splits["test"])
    assert train_set.isdisjoint(val_set), "FAIL: train/val overlap"
    assert train_set.isdisjoint(test_set), "FAIL: train/test overlap"
    assert val_set.isdisjoint(test_set), "FAIL: val/test overlap"
    print("\nOverlap check: PASS (no image id appears in two splits)")

    # Leakage measurement
    print("\nMeasuring split leakage...")
    leakage = measure_leakage(all_splits["train"], all_splits["test"])
    print(f"Leakage: {leakage:.4%} of test images have a near-duplicate in train")

    # Update data_audit.json
    audit_path = Path("docs/data_audit.json")
    if audit_path.exists():
        with open(audit_path) as f:
            audit = json.load(f)
    else:
        audit = {}

    audit["grouped_split"] = {
        "heuristic": (
            "Per-country filename-sorted phash clustering "
            f"(threshold={NEAR_DUPLICATE_THRESHOLD}, hash_size={HASH_SIZE})"
        ),
        "seed": SEED,
        "ratios": SPLIT_RATIOS,
        "counts": {k: len(v) for k, v in all_splits.items()},
        "split_hashes": hashes,
        "leakage_threshold": NEAR_DUPLICATE_THRESHOLD,
        "leakage_fraction": round(leakage, 6),
    }

    with open(audit_path, "w") as f:
        json.dump(audit, f, indent=2)
    print(f"Updated {audit_path} with split + leakage metrics.")


if __name__ == "__main__":
    main()
