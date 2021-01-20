package org.metadatacenter.impex.imp.cadsr;

import org.metadatacenter.impex.upload.FlowUploadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class implements an executor service that clears the map that stores the import status periodically. The goal
 * is to remove from the map old import tasks that we don't need to keep there anymore.
 */
public class CadsrScheduledExecutor {

  final static Logger logger = LoggerFactory.getLogger(CadsrScheduledExecutor.class);

//  public static void initCleanImportMapExecutor() {
//    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
//    Runnable cleanTask = () -> {
//      logger.info("Clearing importStatus map");
//      CadsrImportStatusManager.getInstance().
//    };
//    executor.scheduleAtFixedRate(cleanTask, 3, 2, TimeUnit.SECONDS);
//  }
}

