package org.metadatacenter.submission.ncbi;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Read a CEDAR instance file and generate a file containing its NCBI-compliant XML representation.
 * <p>
 * Will be specialized for different templates.
 */
public interface NcbiSubmissionXMLFileGenerator {
  File generateSubmissionXmlFile(File instanceFile, String submissionLocalFolderPath) throws
      IOException, JAXBException, DatatypeConfigurationException;
}
