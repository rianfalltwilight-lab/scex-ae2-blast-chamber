"""Deterministic resource/model authoring; textures are separately generated artwork."""
from pathlib import Path
import json
root = Path(__file__).resolve().parents[1] / 'src/main/resources'
def put(path, value):
    p = root / path; p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
asset='assets/ae2blast/'
variants={}
for facing,y in [('north',0),('east',90),('south',180),('west',270)]:
    for active in [False,True]:
        variants[f'facing={facing},lit={str(active).lower()}']={'model':'ae2blast:block/blast_chamber'+('_active' if active else ''),'y':y}
put(asset+'blockstates/blast_chamber.json',{'variants':variants})
for active in [False,True]:
    faces={}
    for side in ['north','south','west','east','up','down']:
        tex='#active' if active and side=='north' else '#atlas'
        uv=[0,0,16,16] if tex=='#active' else [0,0,8,8] if side=='north' else [0,8,8,16] if side in ('up','down') else [8,0,16,8]
        faces[side]={'texture':tex,'uv':uv,'cullface':side}
    put(asset+'models/block/blast_chamber'+('_active' if active else '')+'.json',{
        'parent':'minecraft:block/block','textures':{'atlas':'ae2blast:block/chamber_atlas','active':'ae2blast:block/chamber_active','particle':'ae2blast:block/chamber_atlas'},
        'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces}]})
put(asset+'models/item/blast_chamber.json',{'parent':'ae2blast:block/blast_chamber'})
put(asset+'textures/block/chamber_active.png.mcmeta',{'animation':{'width':64,'height':64,'frametime':4,'interpolate':True,'frames':[0,1,2,3]}})
en={'block.ae2blast.blast_chamber':'ME Blast Chamber','itemGroup.ae2blast':'AE2 Blast Chamber','gui.ae2blast.on':'ON','gui.ae2blast.off':'OFF','gui.ae2blast.fuel':'Fuel','gui.ae2blast.charges':'Stored blast: %s',
    'gui.ae2blast.status.0':'Awaiting recipe','gui.ae2blast.status.1':'Processing','gui.ae2blast.status.2':'Output full','gui.ae2blast.status.3':'Redstone paused','gui.ae2blast.status.4':'Disabled','gui.ae2blast.status.5':'Need explosives','gui.ae2blast.status.6':'Finding recipe','jei.ae2blast.title':'Contained Explosion','jei.ae2blast.cost':'%s charges / %s ticks'}
zh={'block.ae2blast.blast_chamber':'ME 爆炸配方机','itemGroup.ae2blast':'AE2 爆炸配方机','gui.ae2blast.on':'开启','gui.ae2blast.off':'关闭','gui.ae2blast.fuel':'燃料','gui.ae2blast.charges':'剩余爆炸当量：%s',
    'gui.ae2blast.status.0':'等待匹配配方','gui.ae2blast.status.1':'正在爆炸合成','gui.ae2blast.status.2':'产出空间不足','gui.ae2blast.status.3':'红石信号暂停','gui.ae2blast.status.4':'已关闭','gui.ae2blast.status.5':'需要火药或 TNT','gui.ae2blast.status.6':'正在寻找配方','jei.ae2blast.title':'密闭爆炸合成','jei.ae2blast.cost':'%s 当量 / %s 刻'}
put(asset+'lang/en_us.json',en);put(asset+'lang/zh_cn.json',zh)
put('data/ae2blast/recipe/blast_chamber.json',{'type':'minecraft:crafting_shaped','category':'misc','pattern':['OIO','FCF','OTO'],'key':{'O':{'item':'minecraft:obsidian'},'I':{'item':'minecraft:iron_ingot'},'F':{'item':'ae2:fluix_crystal'},'C':{'item':'ae2:engineering_processor'},'T':{'item':'minecraft:tnt'}},'result':{'id':'ae2blast:blast_chamber','count':1}})
put('data/ae2blast/loot_table/blocks/blast_chamber.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'ae2blast:blast_chamber'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
for tag in ['mineable/pickaxe','needs_iron_tool']:
    put('data/minecraft/tags/block/'+tag+'.json',{'replace':False,'values':['ae2blast:blast_chamber']})
print('Generated models, states, translations, recipe, loot and tags.')
