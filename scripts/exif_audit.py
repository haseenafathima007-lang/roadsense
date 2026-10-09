#!/usr/bin/env python3
import json
import logging
from pathlib import Path
import exifread

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

def get_decimal_from_dms(dms, ref):
    try:
        degrees = float(dms[0].num) / float(dms[0].den)
        minutes = float(dms[1].num) / float(dms[1].den)
        seconds = float(dms[2].num) / float(dms[2].den)
        
        dec = degrees + (minutes / 60.0) + (seconds / 3600.0)
        
        if ref in ['S', 'W']:
            dec = -dec
        return dec
    except Exception:
        return None

def main():
    target_dir = Path("data/own/chennai")
    
    if not target_dir.exists():
        logging.warning(f"{target_dir} does not exist. Creating empty directory for later use.")
        target_dir.mkdir(parents=True, exist_ok=True)

    stats = {
        "total_images": 0,
        "missing_exif": 0,
        "zero_coordinates": 0,
        "usable_gps": 0
    }

    images = list(target_dir.glob("*.jpg")) + list(target_dir.glob("*.jpeg")) + list(target_dir.glob("*.png"))
    
    for img_path in images:
        stats["total_images"] += 1
        with open(img_path, 'rb') as f:
            tags = exifread.process_file(f, details=False)
            
        if not tags:
            stats["missing_exif"] += 1
            continue
            
        # Extract GPS
        gps_latitude = tags.get('GPS GPSLatitude')
        gps_latitude_ref = tags.get('GPS GPSLatitudeRef')
        gps_longitude = tags.get('GPS GPSLongitude')
        gps_longitude_ref = tags.get('GPS GPSLongitudeRef')
        
        if not (gps_latitude and gps_latitude_ref and gps_longitude and gps_longitude_ref):
            stats["missing_exif"] += 1
            continue
            
        lat = get_decimal_from_dms(gps_latitude.values, gps_latitude_ref.values)
        lon = get_decimal_from_dms(gps_longitude.values, gps_longitude_ref.values)
        
        if lat is None or lon is None:
            stats["missing_exif"] += 1
            continue
            
        if lat == 0.0 and lon == 0.0:
            stats["zero_coordinates"] += 1
        else:
            stats["usable_gps"] += 1

    docs_dir = Path("docs")
    docs_dir.mkdir(exist_ok=True)
    out_file = docs_dir / "exif_audit.json"
    
    with open(out_file, "w") as f:
        json.dump(stats, f, indent=4)
        
    logging.info(f"Audit complete. Results written to {out_file}")
    logging.info(json.dumps(stats, indent=2))

if __name__ == "__main__":
    main()
