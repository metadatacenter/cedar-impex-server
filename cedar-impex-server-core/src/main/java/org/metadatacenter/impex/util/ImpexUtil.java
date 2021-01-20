package org.metadatacenter.impex.util;

public class ImpexUtil {

  public static String getFileNameFromFilePath(String filePath) {
    return filePath.substring(filePath.lastIndexOf("/") + 1);
  }

}
