package org.emmek.IEG.helpers;

import com.poiji.bind.Poiji;

import java.io.File;
import java.util.List;

public class ExcelToList {

    public static List<ClienteModel> excelToListOfClienti(String fileLocation) {
        return Poiji.fromExcel(new File(fileLocation), ClienteModel.class);
    }
}