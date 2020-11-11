package org.metadatacenter.submission.ncbi.pipelines.generic;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.metadatacenter.submission.ncbi.NcbiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;

public class NcbiGenericSubmissionXMLFileGenerator implements org.metadatacenter.submission.ncbi.NcbiSubmissionXMLFileGenerator {

  final static Logger logger = LoggerFactory.getLogger(NcbiGenericSubmissionXMLFileGenerator.class);

  public File generateSubmissionXmlFile(File instanceFile, String submissionLocalFolderPath) throws
      IOException, JAXBException, DatatypeConfigurationException {

    NcbiGenericTemplateInstance2XMLConverter converter = new NcbiGenericTemplateInstance2XMLConverter();

    String submissionXml = null;
    try {
      JsonNode instanceJson = (new ObjectMapper()).readTree(instanceFile);
      submissionXml = converter.convertTemplateInstanceToXML(instanceJson);
    } catch (JsonMappingException e) {
      throw new IOException("The instance uploaded is not compatible with the CAIRR template", e);
    } catch (ParseException e) {
      logger.error(e.getMessage());
    }

    logger.info("XML: " + submissionXml);

    File submissionXmlFile = new File(submissionLocalFolderPath + "/" + NcbiConstants.SUBMISSION_XML_FILE_NAME);
    FileUtils.writeStringToFile(submissionXmlFile, submissionXml);
    return submissionXmlFile;
  }
}
