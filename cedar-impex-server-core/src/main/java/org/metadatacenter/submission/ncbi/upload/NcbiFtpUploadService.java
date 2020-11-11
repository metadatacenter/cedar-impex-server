package org.metadatacenter.submission.ncbi.upload;

import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import org.metadatacenter.config.FTPConfig;
import org.metadatacenter.submission.upload.ftp.FileUploader;
import org.metadatacenter.submission.upload.ftp.FtpUploader;
import org.metadatacenter.submission.upload.ftp.UploaderCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class NcbiFtpUploadService {

  final static Logger logger = LoggerFactory.getLogger(NcbiFtpUploadService.class);

  /**
   * Upload a list of files to the NCBI server via the FTP protocol. For the NCBI submission, the files
   * should include submission.xml and FASTQ files. All these files will be stored in a remote directory
   * provided by the input parameter 'submissionDir'.
   *
   * @param submissionDir The directory name to be created at the remote server to store all the files.
   * @param listOfFiles   A list of files to be uploaded
   * @throws IOException               When upload failed due to I/O difficulties.
   * @throws UploaderCreationException When the FTP uploader failed to be created (e.g., hostname not found or
   *                                   invalid credential)
   */
  public static void uploadToNcbi(String submissionDir, Collection<File> listOfFiles, FTPConfig ftpConfig, boolean
      uploadSubmitReadyFile) throws IOException,
      UploaderCreationException {
    FileUploader uploader = null;

    logger.info("Submission upload, submissionDir = " + submissionDir + ", conf.submissionDir " + ftpConfig
        .getSubmissionDirectory());

    try {
      uploader = FtpUploader.createNewUploader(
          ftpConfig.getHost(),
          ftpConfig.getUser(),
          ftpConfig.getPassword(),
          Optional.of(ftpConfig.getSubmissionDirectory()));
      uploadResourceFiles(uploader, submissionDir, listOfFiles);
      if (uploadSubmitReadyFile) {
        uploadSubmitReadyFile(uploader, submissionDir);
      }
    } catch (IOException e) {
      logger.warn("Submission upload failed: ", e.getMessage());
    } finally {
      if (uploader != null) {
        try {
          uploader.disconnect();
        } catch (IOException e) {
          String message = String.format("Error while disconnecting from %s", ftpConfig.getHost());
          logger.error(message + ": " + e.getMessage());
        }
      }
    }
  }

  private static void uploadResourceFiles(FileUploader uploader, String submissionDir, Collection<File> listOfFiles)
      throws
      IOException {

    logger.info("uploadResourceFiles.submissionDir " + submissionDir);

    for (File file : listOfFiles) {
      Stopwatch stopwatch = Stopwatch.createStarted();
      logger.info("Submission in progress: Uploading '{}' file...", file.getName());
      uploader.store(submissionDir, file);
      logger.info("... uploaded in {} s", stopwatch.elapsed(TimeUnit.SECONDS));
    }
  }

  private static void uploadSubmitReadyFile(FileUploader uploader, String submissionDir) throws IOException {
    logger.info("Submission in progress: Uploading 'submit.ready' file...");
    File submitReady = createSubmitReadyFile();
    try {
      uploader.store(submissionDir, submitReady);
    } finally {
      if (submitReady != null) {
        submitReady.delete(); // remove traces
      }
    }
  }

  private static File createSubmitReadyFile() throws IOException {
    File submitReady = new File("submit.ready");
    Files.touch(submitReady);
    return submitReady;
  }
}
