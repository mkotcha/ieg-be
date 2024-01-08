import sys
import xlwings as xw


def copy_sheet_to_end_of_workbook(source_file, target_file):
    # Open both workbooks
    with xw.App(visible=False) as app:
        source_wb = xw.Book(source_file)
        target_wb = xw.Book(target_file)

        # Copy first sheet of source to end of target workbook
        source_wb.sheets[0].api.Copy(After=target_wb.sheets[-1].api)

        # Save and close
        target_wb.save()
        target_wb.close()
        source_wb.close()


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python script.py source_file.xlsx target_file.xlsx")
    else:
        source_file = sys.argv[1]
        target_file = sys.argv[2]
        copy_sheet_to_end_of_workbook(source_file, target_file)
