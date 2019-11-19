/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.util;


public class SampleConstants {
	// PubChem data source - Compound.
	public static final String COMPOUND  = "Compound";
	
	// PubChem data source - Substance.
	public static final String SUBSTANCE = "Substance";
	
	// PubChem data source - BioAssay.
	public static final String BIOASSAY = "BioAssay";

	// PubChem data source id - cid.
	public static final String COMPOUND_ID  = "cid";
	
	// PubChem data source id - sid.
	public static final String SUBSTANCE_ID = "sid";
	
	// PubChem data source id - bid.
	public static final String BIOASSAY_ID = "bid";

	// PubChem data source array.
	public static final String[] PUBCHEM_DS_LIST = {COMPOUND, SUBSTANCE, BIOASSAY};
	
	// PubChem URL.
	public static final String PUBCHEM_URL = "http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?";

	// SAMPLE_ID
	public static final String SAMPLE_ID = "sampleId";
	
	// POC_ID
	public static final String POC_ID = "pocId";
}
