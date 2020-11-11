package org.metadatacenter.submission.ncbi.status;

import org.metadatacenter.submission.ncbi.NcbiConstants;
import org.metadatacenter.submission.ncbi.status.report.NcbiSubmissionState;
import org.metadatacenter.submission.ncbi.status.report.NcbiSubmissionStatusReport;
import org.metadatacenter.submission.status.SubmissionState;
import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusUtil;
import org.w3c.dom.Document;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringWriter;

public class NcbiSubmissionStatusUtil {

  public static SubmissionStatus toSubmissionStatus(String submissionId, NcbiSubmissionStatusReport report) {
    SubmissionState submissionState = null;
    if (report.getState().equals(NcbiSubmissionState.SUBMITTED)) {
      submissionState = SubmissionState.PROCESSING;
    } else if (report.getState().equals(NcbiSubmissionState.PROCESSING)) {
      submissionState = SubmissionState.PROCESSING;
    } else if (report.getState().equals(NcbiSubmissionState.PROCESSED_ERROR)) {
      submissionState = SubmissionState.ERROR;
    } else if (report.getState().equals(NcbiSubmissionState.FAILED)) {
      submissionState = SubmissionState.REJECTED;
    }
    // TODO: complete with all NCBI states
    else {
      submissionState = SubmissionState.SUCCEEDED;
    }

    String message = SubmissionStatusUtil.getShortStatusMessage(submissionId, submissionState) +
        "\nNCBI STATUS REPORT\n==================" + report.getTextReport();

    return new SubmissionStatus(submissionId, submissionState, message);
  }

  public static String generatePlainTextReport(Document xmlReport) throws TransformerException {
    TransformerFactory factory = TransformerFactory.newInstance();
    InputStream inputStream =
        NcbiSubmissionStatusUtil.class.getClassLoader().getResourceAsStream(NcbiConstants.NCBI_XSLT_PATH);
    Source xslSource = new StreamSource(inputStream);
    Transformer transformer = factory.newTransformer(xslSource);
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(xmlReport), new StreamResult(writer));
    String output = writer.getBuffer().toString();
    return output;
  }

}
