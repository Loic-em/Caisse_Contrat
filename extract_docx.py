import zipfile, re
z = zipfile.ZipFile(r"C:\Users\Loice\Documents\Specifications_fonctionnelles_gestion_interne.docx")
# list
print(z.namelist())
# read document.xml
xml = z.read("word/document.xml").decode("utf-8")
# split into paragraphs
paras = re.findall(r"<w:p[ >].*?</w:p>|<w:p/>", xml, re.S)
out=[]
for p in paras:
    # get text runs
    texts = re.findall(r"<w:t[^>]*>(.*?)</w:t>", p, re.S)
    # handle xml entities
    import html
    line = "".join(html.unescape(t) for t in texts)
    out.append(line)
open("specs.txt","w",encoding="utf-8").write("\n".join(out))
print("paras", len(paras))
print("done")
