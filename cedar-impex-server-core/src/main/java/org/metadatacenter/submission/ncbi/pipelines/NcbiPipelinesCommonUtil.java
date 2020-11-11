package org.metadatacenter.submission.ncbi.pipelines;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

import static org.metadatacenter.submission.ncbi.NcbiConstants.ID_FIELD;
import static org.metadatacenter.submission.ncbi.NcbiConstants.VALUE_FIELD;

public class NcbiPipelinesCommonUtil {

  public static Optional<String> getTemplateFieldValue(JsonNode node, String fieldName) {

    if (node.has(fieldName)) {
      JsonNode fieldNode = node.get(fieldName);
      if (fieldNode.hasNonNull(VALUE_FIELD) && !fieldNode.get(VALUE_FIELD).asText().isEmpty()) {
        return Optional.of(fieldNode.get(VALUE_FIELD).asText());
      } else if (fieldNode.hasNonNull(ID_FIELD) && !fieldNode.get(ID_FIELD).asText().isEmpty()) {
        return Optional.of(fieldNode.get(ID_FIELD).asText());
      } else {
        return Optional.empty();
      }
    } else {

      return Optional.empty();

    }
  }

  public static boolean isValidField(JsonNode node, String fieldName, boolean requiredField, boolean requiredFieldValue) {
    if (node.has(fieldName)) {
      JsonNode fieldNode = node.get(fieldName);
      if (!fieldNode.isArray()) { // it is not an array node
        if (fieldNode.hasNonNull(VALUE_FIELD) && !fieldNode.get(VALUE_FIELD).asText().isEmpty()) {
          return true;
        } else if (fieldNode.hasNonNull(ID_FIELD) && !fieldNode.get(ID_FIELD).asText().isEmpty()) {
          return true;
        } else {
          if (requiredFieldValue) {
            return false;
          } else {
            return true;
          }
        }
      }
      else { // it is an array node
        if (fieldNode.size() > 0) {
          return true;
        }
        else {
          return false;
        }
      }
    } // if the field is not there
    else {
      if (requiredField) {
        return false;
      } else {
        return true;
      }
    }
  }

  public static boolean isValidBioprojectField(JsonNode bioproject, String fieldName, List<String> bioprojectRequiredFieldValues) {
    boolean requiredFieldValue = false;
    if (bioprojectRequiredFieldValues.contains(fieldName)) {
      requiredFieldValue = true;
    }
    return isValidField(bioproject, fieldName, true, requiredFieldValue);
  }

  public static boolean isValidBiosampleField(JsonNode biosample, String fieldName, List<String> biosampleRequiredFieldValues) {
    boolean requiredFieldValue = false;
    if (biosampleRequiredFieldValues.contains(fieldName)) {
      requiredFieldValue = true;
    }
    return isValidField(biosample, fieldName, true, requiredFieldValue);
  }

  public static boolean isValidSraField(JsonNode sra, String fieldName, List<String> sraRequiredFieldValues) {
    boolean requiredFieldValue = false;
    if (sraRequiredFieldValues.contains(fieldName)) {
      requiredFieldValue = true;
    }
    return isValidField(sra, fieldName, true, requiredFieldValue);
  }

  public static Optional<JsonNode> getTemplateElementNode(JsonNode node, String elementName) {
    if (node.has(elementName)) {
      if (node.hasNonNull(elementName) && node.get(elementName).size() > 0) {
        return Optional.of(node.get(elementName));
      } else {
        return Optional.empty();
      }
    } else {
      return Optional.empty();
    }
  }

  public static List<String> extractSraFileNames(JsonNode instance, String sraElement, String sraFieldNameField) {

    JsonNode sras = NcbiPipelinesCommonUtil.getTemplateElementNode(instance, sraElement).get();
    List<String> sraFileNames = new ArrayList();

    for (JsonNode sra : sras) {

      if (sra.hasNonNull(sraFieldNameField) && sra.get(sraFieldNameField).size() > 0) {
        Iterator<JsonNode> fileNameFieldsIt = sra.get(sraFieldNameField).iterator();

        while (fileNameFieldsIt.hasNext()) {
          String fileNameField = fileNameFieldsIt.next().asText();
          Optional<String> fileName = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, fileNameField);
          sraFileNames.add(fileName.get());
        }

      } else {
        return new ArrayList<>();
      }
    }
    return sraFileNames;
  }

}
