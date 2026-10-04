#!/usr/bin/env python3
# usage: add-appimage-buttons.py <download.html> <update site base url>

import os
import sys

BUTTONS = {
    '<div class="linux-all"><br></div>': 'appimage-button-amd64.html',
    '<p class="smaller-fonts">': 'appimage-button-aarch64.html',
}

index, base_url = sys.argv[1], sys.argv[2].rstrip('/') + '/'
with open(index) as f:
    html = f.read()
for marker, button in BUTTONS.items():
    if marker not in html:
        sys.exit(f'::error::{marker} not found in {index}, AppImage buttons would be dropped')
    with open(os.path.join(os.path.dirname(__file__), button)) as f:
        snippet = f.read().replace('https://update.tasks.org/', base_url)
    html = html.replace(marker, snippet + marker, 1)
with open(index, 'w') as f:
    f.write(html)
