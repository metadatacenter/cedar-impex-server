package org.metadatacenter.submission.ncbi.pipelines.cairr;

import biosample.TypeAttribute;
import biosample.TypeBioSample;
import biosample.TypeBioSampleIdentifier;
import com.fasterxml.jackson.databind.JsonNode;
import common.sp.*;
import generated.ObjectFactory;
import generated.*;
import org.metadatacenter.submission.ncbi.pipelines.NcbiPipelinesCommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.metadatacenter.submission.ncbi.pipelines.cairr.NcbiCairrConstants.*;

/**
 * Convert a CEDAR JSON Schema-based NCBI2CEDAR instance to a BioProject/BioSample/SRA XML-based submission.
 */
public class NcbiCairrTemplateInstance2XMLConverter {
  final static Logger log = LoggerFactory.getLogger(NcbiCairrTemplateInstance2XMLConverter.class);

  private List<String> sraIds = new ArrayList<>();

  private final ObjectFactory submissionObjectFactory = new ObjectFactory();
  private final common.sp.ObjectFactory ncbiCommonObjectFactory = new common.sp.ObjectFactory();
  private final biosample.ObjectFactory bioSampleObjectFactory = new biosample.ObjectFactory();
  // private final bioproject.ObjectFactory bioProjectObjectFactory = new bioproject.ObjectFactory();

  private final String instanceDateFormat = "yyyy-MM-dd";
  private final String xmlDateFormat = "yyyy-MM-dd'-'hh:mm";

  public String convertTemplateInstanceToXML(JsonNode instance) throws JAXBException, DatatypeConfigurationException, ParseException {

    Submission ncbiSubmission = submissionObjectFactory.createSubmission();

    Optional<String> submissionsReleaseDate =
        NcbiPipelinesCommonUtil.getTemplateFieldValue(instance, SUBMISSION_RELEASE_DATE_FIELD);

    /*** BioProject ***/

    // Create submission description using BioProject details
    Submission.Description submissionDescription = createSubmissionDescription(instance, submissionsReleaseDate);
    ncbiSubmission.setDescription(submissionDescription);

    Optional<JsonNode> bioProject = NcbiPipelinesCommonUtil.getTemplateElementNode(instance, BIOPROJECT_ELEMENT);

    // BioProject Accession
    String bioprojectAccession = null;
    Optional<String> bpAccession = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioProject.get(), BIOPROJECT_STUDY_ID_FIELD);
    if (bpAccession.isPresent()) {
      bioprojectAccession = bpAccession.get();
    }

    /*** BioSample ***/
    Optional<JsonNode> biosamples = NcbiPipelinesCommonUtil.getTemplateElementNode(instance, BIOSAMPLE_ELEMENT);

    for (JsonNode bioSample : biosamples.get()) {

      // Start <BioSample> section
      TypeBioSample ncbiBioSample = bioSampleObjectFactory.createTypeBioSample();
      ncbiBioSample.setSchemaVersion(BIOSAMPLE_SCHEMA_VERSION);

      // Sample Name
      Optional<String> sampleName = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_SAMPLE_ID_FIELD);
      if (sampleName.isPresent()) {
        ncbiBioSample.setSampleId(createBioSampleIdentifier(sampleName.get()));
      }

      // Submission descriptor
      ncbiBioSample.setDescriptor(createDescriptor(BIOSAMPLE_SUBMISSION_DESCRIPTOR, BIOSAMPLE_SUBMISSION_DESCRIPTOR));

      // Organism
      ncbiBioSample.setOrganism(createOrganism(BIOSAMPLE_ORGANISM));

      // Package
      ncbiBioSample.setPackage(BIOSAMPLE_PACKAGE);

      // Attributes
      ncbiBioSample.setAttributes(createBioSampleAttributes(bioSample, submissionsReleaseDate));

      // XmlContent
      // Development Note: The original NCBI submission doesn't include the BioSample element, so it
      // is required to modify the submission.xsd file (See submission.xsd:441)
      TypeInlineData.XmlContent xmlContent = submissionObjectFactory.createTypeInlineDataXmlContent();
      xmlContent.setBioSample(ncbiBioSample);

      // Data
      Submission.Action.AddData.Data bioSampleData = submissionObjectFactory.createSubmissionActionAddDataData();
      bioSampleData.setContentType("XML");
      bioSampleData.setXmlContent(xmlContent);

      // Identifier
      TypeIdentifier actionIdentifier = ncbiCommonObjectFactory.createTypeIdentifier();
      TypeSPUID bioSampleSpuid = ncbiCommonObjectFactory.createTypeSPUID();
      bioSampleSpuid.setSpuidNamespace(CEDAR_NAMESPACE);
      bioSampleSpuid.setValue(createNewActionId());
      actionIdentifier.setSPUID(bioSampleSpuid);

      // Action/AddData
      Submission.Action.AddData bioSampleSubmissionActionAddData = submissionObjectFactory.createSubmissionActionAddData();
      bioSampleSubmissionActionAddData.setTargetDb(TypeTargetDb.BIO_SAMPLE);
      bioSampleSubmissionActionAddData.setData(bioSampleData);
      bioSampleSubmissionActionAddData.setIdentifier(actionIdentifier);

      // Action
      Submission.Action bioSampleAction = submissionObjectFactory.createSubmissionAction();
      bioSampleAction.setAddData(bioSampleSubmissionActionAddData);
      ncbiSubmission.getAction().add(bioSampleAction);

    }

    /*** SRA ***/
    Optional<JsonNode> sras = NcbiPipelinesCommonUtil.getTemplateElementNode(instance, SRA_ELEMENT);

    // Retrieve the SRAs from the instance
    for (JsonNode sra : sras.get()) {

      Submission.Action.AddFiles sraAddFiles = submissionObjectFactory.createSubmissionActionAddFiles();
      sraAddFiles.setTargetDb(TypeTargetDb.SRA);

      // File type and file names
      Optional<String> fileType = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, SRA_FILE_TYPE_FIELD);

      if (fileType.isPresent()) {

        if (sra.hasNonNull(SRA_FILE_NAME_FIELD) && sra.get(SRA_FILE_NAME_FIELD).size() > 0) {

          Iterator<JsonNode> fileNameFieldsIt = sra.get(SRA_FILE_NAME_FIELD).iterator();

          while (fileNameFieldsIt.hasNext()) {

            String fileNameField = fileNameFieldsIt.next().asText();

            Optional<String> fileName = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, fileNameField);

            if (fileName.isPresent()) {

              Submission.Action.AddFiles.File sraFile = submissionObjectFactory.createSubmissionActionAddFilesFile();
              sraFile.setFilePath(fileName.get());
              sraFile.setDataType(fileType.get());
              sraAddFiles.getFile().add(sraFile);

            } else {
              throw new IllegalArgumentException("File name field not present: " + fileNameField);
            }
          }
        } else {
          // In this case there is a file type but no file names, so we throw an exception
          throw new IllegalArgumentException("Missing array of file names");
        }
      } else {
        // do nothing
      }

      // Reference to BioSample ID (Sample Name)
      Optional<String> sraSampleName = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, SRA_SAMPLE_ID_FIELD);
      if (sraSampleName.isPresent()) {
        TypeFileAttributeRefId bioSampleAttributeRefId = submissionObjectFactory.createTypeFileAttributeRefId();
        bioSampleAttributeRefId.setName("BioSample");
        TypeRefId refId = ncbiCommonObjectFactory.createTypeRefId();
        TypeSPUID spuid = ncbiCommonObjectFactory.createTypeSPUID();
        spuid.setSpuidNamespace(CEDAR_NAMESPACE);
        spuid.setValue(sraSampleName.get());
        refId.setSPUID(spuid);
        bioSampleAttributeRefId.setRefId(refId);
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(bioSampleAttributeRefId);
      }

      // Reference to BioProject ID
      if (!bioprojectAccession.isEmpty()) {
        TypeFileAttributeRefId bioProjectAttributeRefId = submissionObjectFactory.createTypeFileAttributeRefId();
        bioProjectAttributeRefId.setName("BioProject");
        TypeRefId refId = ncbiCommonObjectFactory.createTypeRefId();
        TypePrimaryId primaryId = ncbiCommonObjectFactory.createTypePrimaryId();
        primaryId.setDb("BioProject");
        primaryId.setValue(bioprojectAccession);
        refId.setPrimaryId(primaryId);
        bioProjectAttributeRefId.setRefId(refId);
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(bioProjectAttributeRefId);
      }

      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_TARGET_SUBSTRATE_FIELD, "target_substrate");

      // Target Substrate Quality
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_TARGET_SUBSTRATE_QUALITY_FIELD, "target_substrate_quality");
      // Nucleic Acid Processing ID
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_NUCLEIC_ACID_PROCESSING_ID_FIELD, "library_ID");
      // Template Amount
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_TEMPLATE_AMOUNT_FIELD, "template_amount");
      // Library Generation Method
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_LIBRARY_GENERATION_METHOD_FIELD, "library_generation_method");
      // Library Generation Protocol
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_LIBRARY_GENERATION_PROTOCOL_FIELD, "design_description");
      // Protocol ID
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_PROTOCOL_IDS_FIELD, "protocol_ids");
      // Target Locus for PCR
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_TARGET_LOCUS_FOR_PCR_FIELD, "pcr_target_locus");
      // Forward PCR Primer Target Location
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_FORWARD_PCR_PRIMER_TARGET_LOCATION_FIELD, "forward_pcr_primer_target_location");
      // Reverse PCR Primer Target Location
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_REVERSE_PCR_PRIMER_TARGET_LOCATION_FIELD, "reverse_pcr_primer_target_location");
      // Complete Sequence
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_COMPLETE_SEQUENCES_FIELD, "complete_sequences");
      // Physical Linkage of Different Loci
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_PHYSICAL_LINKAGE_OF_DIFFERENT_LOCI_FIELD, "physical_linkage");
      // Total Reads Passing QC Filter
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_TOTAL_READS_PASSING_QC_FILTER_FIELD, "total_reads_passing_qc_filter");

      // library + sequencing strategy + layout + instrument model must be unique according to
      // https://www.ncbi.nlm.nih.gov/sra/docs/submitmeta/
      // Sequencing Platform
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_SEQUENCING_PLATFORM_FIELD, "instrument_model");
      // Sequencing Read Lengths
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_READ_LENGTHS_FIELD, "read_lengths");
      // Sequencing Facility
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_SEQUENCING_FACILITY_FIELD, "sequencing_facility");
      // Batch Number
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_BATCH_NUMBER_FIELD, "batch_number");
      // Date of Sequencing Run
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_DATE_OF_SEQUENCING_RUN_FIELD, "sequencing_run_date");
      // Sequencing Kit
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_SEQUENCING_KIT_FIELD, "sequencing_kit");
      // Library Strategy
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_LIBRARY_STRATEGY_FIELD, "library_strategy");
      // Library Source
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_LIBRARY_SOURCE_FIELD, "library_source");
      // Library Selection
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_LIBRARY_SELECTION_FIELD, "library_selection");
      // Library Layout
      sraAddFiles = setSraFileAttribute(sraAddFiles, sra, SRA_LIBRARY_LAYOUT_FIELD, "library_layout");

      // Release Status
      if (submissionsReleaseDate.isPresent() && !submissionsReleaseDate.isEmpty()) {  // Use the top-level release date
        TypeReleaseStatus typeReleaseStatus = submissionObjectFactory.createTypeReleaseStatus();
        TypeReleaseStatus.SetReleaseDate filesReleaseDate = submissionObjectFactory.createTypeReleaseStatusSetReleaseDate();
        filesReleaseDate.setReleaseDate(createXMLGregorianCalendar(submissionsReleaseDate.get(), instanceDateFormat));
        typeReleaseStatus.setSetReleaseDate(filesReleaseDate);
        sraAddFiles.setStatus(typeReleaseStatus);
      }

      // End of AIRR SRA Elements

      TypeSPUID sraSampleSpuid = ncbiCommonObjectFactory.createTypeSPUID();
      sraSampleSpuid.setSpuidNamespace(CEDAR_NAMESPACE);
      sraSampleSpuid.setValue(createNewSraId());

      TypeIdentifier sraIdentifier = ncbiCommonObjectFactory.createTypeIdentifier();
      sraIdentifier.setSPUID(sraSampleSpuid);

      sraAddFiles.setIdentifier(sraIdentifier);

      // Action
      Submission.Action sraAction = submissionObjectFactory.createSubmissionAction();
      sraAction.setAddFiles(sraAddFiles);

      ncbiSubmission.getAction().add(sraAction);

    }

    // Generate XML from the submission instance
    StringWriter writer = new StringWriter();
    JAXBContext ctx = JAXBContext.newInstance(Submission.class);
    Marshaller marshaller = ctx.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    marshaller.marshal(ncbiSubmission, writer);

    return writer.toString();
  }

  private TypeOrganism createOrganism(String organismName) {
    TypeOrganism sampleOrganism = ncbiCommonObjectFactory.createTypeOrganism();
    sampleOrganism.setOrganismName(organismName);
    return sampleOrganism;
  }

  private TypeDescriptor createDescriptor(String title, String description) {
    TypeDescriptor sampleDescriptor = ncbiCommonObjectFactory.createTypeDescriptor();
    JAXBElement descriptionElement = new JAXBElement(new QName("p"), String.class, description);
    TypeBlock sampleDescription = ncbiCommonObjectFactory.createTypeBlock();
    sampleDescription.getPOrUlOrOl().add(descriptionElement);
    sampleDescriptor.setTitle(title);
    sampleDescriptor.setDescription(sampleDescription);
    return sampleDescriptor;
  }

  private TypeBioSampleIdentifier createBioSampleIdentifier(String bioSampleID) {
    TypeBioSampleIdentifier sampleID = bioSampleObjectFactory.createTypeBioSampleIdentifier();
    TypeBioSampleIdentifier.SPUID spuid = bioSampleObjectFactory.createTypeBioSampleIdentifierSPUID();
    spuid.setSpuidNamespace(CEDAR_NAMESPACE);
    spuid.setValue(bioSampleID);
    sampleID.getSPUID().add(spuid);
    return sampleID;
  }

  /*
   * Object construction for the submission <Description> section
   */
  private Submission.Description createSubmissionDescription(JsonNode instance, Optional<String> releaseDate) throws
      DatatypeConfigurationException, ParseException {

    JsonNode bioProject = NcbiPipelinesCommonUtil.getTemplateElementNode(instance, BIOPROJECT_ELEMENT).get();
    Submission.Description submissionDescription = submissionObjectFactory.createSubmissionDescription();

    // Description
    submissionDescription.setComment(SUBMISSION_DESCRIPTOR);

    // Email
    TypeContactInfo contactInfo = ncbiCommonObjectFactory.createTypeContactInfo();
    TypeAccount contactSubmitter = submissionObjectFactory.createTypeAccount();
    Optional<String> contactEmail = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioProject, BIOPROJECT_CONTACT_INFO_FIELD);
    if (contactEmail.isPresent()) {
      contactInfo.setEmail(contactEmail.get());
      contactSubmitter.setUserName(contactEmail.get());
    }
    submissionDescription.setSubmitter(contactSubmitter);

    TypeOrganization contactOrganization = submissionObjectFactory.createTypeOrganization();
    contactOrganization.setRole("owner"); // This organization is the owner of the submission
    contactOrganization.setType("lab");

    // Organization Name
    TypeOrganization.Name organizationName = submissionObjectFactory.createTypeOrganizationName();
    Optional<String> orgName = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioProject, BIOPROJECT_LAB_NAME_FIELD);
    if (orgName.isPresent()) {
      organizationName.setValue(orgName.get());
    }
    contactOrganization.setName(organizationName);

    submissionDescription.getOrganization().add(contactOrganization);

    // Release Date
    Submission.Description.Hold submissionDescriptionHold = submissionObjectFactory.createSubmissionDescriptionHold();
    if (releaseDate.isPresent()) {
      submissionDescriptionHold.setReleaseDate(createXMLGregorianCalendar(releaseDate.get(), instanceDateFormat));
      submissionDescription.setHold(submissionDescriptionHold);
    }

    return submissionDescription;
  }

  private TypeBioSample.Attributes createBioSampleAttributes(JsonNode bioSample, Optional<String> releaseDate) throws ParseException {
    // Attributes
    TypeBioSample.Attributes bioSampleAttributes = bioSampleObjectFactory.createTypeBioSampleAttributes();

    // Release Date
    if (releaseDate.isPresent() && !releaseDate.isEmpty()) {  // Use the top-level release date
      String xmlReleaseDate = convertDateFormat(releaseDate.get(), instanceDateFormat, xmlDateFormat);
      bioSampleAttributes.getAttribute().add(createAttribute("ProjectedReleaseDate", xmlReleaseDate));
    }
    else {
      Optional<String> estimatedReleaseDate = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_ESTIMATED_RELEASE_DATE_FIELD);
      if (estimatedReleaseDate.isPresent() && !estimatedReleaseDate.isEmpty()) {
        bioSampleAttributes.getAttribute().add(createAttribute("ProjectedReleaseDate", estimatedReleaseDate.get()));
      }
    }

    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_SUBJECT_ID_FIELD, "SubjectId");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_SYNTHETIC_LIBRARY_FIELD, "SyntheticLibrary");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_ORGANISM_FIELD, "Organism");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_SEX_FIELD, "Sex");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_AGE_FIELD, "Age");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_AGE_EVENT_FIELD, "AgeEvent");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_ANCESTRY_POPULATION_FIELD, "AncestryPopulation");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_ETHNICITY_FIELD, "Ethnicity");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_RACE_FIELD, "Race");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_STRAIN_NAME_FIELD, "StrainName");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_RELATION_TO_OTHER_SUBJECTS_FIELD, "RelationToOtherSubject");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_RELATION_TYPE_FIELD, "RelationType");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_STUDY_GROUP_DESCRIPTION_FIELD, "StudyGroupDescription");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_DIAGNOSIS, "Diagnosis");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_LENGTH_OF_DISEASE, "LengthOfDisease");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_DISEASE_STAGE_FIELD, "DiseaseStage");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_PRIOR_THERAPIES_FIELD, "PriorTherapiesForPrimaryDiseaseUnderStudy");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_IMMUNOGEN_FIELD, "Immunogen");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_INTERVENTION_DEFINITION_FIELD, "InterventionDefinition");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_OTHER_RELEVANT_MEDICAL_HISTORY_FIELD, "OtherRelevantMedicalHistory");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_SAMPLE_TYPE_FIELD, "SampleType");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_TISSUE_FIELD, "Tissue");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_ANATOMIC_SITE_FIELD, "AnatomicSite");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_DISEASE_STATE_OF_SAMPLE_FIELD, "DiseaseStateOfSample");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_SAMPLE_COLLECTION_TIME_FIELD, "SampleCollectionTime");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_COLLECTION_TIME_EVENT_FIELD, "CollectionTimeEventT01");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_BIOMATERIAL_PROVIDER_FIELD, "BiomaterialProvider");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_TISSUE_PROCESSING_FIELD, "TissueProcessing");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_CELL_SUBSET_FIELD, "CellSubset");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_CELL_SUBSET_PHENOTYPE_FIELD, "CellSubsetPhenotype");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_SINGLE_CELL_SORT_FIELD, "SingleCellSort");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_NUMBER_OF_CELLS_IN_EXPERIMENT_FIELD, "NumberOfCellsInExperiment");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_NUMBER_OF_CELLS_PER_SEQUENCING_REACTION_FIELD, "NumberOfCellsPerSequencingReaction");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_CELL_STORAGE_FIELD, "CellSubsetPhenotype");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_CELL_QUALITY_FIELD, "CellQuality");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_CELL_ISOLATION_FIELD, "CellIsolationValue");
    // We also set Isolate to 'Cell Isolation'. It is mandatory to fill out this field
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_CELL_ISOLATION_FIELD, "Isolate");
    bioSampleAttributes = setBiosampleAttribute(bioSampleAttributes, bioSample, BIOSAMPLE_PROCESSING_PROTOCOL_FIELD, "CellProcessingProtocol");

    // Custom attribute to specify that the submission was done using CEDAR
    bioSampleAttributes.getAttribute().add(createAttribute("SubmissionTool", BIOSAMPLE_SUBMISSION_TOOL));

    return bioSampleAttributes;
  }

  private XMLGregorianCalendar createXMLGregorianCalendar(String date, String format) throws DatatypeConfigurationException, ParseException {
    GregorianCalendar calendar = new GregorianCalendar();
    calendar.setTime(new SimpleDateFormat(format).parse(date));
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
  }

  private String convertDateFormat(String date, String sourceFormat, String targetFormat) throws ParseException {
    DateFormat source = new SimpleDateFormat(sourceFormat);
    DateFormat target = new SimpleDateFormat(targetFormat);
    Date d = source.parse(date);
    return target.format(d);
  }

  private String createNewSraId() {
    String id = "SRA-" + UUID.randomUUID();
    sraIds.add(id);
    return id;
  }

  private String createNewActionId() {
    String id = "Action-" + UUID.randomUUID();
    sraIds.add(id);
    return id;
  }

  private TypeAttribute createAttribute(String attributeName, String attributeValue) {
    TypeAttribute attribute = bioSampleObjectFactory.createTypeAttribute();
    attribute.setAttributeName(attributeName);
    attribute.setValue(attributeValue);
    return attribute;
  }

  private TypeBioSample.Attributes setBiosampleAttribute(TypeBioSample.Attributes attributes, JsonNode biosample,
                                                               String fieldName, String attributeName) {
    Optional<String> fieldValue = NcbiPipelinesCommonUtil.getTemplateFieldValue(biosample, fieldName);
    if (fieldValue.isPresent()) {
      attributes.getAttribute().add(createAttribute(attributeName, fieldValue.get()));
    }
    return attributes;
  }

  private Submission.Action.AddFiles setSraFileAttribute(Submission.Action.AddFiles addFiles, JsonNode sra, String fieldName,
                                                     String attributeName) {
    Optional<String> fieldValue = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, fieldName);
    if (fieldValue.isPresent()) {
      TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
      fileAttribute.setName(attributeName);
      fileAttribute.setValue(fieldValue.get());
      addFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
    }
    return addFiles;
  }


}
