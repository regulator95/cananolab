package gov.nih.nci.cananolab.restful.view.edit;

import gov.nih.nci.cananolab.domain.common.Instrument;
import gov.nih.nci.cananolab.dto.common.ExperimentConfigBean;
import gov.nih.nci.cananolab.dto.common.FileBean;
import gov.nih.nci.cananolab.dto.common.FindingBean;
import gov.nih.nci.cananolab.dto.common.PointOfContactBean;
import gov.nih.nci.cananolab.dto.common.ProtocolBean;
import gov.nih.nci.cananolab.dto.particle.characterization.CharacterizationBean;
import gov.nih.nci.cananolab.restful.core.InitSetup;
import gov.nih.nci.cananolab.restful.sample.InitCharacterizationSetup;
import gov.nih.nci.cananolab.restful.sample.InitSampleSetup;
import gov.nih.nci.cananolab.restful.util.CommonUtil;
import gov.nih.nci.cananolab.restful.view.characterization.properties.SimpleCharacterizationProperty;
import gov.nih.nci.cananolab.restful.view.characterization.properties.SimpleCytotoxicity;
import gov.nih.nci.cananolab.restful.view.characterization.properties.SimpleEnzymeInduction;
import gov.nih.nci.cananolab.restful.view.characterization.properties.SimplePhysicalState;
import gov.nih.nci.cananolab.restful.view.characterization.properties.SimpleShape;
import gov.nih.nci.cananolab.restful.view.characterization.properties.SimpleSolubility;
import gov.nih.nci.cananolab.restful.view.characterization.properties.SimpleSurface;
import gov.nih.nci.cananolab.restful.view.characterization.properties.SimpleTransfection;
import gov.nih.nci.cananolab.restful.view.characterization.properties.SimpleTargeting;
import gov.nih.nci.cananolab.security.enums.SecureClassesEnum;
import gov.nih.nci.cananolab.security.service.SpringSecurityAclService;
import gov.nih.nci.cananolab.service.protocol.ProtocolService;
import gov.nih.nci.cananolab.service.sample.CharacterizationService;
import gov.nih.nci.cananolab.service.sample.SampleService;
import gov.nih.nci.cananolab.util.Constants;
import gov.nih.nci.cananolab.util.DateUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class SimpleCharacterizationEditBean
{

	private Logger logger = Logger.getLogger(SimpleCharacterizationEditBean.class);

	String type;
	String name;
	long parentSampleId;

	long charId;	
	String assayType;

	long protocolId;
	long characterizationSourceId;
	Date characterizationDate;

	List<String> charNamesForCurrentType;

	SimpleCharacterizationProperty property = new SimpleCharacterizationProperty();

	String designMethodsDescription;

	SimpleTechniqueAndInstrument techniqueInstruments = new SimpleTechniqueAndInstrument();

	List<SimpleFindingBean> finding = new ArrayList<SimpleFindingBean>();

	String analysisConclusion;

	//When saving, could propagate to other samples
	List<String> selectedOtherSampleNames = new ArrayList<String>();
	boolean copyToOtherSamples;	
	boolean submitNewChar;

	List<String> charTypesLookup;
	//List<String> characterizationNameLookup;
	//List<String> AssayTypeLookup;
	List<SimpleProtocol> protocolLookup;
	List<SimplePOC> charSourceLookup;
	List<String> otherSampleNameLookup;
	List<String> datumConditionValueTypeLookup = new ArrayList<String>();

	List<String> assayTypesByCharNameLookup = new ArrayList<String>();

	List<String> errors = new ArrayList<String>();
	List<String> messages = new ArrayList<String>();

	SimpleFindingBean dirtyFindingBean;

	SimpleExperimentBean dirtyExperimentBean;



	public void setDirtyFindingBean(SimpleFindingBean dirtyFindingBean) {
		this.dirtyFindingBean = dirtyFindingBean;
	}

	public void setDirtyExperimentBean(SimpleExperimentBean dirtyExperimentBean) {
		this.dirtyExperimentBean = dirtyExperimentBean;
	}

	public void transferFromCharacterizationBean(HttpServletRequest request, 
			CharacterizationBean charBean, String sampleId, SampleService service, 
			CharacterizationService characterizationService, ProtocolService protocolService, SpringSecurityAclService springSecurityAclService) 
					throws Exception {

		//TODO: handle type=other than in the list
		this.type = charBean.getCharacterizationType();
		this.parentSampleId = Long.parseLong(sampleId);
		this.name = charBean.getCharacterizationName();
		this.analysisConclusion = charBean.getConclusion();

		setProtocolIdFromProtocolBean(charBean);
		setCharacterizationSourceIdFromPOCBean(charBean.getPocBean());

		transferCharBeanData(request, charBean, springSecurityAclService);

		setupLookups(request, charBean, sampleId, service, characterizationService, protocolService);
	}

	protected void setCharacterizationSourceIdFromPOCBean(PointOfContactBean pocBean) {
		if (pocBean == null || 
				pocBean.getDomain() == null || 
				pocBean.getDomain().getId() == null)
			return;

		this.characterizationSourceId = pocBean.getDomain().getId();
	}

	protected void setProtocolIdFromProtocolBean(CharacterizationBean charBean) {
		if (charBean == null || 
				charBean.getProtocolBean() == null || 
				charBean.getProtocolBean().getDomain() == null ||
				charBean.getProtocolBean().getDomain().getId() == null)
			return;

		this.protocolId = charBean.getProtocolBean().getDomain().getId();
	}

	protected void transferCharBeanData(HttpServletRequest request, CharacterizationBean charBean, SpringSecurityAclService springSecurityAclService) 
			throws Exception {
		if (charBean.getDomainChar() == null) 
			return;

		this.designMethodsDescription = charBean.getDomainChar().getDesignMethodsDescription();
		this.characterizationDate = charBean.getDomainChar().getDate();

		Long id = charBean.getDomainChar().getId();
		if (id != null) {
			this.charId = id.longValue();
			this.assayType = charBean.getAssayType();
		}

		transferExperimentConfigs(charBean.getExperimentConfigs());
		transferFinding(request, charBean.getFindings(), springSecurityAclService);

		transferProperty(request, charBean);

	}

	protected void transferToPropertyBean(CharacterizationBean charBean) 
			throws Exception {
		if (this.property == null) return;

		if (this.name.contains("physical"))
			((SimplePhysicalState)property).transferToPropertyBean(charBean);
		else if (name.contains("shape"))
			((SimpleShape)property).transferToPropertyBean(charBean);
		else if (name.contains("solubility"))
			((SimpleSolubility)property).transferToPropertyBean(charBean);
		else if (name.contains("surface"))
			((SimpleSurface)property).transferToPropertyBean(charBean);
		else if (name.contains("cytotoxicity"))
			((SimpleCytotoxicity)property).transferToPropertyBean(charBean);
		else if (name.contains("enzyme"))
			((SimpleEnzymeInduction)property).transferToPropertyBean(charBean);
		else if (name.contains("transfection"))
			((SimpleTransfection)property).transferToPropertyBean(charBean);
		else if (name.contains("targeting"))
			((SimpleTargeting)property).transferToPropertyBean(charBean);

	}

	protected void transferProperty(HttpServletRequest request, CharacterizationBean charBean) 
			throws Exception {
		if (!charBean.isWithProperties()) return;

		String charName = charBean.getCharacterizationName();
		property = getPropertyClassByCharName(charName);
		property.transferFromPropertyBean(request, charBean, true);
	}

	protected SimpleCharacterizationProperty getPropertyClassByCharName(String charName) 
			throws Exception {
		if (charName.contains("physical"))
			return new SimplePhysicalState();
		else if (charName.contains("shape"))
			return new SimpleShape();
		else if (charName.contains("solubility"))
			return new SimpleSolubility();
		else if (charName.contains("surface"))
			return new SimpleSurface();
		else if (charName.contains("cytotoxicity"))
			return new SimpleCytotoxicity();
		else if (charName.contains("enzyme"))
			return new SimpleEnzymeInduction();
		else if (charName.contains("transfection"))
			return new SimpleTransfection();
		else if (charName.contains("targeting"))
			return new SimpleTargeting();
		else 
			throw new Exception("Unknown charName: " + charName);

	}

	protected void transferFinding(HttpServletRequest request, List<FindingBean> findingBeans, SpringSecurityAclService springSecurityAclService) {
		if (findingBeans == null) return;
		
		String sampleId = (String) request.getSession().getAttribute("sampleId");

		for (FindingBean findingBean : findingBeans) {
			SimpleFindingBean simpleBean = new SimpleFindingBean();
			
			for (FileBean file : findingBean.getFiles())
			{
				boolean isPublic = springSecurityAclService.checkObjectPublic(Long.valueOf(sampleId), SecureClassesEnum.SAMPLE.getClazz());
				file.setPublicStatus(isPublic);
			}
			
			simpleBean.transferFromFindingBean(request, findingBean);


			//List<FileBean> files = findingBean.getFiles();

			this.finding.add(simpleBean);
		}
	}

	protected void transferExperimentConfigs(List<ExperimentConfigBean> expConfigs) {
		if (expConfigs == null) return;

		for (ExperimentConfigBean expConfig : expConfigs) {
			SimpleExperimentBean simpleExp = new SimpleExperimentBean();
			simpleExp.setDescription(expConfig.getDescription());
			simpleExp.setDisplayName(expConfig.getTechniqueDisplayName());

			if (expConfig.getDomain() != null && expConfig.getDomain().getTechnique() != null) {
				simpleExp.setTechniqueType(expConfig.getDomain().getTechnique().getType());
				simpleExp.setAbbreviation(expConfig.getDomain().getTechnique().getAbbreviation());
			}
			if (expConfig.getDomain() != null && expConfig.getDomain().getId() != null)
				simpleExp.setId(expConfig.getDomain().getId());

			List<Instrument> domainInsts = expConfig.getInstruments();
			if (domainInsts != null) {
				for (Instrument inst : domainInsts) {
					SimpleInstrumentBean simpleInst = new SimpleInstrumentBean();
					simpleInst.setManufacturer(inst.getManufacturer());
					simpleInst.setModelName(inst.getModelName());
					simpleInst.setType(inst.getType());

					simpleExp.getInstruments().add(simpleInst);
				}
			}

			this.techniqueInstruments.getExperiments().add(simpleExp);
		}
	}

	protected void setupLookups(HttpServletRequest request, CharacterizationBean charBean, String sampleId, SampleService service, CharacterizationService characterizationService, ProtocolService protocolService) 
			throws Exception {

		String charType = charBean.getCharacterizationType();

		charTypesLookup = InitCharacterizationSetup.getInstance().getCharacterizationTypes(request);
//		charTypesLookup.add("other");
		

		charNamesForCurrentType = new ArrayList<String>();
		SortedSet<String> charNames = InitCharacterizationSetup.getInstance().getCharNamesByCharType(request, charType, characterizationService);
		charNamesForCurrentType.addAll(charNames);
		charNamesForCurrentType.add("other");
		//Check char types and add appropriate other
        switch (charType) {
            case "physico-chemical characterization":
                charNamesForCurrentType.add("other_pc");
                break;
            case "in vivo characterization":
                charNamesForCurrentType.add("other_vv");
                break;
            case "in vitro characterization":
                charNamesForCurrentType.add("other_vt");
                break;
            case "ex vivo":
                charNamesForCurrentType.add("other_ex_vv");
                break;
        }
		

		setProtocolLookup(request, charType, protocolService);
		setPOCLookup(request, sampleId, service);
		otherSampleNameLookup = InitSampleSetup.getInstance().getOtherSampleNames(request, sampleId, service);

		SortedSet<String> valueTypes = (SortedSet<String>)request.getSession().getAttribute("datumConditionValueTypes");
		if (valueTypes != null) 
			this.datumConditionValueTypeLookup.addAll(valueTypes);
		CommonUtil.addOtherToList(this.datumConditionValueTypeLookup);

		this.techniqueInstruments.setupLookups(request);

		if (this.name != null && this.name.length() > 0) {
			SortedSet<String> assayTypes = InitSetup.getInstance().getDefaultAndOtherTypesByLookup(
					request, "charNameAssays",
					this.name, "assayType", "otherAssayType", true);

			this.assayTypesByCharNameLookup.addAll(assayTypes);
			CommonUtil.addOtherToList(this.assayTypesByCharNameLookup);
		}
	}

	protected void setPOCLookup(HttpServletRequest request, String sampleId, SampleService service) 
			throws Exception {
		charSourceLookup = new ArrayList<SimplePOC>();
		List<PointOfContactBean> pocs = service.findPointOfContactsBySampleId(sampleId);

		if (pocs == null) return;

		for (PointOfContactBean poc : pocs) {
			SimplePOC simplePOC = new SimplePOC();
			simplePOC.transferFromPointOfContactBean(poc);
			charSourceLookup.add(simplePOC);
		}
	}

	protected void setProtocolLookup(HttpServletRequest request, String charType, ProtocolService protocolService) 
			throws Exception {
		protocolLookup = new ArrayList<SimpleProtocol>();
		List<ProtocolBean> protoBeans = protocolService.getProtocolsByChar(request, charType);

		if (protoBeans == null)
			return;

		for (ProtocolBean protoBean : protoBeans) {
			SimpleProtocol simpleProto = new SimpleProtocol();
			simpleProto.transferFromProtocolBean(protoBean);
			protocolLookup.add(simpleProto);
		}
	}

	/**
	 * Currently it only transfter what's needed in "submit" / "update"
     */
	public CharacterizationBean transferToCharacterizationBean(CharacterizationBean achar) 
			throws Exception {
		if (achar == null) 
			achar = new CharacterizationBean();

		achar.setCharacterizationName(this.name);
		achar.setCharacterizationType(this.type);

		achar.setAssayType(assayType);

		if (this.characterizationDate != null) {
			String dateString = DateUtils.convertDateToString(this.characterizationDate, Constants.DEFAULT_DATE_FORMAT);
			achar.setDateString(dateString);
		}

		//Protocol
		//Use id to find matching one in lookup list
		//then transfer to a bean
		transferToProtocolBean(achar, this.protocolId);

		//POC
		transferToPOCBean(achar, this.characterizationSourceId);

		//char date
		if (achar.getDomainChar() != null) {
			achar.getDomainChar().setDate(characterizationDate);
		}

		achar.setDescription(this.designMethodsDescription);
		achar.setConclusion(this.analysisConclusion);

		transferToPropertyBean(achar);

		return achar;
	}

	protected void transferToPOCBean(CharacterizationBean charBean, long selectedId) {
		if (selectedId == 0) {//user didn't select one or selected empty, which is ok
			charBean.setPocBean(new PointOfContactBean());
			return;
		}

		SimplePOC selected = null;
		for (SimplePOC simplepoc : this.charSourceLookup) {
			if (selectedId == simplepoc.getId()) {
				selected = simplepoc;
				break;
			}
		}

		if (selected == null) { 
			logger.error("User selected Characterization Source doesn't have a match in lookup list. This should not happen");
			return;
		}

		PointOfContactBean pocBean = charBean.getPocBean();
		Long id = pocBean.getDomain().getId();
		if (id != null && selectedId == id.longValue())
			return; //nothing changed. 

		pocBean	= new PointOfContactBean();
		pocBean.getDomain().setId(selectedId);
		charBean.setPocBean(pocBean);
	}

	protected void transferToProtocolBean(CharacterizationBean charBean, long selectedProtoId) {

		if (selectedProtoId == 0) { //user didn't select one or selected empty, which is ok
			charBean.setProtocolBean(new ProtocolBean());
			return; 
		}

		SimpleProtocol selected = null;
		for (SimpleProtocol simpleProto : this.protocolLookup) {
			if (selectedProtoId == simpleProto.getDomainId()) {
				selected = simpleProto;
				break;
			}
		}

		if (selected == null) {
			logger.error("User selected protocol doesn't have a match in lookup list. This should not happen");
			return;
		}

		ProtocolBean protoBean = charBean.getProtocolBean();
		Long id = protoBean.getDomain().getId();
		if (id != null && selectedProtoId == id.longValue()) {
			return; //proto didn't change. nothing to do
		} 

		protoBean = new ProtocolBean(); //user selected a different one
		protoBean.getDomain().setId(selected.domainId);
		FileBean fileBean = protoBean.getFileBean();
		fileBean.getDomainFile().setId(selected.domainFileId);
		fileBean.getDomainFile().setUri(selected.domainFileUri);

		charBean.setProtocolBean(protoBean);
	}

	public SimpleFindingBean getDirtyFindingBean() {
		for (SimpleFindingBean simplefinding : this.finding) {
			if (simplefinding.isDirty())
				return simplefinding;
		}

		return null;
	}

	public SimpleExperimentBean getDirtyExperimentBean() {
		List<SimpleExperimentBean> expConfigs = this.techniqueInstruments.getExperiments();

		for (SimpleExperimentBean simpleExp : expConfigs) {
			if (simpleExp.isDirty())
				return simpleExp;
		}

		return null;
	}


	public String getAnalysisConclusion() {
		return analysisConclusion;
	}

	public void setAnalysisConclusion(String analysisConclusion) {
		this.analysisConclusion = analysisConclusion;
	}

	public long getCharacterizationSourceId() {
		return characterizationSourceId;
	}

	public void setCharacterizationSourceId(long characterizationSourceId) {
		this.characterizationSourceId = characterizationSourceId;
	}

	public List<SimpleFindingBean> getFinding() {
		return finding;
	}

	public void setFinding(List<SimpleFindingBean> finding) {
		this.finding = finding;
	}

	public long getProtocolId() {
		return protocolId;
	}

	public void setProtocolId(long protocolId) {
		this.protocolId = protocolId;
	}

	public List<String> getMessages() {
		return messages;
	}

	public void setMessages(List<String> messages) {
		this.messages = messages;
	}

	public boolean isCopyToOtherSamples() {
		return copyToOtherSamples;
	}

	public void setCopyToOtherSamples(boolean copyToOtherSamples) {
		this.copyToOtherSamples = copyToOtherSamples;
	}

	public List<String> getSelectedOtherSampleNames() {
		return selectedOtherSampleNames;
	}

	public void setSelectedOtherSampleNames(List<String> selectedOtherSampleNames) {
		this.selectedOtherSampleNames = selectedOtherSampleNames;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}

	public long getCharId() {
		return charId;
	}

	public void setCharId(long charId) {
		this.charId = charId;
	}

	public List<SimpleProtocol> getProtocolLookup() {
		return protocolLookup;
	}

	public void setProtocolLookup(List<SimpleProtocol> protocolLookup) {
		this.protocolLookup = protocolLookup;
	}

	public List<String> getOtherSampleNameLookup() {
		return otherSampleNameLookup;
	}

	public void setOtherSampleNameLookup(List<String> otherSampleNameLookup) {
		this.otherSampleNameLookup = otherSampleNameLookup;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAssayType() {
		return assayType;
	}
	public void setAssayType(String assayType) {
		this.assayType = assayType;
	}

	public Date getCharacterizationDate() {
		return characterizationDate;
	}
	public void setCharacterizationDate(Date characterizationDate) {
		this.characterizationDate = characterizationDate;
	}
	public long getParentSampleId() {
		return parentSampleId;
	}
	public void setParentSampleId(long parentSampleId) {
		this.parentSampleId = parentSampleId;
	}
	public List<String> getCharTypesLookup() {
		return charTypesLookup;
	}
	public void setCharTypesLookup(List<String> charTypesLookup) {
		this.charTypesLookup = charTypesLookup;
	}

	//	public List<String> getAssayTypeLookup() {
	//		return AssayTypeLookup;
	//	}
	//	public void setAssayTypeLookup(List<String> assayTypeLookup) {
	//		AssayTypeLookup = assayTypeLookup;
	//	}	



	public String getDesignMethodsDescription() {
		return designMethodsDescription;
	}



	public List<String> getCharNamesForCurrentType() {
		return charNamesForCurrentType;
	}


	public void setCharNamesForCurrentType(List<String> charNamesForCurrentType) {
		this.charNamesForCurrentType = charNamesForCurrentType;
	}



	public void setDesignMethodsDescription(String designMethodsDescription) {
		this.designMethodsDescription = designMethodsDescription;
	}

	public List<SimplePOC> getCharSourceLookup() {
		return charSourceLookup;
	}

	public void setCharSourceLookup(List<SimplePOC> charSourceLookup) {
		this.charSourceLookup = charSourceLookup;
	}

	public SimpleTechniqueAndInstrument getTechniqueInstruments() {
		return techniqueInstruments;
	}

	public void setTechniqueInstruments(
			SimpleTechniqueAndInstrument techniqueInstruments) {
		this.techniqueInstruments = techniqueInstruments;
	}

	public List<String> getAssayTypesByCharNameLookup() {
		return assayTypesByCharNameLookup;
	}

	public void setAssayTypesByCharNameLookup(
			List<String> assayTypesByCharNameLookup) {
		this.assayTypesByCharNameLookup = assayTypesByCharNameLookup;
	}


	public List<String> getDatumConditionValueTypeLookup() {
		return datumConditionValueTypeLookup;
	}

	public void setDatumConditionValueTypeLookup(
			List<String> datumConditionValueTypeLookup) {
		this.datumConditionValueTypeLookup = datumConditionValueTypeLookup;
	}

	public boolean isSubmitNewChar() {
		return submitNewChar;
	}

	public void setSubmitNewChar(boolean submitNewChar) {
		this.submitNewChar = submitNewChar;
	}

	public SimpleCharacterizationProperty getProperty() {
		return property;
	}

	public void setProperty(SimpleCharacterizationProperty property) {
		this.property = property;
	}

    @Override
    public String toString()
    {
        return "{\"SimpleCharacterizationEditBean\":{"
                + "                        \"type\":\"" + type + "\""
                + ",                         \"name\":\"" + name + "\""
                + ",                         \"parentSampleId\":\"" + parentSampleId + "\""
                + ",                         \"charId\":\"" + charId + "\""
                + ",                         \"assayType\":\"" + assayType + "\""
                + ",                         \"protocolId\":\"" + protocolId + "\""
                + ",                         \"characterizationSourceId\":\"" + characterizationSourceId + "\""
                + ",                         \"characterizationDate\":" + characterizationDate
                + ",                         \"charNamesForCurrentType\":" + charNamesForCurrentType
                + ",                         \"property\":" + property
                + ",                         \"designMethodsDescription\":\"" + designMethodsDescription + "\""
                + ",                         \"techniqueInstruments\":" + techniqueInstruments
                + ",                         \"finding\":" + finding
                + ",                         \"analysisConclusion\":\"" + analysisConclusion + "\""
                + ",                         \"selectedOtherSampleNames\":" + selectedOtherSampleNames
                + ",                         \"copyToOtherSamples\":\"" + copyToOtherSamples + "\""
                + ",                         \"submitNewChar\":\"" + submitNewChar + "\""
                + ",                         \"charTypesLookup\":" + charTypesLookup
                + ",                         \"protocolLookup\":" + protocolLookup
                + ",                         \"charSourceLookup\":" + charSourceLookup
                + ",                         \"otherSampleNameLookup\":" + otherSampleNameLookup
                + ",                         \"datumConditionValueTypeLookup\":" + datumConditionValueTypeLookup
                + ",                         \"assayTypesByCharNameLookup\":" + assayTypesByCharNameLookup
                + ",                         \"errors\":" + errors
                + ",                         \"messages\":" + messages
                + ",                         \"dirtyFindingBean\":" + dirtyFindingBean
                + ",                         \"dirtyExperimentBean\":" + dirtyExperimentBean
                + "}}";
    }
}
