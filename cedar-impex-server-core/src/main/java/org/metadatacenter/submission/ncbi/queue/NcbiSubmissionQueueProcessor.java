package org.metadatacenter.submission.ncbi.queue;

import io.dropwizard.lifecycle.Managed;
import org.metadatacenter.submission.ncbi.NcbiSubmission;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NcbiSubmissionQueueProcessor implements Managed {

  private static final Logger log = LoggerFactory.getLogger(NcbiSubmissionQueueProcessor.class);

  private final NcbiSubmissionQueueService ncbiSubmissionQueueService;
  private final NcbiSubmissionExecutorService ncbiSubmissionExecutorService;
  private boolean doProcessing;

  public NcbiSubmissionQueueProcessor(NcbiSubmissionQueueService ncbiSubmissionQueueService,
                                      NcbiSubmissionExecutorService ncbiSubmissionExecutorService) {
    this.ncbiSubmissionQueueService = ncbiSubmissionQueueService;
    this.ncbiSubmissionExecutorService = ncbiSubmissionExecutorService;
    doProcessing = true;
  }

  private void digestMessages() {
    log.info("NcbiSubmissionQueueProcessor.start()");
    ncbiSubmissionQueueService.initializeBlockingQueue();
    List<String> submissionMessages;
    while (doProcessing) {
      log.info("Waiting for a submission in the NCBI submission queue.");
      submissionMessages = ncbiSubmissionQueueService.waitForMessages();
      NcbiSubmissionQueueEvent event = null;
      if (submissionMessages != null && !submissionMessages.isEmpty()) {
        log.info("Got submission message.");
        String value = submissionMessages.get(1);
        try {
          event = new NcbiSubmissionQueueEvent(JsonMapper.MAPPER.readValue(value, NcbiSubmission.class));
        } catch (IOException e) {
          log.error("There was an error while deserializing submission", e);
        }
      }
      if (event != null) {
        try {
          if (event.getSubmission()!=null) {
            log.info(" no. files: " + event.getSubmission().getLocalFilePaths().size());
          }
          log.info(" created at: " + event.getCreatedAt());
          ncbiSubmissionExecutorService.handleEvent(event);
        } catch (Exception e) {
          log.error("There was an error while handling the message", e);
        }
      } else {
        log.warn("Unable to handle message, it is null.");
      }
    }
    log.info("NcbiSubmissionQueueProcessor finished gracefully");
  }

  @Override
  public void start() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.submit(this::digestMessages);
  }

  @Override
  public void stop() throws Exception {
    log.info("NcbiSubmissionQueueProcessor.stop()");
    log.info("Set looping flag to false");
    doProcessing = false;
    log.info("Close Jedis");
    ncbiSubmissionQueueService.enqueueSubmission(null);
    ncbiSubmissionQueueService.close();
  }
}
