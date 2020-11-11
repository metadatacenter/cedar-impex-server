package org.metadatacenter.submission.immport;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.ImmPortConfig;
import org.metadatacenter.submission.ImmPortGetTokenResponse;
import org.metadatacenter.submission.status.SubmissionState;
import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.metadatacenter.constant.HttpConstants.*;
import static org.metadatacenter.util.json.JsonMapper.MAPPER;

public class ImmPortUtil {

  final static Logger logger = LoggerFactory.getLogger(ImmPortUtil.class);
  private final String workspaceUrl;
  private final String submissionUrl;
  private final String tokenUrl;
  private final String userName;
  private final String password;
  private final String statusUrl;

  public ImmPortUtil(CedarConfig cedarConfig) {
    ImmPortConfig immPortConfig = cedarConfig.getSubmissionConfig().getImmPort();
    userName = immPortConfig.getAuthentication().getUser();
    password = immPortConfig.getAuthentication().getPassword();
    workspaceUrl = immPortConfig.getWorkspaceEndpoint().getUrl() + "/" + userName + "/workspaces";
    submissionUrl = cedarConfig.getSubmissionConfig().getImmPort().getSubmissionEndpoint().getUrl();
    tokenUrl = cedarConfig.getSubmissionConfig().getImmPort().getTokenEndpoint().getUrl();
    statusUrl = cedarConfig.getSubmissionConfig().getImmPort().getStatusEndpoint().getUrl();
  }

  public SubmissionStatus getImmPortSubmissionStatus(String submissionID) {
    CloseableHttpResponse response = null;
    CloseableHttpClient client = null;

    Optional<String> token = getImmPortBearerToken();
    if (!token.isPresent()) {
      logger.warn("Could not get an ImmPort token");
      return new SubmissionStatus(submissionID, SubmissionState.ERROR, "Could not get an ImmPort token");
    }

    try {
      String immPortStatusURL = statusUrl + "/" + submissionID + "/report";
      HttpGet get = new HttpGet(immPortStatusURL);
      get.setHeader(HTTP_HEADER_AUTHORIZATION, HTTP_AUTH_HEADER_BEARER_PREFIX + token.get());
      get.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);
      client = HttpClientBuilder.create().build();
      response = client.execute(get);

      if (response.getStatusLine().getStatusCode() == 200) {
        HttpEntity entity = response.getEntity();
        return immPortSubmissionResponseBody2SubmissionStatus(submissionID, entity);
      } else {
        String errorMessage =
            "Unexpected status code calling " + immPortStatusURL + "; status=" + response.getStatusLine()
                .getStatusCode();
        logger.warn(errorMessage);
        return new SubmissionStatus(submissionID, SubmissionState.ERROR, errorMessage);
      }
    } catch (IOException e) {
      String errorMessage = "IO exception calling import status endpoint: " + e.getMessage();
      logger.warn(errorMessage);
      return new SubmissionStatus(submissionID, SubmissionState.ERROR, errorMessage);
    } finally {
      HttpClientUtils.closeQuietly(response);
      HttpClientUtils.closeQuietly(client);
    }
  }

  public Optional<String> getImmPortBearerToken() {
    CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpPost post = new HttpPost(tokenUrl);
    CloseableHttpResponse response = null;

    try {
      List<NameValuePair> parameters = new ArrayList<>(2);
      parameters.add(new BasicNameValuePair(ImmPortConstants.IMMPORT_USERNAME_FIELD, userName));
      parameters.add(new BasicNameValuePair(ImmPortConstants.IMMPORT_PASSWORD_FIELD, password));
      post.setEntity(new UrlEncodedFormEntity(parameters, "UTF-8"));
      post.setHeader(HTTP_HEADER_ACCEPT, CONTENT_TYPE_APPLICATION_JSON);

      response = client.execute(post);

      if (response.getStatusLine().getStatusCode() == Response.Status.OK.getStatusCode()) {
        HttpEntity entity = response.getEntity();
        // Get ImmPortGetTokenResponse from stream
        ImmPortGetTokenResponse immPortGetTokenResponse = MAPPER
            .readValue(entity.getContent(), ImmPortGetTokenResponse.class);

        if (immPortGetTokenResponse.getStatus().intValue() == 200) {
          return Optional.of(immPortGetTokenResponse.getToken());
        } else {
          logger.warn("Failed to get token from host " + submissionUrl + "; ImmPort status code="
              + immPortGetTokenResponse.getStatus().intValue() + ", error=" + immPortGetTokenResponse.getError());
          return Optional.empty();
        }
      } else {
        logger.warn(
            "Failed to get token from host " + submissionUrl + "; status code=" + response.getStatusLine()
                .getStatusCode());
        return Optional.empty();
      }
    } catch (IOException e) {
      logger.warn("IO error getting token from host " + submissionUrl + "; error=" + e.getMessage());
      return Optional.empty();
    } finally {
      HttpClientUtils.closeQuietly(response);
      HttpClientUtils.closeQuietly(client);
    }
  }

  static private SubmissionStatus immPortSubmissionResponseBody2SubmissionStatus(String submissionID,
                                                                                 HttpEntity responseEntity) throws
      IOException {
    if (responseEntity != null) {
      String responseBody = EntityUtils.toString(responseEntity);
      JsonNode immPortSubmissionResponseBody = MAPPER.readTree(responseBody);

      System.err.println("ImmPort response JSON " + immPortSubmissionResponseBody);

      if (immPortSubmissionResponseBody.has(ImmPortConstants.IMMPORT_RESPONSE_ERROR_FIELD)) {
        return new SubmissionStatus(submissionID, SubmissionState.ERROR,
            SubmissionStatusUtil.getShortStatusMessage(submissionID, SubmissionState.ERROR) +
                immPortSubmissionResponseBody.get(ImmPortConstants.IMMPORT_RESPONSE_ERROR_FIELD).textValue());
      } else {
        if (!immPortSubmissionResponseBody.has(ImmPortConstants.IMMPORT_RESPONSE_STATUS_FIELD)) {
          return new SubmissionStatus(submissionID, SubmissionState.ERROR,
              SubmissionStatusUtil.getShortStatusMessage(submissionID, SubmissionState.ERROR) +
                  "No " + ImmPortConstants.IMMPORT_RESPONSE_STATUS_FIELD + " field in ImmPort submit status response");
        }

        String immPortSubmissionStatus = immPortSubmissionResponseBody.get(ImmPortConstants
            .IMMPORT_RESPONSE_STATUS_FIELD).asText();

        if ("Completed".equals(immPortSubmissionStatus)) {
          if (!immPortSubmissionResponseBody.has(ImmPortConstants.IMMPORT_RESPONSE_STATUS_URL_FIELD)) {
            return new SubmissionStatus(submissionID, SubmissionState.SUCCEEDED,
                SubmissionStatusUtil.getShortStatusMessage(submissionID, SubmissionState.SUCCEEDED) +
                    getSubmissionReportFromResponse(immPortSubmissionResponseBody));
          } else {
            String immPortStatusURL = immPortSubmissionResponseBody.get(ImmPortConstants
                .IMMPORT_RESPONSE_STATUS_URL_FIELD).asText();
            return new SubmissionStatus(submissionID, SubmissionState.SUCCEEDED,
                SubmissionStatusUtil.getShortStatusMessage(submissionID, SubmissionState.SUCCEEDED) +
                    "Status URL = " + immPortStatusURL);
          }
        } else {

          Optional<SubmissionState> submissionState = immPortSubmissionStatus2SubmissionState(submissionID,
              immPortSubmissionStatus);

          if (submissionState.isPresent()) {
            return new SubmissionStatus(submissionID, submissionState.get(), SubmissionStatusUtil
                .getShortStatusMessage(submissionID,
                    submissionState.get()));
          } else {
            return new SubmissionStatus(submissionID, SubmissionState.ERROR,
                SubmissionStatusUtil.getShortStatusMessage(submissionID, SubmissionState.ERROR) +
                    "Unknown ImmPort submission status " + immPortSubmissionStatus);
          }
        }
      }
    } else {
      return new SubmissionStatus(submissionID, SubmissionState.ERROR, "No JSON in ImmPort status response");
    }
  }

  static private String getSubmissionReportFromResponse(JsonNode responseBody) {
    String report = "";
    if (responseBody.has(ImmPortConstants.IMMPORT_RESPONSE_REPORT_FIELD)) {
      report = responseBody.get(ImmPortConstants.IMMPORT_RESPONSE_REPORT_FIELD).textValue();
      report = report.replaceAll("\\r", "");
      report = report.replaceAll("\\n\\n", "\n");
      report = "\n" + report;
    }
    return report;
  }

  static private Optional<SubmissionState> immPortSubmissionStatus2SubmissionState(String submissionID,
                                                                                   String immPortSubmissionStatus) {
    if (ImmPortConstants.IMMPORT_SUBMIT_STATE_PENDING.equals(immPortSubmissionStatus)) {
      return Optional.of(SubmissionState.PROCESSING);
    } else if (ImmPortConstants.IMMPORT_SUBMIT_STATE_PROCESSING.equals(immPortSubmissionStatus)) {
      return Optional.of(SubmissionState.PROCESSING);
    } else if (ImmPortConstants.IMMPORT_SUBMIT_STATE_COMPLETED.equals(immPortSubmissionStatus)) {
      return Optional.of(SubmissionState.SUCCEEDED);
    } else if (ImmPortConstants.IMMPORT_SUBMIT_STATE_REJECTED.equals(immPortSubmissionStatus)) {
      return Optional.of(SubmissionState.REJECTED);
    } else if (ImmPortConstants.IMMPORT_SUBMIT_STATE_ERROR.equals(immPortSubmissionStatus)) {
      return Optional.of(SubmissionState.ERROR);
    } else {
      logger.warn("Unknown ImmPort status " + immPortSubmissionStatus + " returned for submission " + submissionID);
      return Optional.empty();
    }
  }

  public String getWorkspaceUrl() {
    return workspaceUrl;
  }
}
