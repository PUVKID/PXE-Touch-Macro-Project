import zipfile, pathlib, subprocess, os
root = pathlib.Path(r"C:\Users\PUVKID\Desktop\YunseokTouchMacro\work\tools\android-sdk\system-images\android-31\default").resolve()
root.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(r"C:\Users\PUVKID\Desktop\YunseokTouchMacro\work\tools\android31.zip") as z:
    for info in z.infolist():
        dest = (root / info.filename).resolve()
        if root not in dest.parents: raise RuntimeError("invalid zip path")
        if info.is_dir(): dest.mkdir(parents=True, exist_ok=True); continue
        dest.parent.mkdir(parents=True,exist_ok=True)
        with open(dest, "wb") as out:
            sparse = info.filename.endswith(".img")
            if sparse:
                result=subprocess.run(["fsutil","sparse","setflag",str(dest)],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
                sparse=result.returncode==0
            with z.open(info) as stream:
                while True:
                    buf=stream.read(1024*1024)
                    if not buf: break
                    if sparse and buf.count(0)==len(buf): out.seek(len(buf),1)
                    else: out.write(buf)
            out.truncate(info.file_size)
        print(info.filename, info.file_size, flush=True)
print("extraction complete")
