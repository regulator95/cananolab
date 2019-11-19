/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.dto.particle.composition;

import gov.nih.nci.cananolab.domain.common.File;
import gov.nih.nci.cananolab.domain.linkage.Attachment;
import gov.nih.nci.cananolab.domain.linkage.OtherChemicalAssociation;
import gov.nih.nci.cananolab.domain.particle.ChemicalAssociation;
import gov.nih.nci.cananolab.dto.common.FileBean;
import gov.nih.nci.cananolab.util.ClassUtils;
import gov.nih.nci.cananolab.util.Comparators;
import gov.nih.nci.cananolab.util.Constants;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

public class ChemicalAssociationBean extends BaseCompositionEntityBean {
	private ChemicalAssociation domainAssociation;

	private Attachment attachment = new Attachment();

	private AssociatedElementBean associatedElementA = new AssociatedElementBean();

	private AssociatedElementBean associatedElementB = new AssociatedElementBean();

	public ChemicalAssociationBean() {
	}

	public ChemicalAssociationBean(ChemicalAssociation chemicalAssociation) {
		domainAssociation = chemicalAssociation;
		if (chemicalAssociation instanceof Attachment) {
			attachment = (Attachment) chemicalAssociation;
		}
		description = chemicalAssociation.getDescription();
		className = ClassUtils.getShortClassName(chemicalAssociation.getClass()
				.getName());
		if (chemicalAssociation.getFileCollection() != null) {
			for (File file : chemicalAssociation.getFileCollection()) {
				files.add(new FileBean(file));
			}
			files.sort(new Comparators.FileBeanDateComparator());
		}
		associatedElementA = new AssociatedElementBean(chemicalAssociation
				.getAssociatedElementA());
		associatedElementB = new AssociatedElementBean(chemicalAssociation
				.getAssociatedElementB());
		updateType();
	}

	public ChemicalAssociation getDomainAssociation() {
		return domainAssociation;
	}

	public void setDomainAssociation(ChemicalAssociation domainAssociation) {
		this.domainAssociation = domainAssociation;
	}

	public void setupDomainAssociation(String createdBy) throws Exception {
		className = ClassUtils.getShortClassNameFromDisplayName(type);
		Class clazz = ClassUtils.getFullClass(className);
		if (clazz == null) {
			clazz = OtherChemicalAssociation.class;
		}
		if (domainAssociation == null) {
			try {
				domainAssociation = (ChemicalAssociation) clazz.newInstance();
			} catch (ClassCastException ex) {
				String tmpType = type;
				this.setType(null);
				throw new ClassCastException(tmpType);
			}
		}
		if (domainAssociation instanceof OtherChemicalAssociation) {
			((OtherChemicalAssociation) domainAssociation).setType(type);
		} else if (domainAssociation instanceof Attachment) {
			domainAssociation = attachment;
		}
		if (domainAssociation.getId() == null) {
			domainAssociation.setCreatedBy(createdBy);
			domainAssociation.setCreatedDate(new Date());
		}
		domainAssociation.setDescription(description);
		associatedElementA.setupDomainElement(createdBy);
		associatedElementB.setupDomainElement(createdBy);
		domainAssociation.setAssociatedElementA(associatedElementA
				.getDomainElement());
		domainAssociation.setAssociatedElementB(associatedElementB
				.getDomainElement());
		if (domainAssociation.getFileCollection() != null) {
			domainAssociation.getFileCollection().clear();
		} else {
			domainAssociation.setFileCollection(new HashSet<File>());
		}
		for (FileBean file : files) {
			domainAssociation.getFileCollection().add(file.getDomainFile());
		}
	}

	public Attachment getAttachment() {
		return attachment;
	}

	public void setAttachment(Attachment attachment) {
		this.attachment = attachment;
	}

	public AssociatedElementBean getAssociatedElementA() {
		return associatedElementA;
	}

	public AssociatedElementBean getAssociatedElementB() {
		return associatedElementB;
	}

	public void setAssociatedElementA(AssociatedElementBean associatedElementA) {
		this.associatedElementA = associatedElementA;
	}

	public void setAssociatedElementB(AssociatedElementBean associatedElementB) {
		this.associatedElementB = associatedElementB;
	}

	private void updateType() {
		if (domainAssociation instanceof OtherChemicalAssociation) {
			type = ((OtherChemicalAssociation) domainAssociation).getType();
		} else {
			type = ClassUtils.getDisplayName(className);
		}
	}

	public void resetDomainCopy(String createdBy, ChemicalAssociation copy) {
		copy.setCreatedBy(createdBy + ":"
				+ Constants.AUTO_COPY_ANNOTATION_PREFIX + ":" + copy.getId());
		// clear Id
		copy.setId(null);

		// don't need to set associatedElement ID and createdBy because they are
		// already set in composing elements and functionalizing entity copies

		Collection<File> oldFiles = copy.getFileCollection();
		if (oldFiles == null || oldFiles.isEmpty()) {
			copy.setFileCollection(null);
		} else {
			copy.setFileCollection(new HashSet<File>(oldFiles));
			for (File file : copy.getFileCollection()) {
				FileBean fileBean = new FileBean(file);
				fileBean.resetDomainCopy(createdBy, file);
			}
		}
	}
}
