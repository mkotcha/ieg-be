import os
import sys
import win32com.client

# Get the directory of the script
script_dir = os.path.dirname(os.path.realpath(__file__))
parent_dir = os.path.dirname(script_dir)

# Check if sufficient arguments are passed (including the script name)
if len(sys.argv) < 3:
    print("Usage: script_name.py [Excel file path] [PDF file path]")
    sys.exit(1)  # Exit the script if the arguments are not provided

# Assign command line arguments to variables
excel_file_path = os.path.join(parent_dir, sys.argv[1])
pdf_file_path = os.path.join(parent_dir, sys.argv[2])

# Create an Excel Application object
o = win32com.client.Dispatch("Excel.Application")
o.Visible = False  # Excel will run in the background

# Open the workbook
wb = o.Workbooks.Open(excel_file_path)

# Select all sheets in the workbook
wb.Worksheets.Select()

# Export all selected sheets as a PDF
wb.ActiveSheet.ExportAsFixedFormat(0, pdf_file_path)

# Clean up: Close the workbook and quit Excel
wb.Close(SaveChanges=False)  # Close the workbook without saving changes
o.Quit()

# Print the name of the PDF file
print(f"PDF file created: {pdf_file_path}")
