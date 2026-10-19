#!/usr/bin/env bash
# Fetch and build the build-time tools (not shipped with the mod).
set -euo pipefail
cd "$(dirname "$0")/vendor"
[ -d DragonFF ] || git clone --depth 1 https://github.com/Parik27/DragonFF.git
[ -d gta3sc ] || git clone --depth 1 https://github.com/thelink2012/gta3sc.git
mkdir -p gta3sc/build && cd gta3sc/build
cmake .. -DCMAKE_BUILD_TYPE=Release -DCMAKE_POLICY_VERSION_MINIMUM=3.5 >/dev/null
make -j"$(nproc)" gta3sc
echo "tools ready"
