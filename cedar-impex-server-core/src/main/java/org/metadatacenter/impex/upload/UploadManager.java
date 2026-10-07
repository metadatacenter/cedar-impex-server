package org.metadatacenter.impex.upload;

import jakarta.ws.rs.BadRequestException;
import org.metadatacenter.impex.exception.UploadInstanceNotFoundException;
import org.metadatacenter.util.upload.ChunkUploadStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Service-specific view of the shared chunk assembler; status is a detached snapshot. */
public class UploadManager {
  private static final UploadManager INSTANCE = new UploadManager();
  private final ChunkUploadStore uploads = new ChunkUploadStore();
  private UploadManager() { }
  public static UploadManager getInstance() { return INSTANCE; }

  public String accept(FlowData data, String owner, String folder) throws IOException {
    return uploads.accept(owner, data.getUploadId(), data.getTotalFilesCount(), Path.of(folder),
        new ChunkUploadStore.Chunk(data.getFlowIdentifier(), data.getFlowFilename(), data.getFlowChunkNumber(),
            data.getFlowChunkSize(), data.getFlowCurrentChunkSize(), data.getFlowTotalSize(), data.getFlowTotalChunks(),
            false), data.getFlowFileInputStream());
  }

  public boolean isUploadComplete(String owner, String id) throws UploadInstanceNotFoundException {
    return required(owner, id).complete();
  }
  public boolean claimComplete(String owner, String id) { return uploads.claimComplete(owner, id); }
  public void releaseClaim(String owner, String id) { uploads.releaseClaim(owner, id); }
  public void removeUploadStatus(String owner, String id) { uploads.retire(owner, id); }

  private ChunkUploadStore.Status required(String owner, String id) throws UploadInstanceNotFoundException {
    var status = uploads.status(owner, id);
    if (status == null) throw new UploadInstanceNotFoundException("Upload not found: " + id);
    return status;
  }
  public List<String> getUploadFilePaths(String owner, String id) throws UploadInstanceNotFoundException {
    var status = required(owner, id);
    if (!status.complete()) throw new BadRequestException("The upload is not complete");
    return status.files().values().stream().map(ChunkUploadStore.FileStatus::path).toList();
  }
  public UploadStatus getUploadStatus(String owner, String id) {
    var status = uploads.status(owner, id);
    if (status == null) return null;
    Map<String, FileUploadStatus> files = new HashMap<>();
    status.files().forEach((key, f) -> files.put(key,
        new FileUploadStatus(f.totalChunks(), f.uploadedChunks(), f.path())));
    return new UploadStatus(status.totalFiles(), status.uploadedFiles(), files, status.folder());
  }
  public List<String> getUploadFileNames(String owner, String id) throws UploadInstanceNotFoundException {
    return getUploadFilePaths(owner, id).stream().map(p -> Path.of(p).getFileName().toString()).toList();
  }
}
