package org.metadatacenter.submission.upload.ftp;

public class UploaderCreationException extends Exception {

  public UploaderCreationException(String message) {
    super(message);
  }

  public UploaderCreationException(String message, Throwable throwable) {
    super(message, throwable);
  }
}
