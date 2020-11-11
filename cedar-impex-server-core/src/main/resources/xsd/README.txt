
This directory contains XML Schema descriptions for various NCBI submission types.
These schemas were developed by NCBI.
We use these schema files to automatically generate Java classes to produce conforming XML documents.

Note that these files are taken as is from NCBI's site and modified slighty to fix import issues.

The modificatios made are listed below.

SP.common.xsd: 

Common attributes used by NCBI submissions. Used as is.

bioproject.xsd:

Description of a BioProject submission. Modified to include SP.common.xsd from a local file and to use the 'bioproject' namespace.

biosample.xsd:

Description of a BioSample part of a submission. Modified to include SP.common.xsd from a local file.

submission.xsd

Description of an SRA submission. Modifed to fix some namespace issues. In particualur, imports from
SP.common.xsd, bioproject.xsd, and biosample.xsd are assigned namespaces.

Modified to hard code biosample.xsd as the submission data in the Action/AddData/Data/XMLContent element.
Also modified to replace AddFiles element definition with one in schema received from 
Ahmad at Yale (./examples/submission-com.xsd).

BioSampleValidatorResponse.xsd:

Describes response from NCBI BioSample validator (https://www.ncbi.nlm.nih.gov/projects/biosample/validate/).
