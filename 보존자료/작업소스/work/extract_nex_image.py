import zipfile,pathlib,subprocess
root=pathlib.Path(r"C:\Users\PUVKID\Desktop\NEXTools\android-sdk\system-images\android-31\default").resolve()
root.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(r"C:\Users\PUVKID\Desktop\NEXTools\android31.zip") as z:
 for info in z.infolist():
  dest=(root/info.filename).resolve()
  if root not in dest.parents: raise RuntimeError("invalid zip path")
  if info.is_dir():dest.mkdir(parents=True,exist_ok=True);continue
  dest.parent.mkdir(parents=True,exist_ok=True)
  with open(dest,"wb") as out:
   sparse=info.filename.endswith(".img")
   if sparse:sparse=subprocess.run(["fsutil","sparse","setflag",str(dest)],stdout=subprocess.DEVNULL).returncode==0
   with z.open(info) as stream:
    while True:
     buf=stream.read(1048576)
     if not buf:break
     if sparse and buf.count(0)==len(buf):out.seek(len(buf),1)
     else:out.write(buf)
   out.truncate(info.file_size)
print("Android 12 image extracted")
