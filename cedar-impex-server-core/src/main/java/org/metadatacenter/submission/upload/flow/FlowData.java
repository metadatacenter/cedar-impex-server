package org.metadatacenter.submission.upload.flow;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Object that represents a chunk of data uploaded to the server using the Flow.js library
 * See: https://github.com/flowjs/flow.js
 */
public class FlowData {

  public String submissionId;
  public long totalFilesCount;
  public List<String> metadataFiles; // comma separated string of metadata file names
  public long flowChunkNumber;
  public long flowChunkSize;
  public long flowCurrentChunkSize;
  public long flowTotalSize;
  public String flowIdentifier;
  public String flowFilename;
  public String flowRelativePath;
  public long flowTotalChunks;
  public InputStream flowFileInputStream;
  public Map<String, String> additionalParameters; // Stores additional (submission-specific) parameters

  public FlowData(String submissionId, long totalFilesCount, List<String> metadataFiles, long flowChunkNumber, long
      flowChunkSize, long flowCurrentChunkSize, long flowTotalSize, String flowIdentifier, String flowFilename,
                  String flowRelativePath, long flowTotalChunks, InputStream flowFileInputStream, Map<String, String>
                      additionalParameters) {
    this.submissionId = submissionId;
    this.totalFilesCount = totalFilesCount;
    this.metadataFiles = metadataFiles;
    this.flowChunkNumber = flowChunkNumber;
    this.flowChunkSize = flowChunkSize;
    this.flowCurrentChunkSize = flowCurrentChunkSize;
    this.flowTotalSize = flowTotalSize;
    this.flowIdentifier = flowIdentifier;
    this.flowFilename = flowFilename;
    this.flowRelativePath = flowRelativePath;
    this.flowTotalChunks = flowTotalChunks;
    this.flowFileInputStream = flowFileInputStream;
    this.additionalParameters = additionalParameters;
  }

  public String getSubmissionId() {
    return submissionId;
  }

  public void setSubmissionId(String submissionId) {
    this.submissionId = submissionId;
  }

  public long getTotalFilesCount() {
    return totalFilesCount;
  }

  public void setTotalFilesCount(long totalFilesCount) {
    this.totalFilesCount = totalFilesCount;
  }

  public List<String> getMetadataFiles() {
    return metadataFiles;
  }

  public void setMetadataFiles(List<String> metadataFiles) {
    this.metadataFiles = metadataFiles;
  }

  public long getFlowChunkNumber() {
    return flowChunkNumber;
  }

  public void setFlowChunkNumber(long flowChunkNumber) {
    this.flowChunkNumber = flowChunkNumber;
  }

  public long getFlowChunkSize() {
    return flowChunkSize;
  }

  public void setFlowChunkSize(long flowChunkSize) {
    this.flowChunkSize = flowChunkSize;
  }

  public long getFlowCurrentChunkSize() {
    return flowCurrentChunkSize;
  }

  public void setFlowCurrentChunkSize(long flowCurrentChunkSize) {
    this.flowCurrentChunkSize = flowCurrentChunkSize;
  }

  public long getFlowTotalSize() {
    return flowTotalSize;
  }

  public void setFlowTotalSize(long flowTotalSize) {
    this.flowTotalSize = flowTotalSize;
  }

  public String getFlowIdentifier() {
    return flowIdentifier;
  }

  public void setFlowIdentifier(String flowIdentifier) {
    this.flowIdentifier = flowIdentifier;
  }

  public String getFlowFilename() {
    return flowFilename;
  }

  public void setFlowFilename(String flowFilename) {
    this.flowFilename = flowFilename;
  }

  public String getFlowRelativePath() {
    return flowRelativePath;
  }

  public void setFlowRelativePath(String flowRelativePath) {
    this.flowRelativePath = flowRelativePath;
  }

  public long getFlowTotalChunks() {
    return flowTotalChunks;
  }

  public void setFlowTotalChunks(long flowTotalChunks) {
    this.flowTotalChunks = flowTotalChunks;
  }

  public InputStream getFlowFileInputStream() {
    return flowFileInputStream;
  }

  public void setFlowFileInputStream(InputStream flowFileInputStream) {
    this.flowFileInputStream = flowFileInputStream;
  }

  public Map<String, String> getAdditionalParameters() {
    return additionalParameters;
  }

  public void setAdditionalParameters(Map<String, String> additionalParameters) {
    this.additionalParameters = additionalParameters;
  }
}
