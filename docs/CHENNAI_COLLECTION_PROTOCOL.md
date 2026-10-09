# Chennai Data Collection Protocol

## 1. Objective
To collect a high-quality, out-of-distribution (OOD) test dataset in Chennai, India, to evaluate the model's performance on local road conditions, particularly for the `patch` (hard negative) class.

## 2. Capture Protocol & Assumptions
*Note: The physical constraints below are **hypotheses**. We do not currently enforce them via sensors (e.g. depth mapping), but we assume them for the sake of comparable severity (area-ratio) measurement.*

1. **Distance:** Stand approximately 3 meters (10 feet) from the target defect.
2. **Height:** Hold the camera at chest level (approx. 1.2 to 1.5 meters).
3. **Pitch:** Angle the camera downwards at roughly 45 degrees.
4. **Framing:** Ensure the entire defect is within the frame, ideally in the lower-centre portion. Include one wide shot for context if the road condition is highly ambiguous.

## 3. Metadata (EXIF GPS)
The system relies on photo EXIF data for initial location resolution. 
- **CRITICAL:** Use a default camera app with "Location Tags" / "Geotagging" **enabled**.
- **DO NOT** use WhatsApp or other messaging apps to transfer the photos initially, as they strip EXIF data. Use direct file transfers, Google Drive, or similar lossless storage.

## 4. Privacy & Blurring
- **Avoid Faces/Plates:** Try to frame shots to exclude pedestrians and vehicle license plates.
- **Redaction:** Before sharing or uploading the dataset, all visible faces and license plates **must** be blurred. If in doubt, blur more rather than less.

## 5. Labelling & Recording
- **Recording Sheet:** Keep a log containing:
  - `photo_id` (filename)
  - `date`
  - `road_name` / location description
  - `condition_category` (e.g., severe pothole, patch, clean asphalt)
  - `lighting/weather` (sunny, overcast, wet)
- **Minimum Counts:** 
  - Aim for at least 100 images per damage class (`crack_linear`, `alligator`, `pothole`).
  - Aim for at least 100 `patch` (repaired road) images (hard negatives).
  - Aim for 50 clean/undamaged road images (true negatives).
- **Two-Person Labelling:** For the final ground truth, annotations must be done independently by two reviewers. We will compute **Cohen's Kappa** to measure inter-annotator agreement and resolve disputes via a third reviewer.
