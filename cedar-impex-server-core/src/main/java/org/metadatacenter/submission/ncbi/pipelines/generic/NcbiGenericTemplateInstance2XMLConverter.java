package org.metadatacenter.submission.ncbi.pipelines.generic;

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

import static org.metadatacenter.submission.ncbi.pipelines.generic.NcbiGenericConstants.*;

/**
 * Convert a CEDAR JSON Schema-based NCBI2CEDAR instance to a BioProject/BioSample/SRA XML-based submission.
 */
public class NcbiGenericTemplateInstance2XMLConverter {
  final static Logger log = LoggerFactory.getLogger(NcbiGenericTemplateInstance2XMLConverter.class);

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
      Optional<String> sampleName = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_SAMPLE_NAME_FIELD);
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
      Optional<String> sraSampleName = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, SRA_SAMPLE_NAME_FIELD);
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

      // Library ID
      Optional<String> libraryId = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, SRA_LIBRARY_ID_FIELD);
      if (libraryId.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_ID");
        fileAttribute.setValue(libraryId.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Title
      Optional<String> sraTitle = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, SRA_TITLE_FIELD);
      if (sraTitle.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("title");
        fileAttribute.setValue(sraTitle.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Strategy
      Optional<String> libraryStrategy = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, SRA_LIBRARY_STRATEGY_FIELD);
      if (libraryStrategy.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_strategy");
        fileAttribute.setValue(libraryStrategy.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Source
      Optional<String> librarySource = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, SRA_LIBRARY_SOURCE_FIELD);
      if (librarySource.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_source");
        fileAttribute.setValue(librarySource.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Selection
      Optional<String> librarySelection = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, SRA_LIBRARY_SELECTION_FIELD);
      if (librarySelection.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_selection");
        fileAttribute.setValue(librarySelection.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Library Layout
      Optional<String> libraryLayout = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, SRA_LIBRARY_LAYOUT_FIELD);
      if (libraryLayout.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("library_layout");
        fileAttribute.setValue(libraryLayout.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Platform
      Optional<String> sequencingPlatform = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, SRA_PLATFORM_FIELD);
      if (sequencingPlatform.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("platform");
        fileAttribute.setValue(sequencingPlatform.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Instrument Model
      Optional<String> libraryInstrument = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, SRA_INSTRUMENT_MODEL_FIELD);
      if (libraryInstrument.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("instrument_model");
        fileAttribute.setValue(libraryInstrument.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
      }

      // Design Description
      Optional<String> designDescription = NcbiPipelinesCommonUtil.getTemplateFieldValue(sra, SRA_DESIGN_DESCRIPTION_FIELD);
      if (designDescription.isPresent()) {
        TypeFileAttribute fileAttribute = submissionObjectFactory.createTypeFileAttribute();
        fileAttribute.setName("design_description");
        fileAttribute.setValue(designDescription.get());
        sraAddFiles.getAttributeOrMetaOrAttributeRefId().add(fileAttribute);
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
    Optional<String> contactEmail = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioProject, BIOPROJECT_CONTACT_EMAIL_FIELD);
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

    // Sample Title
    Optional<String> sampleTitle = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_SAMPLE_TITLE_FIELD);
    if (sampleTitle.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("sample_title", sampleTitle.get()));
    }

    // Organism
    Optional<String> organism = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_ORGANISM_FIELD);
    if (organism.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("organism", organism.get()));
    }

    // Isolate
    Optional<String> isolate = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_ISOLATE_FIELD);
    if (isolate.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("isolate", isolate.get()));
    }

    // Age
    Optional<String> age = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_AGE_FIELD);
    if (age.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("age", age.get()));
    }

    // Biomaterial Provider
    Optional<String> biomaterialProvider = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_BIOMATERIAL_PROVIDER_FIELD);
    if (biomaterialProvider.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("biomaterial_provider", biomaterialProvider.get()));
    }

    // Sex
    Optional<String> sex = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_SEX_FIELD);
    if (sex.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("sex", sex.get()));
    }

    // Tissue
    Optional<String> tissue = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_TISSUE_FIELD);
    if (tissue.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("tissue", tissue.get()));
    }

    // Cell line
    Optional<String> cellLine = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_CELL_LINE_FIELD);
    if (cellLine.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("cell_line", cellLine.get()));
    }

    // Cell type
    Optional<String> cellType = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_CELL_TYPE_FIELD);
    if (cellType.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("cell_type", cellType.get()));
    }

    // Cell subtype
    Optional<String> cellSubtype = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_CELL_SUBTYPE_FIELD);
    if (cellSubtype.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("cell_subtype", cellSubtype.get()));
    }

    // Culture collection
    Optional<String> cultureCollection = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_CULTURE_COLLECTION_FIELD);
    if (cultureCollection.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("culture_collection", cultureCollection.get()));
    }

    // Development stage
    Optional<String> developmentStage = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_DEVELOPMENT_STAGE_FIELD);
    if (developmentStage.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("development_stage", developmentStage.get()));
    }

    // Disease
    Optional<String> disease = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_DISEASE_FIELD);
    if (disease.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("disease", disease.get()));
    }

    // Disease stage
    Optional<String> diseaseStage = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_DISEASE_STAGE_FIELD);
    if (diseaseStage.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("disease_stage", diseaseStage.get()));
    }

    // Ethnicity
    Optional<String> ethnicity = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_ETHNICITY_FIELD);
    if (ethnicity.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("ethnicity", ethnicity.get()));
    }

    // Health state
    Optional<String> healthState = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_HEALTH_STATE_FIELD);
    if (healthState.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("health_state", healthState.get()));
    }

    // Karyotype
    Optional<String> karyotype = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_KARYOTYPE_FIELD);
    if (karyotype.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("karyotype", karyotype.get()));
    }

    // Phenotype
    Optional<String> phenotype = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_PHENOTYPE_FIELD);
    if (phenotype.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("phenotype", phenotype.get()));
    }

    // Population
    Optional<String> population = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_POPULATION_FIELD);
    if (population.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("population", population.get()));
    }

    // Race
    Optional<String> race = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_RACE_FIELD);
    if (race.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("race", race.get()));
    }

    // Sample type
    Optional<String> sampleType = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_SAMPLE_TYPE_FIELD);
    if (sampleType.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("sample_type", sampleType.get()));
    }

    // Treatment
    Optional<String> treatment = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_TREATMENT_FIELD);
    if (treatment.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("treatment", treatment.get()));
    }

    // Sample Description
    Optional<String> sampleDescription = NcbiPipelinesCommonUtil.getTemplateFieldValue(bioSample, BIOSAMPLE_DESCRIPTION_FIELD);
    if (sampleDescription.isPresent()) {
      bioSampleAttributes.getAttribute().add(createAttribute("sample_description", treatment.get()));
    }

    // Release Date
    if (releaseDate.isPresent() && !releaseDate.isEmpty()) {
      String xmlReleaseDate = convertDateFormat(releaseDate.get(), instanceDateFormat, xmlDateFormat);
      bioSampleAttributes.getAttribute().add(createAttribute("projected_release_date", xmlReleaseDate));
    }

    // Custom attribute to specify that the submission was done using CEDAR
    bioSampleAttributes.getAttribute().add(createAttribute("submission_tool", BIOSAMPLE_SUBMISSION_TOOL));

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

}
