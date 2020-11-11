package org.metadatacenter.submission.ncbi.queue;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.submission.ncbi.NcbiConstants;
import org.metadatacenter.submission.ncbi.NcbiSubmission;
import org.metadatacenter.submission.ncbi.status.NcbiSubmissionStatusTask;
import org.metadatacenter.submission.ncbi.upload.NcbiFtpUploadService;
import org.metadatacenter.submission.status.SubmissionStatusManager;
import org.metadatacenter.submission.status.SubmissionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NcbiSubmissionExecutorService {

  private static final Logger logger = LoggerFactory.getLogger(NcbiSubmissionExecutorService.class);
  private final CedarConfig cedarConfig;

  public NcbiSubmissionExecutorService(CedarConfig cedarConfig) {
    this.cedarConfig = cedarConfig;
  }

  // Main entry point
  public void handleEvent(NcbiSubmissionQueueEvent event) {
    submit(event.getSubmission());
  }

  private void submit(NcbiSubmission submission) {
    try {
      // Read files
      List<File> filesToSubmit = new ArrayList<>();
      for (String filePath : submission.getLocalFilePaths()) {
        filesToSubmit.add(new File(filePath));
      }

      logger.info("Uploading to NCBI...");

      // Track the submission status
      NcbiSubmissionStatusTask submissionStatusTask = new NcbiSubmissionStatusTask(submission.getId(),
          SubmissionType.NCBI, submission.getCedarUserId(), null, cedarConfig.getSubmissionConfig().getNcbi()
          .getSra().getFtp(),
          submission.getSubmissionFolder());
      SubmissionStatusManager.getInstance().setCedarConfig(cedarConfig);
      SubmissionStatusManager.getInstance().addSubmission(submissionStatusTask);

      logger.info("Added submission status task to submission status manager");

      if (NcbiConstants.NCBI_SUBMIT) { // real submission
        NcbiFtpUploadService.uploadToNcbi(submission.getSubmissionFolder(),
            filesToSubmit, cedarConfig.getSubmissionConfig().getNcbi().getSra().getFtp(), submission
                .getUploadSubmitReadyFile());
      } else { // simulated submission
        Thread.sleep(NcbiConstants.NCBI_SIMULATION_MODE_TIMEOUT);
      }

      logger.info("Submission to the NCBI completed! Submission id: " + submission.getId() + "; No. files: " +
          submission.getLocalFilePaths().size());
      logger.info("Deleting the submission local folder: " + submission.getSubmissionFolder());
      // Delete the submission local folder
      //FileUtils.deleteDirectory(new File(submission.getSubmissionFolder()));

    } catch (Exception e) {
      logger.error("Error submitting the data to the NCBI.");
      logger.error(e.getMessage());
    }
  }
}
