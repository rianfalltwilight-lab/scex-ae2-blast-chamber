"""Read-only source/JAR resource checks, including actual PNG payload and model references."""
from pathlib import Path
import hashlib, json, struct, sys, tomllib, zipfile, zlib

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
count = 0
for path in RES.rglob('*.json'):
    json.loads(path.read_text('utf-8')); count += 1
tomllib.loads((RES / 'META-INF/neoforge.mods.toml').read_text('utf-8'))
asset = RES / 'assets/ae2blast'
states = json.loads((asset / 'blockstates/blast_chamber.json').read_text())['variants']
assert len(states) == 8
for state in states.values():
    assert (asset / ('models/' + state['model'].split(':')[1] + '.json')).is_file()
for path in (asset / 'models').rglob('*.json'):
    model = json.loads(path.read_text())
    parent = model.get('parent', '')
    if parent.startswith('ae2blast:'):
        assert (asset / ('models/' + parent.split(':')[1] + '.json')).is_file()
    for ref in model.get('textures', {}).values():
        if ref.startswith('ae2blast:'):
            assert (asset / ('textures/' + ref.split(':')[1] + '.png')).is_file()

def png(path):
    data = path.read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    pos = 8; payload = bytearray(); dims = None
    while pos < len(data):
        size = struct.unpack('>I', data[pos:pos+4])[0]
        kind = data[pos+4:pos+8]; block = data[pos+8:pos+8+size]
        crc = struct.unpack('>I', data[pos+8+size:pos+12+size])[0]
        assert zlib.crc32(kind + block) & 0xffffffff == crc
        if kind == b'IHDR': dims = struct.unpack('>II', block[:8])
        if kind == b'IDAT': payload += block
        pos += size + 12
    assert zlib.decompress(payload)
    return dims

assert png(asset / 'textures/block/chamber_atlas.png') == (128, 128)
assert png(asset / 'textures/block/chamber_active.png') == (128, 128)
meta = json.loads((asset / 'textures/block/chamber_active.png.mcmeta').read_text())['animation']
assert meta['frames'] == [0, 1, 2, 3] and meta['frametime'] == 4 and meta['interpolate'] is True
assert meta['width'] == meta['height'] == 64
assert not (asset / 'textures/block/chamber_atlas.png.mcmeta').exists()
off = json.loads((asset / 'models/block/blast_chamber.json').read_text())
on = json.loads((asset / 'models/block/blast_chamber_active.json').read_text())
assert off['elements'][0]['faces']['north']['texture'] == '#atlas'
assert on['elements'][0]['faces']['north']['texture'] == '#active'
langs = [json.loads((asset / f'lang/{lang}.json').read_text('utf-8')) for lang in ('en_us', 'zh_cn')]
assert langs[0].keys() == langs[1].keys()
jar = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / 'build/libs/ae2-blast-chamber-1.21.1-neoforge-0.1.0.jar'
with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    assert not any('/test/' in p or 'otherpack/' in p or 'ae2blast_test/' in p for p in names)
    assert not any(p.endswith('.jar') for p in names), 'Do not bundle upstream dependencies'
    toml = tomllib.loads(archive.read('META-INF/neoforge.mods.toml').decode())
    assert toml['mods'][0]['version'] == '0.1.0'
    for path in RES.rglob('*'):
        if path.is_file() and path.name != 'neoforge.mods.toml':
            assert archive.read(path.relative_to(RES).as_posix()) == path.read_bytes(), path
print(json.dumps({'passed': True, 'json_files': count, 'blockstates': 8, 'active_frames': 4,
                  'jar_bytes': jar.stat().st_size, 'jar_sha256': hashlib.sha256(jar.read_bytes()).hexdigest()}, indent=2))
