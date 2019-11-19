/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.restful.sample;

import gov.nih.nci.cananolab.domain.common.File;
import gov.nih.nci.cananolab.dto.common.ColumnHeader;
import gov.nih.nci.cananolab.dto.common.ExperimentConfigBean;
import gov.nih.nci.cananolab.dto.common.FileBean;
import gov.nih.nci.cananolab.dto.common.FindingBean;
import gov.nih.nci.cananolab.dto.common.Row;
import gov.nih.nci.cananolab.dto.common.table.TableCell;
import gov.nih.nci.cananolab.dto.particle.SampleBean;
import gov.nih.nci.cananolab.dto.particle.characterization.CharacterizationBean;
import gov.nih.nci.cananolab.dto.particle.characterization.CharacterizationSummaryViewBean;
import gov.nih.nci.cananolab.restful.core.BaseAnnotationBO;
import gov.nih.nci.cananolab.restful.util.PropertyUtil;
import gov.nih.nci.cananolab.restful.view.SimpleCharacterizationSummaryViewBean;
import gov.nih.nci.cananolab.restful.view.SimpleCharacterizationUnitBean;
import gov.nih.nci.cananolab.restful.view.SimpleCharacterizationsByTypeBean;
import gov.nih.nci.cananolab.restful.view.edit.SimpleCell;
import gov.nih.nci.cananolab.restful.view.edit.SimpleCharacterizationEditBean;
import gov.nih.nci.cananolab.restful.view.edit.SimpleCharacterizationSummaryEditBean;
import gov.nih.nci.cananolab.restful.view.edit.SimpleExperimentBean;
import gov.nih.nci.cananolab.restful.view.edit.SimpleFileBean;
import gov.nih.nci.cananolab.restful.view.edit.SimpleFindingBean;
import gov.nih.nci.cananolab.restful.view.edit.SimpleRowBean;
import gov.nih.nci.cananolab.security.enums.SecureClassesEnum;
import gov.nih.nci.cananolab.security.service.SpringSecurityAclService;
import gov.nih.nci.cananolab.security.utils.SpringSecurityUtil;
import gov.nih.nci.cananolab.service.curation.CurationService;
import gov.nih.nci.cananolab.service.protocol.ProtocolService;
import gov.nih.nci.cananolab.service.sample.CharacterizationService;
import gov.nih.nci.cananolab.service.sample.SampleService;
import gov.nih.nci.cananolab.service.sample.exporter.CharacterizationExporter;
import gov.nih.nci.cananolab.util.Constants;
import gov.nih.nci.cananolab.util.DateUtils;
import gov.nih.nci.cananolab.util.ExportUtils;
import gov.nih.nci.cananolab.util.StringUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base action for characterization actions
 * 
 * @author pansu
 */
@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
@Component("characterizationBO")
public class CharacterizationBO extends BaseAnnotationBO {
	
	private Logger logger = Logger.getLogger(CharacterizationBO.class);
	
	@Autowired
	private CharacterizationService characterizationService;
	
	@Autowired
	private CurationService curationServiceDAO;

	@Autowired
	private SampleService sampleService;

	@Autowired
	private SpringSecurityAclService springSecurityAclService;
	
	@Autowired
	private UserDetailsService userDetailsService;
	
	@Autowired
	private ProtocolService protocolService;

	/**
	 * Add or update a characterization to database
	 * 
	 * Upon success, go back to characterizaiton summary edit page
	 * 
	 * @param request
	 * @param simpleEdit
	 * @return
	 * @throws Exception
	 */
	public SimpleCharacterizationSummaryEditBean submitOrUpdate(HttpServletRequest request, 
			SimpleCharacterizationEditBean simpleEdit)
			throws Exception {
		
		CharacterizationBean charBean = (CharacterizationBean)request.getSession().getAttribute("theChar");
		
		simpleEdit.getErrors().clear();
		simpleEdit.getMessages().clear();
		
		charBean = simpleEdit.transferToCharacterizationBean(charBean);
		if (simpleEdit.getCharId() == 0)
			simpleEdit.setSubmitNewChar(true);
		
		List<String> errs = new ArrayList<String>();
		if (!validateInputs(request, charBean, errs)) {
			SimpleCharacterizationSummaryEditBean emptyView = new SimpleCharacterizationSummaryEditBean();
			emptyView.setErrors(errs);
			return emptyView;
		}
		
		this.saveCharacterization(request, charBean, simpleEdit);
		simpleEdit.getMessages().add(PropertyUtil.getPropertyReplacingToken("sample", "message.addCharacterization", "0", 
				charBean.getCharacterizationName()));
		
		InitCharacterizationSetup.getInstance() //save "others" to db
			.persistCharacterizationDropdowns(request, charBean);
		
		SimpleCharacterizationSummaryEditBean success = new SimpleCharacterizationSummaryEditBean();
		success.getMessages().add("The characterization has been saved successfully");
		return success;
	}

//	public ActionForward input(ActionMapping mapping, ActionForm form,
//			HttpServletRequest request, HttpServletResponse response)
//			throws Exception {
//		DynaValidatorForm theForm = (DynaValidatorForm) form;
//		CharacterizationBean charBean = (CharacterizationBean) theForm
//				.get("achar");
//		charBean.updateEmptyFieldsToNull();
//		this.checkOpenForms(charBean, theForm, request);
//
//		// Save uploaded data in session to avoid asking user to upload again.
//		FileBean theFile = charBean.getTheFinding().getTheFile();
//		escapeXmlForFileUri(theFile);
//		String charName = StringUtils.getOneWordLowerCaseFirstLetter(charBean
//				.getCharacterizationName());
//		preserveUploadedFile(request, theFile, charName);
//
//		return mapping.findForward("inputForm");
//	}

	/**
	 * Set up the input form for adding new characterization
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public SimpleCharacterizationEditBean setupNew(HttpServletRequest request, String sampleId, String charType) throws Exception
	{
		if (StringUtils.isEmpty(sampleId))
			throw new Exception("Sample id is empty");
		
		if (StringUtils.isEmpty(charType))
			throw new Exception("Characterization type is empty");
		
		//This method sets tons of lookups. Need to see what's needed and what's not
		//setupInputForm(request, sampleId, charType);
		
		CharacterizationBean charBean = new CharacterizationBean();		
		charBean.setCharacterizationType(charType);
		
		//this.checkOpenForms(charBean, theForm, request);
		// clear copy to otherSamples
//		clearCopy(theForm);
//		return mapping.findForward("inputForm");
		SimpleCharacterizationEditBean editBean = new SimpleCharacterizationEditBean();
		editBean.transferFromCharacterizationBean(request, charBean, sampleId, sampleService, characterizationService, protocolService, springSecurityAclService);
		
		request.getSession().setAttribute("theEditChar", editBean);
		request.getSession().setAttribute("theChar", charBean);
		return editBean;
	}

	/**
	 * Set up drop-downs need for the input form
	 * 
	 * @param request
	 * @throws Exception
	 */
	private void setupInputForm(HttpServletRequest request, String sampleId, String charType) throws Exception
	{
		if (StringUtils.isEmpty(sampleId))
			throw new Exception("Sample id is invalid");
		
		if (!StringUtils.isEmpty(charType))
			protocolService.getProtocolsByChar(request, charType);
		InitCharacterizationSetup.getInstance().setPOCDropdown(request, sampleId, sampleService);
//		InitCharacterizationSetup.getInstance().setCharacterizationDropdowns(
//				request);

		// set up other samples with the same primary point of contact
		InitSampleSetup.getInstance().getOtherSampleNames(request, sampleId, sampleService);
	}

	/**
	 * Set up the input form for editing existing characterization
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public SimpleCharacterizationEditBean setupUpdate(HttpServletRequest request, String sampleId, String charId, String charClassName, String charType)
			throws Exception 
	{
		charId = super.validateCharId(charId);
		
		CharacterizationBean charBean = characterizationService.findCharacterizationById(charId);
		
//
//		// Find out usage of "charNameDatumNames", not used in any JSPs.
//		InitCharacterizationSetup.getInstance().getDatumNamesByCharName(
//				request, charBean.getCharacterizationType(),
//				charBean.getCharacterizationName(), charBean.getAssayType());
//			charBean.getCharacterizationName(), charBean.getAssayType());
		
		//SY: new
		request.getSession().setAttribute("sampleId", sampleId);
		request.getSession().setAttribute("theChar", charBean);
		logger.debug("Setting theChar in session: " + request.getSession().getId());

		SimpleCharacterizationEditBean editBean = new SimpleCharacterizationEditBean();
		editBean.transferFromCharacterizationBean(request, charBean, sampleId, sampleService, characterizationService, protocolService, springSecurityAclService);
		request.getSession().setAttribute("theEditChar", editBean);
		return editBean;
	}

//	private void clearCopy(DynaValidatorForm theForm) {
//		theForm.set("otherSamples", new String[0]);
//		theForm.set("copyData", false);
//	}

	/**
	 * Setup, prepare and save characterization.
	 * 
	 * @param request
	 * @param charBean
	 * @throws Exception
	 */
	private void saveCharacterization(HttpServletRequest request, 
			CharacterizationBean charBean, SimpleCharacterizationEditBean simpleEdit)
			throws Exception
	{
		SampleBean sampleBean = setupSampleById(String.valueOf(simpleEdit.getParentSampleId()), request);
		
		charBean.setupDomain(SpringSecurityUtil.getLoggedInUserName());
		
		if (charBean.getDomainChar().getDate() == null && simpleEdit.getCharacterizationDate() != null)
			charBean.getDomainChar().setDate(simpleEdit.getCharacterizationDate());
		
		Boolean newChar = true;
		if (charBean.getDomainChar().getId() != null) {
			newChar = false;
		}
		logger.debug("Saving new char? " + newChar);
		
		characterizationService.saveCharacterization(sampleBean, charBean);
		// retract from public if updating an existing public record and not curator
		if (!newChar && !SpringSecurityUtil.getPrincipal().isCurator() && 
			springSecurityAclService.checkObjectPublic(sampleBean.getDomain().getId(), SecureClassesEnum.SAMPLE.getClazz()))
		{
			retractFromPublic(request, sampleBean.getDomain().getId(), sampleBean.getDomain().getName(), "sample", SecureClassesEnum.SAMPLE.getClazz());
			
			simpleEdit.getErrors().add(PropertyUtil.getProperty("sample", "message.updateSample.retractFromPublic"));
		}
		
		// save to other samples (only when user click [Submit] button.)
		if (simpleEdit.getSelectedOtherSampleNames().size()>0) {
			SampleBean[] otherSampleBeans = prepareCopy(request, simpleEdit.getSelectedOtherSampleNames(), sampleBean);
			if (otherSampleBeans != null) {
				characterizationService.copyAndSaveCharacterization(charBean, sampleBean,
						otherSampleBeans, simpleEdit.isCopyToOtherSamples()); //field should be renamed to copyDeepData
			}
			
			simpleEdit.setSubmitNewChar(false);
		}
		
		sampleBean = setupSampleById(String.valueOf(simpleEdit.getParentSampleId()), request);
		request.setAttribute("sampleId", sampleBean.getDomain().getId().toString());
		logger.debug("Done saving characterization: " + charBean.getDomainChar().getId());
	}

	private void deleteCharacterization(HttpServletRequest request, CharacterizationBean charBean, String createdBy) throws Exception
	{
		charBean.setupDomain(createdBy);
		characterizationService.deleteCharacterization(charBean.getDomainChar());
		characterizationService.removeAccesses(charBean.getDomainChar());
	}

	public SimpleCharacterizationSummaryEditBean delete(HttpServletRequest request, SimpleCharacterizationEditBean editBean)
			throws Exception
	{		
		CharacterizationBean charBean = (CharacterizationBean)request.getSession().getAttribute("theChar");
		if (charBean == null)
			throw new Exception("No characterization bean in session. Unable to proceed with delete");
		
		Long charId = charBean.getDomainChar().getId();
		if (charId == null)
			throw new Exception("Characterization bean in session has null id. Unable to proceed with delete");
		
		if (charId.longValue() != editBean.getCharId())
			throw new Exception("Characterization id in session doesn't match input char id. Unable to proceed with delete");
		
		deleteCharacterization(request, charBean, SpringSecurityUtil.getLoggedInUserName());
		
		SimpleCharacterizationSummaryEditBean summaryEdit = new SimpleCharacterizationSummaryEditBean();
		summaryEdit.getMessages().add(PropertyUtil.getProperty("sample", "message.deleteCharacterization"));
		
		return this.summaryEdit(String.valueOf(editBean.getParentSampleId()), request, summaryEdit);
	}

	/**
	 * summaryEdit() handles Edit request for Characterization Summary view.
	 * 
	 * @param request
	 * @return ActionForward
	 * @throws Exception
	 */
	public SimpleCharacterizationSummaryEditBean summaryEdit(String sampleId,
			HttpServletRequest request, SimpleCharacterizationSummaryEditBean editBean)
			throws Exception {
		// Prepare data.
		CharacterizationSummaryViewBean sumBean = this.prepareSummary(sampleId, request);
		
		if (editBean == null)
			editBean = new SimpleCharacterizationSummaryEditBean();
		
		List<SimpleCharacterizationsByTypeBean> finalBeans = editBean.transferData(request, sumBean, sampleId);
		return editBean;
	}

	/**
	 * summaryView() handles View request for Characterization Summary report.
	 * 
	 * @param request
	 * @return ActionForward
	 * @throws Exception
	 *             if error occurred.
	 */
	public CharacterizationSummaryViewBean summaryView(String sampleId,
			HttpServletRequest request)
			throws Exception {
		
		if (sampleId == null || sampleId.length() == 0) 
			throw new Exception("sampleId can't be null or empty");
		
		CharacterizationSummaryViewBean viewBean = prepareSummary(sampleId, request);
		List<String> charTypes = prepareCharacterizationTypes(request);
		//setSummaryTab(request, charTypes.size());
		return viewBean;
	}

//	public ActionForward setupView(ActionMapping mapping, ActionForm form,
//			HttpServletRequest request, HttpServletResponse response)
//			throws Exception {
		

//		DynaValidatorForm theForm = (DynaValidatorForm) form;
//		CharacterizationService service = this.setServicesInSession(request);
//		String charId = super.validateId(request, "charId");
//		setupSample(theForm, request);
//		CharacterizationBean charBean = service
//				.findCharacterizationById(charId);
//		request.setAttribute("charBean", charBean);
//		return mapping.findForward("singleSummaryView");
//	}

	/**
	 * Shared function for summaryView(), summaryPrint() and summaryEdit().
	 * Prepare CharacterizationBean based on Sample Id.
	 * 
	 * @param request
	 * @return ActionForward
	 * @throws Exception
	 */
	private CharacterizationSummaryViewBean prepareSummary(String sampleId, HttpServletRequest request)
			throws Exception {
		// Remove previous result from session.
		request.getSession().removeAttribute("characterizationSummaryView");

		//DynaValidatorForm theForm = (DynaValidatorForm) form;
		//String sampleId = form.getSampleId();  //.getString(SampleConstants.SAMPLE_ID);
	
		List<CharacterizationBean> charBeans = characterizationService.findCharacterizationsBySampleId(sampleId);
		CharacterizationSummaryViewBean summaryView = new CharacterizationSummaryViewBean(charBeans);
		
		request.getSession().setAttribute("characterizationSummaryView", summaryView);

		// Save sample bean in session for sample name in export/print.
		//request.getSession().setAttribute("theSample", sampleBean);
		
		return summaryView;
	}

	/**
	 * Shared function for summaryView() and summaryPrint(). Keep submitted
	 * characterization types in the correct display order. Should be called
	 * after calling prepareSummary(), to avoid session timeout issue.
	 * 
	 * @param request
	 * @return ActionForward
	 * @throws Exception
	 */
	private List<String> prepareCharacterizationTypes(HttpServletRequest request) throws Exception {
		
		CharacterizationSummaryViewBean summaryView = (CharacterizationSummaryViewBean) request
				.getSession().getAttribute("characterizationSummaryView");
		// Keep submitted characterization types in the correct display order
		// prepare characterization tabs and forward to appropriate tab
		List<String> allCharacterizationTypes = InitCharacterizationSetup
				.getInstance().getCharacterizationTypes(request);
		Set<String> foundTypes = summaryView.getCharacterizationTypes();
		List<String> characterizationTypes = new ArrayList<String>();
		for (String charType : allCharacterizationTypes) {
			if (foundTypes.contains(charType)
					&& !characterizationTypes.contains(charType)) {
				characterizationTypes.add(charType);
			}
		}
		// other types that are not in allCharacterizationTypes, e.g. other
		// types from grid search
		for (String type : foundTypes) {
			if (!characterizationTypes.contains(type)) {
				characterizationTypes.add(type);
			}
		}
		request.setAttribute("characterizationTypes", characterizationTypes);
		return characterizationTypes;
	}

	/**
	 * summaryPrint() handles Print request for Characterization Summary report.
	 * 
	 * @param request
	 * @param response
	 * @return ActionForward
	 * @throws Exception
	 */
	public CharacterizationSummaryViewBean summaryPrint(String sampleId, 
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		CharacterizationSummaryViewBean charSummaryBean = (CharacterizationSummaryViewBean) request
				.getSession().getAttribute("characterizationSummaryView");
		if (charSummaryBean == null) {
			// Retrieve data again when session timeout.
			this.prepareSummary(sampleId, request);
			charSummaryBean = (CharacterizationSummaryViewBean) request
					.getSession().getAttribute("characterizationSummaryView");
		}
		

		List<String> charTypes = prepareCharacterizationTypes(request);
		
		this.filterType(request, "all", charTypes); // Filter out un-selected types.

		// Marker that indicates page is for printing only, hide tabs/links.
		request.setAttribute("printView", Boolean.TRUE);

		//return mapping.findForward("summaryPrintView");
		return charSummaryBean;
	}

	/**
	 * Export Characterization Summary report.
	 * 
	 * @param request
	 * @param response
	 * @return ActionForward
	 * @throws Exception
	 */
	public String summaryExport(String sampleId, String type,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		if (sampleId == null || sampleId.length() == 0)
			return "Sample Id can't be null or empty";
		
		if (type == null)
			type = "all";
		
		SampleBean sampleBean = (SampleBean) request.getSession().getAttribute(
				"theSample");
		CharacterizationSummaryViewBean charSummaryBean = (CharacterizationSummaryViewBean) request
				.getSession().getAttribute("characterizationSummaryView");
		if (sampleBean == null || charSummaryBean == null) {
			// Prepare data.
			this.prepareSummary(sampleId, request);
			sampleBean = (SampleBean) request.getSession().getAttribute(
					"theSample");
			charSummaryBean = (CharacterizationSummaryViewBean) request
					.getSession().getAttribute("characterizationSummaryView");
		}
		List<String> charTypes = prepareCharacterizationTypes(request);
		List<String> filteredCharTypes = this.filterType(request, type, charTypes);

		//String type = "all"; //request.getParameter("type");
		// per app scan
		if (!StringUtils.xssValidate(type)) {
			type = "";
		}
		String sampleName = sampleBean.getDomain().getName();
		String fileName = ExportUtils.getExportFileName(sampleName,
				"CharacterizationSummaryView", type);
		ExportUtils.prepareReponseForExcel(response, fileName);

		StringBuilder sb = getDownloadUrl(request);
		CharacterizationExporter.exportSummary(filteredCharTypes,
				charSummaryBean, sb.toString(), response.getOutputStream());

		return "success";
	}

	public SimpleCharacterizationEditBean saveExperimentConfig(HttpServletRequest request, 
			SimpleCharacterizationEditBean charEditBean) throws Exception
	{
		logger.debug("Start saving experiment confg");
		
		//editBean's charId could be null, indicating new char
		CharacterizationBean achar = (CharacterizationBean) request.getSession().getAttribute("theChar");
//		SimpleCharacterizationEditBean editBean = 
//				(SimpleCharacterizationEditBean) request.getSession().getAttribute("theEditChar");
		
		SimpleExperimentBean simpleExpConfig = charEditBean.getDirtyExperimentBean();
				
		ExperimentConfigBean configBean = this.findMatchExperimentConfig(achar, simpleExpConfig);
		
		
		simpleExpConfig.transferToExperimentConfigBean(configBean);
		
		configBean.setupDomain(SpringSecurityUtil.getLoggedInUserName());
		logger.debug("Call saveExperimentConfig");
		String sampleId = (String) request.getSession().getAttribute("sampleId");
		characterizationService.saveExperimentConfig(sampleId, configBean);
		logger.debug("Save exp config complete");
		achar.addExperimentConfig(configBean);
		
		//transfer other data fields of the char
		achar = charEditBean.transferToCharacterizationBean(achar);
		
		// This is to validate characterization data fields
		if (!validateInputs(request, achar, charEditBean.getMessages())) {
			return charEditBean;
		}
		
		this.saveCharacterization(request, achar, charEditBean);
		logger.debug("Save char complete");
		characterizationService.assignAccesses(achar.getDomainChar(), configBean.getDomain());
		

		//this.checkOpenForms(achar, theForm, request);
		InitCharacterizationSetup.getInstance().persistCharacterizationDropdowns(request, achar);
		
		// return to setupUpdate to retrieve the data matrix in the correct
		// form from database
		// after saving to database.
		request.setAttribute("charId", achar.getDomainChar().getId().toString());
		request.setAttribute("charType", achar.getCharacterizationType());
		
		
		return setupUpdate(request, String.valueOf(charEditBean.getParentSampleId()), achar.getDomainChar().getId().toString(), 
				achar.getClassName(), achar.getCharacterizationType());
		
	}

//	public ActionForward getFinding(ActionMapping mapping, ActionForm form,
//			HttpServletRequest request, HttpServletResponse response)
//			throws Exception {
//		DynaValidatorForm theForm = (DynaValidatorForm) form;
//		String theFindingId = request.getParameter("findingId");
//		CharacterizationService service = this.setServicesInSession(request);
//		FindingBean findingBean = service.findFindingById(theFindingId);
//		CharacterizationBean achar = (CharacterizationBean) theForm
//				.get("achar");
//		achar.setTheFinding(findingBean);
//
//		request.setAttribute("anchor", "submitFinding");
//		this.checkOpenForms(achar, theForm, request);
//
//		// Feature request [26487] Deeper Edit Links.
//		if (findingBean.getFiles().size() == 1) {
//			request.setAttribute("onloadJavascript", "setTheFile(0)");
//		}
//		request.setAttribute("disableOuterButtons", true);
//		// remove columnHeaders stored in the session;
//		request.getSession().removeAttribute("columnHeaders");
//		return mapping.findForward("inputForm");
//	}

//	public ActionForward resetFinding(ActionMapping mapping, ActionForm form,
//			HttpServletRequest request, HttpServletResponse response)
//			throws Exception {
//		DynaValidatorForm theForm = (DynaValidatorForm) form;
//		FindingBean theFinding = new FindingBean();
//		// theFinding.setNumberOfColumns(1);
//		// theFinding.setNumberOfRows(1);
//		// theFinding.updateMatrix(theFinding.getNumberOfColumns(), theFinding
//		// .getNumberOfRows());
//		CharacterizationBean achar = (CharacterizationBean) theForm
//				.get("achar");
//		achar.setTheFinding(theFinding);
//		request.setAttribute("anchor", "submitFinding");
//		this.checkOpenForms(achar, theForm, request);
//		request.setAttribute("disableOuterButtons", true);
//		request.getSession().removeAttribute("columnHeaders");
//		return mapping.findForward("inputForm");
		
//		return null;
//	}

	public SimpleCharacterizationEditBean saveFinding(HttpServletRequest request, 
			SimpleCharacterizationEditBean editBean
			/*SimpleFindingBean simpleFinding*/)
			throws Exception {
        CharacterizationBean achar = (CharacterizationBean) request.getSession().getAttribute("theChar");
//		SimpleCharacterizationEditBean editBean = 
//				(SimpleCharacterizationEditBean) request.getSession().getAttribute("theEditChar");
		
		SimpleFindingBean simpleFinding = editBean.getDirtyFindingBean();
		
		if (simpleFinding == null)
			throw new Exception("Can't find dirty finding object"); //temp
		
		FindingBean findingBean = this.findMatchFindingBean(achar, simpleFinding);
		
		//FindingBean findingBean = achar.getTheFinding();
		String theFindingId = String.valueOf(simpleFinding.getFindingId());
		
		simpleFinding.transferToFindingBean(findingBean);
		
		if (!validateEmptyFinding(request, findingBean, editBean.getErrors())) {
			return editBean;
		}
		
		// setup domainFile uri for fileBeans
		SampleBean sampleBean = (SampleBean) request.getSession().getAttribute("theSample");
		String internalUriPath = Constants.FOLDER_PARTICLE
				+ '/'
				+ sampleBean.getDomain().getName()
				+ '/'
				+ StringUtils.getOneWordLowerCaseFirstLetter(achar
						.getCharacterizationName());

		findingBean.setupDomain(internalUriPath, SpringSecurityUtil.getLoggedInUserName());
		characterizationService.saveFinding(findingBean);
		achar.addFinding(findingBean);
		
		//transfer other data fields of the char
		editBean.transferToCharacterizationBean(achar);

		// also save characterization
		if (!validateInputs(request, achar, editBean.getMessages())) {
			return editBean;
		}
		
		this.saveCharacterization(request, achar, editBean);
		characterizationService.assignAccesses(achar.getDomainChar(), findingBean.getDomain());
		//this.checkOpenForms(achar, theForm, request);
		InitCharacterizationSetup.getInstance()
			.persistCharacterizationDropdowns(request, achar);
		
		request.setAttribute("anchor", "result");
		// return to setupUpdate to retrieve the data matrix in the correct
		// form from database
		// after saving to database.
		request.setAttribute("charId", achar.getDomainChar().getId().toString());
		request.setAttribute("charType", achar.getCharacterizationType());
		
		return setupUpdate(request, String.valueOf(editBean.getParentSampleId()), achar.getDomainChar().getId().toString(), 
				achar.getClassName(), achar.getCharacterizationType());
	}

	public SimpleFindingBean saveFile(HttpServletRequest request, SimpleFindingBean simpleFinding)
			throws Exception {
		
		CharacterizationBean achar = (CharacterizationBean) request.getSession().getAttribute("theChar");
		SimpleCharacterizationEditBean editBean = 
				(SimpleCharacterizationEditBean) request.getSession().getAttribute("theEditChar");
		SampleBean currentSample = (SampleBean) request.getSession().getAttribute("theSample");
		
		request.getSession().setAttribute("sampleId", String.valueOf(editBean.getParentSampleId()));
		
		FindingBean findingBean = this.findMatchFindingBean(achar, simpleFinding);
		findingBean =  this.transferSimpleFinding(simpleFinding, findingBean);
		
		int theFileIndex = simpleFinding.getTheFileIndex();		
		FileBean theFile = simpleFinding.transferToNewFileBean();
		
		simpleFinding.getErrors().clear();
	
		// create a new copy before adding to finding
		FileBean newFile = theFile.copy();
		
		if (currentSample == null) //should not be
			currentSample = setupSampleById(String.valueOf(editBean.getParentSampleId()), request);
		
		// setup domainFile uri for fileBeans
		String internalUriPath = Constants.FOLDER_PARTICLE
				+ '/'
				+ currentSample.getDomain().getName()
				+ '/'
				+ StringUtils.getOneWordLowerCaseFirstLetter(achar
						.getCharacterizationName());
		newFile.setupDomainFile(internalUriPath, SpringSecurityUtil.getLoggedInUserName());
		
		String timestamp = DateUtils.convertDateToString(new Date(),
				"yyyyMMdd_HH-mm-ss-SSS");
		byte[] newFileData = (byte[]) request.getSession().getAttribute("newFileData");
		
		if(!theFile.getDomainFile().getUriExternal()){
			if(newFileData!=null){
				newFile.setNewFileData((byte[]) request.getSession().getAttribute("newFileData"));
				
				logger.debug("File path: " + newFile.getDomainFile().getUri());
			} else if(theFile.getDomainFile().getId()!=null){
				theFile.getDomainFile().setUri(theFile.getDomainFile().getName());
			}else {
				newFile.getDomainFile().setUri(null);
			}
		}
		
		this.validateFileBean(request, simpleFinding.getErrors(), theFile);
		if (simpleFinding.getErrors().size() > 0)
			return simpleFinding;
		
		findingBean.addFile(newFile, theFileIndex);
		findingBean.setupDomain(internalUriPath, SpringSecurityUtil.getLoggedInUserName());
		characterizationService.saveFinding(findingBean);
				
		achar.addFinding(findingBean);
		simpleFinding.transferFromFindingBean(request, findingBean);
		
		request.setAttribute("anchor", "submitFinding");
//		this.checkOpenForms(achar, theForm, request);
//		InitCharacterizationSetup.getInstance()
//			.persistCharacterizationDropdowns(request, achar);
		
		request.getSession().removeAttribute("newFileData");
		request.getSession().setAttribute("theChar", achar);
		
		/// save char to session
		return simpleFinding;
	}

	private FindingBean transferSimpleFinding(SimpleFindingBean simpleFinding, FindingBean finding) {
		//FindingBean finding = new FindingBean();
		finding.setColumnHeaders(simpleFinding.getColumnHeaders());
		FileBean theFile = new FileBean();
		File file = new File();
		file.setCreatedBy(simpleFinding.getTheFile().getCreatedBy());
		file.setCreatedDate(simpleFinding.getTheFile().getCreatedDate());
		file.setId(simpleFinding.getTheFile().getId());
		file.setTitle(simpleFinding.getTheFile().getTitle());
		file.setType(simpleFinding.getTheFile().getType());
		file.setDescription(simpleFinding.getTheFile().getDescription());
		theFile.setKeywordsStr(simpleFinding.getTheFile().getKeywordsStr());
		theFile.setDomainFile(file);
		finding.setTheFile(theFile);
		finding.setNumberOfColumns(simpleFinding.getNumberOfColumns());
		finding.setNumberOfRows(simpleFinding.getNumberOfRows());
		finding.setTheFileIndex(simpleFinding.getTheFileIndex());
		
		List<Row> rowList = new ArrayList<Row>();
		for(SimpleRowBean rowBean : simpleFinding.getRows()){
			Row row = new Row();
			List<TableCell> cellList = new ArrayList<TableCell>();
			for(SimpleCell cell : rowBean.getCells()){
				TableCell tbCell = new TableCell();
				tbCell.setColumnOrder(cell.getColumnOrder());
				tbCell.setDatumOrCondition(cell.getDatumOrCondition());
				tbCell.setCreatedDate(cell.getCreatedDate());
				tbCell.setValue(cell.getValue());
				cellList.add(tbCell);
			}
			row.setCells(cellList);
			rowList.add(row);
		}
		finding.setRows(rowList);
		List<FileBean> fileList = new ArrayList<FileBean>();
		for(SimpleFileBean fileBean : simpleFinding.getFiles()){
			file = new File();
			theFile = new FileBean();
			file.setCreatedBy(fileBean.getCreatedBy());
			file.setCreatedDate(fileBean.getCreatedDate());
			file.setId(fileBean.getId());
			file.setTitle(fileBean.getTitle());
			file.setUriExternal(fileBean.getUriExternal());
			file.setUri(fileBean.getUri());
			file.setType(fileBean.getType());
			file.setDescription(fileBean.getDescription());
			theFile.setKeywordsStr(fileBean.getKeywordsStr());
			
			theFile.setExternalUrl(fileBean.getExternalUrl());
			theFile.setDomainFile(file);
			fileList.add(theFile);
		}
		finding.setFiles(fileList);
		return finding;
	}

	public SimpleFindingBean removeFile(HttpServletRequest request, SimpleFindingBean simpleFinding)
			throws Exception {
	
		CharacterizationBean achar = (CharacterizationBean) request.getSession().getAttribute("theChar");
		SimpleCharacterizationEditBean editBean = 
				(SimpleCharacterizationEditBean) request.getSession().getAttribute("theEditChar");
		
		FindingBean findingBean = this.findMatchFindingBean(achar, simpleFinding);
		int theFileIndex = simpleFinding.getTheFileIndex();
		findingBean.removeFile(theFileIndex);
		findingBean.setTheFile(new FileBean());
		request.setAttribute("anchor", "submitFinding");
		
		simpleFinding.transferFilesFromFindingBean(request, findingBean.getFiles());
		
		//this.checkOpenForms(achar, theForm, request);
		//return mapping.findForward("inputForm");
		
		return simpleFinding;
	}

	/**
	 * 
	 * @param request
	 * @param simpleFinding
	 * @return
	 * @throws Exception
	 */
	public SimpleFindingBean drawMatrix(HttpServletRequest request, SimpleFindingBean simpleFinding)
			throws Exception {
		
		CharacterizationBean achar = (CharacterizationBean) request.getSession().getAttribute("theChar");
		SimpleCharacterizationEditBean editBean = 
				(SimpleCharacterizationEditBean) request.getSession().getAttribute("theEditChar");
		request.setAttribute("anchor", "result");
		
		FindingBean findingBean = this.findMatchFindingBean(achar, simpleFinding);
		simpleFinding.transferTableNumbersToFindingBean(findingBean);

//		if (request.getParameter("removeColumn") != null) {
//			int columnToRemove = Integer.parseInt(request
//					.getParameter("removeColumn"));
//			findingBean.removeColumn(columnToRemove);
//			this.checkOpenForms(achar, theForm, request);
//			return mapping.findForward("inputForm");
//		} else if (request.getParameter("removeRow") != null) {
//			int rowToRemove = Integer.parseInt(request
//					.getParameter("removeRow"));
//			findingBean.removeRow(rowToRemove);
//			this.checkOpenForms(achar, theForm, request);
//			return mapping.findForward("inputForm");
//		}
		int existingNumberOfColumns = findingBean.getColumnHeaders().size();
		int existingNumberOfRows = findingBean.getRows().size();
		
		if (existingNumberOfColumns > findingBean.getNumberOfColumns()) {
//			ActionMessages msgs = new ActionMessages();
//			ActionMessage msg = new ActionMessage(
//					"message.addCharacterization.removeMatrixColumn");
//			msgs.add(ActionMessages.GLOBAL_MESSAGE, msg);
//			saveMessages(request, msgs);
//			
			
//			findingBean.setNumberOfColumns(existingNumberOfColumns);
//			//this.checkOpenForms(achar, theForm, request);
//			return mapping.getInputForward();
		}
		if (existingNumberOfRows > findingBean.getNumberOfRows()) {
//			ActionMessages msgs = new ActionMessages();
//			ActionMessage msg = new ActionMessage(
//					"message.addCharacterization.removeMatrixRow");
//			msgs.add(ActionMessages.GLOBAL_MESSAGE, msg);
//			saveMessages(request, msgs);
//			findingBean.setNumberOfRows(existingNumberOfRows);
//			this.checkOpenForms(achar, theForm, request);
//			return mapping.getInputForward();
		}
		
		findingBean.updateMatrix(findingBean.getNumberOfColumns(),
				findingBean.getNumberOfRows());
		
		InitCharacterizationSetup.getInstance()
			.persistCharacterizationDropdowns(request, achar);
		
		simpleFinding.transferFromFindingBean(request, findingBean);
		simpleFinding.setColumnHeaders(findingBean.getColumnHeaders());
		simpleFinding.setDefaultValuesForNullHeaders();
		
		request.setAttribute("anchor", "submitFinding");
		
		//this.checkOpenForms(achar, theForm, request);
		// set columnHeaders in the session so jsp can check duplicate columns
		request.getSession().setAttribute("columnHeaders",
				findingBean.getColumnHeaders());
		//return mapping.findForward("inputForm");
		
		return simpleFinding;
	}

	public SimpleCharacterizationEditBean deleteFinding(HttpServletRequest request,
			SimpleFindingBean simpleFinding)
			throws Exception {
		
		CharacterizationBean achar = (CharacterizationBean) request.getSession().getAttribute("theChar");
		SimpleCharacterizationEditBean editBean = (SimpleCharacterizationEditBean) request.getSession().getAttribute("theEditChar");
		
		FindingBean dataSetBean = this.findMatchFindingBean(achar, simpleFinding);
		
		characterizationService.deleteFinding(dataSetBean.getDomain());
		characterizationService.removeAccesses(achar.getDomainChar(), dataSetBean.getDomain());
		
		achar.removeFinding(dataSetBean);
		request.setAttribute("anchor", "result");
		
		//this.checkOpenForms(achar, theForm, request);
		InitCharacterizationSetup.getInstance().persistCharacterizationDropdowns(request, achar);
		
		return setupUpdate(request, String.valueOf(editBean.getParentSampleId()), achar.getDomainChar().getId().toString(), 
				achar.getClassName(), achar.getCharacterizationType());
	}

	public SimpleCharacterizationEditBean deleteExperimentConfig(HttpServletRequest request,
			SimpleExperimentBean simpleExpConfig) throws Exception {
		
		CharacterizationBean achar = (CharacterizationBean) request.getSession().getAttribute("theChar");
		SimpleCharacterizationEditBean editBean = 
				(SimpleCharacterizationEditBean) request.getSession().getAttribute("theEditChar");
		
		editBean.getErrors().clear();
		editBean.getMessages().clear();
		
		ExperimentConfigBean configBean = achar.getTheExperimentConfig();
		simpleExpConfig.transferToExperimentConfigBean(configBean);
		
		characterizationService.deleteExperimentConfig(editBean.getParentSampleId() + "", configBean.getDomain());
		logger.debug("Experiment config deleted");
		achar.removeExperimentConfig(configBean);
		
		if (validateInputs(request, achar, editBean.getMessages())) {
			logger.debug("char validated");
			this.saveCharacterization(request, achar, editBean);
			logger.debug("Char saved");
		}	
		
		characterizationService.removeAccesses(achar.getDomainChar(), configBean.getDomain());
		logger.debug("Access removed");
		//this.checkOpenForms(achar, theForm, request);
		InitCharacterizationSetup.getInstance()
			.persistCharacterizationDropdowns(request, achar);
		
		return setupUpdate(request, String.valueOf(editBean.getParentSampleId()), achar.getDomainChar().getId().toString(), 
				achar.getClassName(), achar.getCharacterizationType());
	}

	/**
	 * Method to support setColumnOrder rest service
	 * @param request
	 * @param simpleFinding
	 * @return
	 * @throws Exception
	 */
	public SimpleFindingBean updateColumnOrder(HttpServletRequest request,
			SimpleFindingBean simpleFinding) throws Exception {
		
		CharacterizationBean achar = (CharacterizationBean) request.getSession().getAttribute("theChar");
//		SimpleCharacterizationEditBean editBean =
//				(SimpleCharacterizationEditBean) request.getSession().getAttribute("theEditChar");
		
		FindingBean findingBean = findMatchFindingBean(achar, simpleFinding);

        for( int f = 0; f < (simpleFinding.getRows()).size(); f++){
             // Each SimpleFindingBean row has a list of SimpleCell
            for( int f0 = 0; f0 < (simpleFinding.getRows()).get(f).getCells().size(); f0++){
                (findingBean.getRows()).get(f).getCells().get(f0).setValue((simpleFinding.getRows()).get(f).getCells().get(f0).getValue());
                (findingBean.getRows()).get(f).getCells().get(f0).setOperand((simpleFinding.getRows()).get(f).getCells().get(f0).getOperand());
            }
        }
		simpleFinding.transferColumnOrderToFindingBean(findingBean);

		findingBean.updateColumnOrder();
		
		simpleFinding.transferFromFindingBean(request, findingBean);

		request.setAttribute("anchor", "submitFinding");
		//this.checkOpenForms(achar, theForm, request);
		InitCharacterizationSetup.getInstance()
			.persistCharacterizationDropdowns(request, achar);

		return simpleFinding;
	}
	
	protected FindingBean findMatchFindingBean(CharacterizationBean achar, 
			SimpleFindingBean simpleFinding) 
	throws Exception {
		
		
		List<FindingBean> findingBeans = achar.getFindings();
		if (findingBeans == null)
			throw new Exception("Current characterization has no finding matching input finding id: " + simpleFinding.getFindingId());
		
		for (FindingBean finding : findingBeans) {
			if (finding.getDomain() != null) {
				Long id = finding.getDomain().getId(); 
				if (id == null && simpleFinding.getFindingId() == 0 || //could be a new finding bean added when saving a file
						id != null && id.longValue() == simpleFinding.getFindingId()) {
					achar.setTheFinding(finding);
					return finding;
				}
			}
		}
		
		if (simpleFinding.getFindingId() <= 0) {//new finding
			FindingBean newBean = new FindingBean();
			achar.setTheFinding(newBean);
			return newBean;
		}
		
		throw new Exception("Current characterization has no finding matching input finding id: " + simpleFinding.getFindingId());
	}
	
	protected ExperimentConfigBean findMatchExperimentConfig(CharacterizationBean achar, SimpleExperimentBean simpleExp) throws Exception
	{		
		long expId = simpleExp.getId();
		
		if (expId <= 0)
			return achar.getTheExperimentConfig();
		
		List<ExperimentConfigBean> expConfigs = achar.getExperimentConfigs();
		
		if (expConfigs == null)
			throw new Exception("Current characterization has null experiment config list. This should not happen");
		
		for (ExperimentConfigBean expConfig : expConfigs) {
			if (expConfig.getDomain() != null && expConfig.getDomain().getId() != null) {
				if (expId == expConfig.getDomain().getId().longValue())
					return expConfig;
			}
			
		}
		
		throw new Exception("Current characterization has no Experiment config matching input experiment id: " 
		+ expId);
	}

//	private void checkOpenForms(CharacterizationBean achar,
//			DynaValidatorForm theForm, HttpServletRequest request)
//			throws Exception {
//		
//		achar.updateEmptyFieldsToNull();
//		String dispatch = request.getParameter("dispatch");
//		String browserDispatch = getBrowserDispatch(request);
//		
//		HttpSession session = request.getSession();
//		Boolean openFile = false, openExperimentConfig = false, openFinding = false;
//		if (dispatch.equals("input") && browserDispatch.equals("addFile")) {
//			openFile = true;
//		}
//		session.setAttribute("openFile", openFile);
//		if (dispatch.equals("input")
//				&& browserDispatch.equals("saveExperimentConfig")) {
//			openExperimentConfig = true;
//		}
//		session.setAttribute("openExperimentConfig", openExperimentConfig);
//		if (dispatch.equals("input")
//				&& (browserDispatch.equals("saveFinding") || browserDispatch
//						.equals("addFile")) || dispatch.equals("addFile")
//				|| dispatch.equals("removeFile")
//				|| dispatch.equals("drawMatrix")
//				|| dispatch.equals("getFinding")
//				|| dispatch.equals("resetFinding")
//				|| dispatch.equals("updateColumnOrder")) {
//			openFinding = true;
//		}
//		session.setAttribute("openFinding", openFinding);
//
//		InitCharacterizationSetup.getInstance()
//				.persistCharacterizationDropdowns(request, achar);
//
//		/**
//		 * If user entered customized Char Type/Name, Assay Type by selecting
//		 * [other], we should show and highlight the value on the edit page.
//		 */
//		String currentCharType = achar.getCharacterizationType();
//		setOtherValueOption(request, currentCharType, "characterizationTypes");
//
//		String currentCharName = achar.getCharacterizationName();
//		setOtherValueOption(request, currentCharName, "charTypeChars");
//
//		String currentAssayType = achar.getAssayType();
//		setOtherValueOption(request, currentAssayType, "charNameAssays");
//
//		// setup detail page
//		if (achar.isWithProperties()) {
//			String detailPage = null;
//			if (!StringUtils.isEmpty(achar.getCharacterizationType())
//					&& !StringUtils.isEmpty(achar.getCharacterizationName())) {
//				detailPage = InitCharacterizationSetup.getInstance()
//						.getDetailPage(achar.getCharacterizationType(),
//								achar.getCharacterizationName());
//			}
//			request.setAttribute("characterizationDetailPage", detailPage);
//		}
//		// if finding contains more than one column, set disableSetColumnOrder
//		// false
//		if (achar.getTheFinding().getNumberOfColumns() > 1
//				&& dataMatrixSaved(achar.getTheFinding())) {
//			request.setAttribute("setColumnOrder", true);
//		} else {
//			request.setAttribute("setColumnOrder", false);
//		}
//	}

	private Boolean dataMatrixSaved(FindingBean theFinding) {
		if (theFinding.getColumnHeaders() != null) {
			for (ColumnHeader header : theFinding.getColumnHeaders()) {
				if (header.getCreatedDate() == null) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Shared function for summaryExport() and summaryPrint(). Filter out
	 * unselected types when user selected one type for print/export.
	 * 
	 * @param request
     */
	private List<String> filterType(HttpServletRequest request, String type,
			List<String> charTypes) {
		//String type = request.getParameter("type");
		List<String> filteredTypes = new ArrayList<String>();
		if( !StringUtils.isEmpty(type) && type.equals("all")) {
			filteredTypes = charTypes;
		} else if (!StringUtils.isEmpty(type) && charTypes.contains(type)) {
			filteredTypes.add(type);
		}
		request.setAttribute("characterizationTypes", filteredTypes);
		return filteredTypes;
	}

	private boolean validateCharacterization(HttpServletRequest request,
			CharacterizationBean achar, List<String> errors) {
		
		String charName = achar.getCharacterizationName();
		String charType = achar.getCharacterizationType();
		
		if (charType == null || charType.length() == 0) {
			errors.add("Characterization type can't be empty.");
			return false;
		}
		
		if (charName == null || charName.length() == 0) {
			errors.add("Characterization name can't be empty.");
			return false;
		}
		
		boolean status = true;
		if (charName.equalsIgnoreCase("shape")) {
			if (achar.getShape().getType() != null
					&& !StringUtils.xssValidate(achar.getShape().getType())) {
				
				errors.add(PropertyUtil.getProperty("sample", "achar.shape.type.invalid"));
				status = false;
			}
			if (achar.getShape().getMaxDimensionUnit() != null
					&& !achar.getShape().getMaxDimensionUnit()
							.matches(Constants.UNIT_PATTERN)) {
				
				errors.add(PropertyUtil.getProperty("sample", "achar.shape.maxDimensionUnit.invalid"));
				status = false;
			}
			if (achar.getShape().getMinDimensionUnit() != null
					&& !achar.getShape().getMinDimensionUnit()
							.matches(Constants.UNIT_PATTERN)) {
				
				errors.add(PropertyUtil.getProperty("sample", "achar.shape.minDimensionUnit.invalid"));
				status = false;
			}
		} else if (charName.equalsIgnoreCase(
				"physical state")) {
			if (achar.getPhysicalState().getType() != null
					&& !StringUtils.xssValidate(achar.getPhysicalState()
							.getType())) {
				
				errors.add(PropertyUtil.getProperty("sample", "achar.physicalState.type.invalid"));
				status = false;
			}
		} else if (charName.equalsIgnoreCase(
				"solubility")) {
			if (achar.getSolubility().getSolvent() != null
					&& !StringUtils.xssValidate(achar.getSolubility()
							.getSolvent())) {
				
				errors.add(PropertyUtil.getProperty("sample", "achar.solubility.solvent.invalid"));
				status = false;
			}
			if (achar.getSolubility().getCriticalConcentrationUnit() != null
					&& !achar.getSolubility().getCriticalConcentrationUnit()
							.matches(Constants.UNIT_PATTERN)) {
			
				errors.add(PropertyUtil.getProperty("sample", "achar.solubility.criticalConcentrationUnit.invalid"));
				status = false;
			}
		} else if (charName.equalsIgnoreCase(
				"enzyme induction")) {
			if (achar.getEnzymeInduction().getEnzyme() != null
					&& !StringUtils.xssValidate(achar.getEnzymeInduction().getEnzyme())) {
				errors.add(PropertyUtil.getProperty("sample", "achar.enzymeInduction.enzyme.invalid"));
				status = false;
			}
		}
		return status;
	}

	private Boolean validateInputs(HttpServletRequest request,
			CharacterizationBean achar, List<String> errors) {
		return validateCharacterization(request, achar, errors);
	}

	private boolean validateEmptyFinding(HttpServletRequest request,
			FindingBean finding, List<String> errors) {
		
		if (finding.getFiles().isEmpty()
				&& finding.getColumnHeaders().isEmpty()) {
			errors.add(PropertyUtil.getProperty("sample", "achar.theFinding.emptyFinding"));
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Copy "isSoluble" property from achar to Solubility entity.
	 * 
	 * @param achar
	 */
	private void copyIsSoluble(CharacterizationBean achar) {
		Boolean soluble = null;
		String isSoluble = achar.getIsSoluble();
		if (!StringUtils.isEmpty(isSoluble)) {
			soluble = Boolean.valueOf(isSoluble);
		}
		if ("Solubility".equals(achar.getClassName())) {
			achar.getSolubility().setIsSoluble(soluble);
		}
	}

	public java.io.File download(String fileId, HttpServletRequest request) throws Exception {
		return downloadImage(characterizationService, fileId, request);
	}
	
	public String download(String fileId, HttpServletRequest request, HttpServletResponse response) throws Exception {
		return downloadFile(characterizationService, fileId, request, response);
	}

	protected boolean validCharTypeAndName(String charType, String charName, List<String> errors) {
		boolean valid = true;
		if (charType == null || charType.length() == 0) {
			errors.add("Characterization type can't be empty");
			valid = false;
		}
		
		if (charName == null || charName.length() == 0) {
			errors.add("Characterization name can't be empty");
			valid = false;
		}
		
		return valid;
	}
	
	/**
	 * Set up the input form for editing existing characterization
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public List<SimpleCharacterizationUnitBean> setupView(HttpServletRequest request, String sampleId, String charId, String charClassName, String charType)
			throws Exception
	{
		charId = super.validateCharId(charId);
		
		CharacterizationBean charBean = characterizationService.findCharacterizationById(charId);
		
		SimpleCharacterizationSummaryViewBean viewHelper = new SimpleCharacterizationSummaryViewBean();

		return viewHelper.tranferCharacterizationBeanData(request, charBean);
	}

	@Override
	public CurationService getCurationServiceDAO() {
		return curationServiceDAO;
	}

	@Override
	public SampleService getSampleService() {
		return sampleService;
	}

	@Override
	public SpringSecurityAclService getSpringSecurityAclService() {
		return springSecurityAclService;
	}

	@Override
	public UserDetailsService getUserDetailsService() {
		return userDetailsService;
	}
	
}
