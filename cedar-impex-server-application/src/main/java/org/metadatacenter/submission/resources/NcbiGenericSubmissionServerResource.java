package org.metadatacenter.submission.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.joda.time.DateTimeZone;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.submission.CEDARValidationResponse;
import org.metadatacenter.submission.exception.SubmissionInstanceNotFoundException;
import org.metadatacenter.submission.ncbi.NcbiConstants;
import org.metadatacenter.submission.ncbi.NcbiSubmission;
import org.metadatacenter.submission.ncbi.NcbiSubmissionUtil;
import org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericSubmissionXMLFileGenerator;
import org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericTemplateInstance2XMLConverter;
import org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericValidator;
import org.metadatacenter.submission.ncbi.queue.NcbiSubmissionQueueService;
import org.metadatacenter.submission.ncbi.validation.BioSampleValidator;
import org.metadatacenter.submission.upload.flow.FlowData;
import org.metadatacenter.submission.upload.flow.FlowUploadUtil;
import org.metadatacenter.submission.upload.flow.SubmissionUploadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
public class NcbiGenericSubmissionServerResource
    extends CedarMicroserviceResource {

  final static Logger logger = LoggerFactory.getLogger(NcbiGenericSubmissionServerResource.class);

  private static NcbiSubmissionQueueService ncbiSubmissionQueueService;

  private final BioSampleValidator bioSampleValidator;
  private final NcbiGenericTemplateInstance2XMLConverter ncbiGenericTemplateInstance2XMLConverter;
  private final NcbiGenericSubmissionXMLFileGenerator ncbiGenericSubmissionXMLFileGenerator;
  private final NcbiGenericValidator ncbiGenericValidator;

  public NcbiGenericSubmissionServerResource(CedarConfig cedarConfig) {
    super(cedarConfig);
    this.bioSampleValidator = new BioSampleValidator();
    this.ncbiGenericTemplateInstance2XMLConverter = new NcbiGenericTemplateInstance2XMLConverter();
    this.ncbiGenericSubmissionXMLFileGenerator = new NcbiGenericSubmissionXMLFileGenerator();
    this.ncbiGenericValidator = new NcbiGenericValidator();
  }

  public static void injectServices(NcbiSubmissionQueueService ncbiSubmissionQueueService) {
    NcbiGenericSubmissionServerResource.ncbiSubmissionQueueService = ncbiSubmissionQueueService;
  }

  /**
   * The body must follow this structure:
   * {
   * "instance": {...}, // Template instance to be submitted (required)
   * "userFileNames": [...] // Array with the names of the data files to be submitted (optional). If this field is
   * provided, the validator will check its consistency against the instance
   * }
   *
   * @return
   * @throws CedarException
   */
  @POST
  @Timed
  @Path("/validate-ncbi")
  public Response validate() throws CedarException {
    final String instanceField = "instance";
    final String userFileNamesField = "userFileNames";

    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    JsonNode requestBody = c.request().getRequestBody().asJson();

    if ((requestBody.hasNonNull(instanceField) && requestBody.size() == 1) ||
        (requestBody.hasNonNull(instanceField) && requestBody.hasNonNull(userFileNamesField) && requestBody.size() == 2)) {

      JsonNode instance = requestBody.get(instanceField);

      // Define validation response objects
      CEDARValidationResponse cedarInstanceValidationResponse;
      CEDARValidationResponse cedarFileNamesValidationResponse = null;
      CEDARValidationResponse fileNamesValidationResponse = null;

      // 1. CEDAR instance validation
      cedarInstanceValidationResponse = this.ncbiGenericValidator.validateInstance(instance);

      // 2. CEDAR file names validation
      if (requestBody.hasNonNull(userFileNamesField)) {
        JsonNode userFileNames = requestBody.get(userFileNamesField);
        if (userFileNames.isArray()) {
          List fileNames = null;
          try {
            fileNames = new ObjectMapper().readValue(userFileNames.traverse(),
                new TypeReference<ArrayList<String>>() {
                });
          } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
          }
          fileNamesValidationResponse =
              this.ncbiGenericValidator.validateFilenames(instance, fileNames);
        }
      }

      // If 1 and 2 went well, we invoke NCBI's external validation
      if (cedarInstanceValidationResponse.getIsValid() && (fileNamesValidationResponse == null || fileNamesValidationResponse.getIsValid())) {
        String submissionXML = null;
        try {
          submissionXML = this.ncbiGenericTemplateInstance2XMLConverter.convertTemplateInstanceToXML(instance);
        } catch (JAXBException | DatatypeConfigurationException | ParseException e) {
          Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        cedarFileNamesValidationResponse = this.bioSampleValidator.validateBioSampleSubmission(submissionXML);
      }

      // If any of the above validation failed, return the corresponding validation messages
      CEDARValidationResponse validationResponse = new CEDARValidationResponse();
      List<String> validationMessages = new ArrayList<>();
      if (cedarInstanceValidationResponse != null && cedarInstanceValidationResponse.getMessages() != null) {
        validationMessages.addAll(cedarInstanceValidationResponse.getMessages());
      }
      if (fileNamesValidationResponse != null && fileNamesValidationResponse.getMessages() != null) {
        validationMessages.addAll(fileNamesValidationResponse.getMessages());
      }
      if (cedarFileNamesValidationResponse != null && cedarFileNamesValidationResponse.getMessages() != null) {
        validationMessages.addAll(cedarFileNamesValidationResponse.getMessages());
      }
      validationResponse.setMessages(validationMessages);

      if (validationResponse.getMessages().size() == 0) {
        validationResponse.setIsValid(true);
      } else {
        validationResponse.setIsValid(false);
      }
      return Response.ok(validationResponse).build();
    } else {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }


  /**
   * This endpoint receives multiple chunks of a submission package and assembles them. The submission may be
   * composed by one or multiple files. When the upload is complete, this method triggers the upload of all files that
   * are part of the submission to the NCBI via FTP. Submissions are processed sequentially using a queue.
   */
  @POST
  @Timed
  @Path("/upload-ncbi-to-cedar")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response uploadNCBIToCEDAR()
      throws CedarException {

    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    // Check that this is a file upload request
    if (ServletFileUpload.isMultipartContent(request)) {

      try {
        String userId = c.getCedarUser().getId();
        // Extract data from the request
        FlowData data = FlowUploadUtil.getFlowData(request);
        // The submission to the NCBI must contain one (and only one) metadata file (instance)
        if (data.getMetadataFiles().size() != 1) {
          String message =
              "Incorrect number of metadata files (submissionId = " + data.getSubmissionId() + "; metadataFiles = " + data
                  .getMetadataFiles().size();
          logger.info(message);
          return Response.status(Response.Status.BAD_REQUEST).build();
        }
        // Every request contains a file chunk that we will save in the appropriate position of a local file
        String submissionLocalFolderPath = FlowUploadUtil
            .getSubmissionLocalFolderPath(NcbiConstants.NCBI_LOCAL_FOLDER_NAME, userId, data.getSubmissionId());
        String filePath = FlowUploadUtil
            .saveToLocalFile(data, userId, request.getContentLength(), submissionLocalFolderPath);
        logger.info("File created. Path: " + filePath);
        // Update the submission upload status
        SubmissionUploadManager.getInstance().updateStatus(data, submissionLocalFolderPath);

        // If the submission upload is complete, trigger the FTP submission to the NCBI servers
        if (SubmissionUploadManager.getInstance().isSubmissionUploadComplete(data.getSubmissionId())) {
          logger.info("NCBI submission successfully uploaded to CEDAR: ");
          logger.info("  submission id: " + data.getSubmissionId());
          logger.info("  submission local folder: " + submissionLocalFolderPath);
          logger.info("  no. files: " + data.getTotalFilesCount());
          // Submit the files to the NCBI
          String ncbiFolderName = FlowUploadUtil.getDateBasedFolderName(DateTimeZone.UTC);
          logger.info("Starting submission from CEDAR to the NCBI. Destination folder: " + ncbiFolderName);

          // Generate the submission object. The submission.xml file is generated by this method
          NcbiSubmission ncbiSubmission = NcbiSubmissionUtil.generateSubmission(data.getSubmissionId(),
              userId, ncbiFolderName, this.ncbiGenericSubmissionXMLFileGenerator);

          // Enqueue submission
          logger.info("Enqueuing submission");
          ncbiSubmissionQueueService.enqueueSubmission(ncbiSubmission);
          // Remove the submission from the status map
          SubmissionUploadManager.getInstance().removeSubmissionStatus(data.getSubmissionId());
        }

      } catch (IOException | FileUploadException | SubmissionInstanceNotFoundException | JAXBException | DatatypeConfigurationException e) {
        logger.error(e.getMessage(), e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
      return Response.ok().build();
    } else {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }


}

