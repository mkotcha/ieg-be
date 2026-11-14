"""
Compone le fatture: per ognuna copia in un unico file, nell'ordine, il primo foglio di ciascuna pagina
(file xlsx già compilati da Java), lo salva e ne esporta il pdf.
Tutte le fatture sono composte con la stessa istanza di Excel, avviata una sola volta.

Uso: python componiFattura.py lavori.json esiti.json

lavori.json: [{"xlsx": "...", "pdf": "...", "pagine": ["pagina_000.xlsx", ...]}, ...]
esiti.json:  [{"xlsx": "...", "ok": true, "errore": null}, ...]  (uno per lavoro, nello stesso ordine)
"""
import ctypes
import json
import os
import random
import sys
import threading
import time
import traceback
from ctypes import wintypes

import pythoncom
import xlwings as xw

XLSX = 51  # xlOpenXMLWorkbook
PDF = 0  # xlTypePDF
TENTATIVI_AVVIO = 5
# tema dei template: la cartella creata da Excel ha il tema predefinito (Office 2023+, Aptos) e i fogli copiati
# prenderebbero colori e font del tema di destinazione. Sovrascrivibile con la variabile d'ambiente IEG_TEMA_EXCEL.
TEMA = os.environ.get(
    "IEG_TEMA_EXCEL",
    r"C:\Program Files\Microsoft Office\root\Document Themes 16\Office 2013 - 2022 Theme.thmx",
)


_user32 = ctypes.windll.user32
_user32.CreateDesktopW.restype = wintypes.HANDLE
_user32.CreateDesktopW.argtypes = [wintypes.LPCWSTR, wintypes.LPCWSTR, ctypes.c_void_p, wintypes.DWORD,
                                   wintypes.DWORD, ctypes.c_void_p]
_user32.SetThreadDesktop.argtypes = [wintypes.HANDLE]
_user32.CloseDesktop.argtypes = [wintypes.HANDLE]
_GENERIC_ALL = 0x10000000
DESKTOP_NASCOSTO = "ieg_excel"


def su_desktop_nascosto(funzione, *args):
    """
    Esegue funzione in un thread associato a un desktop Windows nascosto: l'istanza di Excel avviata da quel thread
    nasce su quel desktop e le finestre che apre comunque, come "Publishing..." con la barra di avanzamento durante
    ExportAsFixedFormat (Excel invisibile la mostra lo stesso e l'API non permette di evitarla), non compaiono sul
    desktop dell'utente. Se il desktop non si può creare esegue funzione normalmente.
    """
    desktop = _user32.CreateDesktopW(DESKTOP_NASCOSTO, None, None, 0, _GENERIC_ALL, None)
    if not desktop:
        print(f"Desktop nascosto non creato (errore {ctypes.GetLastError()}): Excel mostrerà le sue finestre",
              flush=True)
        return funzione(*args)
    esito = {}

    def esegui():
        try:
            # va associato prima che il thread crei finestre o inizializzi COM
            if not _user32.SetThreadDesktop(desktop):
                print(f"SetThreadDesktop non riuscita (errore {ctypes.GetLastError()})", flush=True)
            pythoncom.CoInitialize()
            try:
                esito["valore"] = funzione(*args)
            finally:
                pythoncom.CoUninitialize()
        except BaseException as e:
            esito["errore"] = e

    thread = threading.Thread(target=esegui, name="excel-nascosto")
    thread.start()
    thread.join()
    _user32.CloseDesktop(desktop)
    if "errore" in esito:
        raise esito["errore"]
    return esito.get("valore")


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
        fattura.api.ApplyTheme(TEMA)
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
    if not os.path.isfile(TEMA):
        raise FileNotFoundError(f"Tema Excel non trovato: {TEMA} (impostare IEG_TEMA_EXCEL)")
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
    su_desktop_nascosto(main, sys.argv[1], sys.argv[2])
