/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.restful.sample;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class searches canano metadata based on user supplied criteria
 *
 * @author pansu
 */

import gov.nih.nci.cananolab.dto.particle.AdvancedSampleBean;
import gov.nih.nci.cananolab.dto.particle.AdvancedSampleSearchBean;
import gov.nih.nci.cananolab.restful.bean.LabelValueBean;
import gov.nih.nci.cananolab.restful.core.BaseAnnotationBO;
import gov.nih.nci.cananolab.restful.util.PropertyUtil;
import gov.nih.nci.cananolab.restful.view.SimpleAdvancedSearchResultView;
import gov.nih.nci.cananolab.restful.view.SimpleAdvancedSearchSampleBean;
import gov.nih.nci.cananolab.security.service.SpringSecurityAclService;
import gov.nih.nci.cananolab.service.curation.CurationService;
import gov.nih.nci.cananolab.service.sample.SampleService;
import gov.nih.nci.cananolab.service.sample.exporter.SampleExporter;
import gov.nih.nci.cananolab.util.DateUtils;
import gov.nih.nci.cananolab.util.ExportUtils;

@Transactional(readOnly=false, propagation=Propagation.REQUIRED)
@Component("advancedSampleSearchBO")
public class AdvancedSampleSearchBO extends BaseAnnotationBO
{
	private Logger logger = Logger.getLogger(AdvancedSampleSearchBO.class);

	@Autowired
	private CurationService curationServiceDAO;

	@Autowired
	private SampleService sampleService;

	@Autowired
	private SpringSecurityAclService springSecurityAclService;
	
	@Autowired
	private UserDetailsService userDetailsService;

	// Partial URL for viewing detailed sample info from Excel report file.
	public static final String VIEW_SAMPLE_URL = "sample.do?dispatch=summaryView&page=0";

	public SimpleAdvancedSearchResultView search(HttpServletRequest request, AdvancedSampleSearchBean searchBean)
			throws Exception {
		
		HttpSession session = request.getSession();
		session
		.setAttribute("advancedSampleSearchBean",
				searchBean);
		
//		searchBean.updateQueries();
//		
//		List<AdvancedSampleBean> sampleBeans = querySamples(request, searchBean);
		
		searchBean.updateQueries();
		// retrieve from session if it's not null
		List<AdvancedSampleBean> sampleBeans = (List<AdvancedSampleBean>) session.getAttribute("advancedSampleSearchResults");
		if (sampleBeans == null) {
			sampleBeans = querySamples(request, searchBean);
			if (sampleBeans != null && !sampleBeans.isEmpty()) {
				session
						.setAttribute("advancedSampleSearchResults",
								sampleBeans);
			}
		}
		
		if (sampleBeans == null || sampleBeans.isEmpty()) {
			SimpleAdvancedSearchResultView empty = new SimpleAdvancedSearchResultView();
			List<String> messages = new ArrayList<String>();
			empty.getErrors().add(PropertyUtil.getProperty("sample", "message.advancedSampleSearch.noresult"));
			return empty;

		}
			
		logger.debug("Got " + sampleBeans.size() + " sample ids from adv. queries");
		
		
		//Load full objects
		List<AdvancedSampleBean> loadedSampleBeans = new ArrayList<AdvancedSampleBean>();
		
		int idx = 0;
		for (AdvancedSampleBean sampleBean : sampleBeans) {

			String sampleId = sampleBean.getSampleId();
			AdvancedSampleBean loadedAdvancedSample = sampleService.findAdvancedSampleByAdvancedSearch(sampleId, searchBean);
			loadedSampleBeans.add(loadedAdvancedSample);
			
			logger.debug("Processing sample #: " + idx++);
		}
		
		// save sample result set in session for printing.
		//session.setAttribute("samplesResultList", sampleBeansPerPage);
		//return mapping.findForward("success");

        return transfertoSimpleSampleBeans(loadedSampleBeans, searchBean);
	}

	/**
	 * Export full advance sample search report in excel file.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	
	public String export(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		// 1.retrieve search bean from session - form.
		HttpSession session = request.getSession();
//		DynaValidatorForm searchForm = (DynaValidatorForm) session
//				.getAttribute("advancedSampleSearchForm");

		AdvancedSampleSearchBean searchBean = (AdvancedSampleSearchBean) session.getAttribute("advancedSampleSearchBean");

		// 2.retrieve full list of sample name from session.
		List<AdvancedSampleBean> sampleSearchResult = (List<AdvancedSampleBean>) session.getAttribute("advancedSampleSearchResults");

		if (searchBean != null && sampleSearchResult != null && !sampleSearchResult.isEmpty())
		{
			// Load all samples by name & location if they are not loaded
			// previously.
			List<AdvancedSampleBean> samplesFullList = (List<AdvancedSampleBean>) session.getAttribute("samplesFullList");
			if (samplesFullList == null) {
				samplesFullList = new ArrayList<AdvancedSampleBean>(sampleSearchResult.size());
				for (AdvancedSampleBean sample : sampleSearchResult) {
					String sampleId = sample.getSampleId();
					AdvancedSampleBean loadedAdvancedSample;
					loadedAdvancedSample = sampleService.findAdvancedSampleByAdvancedSearch(sampleId, searchBean);
					samplesFullList.add(loadedAdvancedSample);
				}
				// save full sample result set in session for later use.
				session.setAttribute("samplesFullList", samplesFullList);
			}

			// Export all advanced sample search report.
			ExportUtils.prepareReponseForExcel(response, getExportFileName());
			SampleExporter.exportSummary(searchBean, samplesFullList,
					getViewSampleURL(request), response.getOutputStream());
			return null;
		} else {
			return "session has expired, please try again";
		}
	}

	/**
	 * Print current page advance sample search report.
	 *
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public SimpleAdvancedSearchResultView print(HttpServletRequest request, AdvancedSampleSearchBean searchBean)
			throws Exception
	{
		HttpSession session = request.getSession();
		
		// 1.Get total sample list from session for total result size.
		List<AdvancedSampleBean> sampleBeans = (List<AdvancedSampleBean>) session
				.getAttribute("advancedSampleSearchResults");

		// 2.Get current page sample list from session for display tab.
		List<AdvancedSampleBean> sampleResultList = (List<AdvancedSampleBean>) session
				.getAttribute("samplesResultList");

		
		if (sampleBeans == null || sampleBeans.isEmpty()) {
			SimpleAdvancedSearchResultView empty = new SimpleAdvancedSearchResultView();
			List<String> messages = new ArrayList<String>();
			empty.getErrors().add(PropertyUtil.getProperty("sample", "message.advancedSampleSearch.noresult"));
			return empty;
		}
			
		logger.debug("Got " + sampleBeans.size() + " sample ids from adv. queries");

		//Load full objects
		List<AdvancedSampleBean> loadedSampleBeans = new ArrayList<AdvancedSampleBean>();
		
		int idx = 0;
		for (AdvancedSampleBean sampleBean : sampleBeans)
		{
			String sampleId = sampleBean.getSampleId();
			AdvancedSampleBean loadedAdvancedSample = sampleService.findAdvancedSampleByAdvancedSearch(sampleId, searchBean);
			loadedSampleBeans.add(loadedAdvancedSample);
			
			logger.debug("Processing sample #: " + idx++);
		}
		
		// save sample result set in session for printing.
		//session.setAttribute("samplesResultList", sampleBeansPerPage);
		//return mapping.findForward("success");

        return transfertoSimpleSampleBeans(loadedSampleBeans, searchBean);
	}

	private List<AdvancedSampleBean> querySamples(HttpServletRequest request, AdvancedSampleSearchBean searchBean) throws Exception
	{		
		List<String> sampleNames = sampleService.findSampleIdsByAdvancedSearch(searchBean);
		List<AdvancedSampleBean> sampleBeans = new ArrayList<AdvancedSampleBean>();
		for (String name : sampleNames) {
			AdvancedSampleBean sampleBean = new AdvancedSampleBean(name);
			sampleBeans.add(sampleBean);
		}
		return sampleBeans;
		
//		Map<String, String> sampleIdNames = service.findSampleIdNamesByAdvancedSearch(searchBean);
//		
//		logger.debug("Advanced search returned " + sampleIdNames.size() + " sampleId and name pairs");
//		
//		List<AdvancedSampleBean> sampleBeans = new ArrayList<AdvancedSampleBean>();
//		
//		Iterator<String> ite = sampleIdNames.keySet().iterator();
//		while (ite.hasNext()) {
//			String id = ite.next();
//			String name = sampleIdNames.get(id);
//			AdvancedSampleBean sampleBean = new AdvancedSampleBean(id);
//			sampleBean.setSampleName(name);
//			
//
//			service.loadAccessesForSampleBean(sampleBean);
//			
//			sampleBeans.add(sampleBean);
//		}
		
		//return sampleBeans;
	}

//	private List<AdvancedSampleBean> loadSamples(List<AdvancedSampleBean> sampleBeans, int page, int pageSize,
//			HttpServletRequest request, AdvancedSampleSearchBean searchBean)
//			throws Exception {
//		List<AdvancedSampleBean> loadedSampleBeans = new ArrayList<AdvancedSampleBean>();
//		SampleService service = null;
//		
//		
//		if (request.getSession().getAttribute("sampleService") != null) {
//			service = (SampleService) request.getSession().getAttribute(
//					"sampleService");
//		} else {
//			service = this.setServiceInSession(request);
//		}
//		for (int i = page * pageSize; i < (page + 1) * pageSize; i++) {
//			if (i < sampleBeans.size()) {
//				String sampleId = sampleBeans.get(i).getSampleId();
//				
//
//				
//				AdvancedSampleBean loadedAdvancedSample = service
//						.findAdvancedSampleByAdvancedSearch(sampleId,
//								searchBean, true);
//				loadedSampleBeans.add(loadedAdvancedSample);
//			}
//		}
//		return loadedSampleBeans;
//	}

	public void validateSetup(HttpServletRequest request) throws Exception
	{
		// clear the search results and start over
		request.getSession().removeAttribute("advancedSampleSearchResults");
		request.getSession().removeAttribute("samplesResultList");
		request.getSession().removeAttribute("samplesFullList");
		request.setAttribute(
						"onloadJavascript",
						"displaySampleQueries(); displayCompositionQueries(); displayCharacterizationQueries()");
		//return mapping.findForward("inputForm");
	}

//	public void setup(HttpServletRequest request)
//			throws Exception {
//		
//		request.getSession().removeAttribute("advancedSampleSearchForm");
//		InitCharacterizationSetup.getInstance()
//				.getDecoratedCharacterizationTypes(request);
//		request.getSession().removeAttribute("advancedSampleSearchResults");
//		request.getSession().removeAttribute("samplesResultList");
//		request.getSession().removeAttribute("samplesFullList");
//		//return mapping.getInputForward();
//	}
	
	public Map<String, Object> setup(HttpServletRequest request)
			throws Exception {
		request.getSession().removeAttribute("advancedSampleSearchForm");
		
		request.getSession().removeAttribute("advancedSampleSearchResults");
		request.getSession().removeAttribute("samplesResultList");
		request.getSession().removeAttribute("samplesFullList");
		
		List<LabelValueBean> charTypes = InitCharacterizationSetup.getInstance()
				.getDecoratedCharacterizationTypes(request);
		
		List<String> nanomaterialTypes = InitSampleSetup.getInstance().getNanomaterialEntityTypes(request);
		List<String> fetypes = InitSampleSetup.getInstance().getFunctionalizingEntityTypes(request);
		List<String> functionTypes = InitSampleSetup.getInstance().getFunctionTypes(request);
		
		request.getSession().removeAttribute("sampleSearchResults");
		
		Map<String, Object> mixedMap = new HashMap<String, Object>(); 
		mixedMap.put("functionTypes", functionTypes);
		mixedMap.put("nanomaterialEntityTypes", nanomaterialTypes);
		mixedMap.put("functionalizingEntityTypes", fetypes);
		mixedMap.put("characterizationTypes", charTypes);
		
		return mixedMap;
	}

	/**
	 * Get file name for exporting report as an Excel file.
	 *
	 * @return
	 */
	private static String getExportFileName() {
		StringBuilder sb = new StringBuilder("Advanced_Sample_Search_Report_");
		sb.append(DateUtils.convertDateToString(Calendar.getInstance().getTime()));
		return sb.toString();
	}

	/**
	 * Get view sample URL.
	 *
	 * @return
	 */
	private static String getViewSampleURL(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder();
		String requestUrl = request.getRequestURL().toString();
		int index = requestUrl.lastIndexOf("/");
		sb.append(requestUrl.substring(0, index + 1));
		sb.append(VIEW_SAMPLE_URL);

		return sb.toString();
	}
	
	/**
	 * 
	 */
	protected SimpleAdvancedSearchResultView transfertoSimpleSampleBeans(List<AdvancedSampleBean> sampleBeans, AdvancedSampleSearchBean searchBean)
	{		
		SimpleAdvancedSearchResultView resultView = new SimpleAdvancedSearchResultView();
		resultView.createColumnTitles(searchBean.getQueryAsColumnNames());
	
		List<SimpleAdvancedSearchSampleBean> simpleBeans = new ArrayList<SimpleAdvancedSearchSampleBean>();
		
		for (AdvancedSampleBean bean : sampleBeans) {
			
			SimpleAdvancedSearchSampleBean simpleBean = new SimpleAdvancedSearchSampleBean();
			simpleBean.transferAdvancedSampleBeanForResultView(bean, searchBean, resultView.getColumnTitles());
			simpleBeans.add(simpleBean);
		}
		
		resultView.setSamples(simpleBeans);
		resultView.transformToTableView();
		return resultView;
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
