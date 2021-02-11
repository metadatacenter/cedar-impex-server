package org.metadatacenter.impex.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.metadatacenter.cadsr.form.schema.Form;
import org.metadatacenter.cadsr.ingestor.form.FormParseResult;
import org.metadatacenter.cadsr.ingestor.form.FormUtil;
import org.metadatacenter.cadsr.ingestor.util.CedarServerUtil;
import org.metadatacenter.cadsr.ingestor.util.CedarServices;
import org.metadatacenter.cadsr.ingestor.util.Constants;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.impex.exception.UploadInstanceNotFoundException;
import org.metadatacenter.impex.imp.cadsr.CadsrImportStatus;
import org.metadatacenter.impex.imp.cadsr.CadsrImportStatusManager;
import org.metadatacenter.impex.imp.cadsr.CadsrImportStatusManager.ImportStatus;
import org.metadatacenter.impex.upload.FlowData;
import org.metadatacenter.impex.upload.FlowUploadUtil;
import org.metadatacenter.impex.upload.UploadManager;
import org.metadatacenter.impex.util.ImpexUtil;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.util.http.CedarResponse;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.IOException;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
public class ImpexServerResource extends CedarMicroserviceResource {

  final static Logger logger = LoggerFactory.getLogger(ImpexServerResource.class);

  public ImpexServerResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  /**
   * Used to upload and import caDSR forms into CEDAR. The import is executed asynchronously in an independent thread.
   * The client doesn't need to wait for the import task to be completed but it's responsible for using the
   * import-cadsr-forms-status endpoint to check the import status.
   * <p>
   * If folder Id is not provided, the imported forms will be saved into the user's home folder
   *
   * @return
   * @throws CedarException
   */
  @POST
  @Timed
  @Path("/import-cadsr-forms")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response importCadsrForm(@QueryParam("folderId") String folderId) throws CedarException {

    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    // Check that it's a file upload request
    if (ServletFileUpload.isMultipartContent(request)) {

      try {
        String userId = c.getCedarUser().getId();
        // Extract data from the request
        FlowData data = FlowUploadUtil.getFlowData(request);
        // Every request contains a file chunk that we will save to the appropriate position of a local file
        String submissionLocalFolderPath = FlowUploadUtil
            .getUploadLocalFolderPath("impex-upload", userId, data.getUploadId());
        FlowUploadUtil.saveToLocalFile(data, userId, request.getContentLength(), submissionLocalFolderPath);

        // Update the submission upload status
        UploadManager.getInstance().updateStatus(data, submissionLocalFolderPath);

        // When the upload is complete, trigger the import process
        if (UploadManager.getInstance().isUploadComplete(data.getUploadId())
            && !CadsrImportStatusManager.getInstance().exists(data.getUploadId())) {

          logger.info("File(s) successfully uploaded to the Impex server: ");
          logger.info("  - Upload id: " + data.getUploadId());
          logger.info("  - Local path: " + submissionLocalFolderPath);
          logger.info("  - No. files: " + data.getTotalFilesCount());
          logger.info("  - Uploaded file names: ");
          for (String fileName : UploadManager.getInstance().getUploadFileNames(data.getUploadId())) {
            logger.info("    - " + fileName);
          }

          // Import files into CEDAR asynchronously
          new Thread(() -> {
            String fileName = null;
            try {
              String cedarFolderId = folderId != null ? folderId : c.getCedarUser().getHomeFolderId();
              // Set import status to 'PENDING' for all the files that are part of the upload
              CadsrImportStatusManager.getInstance().initImportStatus(data.getUploadId(), cedarFolderId);

              for (String formFilePath : UploadManager.getInstance().getUploadFilePaths(data.getUploadId())) {

                try {
                  // Set status to IN_PROGRESS
                  fileName = ImpexUtil.getFileNameFromFilePath(formFilePath);
                  CadsrImportStatusManager.getInstance().setStatus(data.getUploadId(), fileName,
                      ImportStatus.IN_PROGRESS);
                  logger.info("Importing file: " + formFilePath);
                  // Translate form to CEDAR template
                  Form form = FormUtil.getForm(new FileInputStream(formFilePath));
                  FormParseResult parseResult = FormUtil.getTemplateMapFromForm(form,
                      data.getUploadId() + "_" + fileName);
                  //logger.info(JsonMapper.MAPPER.writeValueAsString(parseResult.getTemplateMap()));
                  CadsrImportStatusManager.getInstance().writeReportMessages(data.uploadId, fileName,
                      parseResult.getReportMessages());
                  // Upload template to CEDAR
                  Constants.CedarServer cedarServer = CedarServerUtil.toCedarServerFromHostName(cedarConfig.getHost());
                  String apiKey = c.getCedarUser().getFirstActiveApiKey();
                  CedarServices.createTemplate(parseResult.getTemplateMap(), cedarFolderId, cedarServer, apiKey);
                  // Set status to COMPLETE
                  CadsrImportStatusManager.getInstance().setStatus(data.getUploadId(), fileName, ImportStatus.COMPLETE);
                } catch (JAXBException e) {
                  CadsrImportStatusManager.getInstance().writeReportMessage(data.uploadId, fileName, "Parsing error. " +
                      "Please check that the input file contains a valid XML caDSR form");
                  logger.error("Error parsing input file");
                  CadsrImportStatusManager.getInstance().setStatus(data.getUploadId(), fileName, ImportStatus.ERROR);
                } catch (IOException | RuntimeException e) {
                  CadsrImportStatusManager.getInstance().writeReportMessage(data.uploadId, fileName, e.getMessage());
                  logger.error(e.getMessage());
                  CadsrImportStatusManager.getInstance().setStatus(data.getUploadId(), fileName, ImportStatus.ERROR);
                }
              }
              // Remove the upload from the status map
              UploadManager.getInstance().removeUploadStatus(data.getUploadId());

            } catch (UploadInstanceNotFoundException e) {
              logger.error(e.getMessage());
            }
          }).start();
        }
      } catch (FileUploadException e) {
        return CedarResponse.internalServerError()
            .errorMessage("Error uploading file")
            .exception(e).build();
      } catch (IllegalAccessException e) {
        return CedarResponse.internalServerError()
            .exception(e).build();
      } catch (UploadInstanceNotFoundException e) {
        return CedarResponse.internalServerError()
            .errorMessage("Upload Id not found")
            .exception(e).build();
      } catch (IOException e) {
        return CedarResponse.internalServerError()
            .exception(e).build();
      }
      return Response.ok().build();
    } else {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  /* In production I may want to this method instead of the method below (it doesn't expose all uploads) */
//  @GET
//  @Timed
//  @Path("/import-cadsr-forms-status")
//  public Response importStatus(@QueryParam("uploadId") @NotEmpty String uploadId) throws CedarException {
//
//    CedarRequestContext c = buildRequestContext();
//    c.must(c.user()).be(LoggedIn);
//
//    try {
//      if (!CadsrImportStatusManager.getInstance().exists(uploadId)) {
//        return CedarResponse.notFound().errorMessage("The specified uploadId cannot be found").id(uploadId).build();
//      } else {
//        CadsrImportStatus status = CadsrImportStatusManager.getInstance().getStatus(uploadId);
//        JsonNode output = JsonMapper.MAPPER.valueToTree(status);
//        return Response.ok().entity(output).build();
//      }
//    } catch (Exception e) {
//      return CedarResponse.internalServerError().exception(e).build();
//    }
//  }

  @GET
  @Timed
  @Path("/import-cadsr-forms-status")
  public Response importStatus(@QueryParam("uploadId") String uploadId) throws CedarException {

    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    try {
      if (uploadId != null) {
        if (!CadsrImportStatusManager.getInstance().exists(uploadId)) {
          return CedarResponse.notFound().errorMessage("The specified uploadId cannot be found").id(uploadId).build();
        } else {
          CadsrImportStatus status = CadsrImportStatusManager.getInstance().getStatus(uploadId);
          JsonNode output = JsonMapper.MAPPER.valueToTree(status);
          return Response.ok().entity(output).build();
        }
      } else {
        JsonNode output = JsonMapper.MAPPER.valueToTree(CadsrImportStatusManager.getInstance().getAllStatuses());
        return Response.ok().entity(output).build();
      }
    } catch (Exception e) {
      return CedarResponse.internalServerError().exception(e).build();
    }
  }
}

