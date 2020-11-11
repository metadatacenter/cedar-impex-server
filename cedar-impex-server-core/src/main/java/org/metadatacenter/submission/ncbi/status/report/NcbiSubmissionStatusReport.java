package org.metadatacenter.submission.ncbi.status.report;

/**
 * This class represents the submission status extracted from a report.[number].xml file
 */
public class NcbiSubmissionStatusReport {

  private final NcbiSubmissionState state;
  private final String xmlReport;
  private final String textReport; // generated from the xml report

  public NcbiSubmissionStatusReport(NcbiSubmissionState state, String xmlReport, String textReport) {
    this.state = state;
    this.xmlReport = xmlReport;
    this.textReport = textReport;
  }

  public NcbiSubmissionState getState() {
    return state;
  }

  public String getXmlReport() {
    return xmlReport;
  }

  public String getTextReport() {
    return textReport;
  }
}
