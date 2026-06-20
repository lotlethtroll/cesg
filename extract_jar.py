import zipfile

jar_path = '/c/Users/astim/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-280/92471e8fc5ed4e3c2279001d77ebcec6fd38bcab/create-1.21.1-6.0.10-280-slim.jar'

files_to_extract = [
    'assets/create/models/block/spout/middle.json',
    'assets/create/models/block/spout/bottom.json'
]

output_path = '/c/Users/astim/OneDrive/Documents/GitHub/Create-End-Storage-and-Gateways/jar_output.txt'

with zipfile.ZipFile(jar_path, 'r') as z:
    with open(output_path, 'w', encoding='utf-8') as out:
        for f in files_to_extract:
            out.write('=== ' + f + ' ===\n')
            out.write(z.read(f).decode('utf-8'))
            out.write('\n\n')

print('Done')
