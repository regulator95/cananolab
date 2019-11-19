/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.dto.particle.composition;

import gov.nih.nci.cananolab.domain.common.File;
import gov.nih.nci.cananolab.domain.nanomaterial.Biopolymer;
import gov.nih.nci.cananolab.domain.nanomaterial.CarbonNanotube;
import gov.nih.nci.cananolab.domain.nanomaterial.Dendrimer;
import gov.nih.nci.cananolab.domain.nanomaterial.Emulsion;
import gov.nih.nci.cananolab.domain.nanomaterial.Fullerene;
import gov.nih.nci.cananolab.domain.nanomaterial.Liposome;
import gov.nih.nci.cananolab.domain.nanomaterial.OtherNanomaterialEntity;
import gov.nih.nci.cananolab.domain.nanomaterial.Polymer;
import gov.nih.nci.cananolab.domain.particle.ComposingElement;
import gov.nih.nci.cananolab.domain.particle.Function;
import gov.nih.nci.cananolab.domain.particle.NanomaterialEntity;
import gov.nih.nci.cananolab.dto.common.FileBean;
import gov.nih.nci.cananolab.util.ClassUtils;
import gov.nih.nci.cananolab.util.Comparators;
import gov.nih.nci.cananolab.util.Constants;
import gov.nih.nci.cananolab.util.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Represents the view bean for the NanomaterialEntity domain object
 *
 * @author pansu
 *
 */
public class NanomaterialEntityBean extends BaseCompositionEntityBean {
	private Polymer polymer = new Polymer();

	private Biopolymer biopolymer = new Biopolymer();

	private Dendrimer dendrimer = new Dendrimer();

	private CarbonNanotube carbonNanotube = new CarbonNanotube();

	private Liposome liposome = new Liposome();

	private Emulsion emulsion = new Emulsion();

	private Fullerene fullerene = new Fullerene();

	private List<ComposingElementBean> composingElements = new ArrayList<ComposingElementBean>();

	private NanomaterialEntity domainEntity;

	private boolean withProperties = false;

	private ComposingElementBean theComposingElement = new ComposingElementBean();

	private String isPolymerized;

	private String isCrossLinked;

	public NanomaterialEntityBean() {
	}

	public NanomaterialEntityBean(NanomaterialEntity nanomaterialEntity) {
		description = nanomaterialEntity.getDescription();
		domainEntity = nanomaterialEntity;
		if (domainEntity instanceof Biopolymer) {
			biopolymer = (Biopolymer) domainEntity;
			withProperties = true;
		} else if (domainEntity instanceof Dendrimer) {
			dendrimer = (Dendrimer) domainEntity;
			withProperties = true;
		} else if (domainEntity instanceof CarbonNanotube) {
			carbonNanotube = (CarbonNanotube) domainEntity;
			withProperties = true;
		} else if (domainEntity instanceof Liposome) {
			liposome = (Liposome) domainEntity;
			withProperties = true;
			if (liposome.getPolymerized() != null) {
				this.isPolymerized = liposome.getPolymerized().toString();
			}
		} else if (domainEntity instanceof Emulsion) {
			emulsion = (Emulsion) domainEntity;
			if (emulsion.getPolymerized() != null) {
				this.isPolymerized = emulsion.getPolymerized().toString();
			}
			withProperties = true;
		} else if (domainEntity instanceof Polymer) {
			polymer = (Polymer) domainEntity;
			withProperties = true;
			if (polymer.getCrossLinked() != null) {
				this.isCrossLinked = polymer.getCrossLinked().toString();
			}
		} else if (domainEntity instanceof Fullerene) {
			fullerene = (Fullerene) domainEntity;
			withProperties = true;
		}
		className = ClassUtils.getShortClassName(nanomaterialEntity.getClass()
				.getName());
		if (nanomaterialEntity.getComposingElementCollection() != null) {
			for (ComposingElement composingElement : nanomaterialEntity
					.getComposingElementCollection()) {
				composingElements
						.add(new ComposingElementBean(composingElement));
			}
			Collections.sort(composingElements,
					new Comparators.ComposingElementBeanDateComparator());
		}
		if (nanomaterialEntity.getFileCollection() != null) {
			for (File file : nanomaterialEntity.getFileCollection()) {
				files.add(new FileBean(file));
			}
			Collections.sort(files, new Comparators.FileBeanDateComparator());
		}
		updateType();
	}

	public ComposingElementBean getComposingElementById(String composingElementId) {
		for(ComposingElementBean elementBean:composingElements){
			if (elementBean.getDomainId().equals(composingElementId)){
				return elementBean;
			}
		}
		return null;
	}

	public NanomaterialEntity getDomainCopy(String createdBy) {
		NanomaterialEntity copy = (NanomaterialEntity) ClassUtils.deepCopy(this
				.getDomainEntity());
		resetDomainCopy(createdBy, copy);
		return copy;
	}

	public void resetDomainCopy(String createdBy, NanomaterialEntity copy) {
		copy.setId(null);
		copy.setCreatedBy(createdBy + ":"
				+ Constants.AUTO_COPY_ANNOTATION_PREFIX);
		Collection<ComposingElement> oldComposingElements = copy
				.getComposingElementCollection();
		if (oldComposingElements == null || oldComposingElements.isEmpty()) {
			copy.setComposingElementCollection(null);
		} else {
			// have to create a new collection otherwise Hibernate complains
			copy.setComposingElementCollection(new HashSet<ComposingElement>(
					oldComposingElements));
			for (ComposingElement ce : copy.getComposingElementCollection()) {
				// append original ID in created by to aid in chemical
				// association copy
				ce.setCreatedBy(createdBy + ":"
						+ Constants.AUTO_COPY_ANNOTATION_PREFIX + ":"
						+ ce.getId());
				ce.setId(null);
				Collection<Function> oldFunctions = ce
						.getInherentFunctionCollection();
				if (oldFunctions == null || oldFunctions.isEmpty()) {
					ce.setInherentFunctionCollection(null);
				} else {
					ce.setInherentFunctionCollection(new HashSet<Function>(
							oldFunctions));
					for (Function function : ce.getInherentFunctionCollection()) {
						FunctionBean functionBean = new FunctionBean(function);
						functionBean.resetDomainCopy(createdBy, function);
					}
				}
			}
		}

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

	public Dendrimer getDendrimer() {
		return dendrimer;
	}

	public Biopolymer getBiopolymer() {
		return biopolymer;
	}

	public CarbonNanotube getCarbonNanotube() {
		return carbonNanotube;
	}

	public List<ComposingElementBean> getComposingElements() {
		return composingElements;
	}
	
	public void setComposingElements(List<ComposingElementBean> composingElements) {
		this.composingElements= composingElements;
	}

	public Emulsion getEmulsion() {
		return emulsion;
	}

	public Fullerene getFullerene() {
		return fullerene;
	}

	public Liposome getLiposome() {
		return liposome;
	}

	public Polymer getPolymer() {
		return polymer;
	}

	public NanomaterialEntity getDomainEntity() {
		return domainEntity;
	}
	public void setDomainEntity(NanomaterialEntity nanoEntity) {
		this.domainEntity = nanoEntity;
	}

	public void setPolymer(Polymer polymer) {
		this.polymer = polymer;
	}

	public void setBiopolymer(Biopolymer biopolymer) {
		this.biopolymer = biopolymer;
	}

	public void setDendrimer(Dendrimer dendrimer) {
		this.dendrimer = dendrimer;
	}

	public void setCarbonNanotube(CarbonNanotube carbonNanotube) {
		this.carbonNanotube = carbonNanotube;
	}

	public void setLiposome(Liposome liposome) {
		this.liposome = liposome;
	}

	public void setEmulsion(Emulsion emulsion) {
		this.emulsion = emulsion;
	}

	public void setFullerene(Fullerene fullerene) {
		this.fullerene = fullerene;
	}

	public void setupDomainEntity(String createdBy) throws Exception {
		className = ClassUtils.getShortClassNameFromDisplayName(type);
		Class clazz = ClassUtils.getFullClass("nanomaterial." + className);
		if (clazz == null) {
			clazz = OtherNanomaterialEntity.class;
		}
		if (domainEntity == null) {
			domainEntity = (NanomaterialEntity) clazz.newInstance();
		}
		this.updateEmptyFieldsToNull();
		if (domainEntity instanceof OtherNanomaterialEntity) {
			((OtherNanomaterialEntity) domainEntity).setType(type);
		} else if (domainEntity instanceof Biopolymer) {
			domainEntity = biopolymer;
		} else if (domainEntity instanceof Dendrimer) {
			domainEntity = dendrimer;
		} else if (domainEntity instanceof CarbonNanotube) {
			domainEntity = carbonNanotube;
		} else if (domainEntity instanceof Liposome) {
			domainEntity = liposome;
		} else if (domainEntity instanceof Emulsion) {
			domainEntity = emulsion;
		} else if (domainEntity instanceof Polymer) {
			domainEntity = polymer;
		} else if (domainEntity instanceof Fullerene) {
			domainEntity = fullerene;
		}
		// updated created_date and created_by if id is null
		if (domainEntity.getId() == null) {
			domainEntity.setCreatedBy(createdBy);
			domainEntity.setCreatedDate(Calendar.getInstance().getTime());
		}
		// updated created_by if created_by contains copy, but keep the original
		// created_date
		if (domainEntity.getId() != null
				|| !StringUtils.isEmpty(domainEntity.getCreatedBy())
				&& domainEntity.getCreatedBy().contains(
						Constants.AUTO_COPY_ANNOTATION_PREFIX)) {
			domainEntity.setCreatedBy(createdBy);
			domainEntity.setCreatedDate(Calendar.getInstance().getTime());
		}
		domainEntity.setDescription(description);
		if (domainEntity.getComposingElementCollection() != null) {
			domainEntity.getComposingElementCollection().clear();
		} else {
			domainEntity
					.setComposingElementCollection(new HashSet<ComposingElement>());
		}
		for (ComposingElementBean composingElementBean : composingElements) {
			domainEntity.getComposingElementCollection().add(
					composingElementBean.getDomain());
			composingElementBean.getDomain()
					.setNanomaterialEntity(domainEntity);
		}
		if (files.isEmpty()) {
			domainEntity.setFileCollection(null);
		} else if (domainEntity.getFileCollection() != null) {
			domainEntity.getFileCollection().clear();
		} else {
			domainEntity.setFileCollection(new HashSet<File>());
		}
		for (FileBean file : files) {
			domainEntity.getFileCollection().add(file.getDomainFile());
		}
	}

	public void addComposingElement(ComposingElementBean element) {
		// if an old one exists, remove it first
		int index = composingElements.indexOf(element);
		if (index != -1) {
			composingElements.remove(element);
			// retain the original order
			composingElements.add(index, element);
		} else {
			composingElements.add(element);
		}
	}

	public void removeComposingElement(ComposingElementBean element) {
		composingElements.remove(element);
	}

	private void updateType() {
		if (domainEntity instanceof OtherNanomaterialEntity) {
			type = ((OtherNanomaterialEntity) domainEntity).getType();
		} else {
			type = ClassUtils.getDisplayName(className);
		}
	}

	public boolean isWithProperties() {
		return withProperties;
	}

	public ComposingElementBean getTheComposingElement() {
		return theComposingElement;
	}

	public void setTheComposingElement(ComposingElementBean theComposingElement) {
		this.theComposingElement = theComposingElement;
	}

	// used for DWR ajax in bodySubmitChemicalAssociation.jsp
	public String getDomainId() {
		if (getDomainEntity().getId() != null) {
			return getDomainEntity().getId().toString();
		} else {
			return null;
		}
	}

	// used for DWR ajax
	public String getDisplayName() {
		return type;
	}

	public String getIsPolymerized() {
		return isPolymerized;
	}

	public void setIsPolymerized(String isPolymerized) {
		this.isPolymerized = isPolymerized;
	}

	public String getIsCrossLinked() {
		return isCrossLinked;
	}

	public void setIsCrossLinked(String isCrossLinked) {
		this.isCrossLinked = isCrossLinked;
	}

	public void updateEmptyFieldsToNull() {
		if (theComposingElement.getDomain().getPubChemId() != null
				&& theComposingElement.getDomain().getPubChemId() == 0) {
			theComposingElement.getDomain().setPubChemId(null);
		}
		if (theComposingElement.getDomain().getValue() != null
				&& theComposingElement.getDomain().getValue() == 0) {
			theComposingElement.getDomain().setValue(null);
		}
		if (liposome != null) {
			if (StringUtils.isEmpty(isPolymerized)) {
				liposome.setPolymerized(null);
			} else {
				liposome.setPolymerized(Boolean.valueOf(isPolymerized));
			}
		}
		if (emulsion != null) {
			if (StringUtils.isEmpty(isPolymerized)) {
				emulsion.setPolymerized(null);
			} else {
				emulsion.setPolymerized(Boolean.valueOf(isPolymerized));
			}
		}
		if (dendrimer != null && dendrimer.getGeneration() != null
				&& dendrimer.getGeneration() == 0) {
			dendrimer.setGeneration(null);
		}
		if (carbonNanotube != null) {
			if (carbonNanotube.getAverageLength() != null
					&& carbonNanotube.getAverageLength() == 0) {
				carbonNanotube.setAverageLength(null);
			}
			if (carbonNanotube.getDiameter() != null
					&& carbonNanotube.getDiameter() == 0) {
				carbonNanotube.setDiameter(null);
			}
		}
		if (fullerene != null) {
			if (fullerene.getAverageDiameter() != null
					&& fullerene.getAverageDiameter() == 0) {
				fullerene.setAverageDiameter(null);
			}
			if (fullerene.getNumberOfCarbon() != null
					&& fullerene.getNumberOfCarbon() == 0) {
				fullerene.setNumberOfCarbon(null);
			}
		}
		if (polymer != null) {
			if (polymer.getCrossLinkDegree() != null
					&& polymer.getCrossLinkDegree() == 0) {
				polymer.setCrossLinkDegree(null);
			}
			if (StringUtils.isEmpty(isCrossLinked)) {
				polymer.setCrossLinked(null);
			} else {
				polymer.setCrossLinked(Boolean.valueOf(isCrossLinked));
			}
		}
	}
}