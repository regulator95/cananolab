package gov.nih.nci.cananolab.restful.view.edit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import gov.nih.nci.cananolab.dto.common.FileBean;
import gov.nih.nci.cananolab.dto.particle.composition.ChemicalAssociationBean;
import gov.nih.nci.cananolab.security.enums.SecureClassesEnum;
import gov.nih.nci.cananolab.security.service.SpringSecurityAclService;

public class SimpleChemicalAssociationBean {

	SimpleFileBean simpleFile;
	
	String type = "";
	String bondType = "";
	String description = "";
	SimpleAssociatedElement associatedElementA;
	SimpleAssociatedElement associatedElementB;
	String sampleId = "";
	List<String> errors;
	List<SimpleFileBean> files;
	Long associationId = 0L;
	String createdBy = "";
	Date createdDate;
	
	public Long getAssociationId() {
		return associationId;
	}
	public void setAssociationId(Long associationId) {
		this.associationId = associationId;
	}
	public String getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	public List<SimpleFileBean> getFiles() {
		return files;
	}
	public void setFiles(List<SimpleFileBean> files) {
		this.files = files;
	}
	public String getSampleId() {
		return sampleId;
	}
	public void setSampleId(String sampleId) {
		this.sampleId = sampleId;
	}
	public List<String> getErrors() {
		return errors;
	}
	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
	public SimpleFileBean getSimpleFile() {
		return simpleFile;
	}
	public void setSimpleFile(SimpleFileBean simpleFile) {
		this.simpleFile = simpleFile;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getBondType() {
		return bondType;
	}
	public void setBondType(String bondType) {
		this.bondType = bondType;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public SimpleAssociatedElement getAssociatedElementA() {
		return associatedElementA;
	}
	public void setAssociatedElementA(SimpleAssociatedElement associatedElementA) {
		this.associatedElementA = associatedElementA;
	}
	public SimpleAssociatedElement getAssociatedElementB() {
		return associatedElementB;
	}
	public void setAssociatedElementB(SimpleAssociatedElement associatedElementB) {
		this.associatedElementB = associatedElementB;
	}
	
	public void trasferToSimpleChemicalAssociation(ChemicalAssociationBean chemBean, HttpServletRequest request, SpringSecurityAclService springSecurityAclService){
		
		this.setType(chemBean.getType());
		this.setDescription(chemBean.getDescription());
		this.setBondType(chemBean.getAttachment().getBondType());
		this.setSampleId((String) request.getSession().getAttribute("sampleId"));
		this.setCreatedBy(chemBean.getDomainAssociation().getCreatedBy());
		this.setCreatedDate(chemBean.getDomainAssociation().getCreatedDate());
		this.setAssociationId(chemBean.getDomainAssociation().getId());
		associatedElementA = new SimpleAssociatedElement();
		
		associatedElementA.setClassName(chemBean.getAssociatedElementA().getClassName());
		associatedElementA.setCompositionType(chemBean.getAssociatedElementA().getCompositionType());
		associatedElementA.setEntityDisplayName(chemBean.getAssociatedElementA().getEntityDisplayName());
		associatedElementA.setEntityId(chemBean.getAssociatedElementA().getEntityId());
		
		SimpleComposingElementBean comp = new SimpleComposingElementBean();
		comp.setId(chemBean.getAssociatedElementA().getComposingElement().getId());
		comp.setPubChemDataSourceName(chemBean.getAssociatedElementA().getComposingElement().getPubChemDataSourceName());
		comp.setPubChemId(chemBean.getAssociatedElementA().getComposingElement().getPubChemId());
		comp.setDescription(chemBean.getAssociatedElementA().getComposingElement().getDescription());
		comp.setCreatedBy(chemBean.getAssociatedElementA().getComposingElement().getCreatedBy());
		comp.setMolecularFormula(chemBean.getAssociatedElementA().getComposingElement().getMolecularFormula());
		comp.setMolecularFormulaType(chemBean.getAssociatedElementA().getComposingElement().getMolecularFormulaType());
		comp.setName(chemBean.getAssociatedElementA().getComposingElement().getName());
		comp.setType(chemBean.getAssociatedElementA().getComposingElement().getType());
		comp.setValue(chemBean.getAssociatedElementA().getComposingElement().getValue());
		comp.setValueUnit(chemBean.getAssociatedElementA().getComposingElement().getValueUnit());
		comp.setId(chemBean.getAssociatedElementA().getComposingElement().getId());
		
		associatedElementA.setComposingElement(comp);
		this.setAssociatedElementA(associatedElementA);
		
		associatedElementB = new SimpleAssociatedElement();
		
		associatedElementB.setClassName(chemBean.getAssociatedElementB().getClassName());
		associatedElementB.setCompositionType(chemBean.getAssociatedElementB().getCompositionType());
		associatedElementB.setEntityDisplayName(chemBean.getAssociatedElementB().getEntityDisplayName());
		associatedElementB.setEntityId(chemBean.getAssociatedElementB().getEntityId());
		
		comp = new SimpleComposingElementBean();
		comp.setId(chemBean.getAssociatedElementB().getComposingElement().getId());
		comp.setPubChemDataSourceName(chemBean.getAssociatedElementB().getComposingElement().getPubChemDataSourceName());
		comp.setPubChemId(chemBean.getAssociatedElementB().getComposingElement().getPubChemId());
		comp.setDescription(chemBean.getAssociatedElementB().getComposingElement().getDescription());
		comp.setCreatedBy(chemBean.getAssociatedElementB().getComposingElement().getCreatedBy());
		comp.setMolecularFormula(chemBean.getAssociatedElementB().getComposingElement().getMolecularFormula());
		comp.setMolecularFormulaType(chemBean.getAssociatedElementB().getComposingElement().getMolecularFormulaType());
		comp.setName(chemBean.getAssociatedElementB().getComposingElement().getName());
		comp.setType(chemBean.getAssociatedElementB().getComposingElement().getType());
		comp.setValue(chemBean.getAssociatedElementB().getComposingElement().getValue());
		comp.setValueUnit(chemBean.getAssociatedElementB().getComposingElement().getValueUnit());
		comp.setId(chemBean.getAssociatedElementB().getComposingElement().getId());
		
		this.setAssociatedElementB(associatedElementB);

		files = new ArrayList<SimpleFileBean>();
		associatedElementB.setComposingElement(comp);
		for(FileBean fileBean : chemBean.getFiles()){
			SimpleFileBean simpleBean = new SimpleFileBean();
			simpleBean.setDescription(fileBean.getDescription());
			simpleBean.setId(fileBean.getDomainFile().getId());
			simpleBean.setKeywordsStr(fileBean.getKeywordsStr());
			simpleBean.setTitle(fileBean.getDomainFile().getTitle());
			simpleBean.setType(fileBean.getDomainFile().getType());
			simpleBean.setUri(fileBean.getDomainFile().getUri());
			simpleBean.setUriExternal(fileBean.getDomainFile().getUriExternal());
			simpleBean.setExternalUrl(fileBean.getExternalUrl());
			simpleBean.setSampleId((String) request.getSession().getAttribute("sampleId"));
			simpleBean.setCreatedBy(fileBean.getDomainFile().getCreatedBy());
			simpleBean.setCreatedDate(fileBean.getDomainFile().getCreatedDate());
			simpleBean.setTheAccess(fileBean.getTheAccess());
			//simpleBean.setIsPublic(fileBean.getPublicStatus());
			boolean isPublic = springSecurityAclService.checkObjectPublic(Long.parseLong(simpleBean.getSampleId()), SecureClassesEnum.SAMPLE.getClazz());
			simpleBean.setIsPublic(isPublic);
			files.add(simpleBean);
		}
		this.setFiles(files);
	}
}
