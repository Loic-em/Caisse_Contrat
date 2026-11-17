from pypdf import PdfReader
r = PdfReader(r"C:\Users\Loice\Documents\Cahier des charges – Logiciel de gestion interne (Caisse et Contrats de travail).pdf")
print("pages", len(r.pages))
out=[]
for i,p in enumerate(r.pages):
    out.append("=== PAGE %d ===" % (i+1))
    out.append(p.extract_text() or "")
open("cahier.txt","w",encoding="utf-8").write("\n".join(out))
print("done")
