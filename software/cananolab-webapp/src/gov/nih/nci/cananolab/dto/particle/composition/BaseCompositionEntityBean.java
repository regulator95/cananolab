/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.dto.particle.composition;

import gov.nih.nci.cananolab.dto.common.FileBean;
import gov.nih.nci.cananolab.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for NanomaterialEntityBean, FunctionalizingEntityBean and
 * ChemicalAssociationBean
 *
 * @author pansu
 *
 */
public class BaseCompositionEntityBean {
	protected String type;

	protected String description;

	protected FileBean theFile = new FileBean();

	protected String className;

	protected List<FileBean> files = new ArrayList<FileBean>();

	protected String domainId, displayName, name; // used for DWR ajax in

	// bodySubmitChemicalAssociation.jsp

	public BaseCompositionEntityBean() {
	}

	public void addFile(FileBean file) {
		// if an old one exists, remove it first
		int index = files.indexOf(file);
		if (index != -1) {
			files.remove(file);
			// retain the original order
			files.add(index, file);
		} else {
			files.add(file);
		}
	}

	public void removeFile(FileBean file) {
		files.remove(file);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public String getDescriptionDisplayName() {
		return StringUtils.escapeXmlButPreserveLineBreaks(description);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public FileBean getTheFile() {
		return theFile;
	}

	public void setTheFile(FileBean theFile) {
		this.theFile = theFile;
	}

	public List<FileBean> getFiles() {
		return files;
	}

	public void setFiles(List<FileBean> files) {
		this.files = files;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getDomainId() {
		return domainId;
	}

	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(String displayName){
		this.displayName = displayName;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
}
