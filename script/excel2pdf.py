import os
import sys
import win32com.client

script_dir = os.path.dirname(os.path.realpath(__file__))
parent_dir = os.path.dirname(script_dir)

if len(sys.argv) < 3:
    print("Usage: excel2pdf.py [Excel file path] [PDF file path]")
    sys.exit(1)

excel_file_path = os.path.join(parent_dir, sys.argv[1])
pdf_file_path = os.path.join(parent_dir, sys.argv[2])

o = win32com.client.Dispatch("Excel.Application")
o.Visible = False

wb = o.Workbooks.Open(excel_file_path)
wb.Worksheets.Select()
wb.ActiveSheet.ExportAsFixedFormat(0, pdf_file_path)
wb.Close(SaveChanges=False)
o.Quit()

print(f"PDF creato: {pdf_file_path}")
