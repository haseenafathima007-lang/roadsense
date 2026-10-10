#!/usr/bin/env python3
"""
fetch_osm.py — One-time Overpass API download for a bounding box.

Usage:
    python3 scripts/fetch_osm.py --bbox <south> <west> <north> <east> \
        --output data/osm/extract.geojson

IMPORTANT:
  - ASK THE USER before running this script.
  - The downloaded extract is NEVER committed (data/osm/*.geojson in .gitignore).
  - This script records source, query, date, and ODbL attribution into data/osm/README.md.
  - This script ONLY runs on demand for a one-time download; it is NOT called at runtime.

ODbL Attribution:
  Data © OpenStreetMap contributors, licensed under the Open Database Licence (ODbL).
  https://www.openstreetmap.org/copyright
"""

import argparse
import datetime
import json
import os
import sys
import urllib.request
import urllib.parse


OVERPASS_URL = "https://overpass-api.de/api/interpreter"
ODBL_NOTICE = "Data © OpenStreetMap contributors, ODbL 1.0 https://osm.org/copyright"

OVERPASS_QUERY_TEMPLATE = """
[out:json][timeout:60][bbox:{south},{west},{north},{east}];
(
  way["highway"~"^(motorway|trunk|primary|secondary|tertiary|residential)$"];
);
out geom;
"""


def fetch_overpass(query: str) -> dict:
    """Send a POST request to Overpass and return parsed JSON."""
    data = urllib.parse.urlencode({"data": query}).encode()
    req = urllib.request.Request(OVERPASS_URL, data=data, method="POST")
    with urllib.request.urlopen(req, timeout=120) as resp:
        return json.loads(resp.read().decode())


def overpass_to_geojson(overpass: dict) -> dict:
    """Convert Overpass JSON (way with geometry) to a GeoJSON FeatureCollection."""
    features = []
    for element in overpass.get("elements", []):
        if element.get("type") != "way":
            continue
        geometry = element.get("geometry", [])
        if len(geometry) < 2:
            continue
        coords = [[p["lon"], p["lat"]] for p in geometry]
        tags = element.get("tags", {})
        features.append({
            "type": "Feature",
            "properties": {
                "id": str(element["id"]),
                "highway": tags.get("highway", "unclassified"),
                "name": tags.get("name", ""),
                "attribution": ODBL_NOTICE,
            },
            "geometry": {
                "type": "LineString",
                "coordinates": coords,
            },
        })
    return {
        "type": "FeatureCollection",
        "features": features,
        "metadata": {
            "source": "OpenStreetMap via Overpass API",
            "licence": "ODbL 1.0 — https://osm.org/copyright",
            "attribution": ODBL_NOTICE,
            "downloaded_at": datetime.datetime.utcnow().isoformat() + "Z",
        },
    }


def write_readme(readme_path: str, bbox: dict, query: str, geojson_path: str, count: int) -> None:
    """Record provenance metadata into data/osm/README.md."""
    content = f"""# OSM Road Link Extract

## Attribution

{ODBL_NOTICE}

This extract is used only as a cached local snapshot for road-link snapping.
It is **not** committed to the repository. Regenerate by running `scripts/fetch_osm.py`.

## Provenance

| Field | Value |
|---|---|
| Source | OpenStreetMap via Overpass API ({OVERPASS_URL}) |
| Downloaded | {datetime.datetime.utcnow().strftime('%Y-%m-%d %H:%M UTC')} |
| Bounding box | S={bbox['south']}, W={bbox['west']}, N={bbox['north']}, E={bbox['east']} |
| Features | {count} road-link LineString features |
| Output file | `{geojson_path}` |
| Licence | ODbL 1.0 — https://osm.org/copyright |

## Overpass Query Used

```
{query.strip()}
```

## Usage

Load `{geojson_path}` with `CachedOsmContext.load(Files.newInputStream(path))`.
The file must NOT be committed. Add `data/osm/*.geojson` to `.gitignore`.
"""
    os.makedirs(os.path.dirname(readme_path), exist_ok=True)
    with open(readme_path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Wrote provenance to {readme_path}")


def main() -> None:
    parser = argparse.ArgumentParser(
        description="One-time OSM road network download. ASK USER before running."
    )
    parser.add_argument("--bbox", nargs=4, metavar=("SOUTH", "WEST", "NORTH", "EAST"),
                        type=float, required=True,
                        help="Bounding box: south west north east (decimal degrees)")
    parser.add_argument("--output", default="data/osm/extract.geojson",
                        help="Output GeoJSON file path (default: data/osm/extract.geojson)")
    parser.add_argument("--dry-run", action="store_true",
                        help="Print the query without fetching anything")
    args = parser.parse_args()

    south, west, north, east = args.bbox
    bbox = {"south": south, "west": west, "north": north, "east": east}
    query = OVERPASS_QUERY_TEMPLATE.format(**bbox)

    if args.dry_run:
        print("DRY RUN — would send this query:")
        print(query)
        sys.exit(0)

    print(f"Fetching road links from Overpass API for bbox {bbox} ...")
    print(f"Attribution: {ODBL_NOTICE}")

    raw = fetch_overpass(query)
    geojson = overpass_to_geojson(raw)
    count = len(geojson["features"])

    os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(geojson, f, indent=2)
    print(f"Wrote {count} features to {args.output}")

    readme_path = os.path.join(os.path.dirname(args.output), "README.md")
    write_readme(readme_path, bbox, query, args.output, count)


if __name__ == "__main__":
    main()
