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
import org.metadatacenter.submission.ncbi.pipelines.cairr.NcbiCairrSubmissionXMLFileGenerator;
import org.metadatacenter.submission.ncbi.pipelines.cairr.NcbiCairrTemplateInstance2XMLConverter;
import org.metadatacenter.submission.ncbi.pipelines.cairr.NcbiCairrValidator;
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

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

/**
 * See here for submission instructions to NCBI:
 * <p>
 * https://docs.google.com/document/d/1tmPinCgaTwBkTsOwjitquFc0ZUN65w5xZs30q5phRkY/edit
 */
@Path("/command") @Produces(MediaType.APPLICATION_JSON) public class NcbiCairrSubmissionServerResource
  extends CedarMicroserviceResource
{

  final static Logger logger = LoggerFactory.getLogger(NcbiCairrSubmissionServerResource.class);

  private static NcbiSubmissionQueueService ncbiSubmissionQueueService;

  private final BioSampleValidator bioSampleValidator;
  private final NcbiCairrTemplateInstance2XMLConverter ncbiCairrTemplate2SRAXMLConverter;
  private final NcbiCairrSubmissionXMLFileGenerator ncbiCairrSubmissionXMLFileGenerator;
  private final NcbiCairrValidator ncbiCairrValidator;

  public NcbiCairrSubmissionServerResource(CedarConfig cedarConfig)
  {
    super(cedarConfig);
    this.bioSampleValidator = new BioSampleValidator();
    this.ncbiCairrTemplate2SRAXMLConverter = new NcbiCairrTemplateInstance2XMLConverter();
    this.ncbiCairrSubmissionXMLFileGenerator = new NcbiCairrSubmissionXMLFileGenerator();
    this.ncbiCairrValidator = new NcbiCairrValidator();
  }

  public static void injectServices(NcbiSubmissionQueueService ncbiSubmissionQueueService)
  {
    NcbiCairrSubmissionServerResource.ncbiSubmissionQueueService = ncbiSubmissionQueueService;
  }

  @POST @Timed @Path("/validate-cairr")
  public Response validate() throws CedarException {

    final String instanceField = "instance";
    final String userFileNamesField = "userFileNames";

    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    JsonNode requestBody = c.request().getRequestBody().asJson();

    if ((requestBody.hasNonNull(instanceField) && requestBody.size() == 1) ||
        (requestBody.hasNonNull(instanceField) && requestBody.hasNonNull(userFileNamesField) && requestBody.size() == 2)) {

      JsonNode instance = requestBody.get(instanceField);

      // Define responses
      CEDARValidationResponse cedarInstanceValidationResponse = null;
      CEDARValidationResponse cedarFileNamesValidationResponse = null;
      CEDARValidationResponse fileNamesValidationResponse = null;

      // 1. CEDAR instance validation
      cedarInstanceValidationResponse = this.ncbiCairrValidator.validateInstance(instance);

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
              this.ncbiCairrValidator.validateFilenames(instance, fileNames);
        }
      }

      // If 1 and 2 went well, we invoke NCBI's external validation
      if (cedarInstanceValidationResponse.getIsValid() && (fileNamesValidationResponse == null || fileNamesValidationResponse.getIsValid())) {
        String submissionXML = null;
        try {
          submissionXML = this.ncbiCairrTemplate2SRAXMLConverter.convertTemplateInstanceToXML(instance);
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
  @POST @Timed @Path("/upload-cairr-to-cedar") @Consumes(MediaType.MULTIPART_FORM_DATA) public Response uploadCAIRRToCEDAR()
    throws CedarException
  {

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
          logger.info("Submission successfully uploaded to CEDAR: ");
          logger.info("  submission id: " + data.getSubmissionId());
          logger.info("  submission local folder: " + submissionLocalFolderPath);
          logger.info("  no. files: " + data.getTotalFilesCount());
          // Submit the files to the NCBI
          String ncbiFolderName = FlowUploadUtil.getDateBasedFolderName(DateTimeZone.UTC);
          logger.info("Starting submission from CEDAR to the NCBI. Destination folder: " + ncbiFolderName);

          // Generate the submission object. The submission.xml file is generated by this method
          NcbiSubmission ncbiSubmission = NcbiSubmissionUtil
            .generateSubmission(data.getSubmissionId(), userId, ncbiFolderName,
              this.ncbiCairrSubmissionXMLFileGenerator);

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

