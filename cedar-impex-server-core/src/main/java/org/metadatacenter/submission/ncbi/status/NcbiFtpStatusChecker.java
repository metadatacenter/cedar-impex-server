package org.metadatacenter.submission.ncbi.status;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.metadatacenter.config.FTPConfig;
import org.metadatacenter.submission.ncbi.NcbiConstants;
import org.metadatacenter.submission.ncbi.status.report.NcbiSubmissionState;
import org.metadatacenter.submission.ncbi.status.report.NcbiSubmissionStatusReport;
import org.metadatacenter.submission.status.*;
import org.metadatacenter.submission.upload.ftp.UploaderCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class NcbiFtpStatusChecker {

  final static Logger logger = LoggerFactory.getLogger(NcbiFtpStatusChecker.class);

  public static SubmissionStatus getNcbiSubmissionStatus(String submissionID, FTPConfig ftpConfig,
                                                         String submissionFolder, String lastStatusReportFile)
      throws SAXException, ParserConfigurationException, TransformerException, UploaderCreationException,
      IOException, InterruptedException {

    FTPClient ftpClient = null;
    SubmissionStatus submissionStatus = null;
    try {

      String submissionPath = ftpConfig.getSubmissionDirectory() + "/" + submissionFolder;

      logger.info("Checking NCBI submission status (submissionPath: " + submissionPath + ")");

      // Open the FTP connection
      ftpClient = connect(ftpConfig.getHost(), ftpConfig.getUser(),
          ftpConfig.getPassword());

      // TODO: remove this block. It is used for testing
      if (!NcbiConstants.NCBI_SUBMIT || !NcbiConstants.NCBI_UPLOAD_SUBMIT_READY_FILE) {
        submissionPath = NcbiConstants.NCBI_TEST_SUBMISSION_PATH;
      }

      // Go to the submission folder
      int count = 0;
      while (count <= 3) {
        if (!ftpClient.changeWorkingDirectory(submissionPath)) {
          if (count < 3) {
            count++;
            logger.warn("Couldn't go to the submission folder (path: " + submissionPath + "). Retrying...");
            Thread.sleep(3000);
          } else {
            SubmissionStatusManager.getInstance().removeSubmission(submissionID);
            throw new IOException("Couldn't go to the submission folder (path: " + submissionPath + ")");
          }
        } else {
          break;
        }
      }

      FTPFile[] files = ftpClient.listFiles();
      Optional<String> mostRecentReportFileName = getMostRecentReportFileName(files);
      if (mostRecentReportFileName.isPresent()) { // the folder contains a report file (at the minimum)
        if (!mostRecentReportFileName.get().equals(lastStatusReportFile)) { // there is a new report
          // update the variable that stores the name of the last report checked
          SubmissionStatusDescriptor submissionStatusDescriptor = SubmissionStatusManager.getInstance()
              .getCurrentSubmissions().get(submissionID);
          NcbiSubmissionStatusTask statusTask = (NcbiSubmissionStatusTask) submissionStatusDescriptor
              .getSubmissionStatusTask();
          statusTask.setLastStatusReportFile(mostRecentReportFileName.get());

          // generate submission status from the most recent report file
          InputStream inputStream = ftpClient.retrieveFileStream(mostRecentReportFileName.get());
          NcbiSubmissionStatusReport statusFromReport = getSubmissionStatusFromReport(inputStream);
          submissionStatus = NcbiSubmissionStatusUtil.toSubmissionStatus(submissionID, statusFromReport);
          logger.info("The submission status has been updated (submissionId = " + submissionID + ")");
          logger.info(submissionStatus.toString());
        } else { // the report file has already been checked so the status will be the same
          submissionStatus =
              SubmissionStatusManager.getInstance().getCurrentSubmissions().get(submissionID).getSubmissionStatus();
        }
      } else { // the folder does not contain any report file yet
        String message = SubmissionStatusUtil.getShortStatusMessage(submissionID, SubmissionState.PROCESSING)
            + "\n" + "The submission is being processed";
        submissionStatus = new SubmissionStatus(submissionID, SubmissionState.PROCESSING, message);
      }
      ftpClient.logout();
    } catch (IOException | ParserConfigurationException | TransformerException | SAXException |
        UploaderCreationException e) {
      logger.info("Exception checking submission status (submissionId = " + submissionID + "): " + e.getMessage());
      throw e;
    } finally {
      // Close the FTP connection
      if (ftpClient.isConnected()) {
        try {
          ftpClient.disconnect();
        } catch (IOException e) {
          // do nothing
        }
      }
    }
    return submissionStatus;
  }

  private static Optional<String> getMostRecentReportFileName(FTPFile[] files) {
    String lastReportFileName = null;
    int lastReportNumber = -1;
    for (FTPFile file : files) {
      if (file.isFile()) {
        if (file.getName().matches(NcbiConstants.NCBI_REPORT_REGEX)) {
          int currentReportNumber = getReportNumber(file.getName());
          if (currentReportNumber > lastReportNumber) {
            lastReportFileName = file.getName();
            lastReportNumber = currentReportNumber;
          }
        }
      }
    }
    return Optional.ofNullable(lastReportFileName);
  }

  /**
   * Returns the number of the status report from the file name (e.g., report.2.xml -> 2)
   */
  private static int getReportNumber(String fileName) {
    int index1 = 6; // index of the first '.'
    int index2 = fileName.indexOf(".xml");
    return Integer.parseInt(fileName.substring(index1 + 1, index2));
  }

  private static NcbiSubmissionStatusReport getSubmissionStatusFromReport(InputStream inputStream)
      throws ParserConfigurationException, IOException, SAXException, TransformerException {

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document statusReport = builder.parse(inputStream);

    // Get the submission status from the xml
    Node submissionStatus = statusReport.getElementsByTagName("SubmissionStatus").item(0);
    String status = submissionStatus.getAttributes().getNamedItem("status").getNodeValue();
    // Generate plain text report
    String textReport = NcbiSubmissionStatusUtil.generatePlainTextReport(statusReport);

    return new NcbiSubmissionStatusReport(NcbiSubmissionState.fromString(status), statusReport.toString(),
        textReport);
  }

  private static FTPClient connect(String host, String user, String password) throws UploaderCreationException {
    FTPClient ftpClient = new FTPClient();
    try {
      ftpClient.connect(host);
      int replyCode = ftpClient.getReplyCode();
      if (!FTPReply.isPositiveCompletion(replyCode)) {
        showServerReply(ftpClient);
        throw new UploaderCreationException("Failed to connect to the FTP server: " + host);
      }
      boolean success = ftpClient.login(user, password);
      if (!success) {
        ftpClient.disconnect();
        showServerReply(ftpClient);
        throw new UploaderCreationException("Invalid username and password to login to the FTP server: " + host);
      }
      ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
      //ftpClient.enterRemotePassiveMode();
      ftpClient.enterLocalPassiveMode();
      ftpClient.setControlKeepAliveTimeout(3000); // set timeout to 5 minutes

      return ftpClient;
    } catch (IOException ex) {
      logger.error(ex.getMessage());
      throw new UploaderCreationException("Error while creating the FTP client", ex);
    }
  }

  private static void showServerReply(FTPClient ftpClient) {
    String[] replies = ftpClient.getReplyStrings();
    if (replies != null && replies.length > 0) {
      for (String reply : replies) {
        logger.error(reply);
      }
    }
  }
}

