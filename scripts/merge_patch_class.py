import os
import shutil
from pathlib import Path
import yaml

def merge_patch_data(cvat_export_dir, yolo_target_dir):
    """
    Merges CVAT YOLO 1.1 exported patch dataset into an existing yolo_rdd3 dataset.
    CVAT exports 'patch' as class 0. We need to remap it to class 4.
    """
    cvat_export_dir = Path(cvat_export_dir)
    yolo_target_dir = Path(yolo_target_dir)
    
    cvat_images_dir = cvat_export_dir / "images"
    cvat_labels_dir = cvat_export_dir / "labels"
    
    if not cvat_images_dir.exists() or not cvat_labels_dir.exists():
        print(f"Error: {cvat_export_dir} must contain 'images' and 'labels' directories.")
        return
        
    for split in ["train", "val"]:
        target_img_dir = yolo_target_dir / "images" / split
        target_lbl_dir = yolo_target_dir / "labels" / split
        target_img_dir.mkdir(parents=True, exist_ok=True)
        target_lbl_dir.mkdir(parents=True, exist_ok=True)
        
        # We assume the user placed them in 'train' or 'val' subdirs in CVAT export too.
        # CVAT typically exports flat 'obj_train_data' or split folders if a split was defined.
        # For simplicity, we just look at the flat directory if subdirs don't exist.
        split_img_dir = cvat_images_dir / split if (cvat_images_dir / split).exists() else cvat_images_dir
        split_lbl_dir = cvat_labels_dir / split if (cvat_labels_dir / split).exists() else cvat_labels_dir
        
        if not split_img_dir.exists():
            continue
            
        for img_path in split_img_dir.glob("*.*"):
            if img_path.suffix.lower() not in [".jpg", ".png", ".jpeg"]:
                continue
            
            # Copy image
            shutil.copy(img_path, target_img_dir / img_path.name)
            
            # Process label if exists
            lbl_path = split_lbl_dir / f"{img_path.stem}.txt"
            if lbl_path.exists():
                with open(lbl_path, "r") as f:
                    lines = f.readlines()
                
                new_lines = []
                for line in lines:
                    parts = line.strip().split()
                    if len(parts) >= 5:
                        cls_id = int(parts[0])
                        if cls_id == 0: # Remap from 0 to 4
                            parts[0] = "4"
                        new_lines.append(" ".join(parts))
                        
                with open(target_lbl_dir / lbl_path.name, "w") as f:
                    f.write("\n".join(new_lines) + "\n")
                    
    # Update data.yaml
    data_yaml_path = yolo_target_dir / "data.yaml"
    if data_yaml_path.exists():
        with open(data_yaml_path, "r") as f:
            data = yaml.safe_load(f)
            
        if data["nc"] == 4:
            data["nc"] = 5
            data["names"].append("patch")
            
            with open(data_yaml_path, "w") as f:
                yaml.safe_dump(data, f, sort_keys=False)
            print(f"Updated {data_yaml_path} to include 5 classes (added 'patch').")
    
    print("Merge complete.")

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("--cvat_dir", required=True, help="Path to CVAT YOLO export")
    parser.add_argument("--target_dir", required=True, help="Path to destination dataset (e.g., data/processed/yolo_rdd3)")
    args = parser.parse_args()
    
    merge_patch_data(args.cvat_dir, args.target_dir)
