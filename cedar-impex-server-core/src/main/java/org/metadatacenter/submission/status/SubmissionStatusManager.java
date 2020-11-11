package org.metadatacenter.submission.status;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.submission.notifications.StatusNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// TODO Use Redis queue
// TODO Need to add insertion time in descriptor and clean old ones

public class SubmissionStatusManager {
  final static Logger logger = LoggerFactory.getLogger(SubmissionStatusManager.class);
  private static SubmissionStatusManager singleInstance;
  private final ExecutorService executor;
  private final ConcurrentHashMap<String, SubmissionStatusDescriptor> submissions = new ConcurrentHashMap<>();
  // TODO: this is not nice. Find another way of having these variable available to call the messaging server
  private CedarConfig cedarConfig;

  private SubmissionStatusManager() {
    this.executor = Executors.newFixedThreadPool(10);
  }

  public static synchronized SubmissionStatusManager getInstance() {
    if (singleInstance == null) {
      singleInstance = new SubmissionStatusManager();
      singleInstance.start();
    }
    return singleInstance;
  }

  public void setCedarConfig(CedarConfig cedarConfig) {
    this.cedarConfig = cedarConfig;
  }

  private void start() {
    logger.info("Starting the submission status manager");
    executor.submit(new SubmissionStatusManagerRunnable(this));
  }

  public void stop() {
    logger.info("Stopping the submission status manager");
    executor.shutdown();
    try {
      executor.awaitTermination(100, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      logger.warn("Submission status manager shutdown interrupted: " + e.getMessage());
    }
  }

  public String addSubmission(SubmissionStatusTask submissionStatusTask) {
    String submissionID = submissionStatusTask.getSubmissionID();
    SubmissionStatus submissionStatus = new SubmissionStatus(submissionID, SubmissionState.SUBMITTED,
        SubmissionStatusUtil.getShortStatusMessage(submissionID, SubmissionState.SUBMITTED));
    SubmissionStatusDescriptor submissionStatusDescriptor = new SubmissionStatusDescriptor(submissionID,
        submissionStatusTask.getUserID(), submissionStatusTask.getStatusURL(), submissionStatus, submissionStatusTask);

    this.submissions.put(submissionID, submissionStatusDescriptor);

    notifyUser(submissionStatusDescriptor);

    return submissionID;
  }

  public void updateSubmission(SubmissionStatus submissionStatus) {
    String submissionID = submissionStatus.getSubmissionID();

    if (!this.submissions.containsKey(submissionID)) {
      logger.warn("Attempt to update unknown submission " + submissionID);
    } else {
      SubmissionStatusDescriptor currentSubmissionStatusDescriptor = submissions.get(submissionID);
      SubmissionStatusDescriptor newSubmissionStatusDescriptor = new SubmissionStatusDescriptor(submissionID,
          currentSubmissionStatusDescriptor.getUserID(), currentSubmissionStatusDescriptor.getStatusURL(),
          submissionStatus, currentSubmissionStatusDescriptor.getSubmissionStatusTask());

      // If the status has changed, notify user. We consider that the status has changed in the following cases:
      // 1. The new state is different than the previous one (e.g., STARTED vs SUBMITTED)
      // 2. The new state is the same, but the messages are different (except for the STARTED state)
      SubmissionStatus currentStatus = currentSubmissionStatusDescriptor.getSubmissionStatus();
      SubmissionStatus newStatus = newSubmissionStatusDescriptor.getSubmissionStatus();
      if ((currentStatus.getSubmissionState() != newStatus.getSubmissionState()) ||
          (!currentStatus.getStatusMessage().equals(newStatus.getStatusMessage()) &&
              !currentStatus.getSubmissionState().equals(SubmissionState.SUBMITTED))) {
        notifyUser(newSubmissionStatusDescriptor);
      }

      logger.info("Submission status: " + newStatus.getSummary());

      this.submissions.put(submissionID, newSubmissionStatusDescriptor);

      if (submissionStatus.getSubmissionState() == SubmissionState.SUCCEEDED
          || submissionStatus.getSubmissionState() == SubmissionState.REJECTED
          || submissionStatus.getSubmissionState() == SubmissionState.ERROR) {
        removeSubmission(submissionID);
      }
    }
  }

  public void removeSubmission(String submissionID) {
    if (!this.submissions.containsKey(submissionID)) {
      logger.warn("Attempt to remove unknown submission " + submissionID);
    } else {
      SubmissionStatusDescriptor submissionStatusDescriptor = this.submissions.get(submissionID);

      if (submissionStatusDescriptor.getSubmissionStatus().getSubmissionState() != SubmissionState.SUCCEEDED) {
        logger.warn("Removing incomplete submission " + submissionID);
      }

      logger.info("Removing submission " + submissionID);

      this.submissions.remove(submissionID);
    }
  }

  public Map<String, SubmissionStatusDescriptor> getCurrentSubmissions() {
    synchronized (submissions) {
      return Collections.unmodifiableMap(submissions);
    }
  }

  private void notifyUser(SubmissionStatusDescriptor submissionStatusDescriptor) {
    logger.info("Notifying user for submission " + submissionStatusDescriptor.getSubmissionID() + "; status = "
        + submissionStatusDescriptor.getSubmissionStatus().getSubmissionState() + ", message = "
        + submissionStatusDescriptor.getSubmissionStatus().getStatusMessage());

    StatusNotifier.getInstance().sendMessage(submissionStatusDescriptor);
  }
}
