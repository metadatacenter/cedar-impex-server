package org.metadatacenter.impex.ncbi.pipelines;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public class NcbiPipelinesCommonValidator {

  public static List<String> validateBioproject(JsonNode bioproject, List<String> bioprojectFields, List<String> bioprojectRequiredFieldValues) {
    List<String> messages = new ArrayList<>();
    for (String fieldName : bioprojectFields) {
      if (!NcbiPipelinesCommonUtil.isValidBioprojectField(bioproject, fieldName, bioprojectRequiredFieldValues)) {
        messages.add(fieldName + " value must be supplied for BioProject");
      }
    }
    return messages;
  }

  public static List<String> validateBiosample(JsonNode biosamples, List<String> biosampleFields, List<String> biosampleRequiredFieldValues) {
    List<String> messages = new ArrayList<>();
    for (JsonNode biosample : biosamples) {
      for (String fieldName : biosampleFields) {
        if (!NcbiPipelinesCommonUtil.isValidBiosampleField(biosample, fieldName, biosampleRequiredFieldValues)) {
          messages.add(fieldName + " value must be supplied for BioSample");
        }
      }
    }
    return messages;
  }

  public static List<String> validateSra(JsonNode sras, List<String> sraFields, String sraFileNameField, List<String> sraRequiredFieldValues) {
    List<String> messages = new ArrayList<>();
    for (JsonNode sra : sras) {
      for (String fieldName : sraFields) {
        if (!NcbiPipelinesCommonUtil.isValidSraField(sra, fieldName, sraRequiredFieldValues)) {
          messages.add(fieldName + " value must be supplied for SRA");
        }
      }
      // File type and file names
      Iterator<JsonNode> fileNamesIt = sra.get(sraFileNameField).iterator();
      while (fileNamesIt.hasNext()) {
        String fileNameField = fileNamesIt.next().asText();
        Optional<String> fileName = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, fileNameField);
        if (!fileName.isPresent()) {
          messages.add("File name field not present: " + fileNameField);
        }
      }
    }
    return messages;
  }

  public static List<String> validateBiosampleSraRefs(JsonNode biosamples, JsonNode sras, String biosampleSampleIdField, String sraSampleIdField) {
    List<String> messages = new ArrayList<>();

    List<String> biosampleSampleNames = new ArrayList<>();
    for (JsonNode biosample : biosamples) {
      Optional<String> biosampleSampleName = NcbiPipelinesCommonUtil.getTemplateFieldValue(biosample, biosampleSampleIdField);
      if (biosampleSampleName.isPresent()) {
        biosampleSampleNames.add(biosampleSampleName.get());
      }
    }

    List<String> sraSampleNames = new ArrayList<>();
    for (JsonNode sra : sras) {
      Optional<String> sraSampleName = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, sraSampleIdField);
      if (sraSampleName.isPresent()) {
        sraSampleNames.add(sraSampleName.get());
      }
    }

    for (String biosampleSampleName : biosampleSampleNames) {
      if (!sraSampleNames.contains(biosampleSampleName)) {
        messages.add("Sample name in the BioSample section is not present in the SRA section: " + biosampleSampleName);
      }
    }

    for (String sraSampleName : sraSampleNames) {
      if (!biosampleSampleNames.contains(sraSampleName)) {
        messages.add("Sample name in the SRA section is not present in the BioSample section: " + sraSampleName);
      }
    }

    return messages;
  }

}
