package org.metadatacenter.submission.ncbi.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.metadatacenter.config.CacheServerPersistent;
import org.metadatacenter.server.queue.util.QueueServiceWithBlockingQueue;
import org.metadatacenter.submission.ncbi.NcbiSubmission;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

public class NcbiSubmissionQueueService extends QueueServiceWithBlockingQueue {

  private static final Logger log = LoggerFactory.getLogger(NcbiSubmissionQueueService.class);

  public NcbiSubmissionQueueService(CacheServerPersistent cacheConfig) {
    super(cacheConfig, NCBI_SUBMISSION_QUEUE_ID);
  }

  public void enqueueSubmission(NcbiSubmission submission) {
    try (Jedis jedis = pool.getResource()) {
      String json = null;
      try {
        json = JsonMapper.MAPPER.writeValueAsString(submission);
      } catch (JsonProcessingException e) {
        log.error("Error while enqueueing submission", e);
      }
      jedis.rpush(queueName, json);
      int a = 2;
    }
  }
}
