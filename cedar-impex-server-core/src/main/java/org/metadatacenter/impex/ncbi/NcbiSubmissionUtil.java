package org.metadatacenter.impex.ncbi;

import org.metadatacenter.impex.upload.flow.FileUploadStatus;
import org.metadatacenter.impex.upload.flow.FlowUploadUtil;
import org.metadatacenter.impex.upload.flow.SubmissionUploadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NcbiSubmissionUtil {

  final static Logger log = LoggerFactory.getLogger(NcbiSubmissionUtil.class);

  public static NcbiSubmission generateSubmission(String submissionId, String userId, String ncbiFolderName,
                                                  NcbiSubmissionXMLFileGenerator submissionXMLFileGenerator)
      throws IOException, JAXBException, DatatypeConfigurationException {

    List<String> submissionFilePaths = new ArrayList<>();
    Map<String, FileUploadStatus> filesUploadStatus = SubmissionUploadManager.getInstance()
        .getSubmissionsUploadStatus(submissionId).getFilesUploadStatus();

    // Iterate over the submission files. Use the instance to generate the submission.xml file, keep the xml file
    for (Map.Entry<String, FileUploadStatus> entry : filesUploadStatus.entrySet()) {
      FileUploadStatus fileUploadStatus = entry.getValue();

        submissionFilePaths.add(fileUploadStatus.getFileLocalPath());

    }

    // Generate the submission object
    NcbiSubmission submission =
        new NcbiSubmission(submissionId, userId, submissionFilePaths,
            ncbiFolderName, NcbiConstants.NCBI_UPLOAD_SUBMIT_READY_FILE);

    return submission;
  }
}
