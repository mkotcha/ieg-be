import os
import sys
import xlwings as xw


def copy_sheet_to_beginning_of_workbook(source_file, target_file, sheet_name):
    # Istanza di Excel dedicata: i file si aprono solo qui (xw.Book cercherebbe in tutte le istanze
    # aperte e potrebbe agganciarsi a una che si sta chiudendo)
    with xw.App(visible=False, add_book=False) as app:
        app.display_alerts = False
        source_wb = app.books.open(os.path.abspath(source_file))
        target_wb = app.books.open(os.path.abspath(target_file))

        # Copy first sheet of source to beginning of target workbook
        source_wb.sheets[0].api.Copy(Before=target_wb.sheets[0].api)
        target_wb.sheets[0].name = sheet_name
        # Delete the empty sheet created with the workbook
        if "sheet" in [s.name for s in target_wb.sheets]:
            target_wb.sheets["sheet"].delete()

        # Save and close
        target_wb.save()
        target_wb.close()
        source_wb.close()


if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python copyPagina1.py source_file.xlsx target_file.xlsx sheet_name")
        sys.exit(1)
    copy_sheet_to_beginning_of_workbook(sys.argv[1], sys.argv[2], sys.argv[3])
