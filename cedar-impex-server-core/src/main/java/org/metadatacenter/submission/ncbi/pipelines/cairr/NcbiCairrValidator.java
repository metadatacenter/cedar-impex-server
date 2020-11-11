package org.metadatacenter.submission.ncbi.pipelines.cairr;

import com.fasterxml.jackson.databind.JsonNode;
import org.metadatacenter.submission.CEDARValidationResponse;
import org.metadatacenter.submission.ncbi.pipelines.NcbiPipelinesCommonUtil;
import org.metadatacenter.submission.ncbi.pipelines.NcbiPipelinesCommonValidator;

import java.util.*;

import static org.metadatacenter.submission.ncbi.pipelines.cairr.NcbiCairrConstants.*;

public class NcbiCairrValidator {

  public CEDARValidationResponse validateInstance(JsonNode instance) {

    CEDARValidationResponse validationResponse = new CEDARValidationResponse();
    List<String> messages = new ArrayList<>();

    /** Validate top level fields **/
    NcbiPipelinesCommonUtil.isValidField(instance, SUBMISSION_RELEASE_DATE_FIELD, true, false);

    /** Validate BioProject **/
    Optional<JsonNode> bioproject = NcbiPipelinesCommonUtil.getTemplateElementNode(instance, BIOPROJECT_ELEMENT);
    if (bioproject.isEmpty()) {
      messages.add("Missing element: " + BIOPROJECT_ELEMENT);
    }
    else {
      messages.addAll(NcbiPipelinesCommonValidator.validateBioproject(bioproject.get(), BIOPROJECT_FIELDS, BIOPROJECT_REQUIRED_FIELD_VALUES));
    }

    /** Validate BioSample **/
    Optional<JsonNode> biosamples = NcbiPipelinesCommonUtil.getTemplateElementNode(instance, BIOSAMPLE_ELEMENT);
    if (biosamples.isEmpty()) {
      messages.add("Missing element: " + BIOSAMPLE_ELEMENT);
    }
    else {
      messages.addAll(NcbiPipelinesCommonValidator.validateBiosample(biosamples.get(), BIOSAMPLE_FIELDS, BIOSAMPLE_REQUIRED_FIELD_VALUES));
    }

    /** Validate SRA **/
    Optional<JsonNode> sras = NcbiPipelinesCommonUtil.getTemplateElementNode(instance, SRA_ELEMENT);
    if (sras.isEmpty()) {
      messages.add("Missing element: " + SRA_ELEMENT);
    }
    else {
      messages.addAll(NcbiPipelinesCommonValidator.validateSra(sras.get(), SRA_FIELDS, SRA_FILE_NAME_FIELD, SRA_REQUIRED_FIELD_VALUES));
    }

    /** Check the BioSample-SRA references **/
    messages.addAll(NcbiPipelinesCommonValidator.validateBiosampleSraRefs(biosamples.get(), sras.get(),
        BIOSAMPLE_SAMPLE_ID_FIELD, SRA_SAMPLE_ID_FIELD));

    /** Return validation messages **/
    validationResponse.setMessages(messages);

    if (messages.size() == 0) {
      validationResponse.setIsValid(true);
    } else {
      validationResponse.setIsValid(false);
    }

    return validationResponse;
  }

  /**
   * Checks the consistency between the file names selected to be uploaded and the file names included in the instance (SRA section)
   * @return
   */
  public CEDARValidationResponse validateFilenames(JsonNode instance, List<String> userFileNames) {

    CEDARValidationResponse validationResponse = new CEDARValidationResponse();
    List<String> messages = new ArrayList<>();

    // Extract file names from the sra section
    List<String> sraFilenames = NcbiPipelinesCommonUtil.extractSraFileNames(instance, SRA_ELEMENT, SRA_FILE_NAME_FIELD);

    for (String sraFilename : sraFilenames) {
      if (!userFileNames.contains(sraFilename)) {
        messages.add("File name in the SRA section is not present in the files selected by the user: '" + sraFilename + "'");
      }
    }

    for (String userFileName : userFileNames) {
      if (!sraFilenames.contains(userFileName)) {
        messages.add("File name selected by the user is not present in the SRA section: '" + userFileName + "'");
      }
    }

    /** Return validation messages **/
    validationResponse.setMessages(messages);

    if (messages.size() == 0) {
      validationResponse.setIsValid(true);
    } else {
      validationResponse.setIsValid(false);
    }

    return validationResponse;
  }

}
