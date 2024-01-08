import pandas as pd
import sys
from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas


def excel_to_pdf(excel_file, pdf_file):
    # Read the Excel file
    df = pd.read_excel(excel_file)

    # Create a PDF canvas and set up basic parameters
    c = canvas.Canvas(pdf_file, pagesize=letter)
    width, height = letter
    x_offset = 50  # Starting x-coordinate
    y_offset = height - 50  # Starting y-coordinate
    line_height = 20  # Height of each line

    # Write the column headers
    for i, column in enumerate(df.columns):
        c.drawString(x_offset + (i * 100), y_offset, str(column))
    y_offset -= line_height

    # Write the data rows
    for index, row in df.iterrows():
        for i, value in enumerate(row):
            c.drawString(x_offset + (i * 100), y_offset, str(value))
        y_offset -= line_height
        if y_offset < 50:  # Start a new page if the current one is full
            c.showPage()
            y_offset = height - 50

    # Save the PDF
    c.save()


# Example usage
if len(sys.argv) == 3:
    excel_file_path = sys.argv[1]
    pdf_file_path = sys.argv[2]
    excel_to_pdf(excel_file_path, pdf_file_path)
else:
    print("Usage: python excel2pdf.py [excel_file_path] [pdf_file_path]")
