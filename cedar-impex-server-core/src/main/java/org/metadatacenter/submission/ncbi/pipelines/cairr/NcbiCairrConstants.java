package org.metadatacenter.submission.ncbi.pipelines.cairr;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NcbiCairrConstants {

  /*** Information directly inserted into the submission.xml file ***/
  // Submission
  protected static final String CEDAR_NAMESPACE = "CEDAR";
  protected static final String SUBMISSION_DESCRIPTOR = "AIRR-CEDAR Submission";
  // Biosample section
  protected static final String BIOSAMPLE_SCHEMA_VERSION = "2.0";
  protected static final String BIOSAMPLE_SUBMISSION_DESCRIPTOR = SUBMISSION_DESCRIPTOR;
  protected static final String BIOSAMPLE_SUBMISSION_TOOL = "CEDAR";
  protected static final String BIOSAMPLE_ORGANISM = "Homo sapiens";
  protected static final String BIOSAMPLE_PACKAGE = "Human.1.0";

  /*** Submission general information ***/
  protected static final String SUBMISSION_RELEASE_DATE_FIELD = "Submissions Release Date";

  /*** BioProject section ***/
  protected static final String BIOPROJECT_ELEMENT = "BioProject for AIRR NCBI";
  protected static final String BIOPROJECT_STUDY_ID_FIELD = "Study ID";
  protected static final String BIOPROJECT_STUDY_TITLE_FIELD = "Study Title";
  protected static final String BIOPROJECT_STUDY_TYPE_FIELD = "Study Type";
  protected static final String BIOPROJECT_STUDY_CRITERIA_FIELD = "Study Criteria";
  protected static final String BIOPROJECT_FUNDING_AGENCY_FIELD = "Funding Agency";
  protected static final String BIOPROJECT_CONTACT_INFO_FIELD = "Contact Information (data collection)";
  protected static final String BIOPROJECT_LAB_NAME_FIELD = "Lab Name";
  protected static final String BIOPROJECT_LAB_ADDRESS_FIELD = "Lab Address";
  protected static final String BIOPROJECT_RELEVANT_PUBLICATIONS_FIELD = "Relevant Publications";


  protected static final List<String> BIOPROJECT_FIELDS =
      Collections.unmodifiableList(Arrays.asList(
          BIOPROJECT_STUDY_ID_FIELD,
          BIOPROJECT_CONTACT_INFO_FIELD,
          BIOPROJECT_LAB_NAME_FIELD
      ));


  protected static final List<String> BIOPROJECT_REQUIRED_FIELD_VALUES =
      Collections.unmodifiableList(Arrays.asList(
          BIOPROJECT_STUDY_ID_FIELD,
          BIOPROJECT_CONTACT_INFO_FIELD,
          BIOPROJECT_LAB_NAME_FIELD
      ));

  /*** BioSample section ***/
  // Subject
  protected static final String BIOSAMPLE_ELEMENT = "BioSample for AIRR NCBI";
  protected static final String BIOSAMPLE_SUBJECT_ID_FIELD = "Subject ID";
  protected static final String BIOSAMPLE_SYNTHETIC_LIBRARY_FIELD = "Synthetic Library";
  protected static final String BIOSAMPLE_ORGANISM_FIELD = "Organism";
  protected static final String BIOSAMPLE_SEX_FIELD = "Sex";
  protected static final String BIOSAMPLE_AGE_FIELD = "Age";
  protected static final String BIOSAMPLE_AGE_EVENT_FIELD = "Age Event";
  protected static final String BIOSAMPLE_ANCESTRY_POPULATION_FIELD = "Ancestry Population";
  protected static final String BIOSAMPLE_ETHNICITY_FIELD = "Ethnicity";
  protected static final String BIOSAMPLE_RACE_FIELD = "Race";
  protected static final String BIOSAMPLE_STRAIN_NAME_FIELD = "Strain Name";
  protected static final String BIOSAMPLE_RELATION_TO_OTHER_SUBJECTS_FIELD = "Relation to Other Subjects";
  protected static final String BIOSAMPLE_RELATION_TYPE_FIELD = "Relation Type";
  protected static final String BIOSAMPLE_ESTIMATED_RELEASE_DATE_FIELD = "Estimated Release Date";
  // Diagnosis
  protected static final String BIOSAMPLE_STUDY_GROUP_DESCRIPTION_FIELD = "Study Group Description";
  protected static final String BIOSAMPLE_DIAGNOSIS = "Diagnosis1";
  protected static final String BIOSAMPLE_LENGTH_OF_DISEASE = "Length of Disease";
  protected static final String BIOSAMPLE_DISEASE_STAGE_FIELD = "Disease Stage";
  protected static final String BIOSAMPLE_PRIOR_THERAPIES_FIELD = "Prior Therapies for Primary Disease under Study";
  protected static final String BIOSAMPLE_IMMUNOGEN_FIELD = "Immunogen";
  protected static final String BIOSAMPLE_INTERVENTION_DEFINITION_FIELD = "Intervention Definition";
  protected static final String BIOSAMPLE_OTHER_RELEVANT_MEDICAL_HISTORY_FIELD = "Other Relevant Medical History";
  // Biological Sample
  protected static final String BIOSAMPLE_SAMPLE_ID_FIELD = "Sample ID";
  protected static final String BIOSAMPLE_SAMPLE_TYPE_FIELD = "Sample Type";
  protected static final String BIOSAMPLE_TISSUE_FIELD = "Tissue";
  protected static final String BIOSAMPLE_ANATOMIC_SITE_FIELD = "Anatomic Site";
  protected static final String BIOSAMPLE_DISEASE_STATE_OF_SAMPLE_FIELD = "Disease State of Sample";
  protected static final String BIOSAMPLE_SAMPLE_COLLECTION_TIME_FIELD = "Sample Collection Time";
  protected static final String BIOSAMPLE_COLLECTION_TIME_EVENT_FIELD = "Collection Time Event";
  protected static final String BIOSAMPLE_BIOMATERIAL_PROVIDER_FIELD = "Biomaterial Provider";
  // Processing
  protected static final String BIOSAMPLE_TISSUE_PROCESSING_FIELD = "Tissue Processing";
  protected static final String BIOSAMPLE_CELL_SUBSET_FIELD = "Cell Subset";
  protected static final String BIOSAMPLE_CELL_SUBSET_PHENOTYPE_FIELD = "Cell Subset Phenotype";
  protected static final String BIOSAMPLE_SINGLE_CELL_SORT_FIELD = "Single-cell Sort";
  protected static final String BIOSAMPLE_NUMBER_OF_CELLS_IN_EXPERIMENT_FIELD = "Number of Cells in Experiment";
  protected static final String BIOSAMPLE_NUMBER_OF_CELLS_PER_SEQUENCING_REACTION_FIELD = "Number of Cells per " +
      "Sequencing Reaction";
  protected static final String BIOSAMPLE_CELL_STORAGE_FIELD = "Cell Storage";
  protected static final String BIOSAMPLE_CELL_QUALITY_FIELD = "Cell Quality";
  protected static final String BIOSAMPLE_CELL_ISOLATION_FIELD = "Cell Isolation";
  protected static final String BIOSAMPLE_PROCESSING_PROTOCOL_FIELD = "Processing Protocol";
  protected static final String BIOSAMPLE_OPTIONAL_ATTRIBUTE_FIELD = "Optional BioSample Attribute";

  protected static final List<String> BIOSAMPLE_FIELDS =
      Collections.unmodifiableList(Arrays.asList(
          BIOSAMPLE_SUBJECT_ID_FIELD,
          BIOSAMPLE_SYNTHETIC_LIBRARY_FIELD,
          BIOSAMPLE_ORGANISM_FIELD,
          BIOSAMPLE_SEX_FIELD,
          BIOSAMPLE_AGE_FIELD,
          BIOSAMPLE_AGE_EVENT_FIELD,
          BIOSAMPLE_ANCESTRY_POPULATION_FIELD,
          BIOSAMPLE_ETHNICITY_FIELD,
          BIOSAMPLE_RACE_FIELD,
          BIOSAMPLE_STRAIN_NAME_FIELD,
          BIOSAMPLE_RELATION_TO_OTHER_SUBJECTS_FIELD,
          BIOSAMPLE_RELATION_TYPE_FIELD,
          BIOSAMPLE_ESTIMATED_RELEASE_DATE_FIELD,
          BIOSAMPLE_STUDY_GROUP_DESCRIPTION_FIELD,
          BIOSAMPLE_DIAGNOSIS,
          BIOSAMPLE_LENGTH_OF_DISEASE,
          BIOSAMPLE_DISEASE_STAGE_FIELD,
          BIOSAMPLE_PRIOR_THERAPIES_FIELD,
          BIOSAMPLE_IMMUNOGEN_FIELD,
          BIOSAMPLE_INTERVENTION_DEFINITION_FIELD,
          BIOSAMPLE_OTHER_RELEVANT_MEDICAL_HISTORY_FIELD,
          BIOSAMPLE_SAMPLE_ID_FIELD,
          BIOSAMPLE_SAMPLE_TYPE_FIELD,
          BIOSAMPLE_TISSUE_FIELD,
          BIOSAMPLE_ANATOMIC_SITE_FIELD,
          BIOSAMPLE_DISEASE_STATE_OF_SAMPLE_FIELD,
          BIOSAMPLE_SAMPLE_COLLECTION_TIME_FIELD,
          BIOSAMPLE_COLLECTION_TIME_EVENT_FIELD,
          BIOSAMPLE_BIOMATERIAL_PROVIDER_FIELD,
          BIOSAMPLE_TISSUE_PROCESSING_FIELD,
          BIOSAMPLE_CELL_SUBSET_FIELD,
          BIOSAMPLE_CELL_SUBSET_PHENOTYPE_FIELD,
          BIOSAMPLE_SINGLE_CELL_SORT_FIELD,
          BIOSAMPLE_NUMBER_OF_CELLS_IN_EXPERIMENT_FIELD,
          BIOSAMPLE_NUMBER_OF_CELLS_PER_SEQUENCING_REACTION_FIELD,
          BIOSAMPLE_CELL_STORAGE_FIELD,
          BIOSAMPLE_CELL_QUALITY_FIELD,
          BIOSAMPLE_CELL_ISOLATION_FIELD,
          BIOSAMPLE_PROCESSING_PROTOCOL_FIELD,
          BIOSAMPLE_OPTIONAL_ATTRIBUTE_FIELD
      ));

  protected static final List<String> BIOSAMPLE_REQUIRED_FIELD_VALUES =
      Collections.unmodifiableList(Arrays.asList(
          BIOSAMPLE_SUBJECT_ID_FIELD,
          BIOSAMPLE_SYNTHETIC_LIBRARY_FIELD,
          BIOSAMPLE_ORGANISM_FIELD,
          BIOSAMPLE_SEX_FIELD,
          BIOSAMPLE_AGE_FIELD,
          BIOSAMPLE_AGE_EVENT_FIELD,
          BIOSAMPLE_ANCESTRY_POPULATION_FIELD,
          BIOSAMPLE_ETHNICITY_FIELD,
          BIOSAMPLE_RACE_FIELD,
          BIOSAMPLE_STRAIN_NAME_FIELD,
          BIOSAMPLE_RELATION_TO_OTHER_SUBJECTS_FIELD,
          BIOSAMPLE_RELATION_TYPE_FIELD,
          BIOSAMPLE_STUDY_GROUP_DESCRIPTION_FIELD,
          BIOSAMPLE_DIAGNOSIS,
          BIOSAMPLE_LENGTH_OF_DISEASE,
          BIOSAMPLE_DISEASE_STAGE_FIELD,
          BIOSAMPLE_PRIOR_THERAPIES_FIELD,
          BIOSAMPLE_IMMUNOGEN_FIELD,
          BIOSAMPLE_INTERVENTION_DEFINITION_FIELD,
          BIOSAMPLE_OTHER_RELEVANT_MEDICAL_HISTORY_FIELD,
          BIOSAMPLE_SAMPLE_ID_FIELD,
          BIOSAMPLE_SAMPLE_TYPE_FIELD,
          BIOSAMPLE_TISSUE_FIELD,
          BIOSAMPLE_ANATOMIC_SITE_FIELD,
          BIOSAMPLE_DISEASE_STATE_OF_SAMPLE_FIELD,
          BIOSAMPLE_SAMPLE_COLLECTION_TIME_FIELD,
          BIOSAMPLE_COLLECTION_TIME_EVENT_FIELD,
          BIOSAMPLE_BIOMATERIAL_PROVIDER_FIELD,
          BIOSAMPLE_TISSUE_PROCESSING_FIELD,
          BIOSAMPLE_CELL_SUBSET_FIELD,
          BIOSAMPLE_CELL_SUBSET_PHENOTYPE_FIELD,
          BIOSAMPLE_SINGLE_CELL_SORT_FIELD,
          BIOSAMPLE_NUMBER_OF_CELLS_IN_EXPERIMENT_FIELD,
          BIOSAMPLE_NUMBER_OF_CELLS_PER_SEQUENCING_REACTION_FIELD,
          BIOSAMPLE_CELL_STORAGE_FIELD,
          BIOSAMPLE_CELL_QUALITY_FIELD,
          BIOSAMPLE_CELL_ISOLATION_FIELD,
          BIOSAMPLE_PROCESSING_PROTOCOL_FIELD
      ));

  /*** SRA section ***/
  protected static final String SRA_ELEMENT = "Sequence Read Archive for AIRR NCBI";
  protected static final String SRA_SAMPLE_ID_FIELD = "Sample ID";
  protected static final String SRA_TARGET_SUBSTRATE_FIELD = "Target Substrate";
  protected static final String SRA_TARGET_SUBSTRATE_QUALITY_FIELD = "Target Substrate Quality";
  protected static final String SRA_TEMPLATE_AMOUNT_FIELD = "Template Amount";
  protected static final String SRA_NUCLEIC_ACID_PROCESSING_ID_FIELD = "Nucleic Acid Processing ID";
  protected static final String SRA_LIBRARY_STRATEGY_FIELD = "Library Strategy";
  protected static final String SRA_LIBRARY_SOURCE_FIELD = "Library Source";
  protected static final String SRA_LIBRARY_SELECTION_FIELD = "Library Selection";
  protected static final String SRA_LIBRARY_LAYOUT_FIELD = "Library Layout";
  protected static final String SRA_LIBRARY_GENERATION_METHOD_FIELD = "Library Generation Method";
  protected static final String SRA_LIBRARY_GENERATION_PROTOCOL_FIELD = "Library Generation Protocol";
  protected static final String SRA_PROTOCOL_IDS_FIELD = "Protocol IDs";
  protected static final String SRA_TARGET_LOCUS_FOR_PCR_FIELD = "Target Locus for PCR";
  protected static final String SRA_FORWARD_PCR_PRIMER_TARGET_LOCATION_FIELD = "Forward PCR Primer Target Location";
  protected static final String SRA_REVERSE_PCR_PRIMER_TARGET_LOCATION_FIELD = "Reverse PCR Primer Target Location";
  protected static final String SRA_COMPLETE_SEQUENCES_FIELD = "Complete Sequences";
  protected static final String SRA_PHYSICAL_LINKAGE_OF_DIFFERENT_LOCI_FIELD = "Physical Linkage of Different Loci";
  protected static final String SRA_TOTAL_READS_PASSING_QC_FILTER_FIELD = "Total Reads Passing QC Filter";
  protected static final String SRA_SEQUENCING_PLATFORM_FIELD = "Sequencing Platform";
  protected static final String SRA_READ_LENGTHS_FIELD = "Read Lengths";
  protected static final String SRA_SEQUENCING_FACILITY_FIELD = "Sequencing Facility";
  protected static final String SRA_BATCH_NUMBER_FIELD = "Batch Number";
  protected static final String SRA_DATE_OF_SEQUENCING_RUN_FIELD = "Date of Sequencing Run";
  protected static final String SRA_SEQUENCING_KIT_FIELD = "Sequencing Kit";
  protected static final String SRA_FILE_TYPE_FIELD = "File Type";
  protected static final String SRA_FILE_NAME_FIELD = "filename";

  protected static final List<String> SRA_FIELDS = Collections.unmodifiableList(Arrays.asList(
      SRA_SAMPLE_ID_FIELD,
      SRA_TARGET_SUBSTRATE_FIELD,
      SRA_TARGET_SUBSTRATE_QUALITY_FIELD,
      SRA_TEMPLATE_AMOUNT_FIELD,
      SRA_NUCLEIC_ACID_PROCESSING_ID_FIELD,
      SRA_LIBRARY_STRATEGY_FIELD,
      SRA_LIBRARY_SOURCE_FIELD,
      SRA_LIBRARY_SELECTION_FIELD,
      SRA_LIBRARY_LAYOUT_FIELD,
      SRA_LIBRARY_GENERATION_METHOD_FIELD,
      SRA_LIBRARY_GENERATION_PROTOCOL_FIELD,
      SRA_PROTOCOL_IDS_FIELD,
      SRA_TARGET_LOCUS_FOR_PCR_FIELD,
      SRA_FORWARD_PCR_PRIMER_TARGET_LOCATION_FIELD,
      SRA_REVERSE_PCR_PRIMER_TARGET_LOCATION_FIELD,
      SRA_COMPLETE_SEQUENCES_FIELD,
      SRA_PHYSICAL_LINKAGE_OF_DIFFERENT_LOCI_FIELD,
      SRA_TOTAL_READS_PASSING_QC_FILTER_FIELD,
      SRA_SEQUENCING_PLATFORM_FIELD,
      SRA_READ_LENGTHS_FIELD,
      SRA_SEQUENCING_FACILITY_FIELD,
      SRA_BATCH_NUMBER_FIELD,
      SRA_DATE_OF_SEQUENCING_RUN_FIELD,
      SRA_SEQUENCING_KIT_FIELD,
      SRA_FILE_TYPE_FIELD,
      SRA_FILE_NAME_FIELD
  ));

  protected static final List<String> SRA_REQUIRED_FIELD_VALUES = SRA_FIELDS;

  // PRIVATE //

  /**
   * The caller references the constants using Constants.EMPTY_STRING,
   * and so on. Thus, the caller should be prevented from constructing objects of
   * this class, by declaring this private constructor.
   */
  private NcbiCairrConstants() {
    // This restricts instantiation
    throw new AssertionError();
  }

}
