package org.metadatacenter.submission.status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class SubmissionStatusManagerRunnable implements Runnable {
  final static Logger logger = LoggerFactory.getLogger(SubmissionStatusManagerRunnable.class);

  private static final int NUMBER_OF_THREADS = 10;
  private static final int CHECK_INTERVAL = 5000;

  private final SubmissionStatusManager submissionStatusManager;

  public SubmissionStatusManagerRunnable(SubmissionStatusManager submissionStatusManager) {
    this.submissionStatusManager = submissionStatusManager;
  }

  @Override
  public void run() {
    ExecutorService threadPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    Map<String, Future<SubmissionStatus>> futures = new HashMap<>();
    CompletionService<SubmissionStatus> pool = new ExecutorCompletionService<>(threadPool);

    while (true) {
      try {
        Map<String, SubmissionStatusDescriptor> currentSubmissions = submissionStatusManager.getCurrentSubmissions();

        if (!currentSubmissions.isEmpty()) {

          for (SubmissionStatusDescriptor submissionStatusDescriptor : currentSubmissions.values()) {
            SubmissionStatusTask submissionStatusTask = submissionStatusDescriptor.getSubmissionStatusTask();
            Future<SubmissionStatus> submissionStatusFuture = pool.submit(submissionStatusTask);

            futures.put(submissionStatusDescriptor.getSubmissionID(), submissionStatusFuture);
          }

          for (int i = 0; i < currentSubmissions.size(); i++) {
            SubmissionStatus submissionStatus = pool.take().get(1000, TimeUnit.MILLISECONDS);
            String submissionID = submissionStatus.getSubmissionID();

            futures.remove(submissionID);
            this.submissionStatusManager.updateSubmission(submissionStatus);
          }
        }
        Thread.sleep(CHECK_INTERVAL);
      } catch (CancellationException e) {
        logger.error("Cancellation exception : " + e.getMessage());
      } catch (InterruptedException e) {
        logger.error("Interrupted exception : " + e.getMessage());
      } catch (ExecutionException e) {
        logger.error("Execution exception : " + e.getMessage());
      } catch (TimeoutException e) {
        logger.error("Timeout exception : " + e.getMessage());
      } finally {
        for (String submissionID : futures.keySet()) {
          Future<SubmissionStatus> future = futures.get(submissionID);
          future.cancel(true); // Cancel tasks that did not complete in time
          logger.error("Status call for submission " + submissionID + " did not complete; cancelling");
        }
        futures.clear();
      }
    }
  }
}

