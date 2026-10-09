import zipfile
import os

outer_zip = '/Users/haseena/Desktop/Datasets/RDD2022_released_through_CRDDC2022.zip'
extract_dir = 'data/raw/'

print("Extracting outer zip...")
with zipfile.ZipFile(outer_zip, 'r') as z:
    z.extractall(extract_dir)

rdd_dir = os.path.join(extract_dir, 'RDD2022')
for file in os.listdir(rdd_dir):
    if file.endswith('.zip'):
        zip_path = os.path.join(rdd_dir, file)
        country_dir = os.path.join(rdd_dir, file[:-4])
        print(f"Extracting inner zip {file} to {country_dir}...")
        try:
            with zipfile.ZipFile(zip_path, 'r') as z:
                z.extractall(rdd_dir) # The inner zips usually contain the folder itself e.g. China_Drone/
            os.remove(zip_path) # Remove inner zip after extraction to save space
        except Exception as e:
            print(f"Failed to extract {file}: {e}")

print("Extraction complete.")
