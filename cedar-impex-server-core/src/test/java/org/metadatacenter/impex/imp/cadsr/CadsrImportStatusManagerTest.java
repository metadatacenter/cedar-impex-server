package org.metadatacenter.impex.imp.cadsr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.metadatacenter.impex.imp.cadsr.CadsrImportStatusManager.ImportStatus;
import org.metadatacenter.impex.upload.FlowData;
import org.metadatacenter.impex.upload.FlowUploadUtil;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The impex server keeps each caDSR import's progress, file by file, for its owner to poll: pending, in progress,
 * complete or in error, with the report the translation wrote. Only the owner sees an import. An import is
 * forgotten once all its files have ended long enough ago: ten minutes after completing, and an hour after failing,
 * so a user has time to read why.
 */
class CadsrImportStatusManagerTest {

  private static final CadsrImportStatusManager MANAGER = CadsrImportStatusManager.getInstance();

  @TempDir
  Path folder;

  @Test
  void eachFileStartsPendingAndMovesThroughItsImport() throws Exception {
    String uploadId = uploaded("owner", "form1.xml", "form2.xml");
    assertTrue(MANAGER.exists(uploadId));

    CadsrImportStatus status = MANAGER.getStatus(uploadId);
    assertEquals("owner", status.getOwnerUserId());
    assertEquals("folder-1", status.getDestinationCedarFolderId());
    assertEquals(ImportStatus.PENDING, status.getFilesImportStatus().get("form1.xml").getImportStatus());

    Instant pendingAt = status.getFilesImportStatus().get("form2.xml").getStatusTime();
    MANAGER.setStatus(uploadId, "form2.xml", ImportStatus.IN_PROGRESS);
    MANAGER.writeReportMessages(uploadId, "form2.xml", List.of("Added section: DEMOGRAPHY", "Form translation complete"));
    MANAGER.writeReportMessage(uploadId, "form2.xml", "Stamped", true);
    MANAGER.setStatus(uploadId, "form2.xml", ImportStatus.COMPLETE);

    CadsrFileImportStatus form2 = MANAGER.getStatus(uploadId).getFilesImportStatus().get("form2.xml");
    assertEquals(ImportStatus.COMPLETE, form2.getImportStatus());
    assertFalse(form2.getStatusTime().isBefore(pendingAt), "the age of a file runs from its latest change");
    String[] lines = form2.getReport().split(System.lineSeparator());
    assertEquals("Added section: DEMOGRAPHY", lines[0]);
    assertEquals("Form translation complete", lines[1]);
    assertTrue(lines[2].matches("\\[\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}] Stamped"), lines[2]);
    assertEquals("form2.xml", form2.getFileName());
  }

  @Test
  void anUnknownUploadOrFileIsRefused() throws Exception {
    String uploadId = uploaded("owner", "form1.xml");
    assertThrows(IllegalArgumentException.class, () -> MANAGER.setStatus("no-such-upload", "form1.xml", ImportStatus.ERROR));
    assertThrows(IllegalArgumentException.class, () -> MANAGER.setStatus(uploadId, "other.xml", ImportStatus.ERROR));
    assertThrows(IllegalArgumentException.class, () -> MANAGER.writeReportMessage("no-such-upload", "form1.xml", "x"));
    assertThrows(IllegalArgumentException.class, () -> MANAGER.writeReportMessage(uploadId, "other.xml", "x"));
  }

  @Test
  void onlyTheOwnerSeesAnImport() throws Exception {
    String mine = uploaded("ada", "form1.xml");
    String theirs = uploaded("grace", "form1.xml");

    assertEquals(mine, MANAGER.getStatusForUser(mine, "ada").getUploadId());
    assertNull(MANAGER.getStatusForUser(theirs, "ada"), "another user's import reads as not found");
    assertNull(MANAGER.getStatusForUser(mine, null));
    Map<String, CadsrImportStatus> adas = MANAGER.getStatusesForUser("ada");
    assertTrue(adas.containsKey(mine));
    assertFalse(adas.containsKey(theirs));
    assertTrue(MANAGER.getStatusesForUser(null).isEmpty());
  }

  @Test
  void anImportIsForgottenOnceEveryFileEndedLongEnoughAgo() throws Exception {
    String completed = uploaded("owner", "form1.xml");
    MANAGER.setStatus(completed, "form1.xml", ImportStatus.COMPLETE);
    String failed = uploaded("owner", "form1.xml");
    MANAGER.setStatus(failed, "form1.xml", ImportStatus.ERROR);
    String running = uploaded("owner", "form1.xml", "form2.xml");
    MANAGER.setStatus(running, "form1.xml", ImportStatus.COMPLETE);
    MANAGER.setStatus(running, "form2.xml", ImportStatus.IN_PROGRESS);
    Instant now = Instant.now();

    MANAGER.removeExpiredImports(now.plus(Duration.ofMinutes(5)));
    assertTrue(MANAGER.exists(completed), "a completed import stays for ten minutes");

    MANAGER.removeExpiredImports(now.plus(Duration.ofMinutes(11)));
    assertFalse(MANAGER.exists(completed));
    assertTrue(MANAGER.exists(failed), "a failed import stays for an hour");

    MANAGER.removeExpiredImports(now.plus(Duration.ofMinutes(61)));
    assertFalse(MANAGER.exists(failed));
    assertTrue(MANAGER.exists(running), "an import with a file still running stays");
  }

  /** Accepts a complete one-chunk upload of the named files for the owner, and starts its import status. */
  private String uploaded(String owner, String... fileNames) throws Exception {
    String uploadId = UUID.randomUUID().toString();
    for (String fileName : fileNames) {
      byte[] bytes = ("<form name=\"" + fileName + "\"/>").getBytes(StandardCharsets.UTF_8);
      FlowUploadUtil.saveToLocalFile(new FlowData(uploadId, fileNames.length, 1, bytes.length, bytes.length,
          bytes.length, "id-" + fileName, fileName, fileName, 1, new ByteArrayInputStream(bytes), Map.of()),
          owner, -1, folder.resolve(uploadId).toString());
    }
    MANAGER.initImportStatus(uploadId, owner, "folder-1");
    return uploadId;
  }
}
