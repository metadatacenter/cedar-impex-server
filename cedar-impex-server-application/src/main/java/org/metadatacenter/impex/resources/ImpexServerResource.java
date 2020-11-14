package org.metadatacenter.impex.resources;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.joda.time.DateTimeZone;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceResource;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.exception.CedarException;
import org.metadatacenter.impex.exception.SubmissionInstanceNotFoundException;
import org.metadatacenter.impex.ncbi.NcbiConstants;
import org.metadatacenter.impex.ncbi.NcbiSubmission;
import org.metadatacenter.impex.ncbi.NcbiSubmissionUtil;
import org.metadatacenter.impex.upload.flow.FlowData;
import org.metadatacenter.impex.upload.flow.FlowUploadUtil;
import org.metadatacenter.impex.upload.flow.SubmissionUploadManager;
import org.metadatacenter.rest.context.CedarRequestContext;
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

import static org.metadatacenter.rest.assertion.GenericAssertions.LoggedIn;

@Path("/command")
@Produces(MediaType.APPLICATION_JSON)
public class ImpexServerResource
    extends CedarMicroserviceResource {

  final static Logger logger = LoggerFactory.getLogger(ImpexServerResource.class);

  public ImpexServerResource(CedarConfig cedarConfig) {
    super(cedarConfig);
  }

  @POST
  @Timed
  @Path("/import-cadsr-forms")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Response importCadsrForm()
      throws CedarException {

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
            .getSubmissionLocalFolderPath("impex-upload", userId, data.getSubmissionId());
        String filePath = FlowUploadUtil
            .saveToLocalFile(data, userId, request.getContentLength(), submissionLocalFolderPath);
        logger.info("Saving file chunk to: " + filePath);
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

          // Remove the submission from the status map
          SubmissionUploadManager.getInstance().removeSubmissionStatus(data.getSubmissionId());
        }

      } catch (IOException | FileUploadException /*SubmissionInstanceNotFoundException*/ e) {

        logger.error(e.getMessage(), e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (SubmissionInstanceNotFoundException e) {
        e.printStackTrace();
      }
      return Response.ok().build();
    } else {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
  }

  @POST
  @Timed
  @Path("/import-cadsr-forms-status")
  public Response importStatus()
      throws CedarException {

    CedarRequestContext c = buildRequestContext();
    c.must(c.user()).be(LoggedIn);

    return Response.status(Response.Status.BAD_REQUEST).build();
  }


}

