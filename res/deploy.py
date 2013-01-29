import requests
from cStringIO import StringIO
import string
import random
import re
import os

def randstring(size=24, chars=string.ascii_uppercase + string.digits):
    return ''.join(random.choice(chars) for x in range(size))
 
headers = {
	'User-Agent': 'CurseForge Uploader Script/1.0',
        'X-API-Key': os.environ['BUKKITDEV_KEY']
}
 
versions = requests.get('http://dev.bukkit.org/game-versions.json').json()
version = 0
for i in versions.keys():
    if(int(i) > version):
        version = int(i)
 
files = {'file': ('CommandIt.jar', open("CommandIt.jar"))}
data = {
	'name': 'CommandIt v@SIMPLE_VERSION@',
	'game_versions': str(version),
	'file_type': 'r',
	'change_log': os.environ['CHANGE_LOG'],
	'change_markup_type': 'creole',
	'known_caveats': os.environ['CAVEATS'],
	'caveats_markup_type': 'creole',
}
 
r = requests.post('http://dev.bukkit.org/server-mods/cmdit/upload-file.json', data=data, headers=headers, files=files)
p = requests.get(r.headers['location']).text
m = re.search('"http://dev.bukkit.org/media/files/(.*/.*/.*)"', p)
url = m.group(1)
f = open("version.txt", 'w');
f.write("""
version: @VERSION@
download: %s
""" % url)
f.close();
