package org.metadatacenter.impex.upload;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FlowUploadContractTest {
  @TempDir Path root;
  FlowData data(String id, long chunk, byte[] bytes) {
    return new FlowData(id, 1, chunk, 4, 4, 8,
        "file", "data.xml", "data.xml", 2, new ByteArrayInputStream(bytes), Map.of());
  }
  @ParameterizedTest @ValueSource(ints={-1, 1000})
  void adapterCountsDistinctStoredChunksRegardlessOfMultipartLength(int requestLength) throws Exception {
    String id = UUID.randomUUID().toString();
    var manager = UploadManager.getInstance();
    byte[] first = {1,2,3,4}, last = {5,6,7,8};
    FlowUploadUtil.saveToLocalFile(data(id,1,first), "owner", requestLength, root.toString());
    FlowUploadUtil.saveToLocalFile(data(id,1,first), "owner", requestLength, root.toString());
    assertFalse(manager.isUploadComplete("owner", id));
    assertThrows(BadRequestException.class, () -> FlowUploadUtil.saveToLocalFile(data(id,2,new byte[5]), "owner", requestLength, root.toString()));
    assertFalse(manager.isUploadComplete("owner", id));
    FlowUploadUtil.saveToLocalFile(data(id,2,last), "owner", requestLength, root.toString());
    assertTrue(manager.isUploadComplete("owner", id));
    assertTrue(manager.claimComplete("owner", id));
    assertFalse(manager.claimComplete("owner", id));
    assertArrayEquals(new byte[]{1,2,3,4,5,6,7,8}, Files.readAllBytes(root.resolve("data.xml")));
    
  }
  @Test void uploadIdentifiersCannotAliasTheSameFolder() {
    assertThrows(BadRequestException.class, () -> FlowUploadUtil.getUploadLocalFolderPath("uploads","owner","path/id"));
  }
}
