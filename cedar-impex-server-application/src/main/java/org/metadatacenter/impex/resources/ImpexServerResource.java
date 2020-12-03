package org.metadatacenter.impex.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.hibernate.validator.constraints.NotEmpty;
import org.metadatacenter.cadsr.form.schema.Form;
import org.metadatacenter.cadsr.ingestor.form.FormUtil;
import org.metadatacenter.cadsr.ingestor.util.CedarServices;
import org.metadatacenter.cadsr.ingestor.util.Constants;
import org.metadatacenter.cadsr.ingestor.util.GeneralUtil;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.impex.exception.UploadInstanceNotFoundException;
import org.metadatacenter.impex.imp.cadsr.CadsrImportStatus;
import org.metadatacenter.impex.imp.cadsr.CadsrImportStatusManager;
import org.metadatacenter.impex.upload.FlowData;
import org.metadatacenter.impex.upload.FlowUploadUtil;
import org.metadatacenter.impex.upload.UploadManager;
import org.metadatacenter.rest.context.CedarRequestContext;
import org.metadatacenter.util.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
public class ImpexServerResource extends CedarMicroserviceResource {

  final static Logger logger = LoggerFactory.getLogger(ImpexServerResource.class);

  public ImpexServerResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  @POST
  @Timed
  @Path("/import-cadsr-forms")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response importCadsrForm() throws CedarException {

    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    // Check that this is a file upload request
    if (ServletFileUpload.isMultipartContent(request)) {

      try {
        String userId = c.getCedarUser().getId();
        // Extract data from the request
        FlowData data = FlowUploadUtil.getFlowData(request);
        // Every request contains a file chunk that we will save in the appropriate position of a local file
        // TODO: read base folder name from constants file
        String submissionLocalFolderPath = FlowUploadUtil
            .getUploadLocalFolderPath("impex-upload", userId, data.getUploadId());
        String filePath = FlowUploadUtil
            .saveToLocalFile(data, userId, request.getContentLength(), submissionLocalFolderPath);
        //logger.info("Saving file chunk to: " + filePath);

        // Update the submission upload status
        UploadManager.getInstance().updateStatus(data, submissionLocalFolderPath);

        // When the upload is complete, trigger the import process
        if (UploadManager.getInstance().isUploadComplete(data.getUploadId())) {
          logger.info("File(s) successfully uploaded to the Impex server: ");
          logger.info("  - Upload id: " + data.getUploadId());
          logger.info("  - Local path: " + submissionLocalFolderPath);
          logger.info("  - No. files: " + data.getTotalFilesCount());
          logger.info("  - Uploaded file names: ");
          for (String fileName : UploadManager.getInstance().getUploadFileNames(data.getUploadId())) {
            logger.info("    - " + fileName);
          }

          //--start import--
          String cedarFolderId = c.getCedarUser().getHomeFolderId();

          CadsrImportStatusManager.getInstance().addStatus(data.getUploadId(), cedarFolderId);

          // Import files into CEDAR
          for (String formFilePath : UploadManager.getInstance().getUploadFilePaths(data.getUploadId())) {
            logger.info("Importing file: " + formFilePath);
            Form form = FormUtil.getForm(new FileInputStream(formFilePath));
            Map templateMap = FormUtil.getTemplateMapFromForm(form);


            // TODO: cedarConfig.getHost instead of using CedarEnvironment

            String apiKey = c.getCedarUser().getFirstActiveApiKey();
            CedarServices.createTemplate(templateMap, cedarFolderId, Constants.CedarServer.LOCAL, apiKey);
            System.out.println(GeneralUtil.convertMapToJson(templateMap));
          }

          //--end import-

          // Remove the submission from the status map
          UploadManager.getInstance().removeUploadStatus(data.getUploadId());
        }

      } catch (IOException | FileUploadException /*SubmissionInstanceNotFoundException*/ e) {
        logger.error(e.getMessage(), e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (UploadInstanceNotFoundException e) {
        e.printStackTrace();
      } catch (JAXBException e) {
        e.printStackTrace();
      }
      return Response.ok().build();
    } else {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @GET
  @Timed
  @Path("/import-cadsr-forms-status")
  public Response importStatus(@QueryParam("uploadId") @NotEmpty String uploadId) throws CedarException {

    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    try {
      CadsrImportStatus status = CadsrImportStatusManager.getInstance().getStatus(uploadId);
      JsonNode output = JsonMapper.MAPPER.valueToTree(status);
      return Response.ok().entity(output).build();
    }
    catch (Exception e) { // TODO: refine exception
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

  }


}

