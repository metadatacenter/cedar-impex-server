package org.metadatacenter.submission.ncbi.pipelines.generic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NcbiGenericConstants {

  /*** Information directly inserted into the submission.xml file ***/
  // Submission
  protected static final String CEDAR_NAMESPACE = "CEDAR";
  protected static final String SUBMISSION_DESCRIPTOR = "CEDAR-NCBI Submission";
  // Biosample section
  protected static final String BIOSAMPLE_SCHEMA_VERSION = "2.0";
  protected static final String BIOSAMPLE_SUBMISSION_DESCRIPTOR = SUBMISSION_DESCRIPTOR;
  protected static final String BIOSAMPLE_SUBMISSION_TOOL = "CEDAR";
  protected static final String BIOSAMPLE_ORGANISM = "Homo sapiens";
  protected static final String BIOSAMPLE_PACKAGE = "Human.1.0";

  /*** Submission general information ***/
  protected static final String SUBMISSION_RELEASE_DATE_FIELD = "Release Date";

  /*** BioProject section ***/
  protected static final String BIOPROJECT_ELEMENT = "NCBI BioProject for Human Tissue";
  protected static final String BIOPROJECT_STUDY_ID_FIELD = "Study ID";
  protected static final String BIOPROJECT_STUDY_TITLE_FIELD = "Study Title";
  protected static final String BIOPROJECT_STUDY_TYPE_FIELD = "Study Type";
  protected static final String BIOPROJECT_STUDY_CRITERIA_FIELD = "Study Criteria";
  protected static final String BIOPROJECT_CONTACT_INFO_FIELD = "Contact Information";
  protected static final String BIOPROJECT_CONTACT_EMAIL_FIELD = "Contact Email";
  protected static final String BIOPROJECT_LAB_NAME_FIELD = "Lab Name";
  protected static final String BIOPROJECT_LAB_ADDRESS_FIELD = "Lab Address";
  protected static final String BIOPROJECT_RELEVANT_PUBLICATIONS_FIELD = "Relevant Publications";

  protected static final List<String> BIOPROJECT_FIELDS =
      Collections.unmodifiableList(Arrays.asList(
          BIOPROJECT_STUDY_ID_FIELD,
          BIOPROJECT_CONTACT_INFO_FIELD,
          BIOPROJECT_CONTACT_EMAIL_FIELD,
          BIOPROJECT_LAB_NAME_FIELD
      ));

  protected static final List<String> BIOPROJECT_REQUIRED_FIELD_VALUES =
      Collections.unmodifiableList(Arrays.asList(
          BIOPROJECT_STUDY_ID_FIELD,
          BIOPROJECT_CONTACT_INFO_FIELD,
          BIOPROJECT_CONTACT_EMAIL_FIELD,
          BIOPROJECT_LAB_NAME_FIELD
      ));


  /*** BioSample section ***/
  // Instance fields
  protected static final String BIOSAMPLE_ELEMENT = "NCBI BioSample for Human Tissue";
  protected static final String BIOSAMPLE_SAMPLE_NAME_FIELD = "Sample Name";
  protected static final String BIOSAMPLE_SAMPLE_TITLE_FIELD = "Sample Title";
  protected static final String BIOSAMPLE_ORGANISM_FIELD = "Organism";
  protected static final String BIOSAMPLE_ISOLATE_FIELD = "Isolate";
  protected static final String BIOSAMPLE_AGE_FIELD = "Age";
  protected static final String BIOSAMPLE_BIOMATERIAL_PROVIDER_FIELD = "Biomaterial Provider";
  protected static final String BIOSAMPLE_SEX_FIELD = "Sex";
  protected static final String BIOSAMPLE_TISSUE_FIELD = "Tissue";
  protected static final String BIOSAMPLE_CELL_LINE_FIELD = "Cell Line";
  protected static final String BIOSAMPLE_CELL_TYPE_FIELD = "Cell Type";
  protected static final String BIOSAMPLE_CELL_SUBTYPE_FIELD = "Cell Subtype";
  protected static final String BIOSAMPLE_CULTURE_COLLECTION_FIELD = "Culture Collection";
  protected static final String BIOSAMPLE_DEVELOPMENT_STAGE_FIELD = "Development Stage";
  protected static final String BIOSAMPLE_DISEASE_FIELD = "Disease";
  protected static final String BIOSAMPLE_DISEASE_STAGE_FIELD = "Disease Stage";
  protected static final String BIOSAMPLE_ETHNICITY_FIELD = "Ethnicity";
  protected static final String BIOSAMPLE_HEALTH_STATE_FIELD = "Health State";
  protected static final String BIOSAMPLE_KARYOTYPE_FIELD = "Karyotype";
  protected static final String BIOSAMPLE_PHENOTYPE_FIELD = "Phenotype";
  protected static final String BIOSAMPLE_POPULATION_FIELD = "Population";
  protected static final String BIOSAMPLE_RACE_FIELD = "Race";
  protected static final String BIOSAMPLE_SAMPLE_TYPE_FIELD = "Sample Type";
  protected static final String BIOSAMPLE_TREATMENT_FIELD = "Treatment";
  protected static final String BIOSAMPLE_DESCRIPTION_FIELD = "Sample Description";

  protected static final List<String> BIOSAMPLE_FIELDS =
      Collections.unmodifiableList(Arrays.asList(
          BIOSAMPLE_SAMPLE_NAME_FIELD,
          BIOSAMPLE_SAMPLE_TITLE_FIELD,
          BIOSAMPLE_ORGANISM_FIELD,
          BIOSAMPLE_ISOLATE_FIELD,
          BIOSAMPLE_AGE_FIELD,
          BIOSAMPLE_BIOMATERIAL_PROVIDER_FIELD,
          BIOSAMPLE_SEX_FIELD,
          BIOSAMPLE_TISSUE_FIELD,
          BIOSAMPLE_CELL_LINE_FIELD,
          BIOSAMPLE_CELL_TYPE_FIELD,
          BIOSAMPLE_CELL_SUBTYPE_FIELD,
          BIOSAMPLE_CULTURE_COLLECTION_FIELD,
          BIOSAMPLE_DEVELOPMENT_STAGE_FIELD,
          BIOSAMPLE_DISEASE_FIELD,
          BIOSAMPLE_DISEASE_STAGE_FIELD,
          BIOSAMPLE_ETHNICITY_FIELD,
          BIOSAMPLE_HEALTH_STATE_FIELD,
          BIOSAMPLE_KARYOTYPE_FIELD,
          BIOSAMPLE_PHENOTYPE_FIELD,
          BIOSAMPLE_POPULATION_FIELD,
          BIOSAMPLE_RACE_FIELD,
          BIOSAMPLE_SAMPLE_TYPE_FIELD,
          BIOSAMPLE_TREATMENT_FIELD,
          BIOSAMPLE_DESCRIPTION_FIELD
      ));

  protected static final List<String> BIOSAMPLE_REQUIRED_FIELD_VALUES =
      Collections.unmodifiableList(Arrays.asList(
          BIOSAMPLE_SAMPLE_NAME_FIELD,
          BIOSAMPLE_ORGANISM_FIELD,
          BIOSAMPLE_ISOLATE_FIELD,
          BIOSAMPLE_AGE_FIELD,
          BIOSAMPLE_BIOMATERIAL_PROVIDER_FIELD,
          BIOSAMPLE_SEX_FIELD,
          BIOSAMPLE_TISSUE_FIELD
      ));

  /*** SRA section ***/
  protected static final String SRA_ELEMENT = "NCBI SRA for Human Tissue";
  protected static final String SRA_SAMPLE_NAME_FIELD = "Sample Name";
  protected static final String SRA_LIBRARY_ID_FIELD = "Library ID";
  protected static final String SRA_TITLE_FIELD = "Title";
  protected static final String SRA_LIBRARY_STRATEGY_FIELD = "Library Strategy";
  protected static final String SRA_LIBRARY_SOURCE_FIELD = "Library Source";
  protected static final String SRA_LIBRARY_SELECTION_FIELD = "Library Selection";
  protected static final String SRA_LIBRARY_LAYOUT_FIELD = "Library Layout";
  protected static final String SRA_PLATFORM_FIELD = "Platform";
  protected static final String SRA_INSTRUMENT_MODEL_FIELD = "Instrument Model";
  protected static final String SRA_DESIGN_DESCRIPTION_FIELD = "Design Description";
  protected static final String SRA_FILE_TYPE_FIELD = "File Type";
  protected static final String SRA_FILE_NAME_FIELD = "File Name";

  protected static final List<String> SRA_FIELDS =
      Collections.unmodifiableList(Arrays.asList(
          SRA_SAMPLE_NAME_FIELD,
          SRA_LIBRARY_ID_FIELD,
          SRA_TITLE_FIELD,
          SRA_LIBRARY_STRATEGY_FIELD,
          SRA_LIBRARY_SOURCE_FIELD,
          SRA_LIBRARY_SELECTION_FIELD,
          SRA_LIBRARY_LAYOUT_FIELD,
          SRA_PLATFORM_FIELD,
          SRA_INSTRUMENT_MODEL_FIELD,
          SRA_DESIGN_DESCRIPTION_FIELD,
          SRA_FILE_TYPE_FIELD,
          SRA_FILE_NAME_FIELD
      ));

  protected static final List<String> SRA_REQUIRED_FIELD_VALUES =
      Collections.unmodifiableList(Arrays.asList(
          SRA_SAMPLE_NAME_FIELD,
          SRA_LIBRARY_ID_FIELD,
          SRA_TITLE_FIELD,
          SRA_LIBRARY_STRATEGY_FIELD,
          SRA_LIBRARY_SOURCE_FIELD,
          SRA_LIBRARY_SELECTION_FIELD,
          SRA_LIBRARY_LAYOUT_FIELD,
          SRA_PLATFORM_FIELD,
          SRA_INSTRUMENT_MODEL_FIELD,
          SRA_DESIGN_DESCRIPTION_FIELD,
          SRA_FILE_TYPE_FIELD,
          SRA_FILE_NAME_FIELD
      ));

  // PRIVATE //

  /**
   * The caller references the constants using Constants.EMPTY_STRING,
   * and so on. Thus, the caller should be prevented from constructing objects of
   * this class, by declaring this private constructor.
   */
  private NcbiGenericConstants() {
    // This restricts instantiation
    throw new AssertionError();
  }

}
