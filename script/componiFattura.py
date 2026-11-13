"""
Compone le fatture: per ognuna copia in un unico file, nell'ordine, il primo foglio di ciascuna pagina
(file xlsx già compilati da Java), lo salva e ne esporta il pdf.
Tutte le fatture sono composte con la stessa istanza di Excel, avviata una sola volta.

Uso: python componiFattura.py lavori.json esiti.json

lavori.json: [{"xlsx": "...", "pdf": "...", "pagine": ["pagina_000.xlsx", ...]}, ...]
esiti.json:  [{"xlsx": "...", "ok": true, "errore": null}, ...]  (uno per lavoro, nello stesso ordine)
"""
import json
import os
import random
import sys
import time
import traceback

import xlwings as xw

XLSX = 51  # xlOpenXMLWorkbook
PDF = 0  # xlTypePDF
TENTATIVI_AVVIO = 5


def avvia_excel():
    """Avvia un'istanza di Excel dedicata, riprovando se l'avvio non riesce."""
    for tentativo in range(1, TENTATIVI_AVVIO + 1):
        try:
            app = xw.App(visible=False, add_book=False)
            app.display_alerts = False
            app.screen_updating = False
            return app
        except Exception as e:
            if tentativo == TENTATIVI_AVVIO:
                raise
            attesa = tentativo * 2 + random.random() * 2
            print(f"Avvio di Excel non riuscito ({e}), nuovo tentativo tra {attesa:.1f} s", flush=True)
            time.sleep(attesa)


def chiudi_excel(app):
    try:
        app.quit()
    except Exception:
        try:
            app.kill()
        except Exception:
            pass


def componi(app, lavoro):
    fattura = app.books.add()
    try:
        foglio_iniziale = fattura.sheets[0]
        for pagina in lavoro["pagine"]:
            sorgente = app.books.open(os.path.abspath(pagina))
            sorgente.sheets[0].api.Copy(After=fattura.sheets[-1].api)
            sorgente.close()
        foglio_iniziale.delete()
        try:
            # il file si apre sul riepilogo (attivazione via COM, senza chiedere il focus della finestra)
            fattura.api.Worksheets(1).Activate()
        except Exception:
            pass
        fattura.api.SaveAs(os.path.abspath(lavoro["xlsx"]), FileFormat=XLSX)
        fattura.api.ExportAsFixedFormat(PDF, os.path.abspath(lavoro["pdf"]))
    finally:
        fattura.close()


def main(file_lavori, file_esiti):
    with open(file_lavori, encoding="utf-8") as f:
        lavori = json.load(f)
    esiti = []
    app = avvia_excel()
    try:
        for lavoro in lavori:
            inizio = time.time()
            errore = None
            for tentativo in (1, 2):
                try:
                    componi(app, lavoro)
                    errore = None
                    break
                except Exception:
                    errore = traceback.format_exc()
                    # Excel potrebbe essere caduto: si riavvia e si riprova una volta
                    chiudi_excel(app)
                    app = avvia_excel()
            esiti.append({"xlsx": lavoro["xlsx"], "ok": errore is None, "errore": errore})
            print(f"{'ok' if errore is None else 'ERRORE'} {time.time() - inizio:.1f}s {lavoro['pdf']}", flush=True)
    finally:
        chiudi_excel(app)
        with open(file_esiti, "w", encoding="utf-8") as f:
            json.dump(esiti, f, ensure_ascii=False)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Uso: python componiFattura.py lavori.json esiti.json")
        sys.exit(1)
    main(sys.argv[1], sys.argv[2])
