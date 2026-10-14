"""Reject guide bytecode referring to EcoEnchants classes absent from the delivery JAR."""
import re,struct,zipfile,sys

def references(data):
 assert data[:4]==b'\xca\xfe\xba\xbe'
 count=struct.unpack_from('>H',data,8)[0];offset=10;index=1;strings=[]
 sizes={3:4,4:4,5:8,6:8,7:2,8:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}
 while index<count:
  tag=data[offset];offset+=1
  if tag==1:
   length=struct.unpack_from('>H',data,offset)[0];offset+=2
   strings.append(data[offset:offset+length]);offset+=length
  else:
   offset+=sizes[tag]
   if tag in (5,6):index+=1
  index+=1
 return {m.decode() for value in strings for m in re.findall(rb'(?:^|L)(com/willfp/ecoenchants/[a-zA-Z0-9_/$]+)',value)}

def check(path):
 with zipfile.ZipFile(path) as z:
  names=set(z.namelist());missing={};checked=0
  for name in names:
   if name.endswith('.class') and any('/'+s in name for s in ['EnchantGUI','EnchantmentScrollPane','GuideBrowser','GuideQuery','GuideState']):
    checked+=1
    absent=sorted(ref for ref in references(z.read(name)) if ref+'.class' not in names)
    if absent:missing[name]=absent
  assert not missing,('Missing runtime class dependencies',missing)
  return checked

if __name__=='__main__':print('Guide classes checked:',check(sys.argv[1]))
