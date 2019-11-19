/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.service.sample.helper;

import gov.nih.nci.cananolab.domain.common.Datum;
import gov.nih.nci.cananolab.domain.common.Finding;
import gov.nih.nci.cananolab.domain.common.PointOfContact;
import gov.nih.nci.cananolab.domain.particle.Characterization;
import gov.nih.nci.cananolab.domain.particle.Function;
import gov.nih.nci.cananolab.domain.particle.FunctionalizingEntity;
import gov.nih.nci.cananolab.domain.particle.NanomaterialEntity;
import gov.nih.nci.cananolab.domain.particle.Sample;
import gov.nih.nci.cananolab.dto.particle.AdvancedSampleBean;
import gov.nih.nci.cananolab.dto.particle.AdvancedSampleSearchBean;
import gov.nih.nci.cananolab.dto.particle.CharacterizationQueryBean;
import gov.nih.nci.cananolab.dto.particle.CompositionQueryBean;
import gov.nih.nci.cananolab.dto.particle.SampleQueryBean;
import gov.nih.nci.cananolab.exception.NoAccessException;
import gov.nih.nci.cananolab.security.enums.SecureClassesEnum;
import gov.nih.nci.cananolab.security.service.SpringSecurityAclService;
import gov.nih.nci.cananolab.system.applicationservice.CaNanoLabApplicationService;
import gov.nih.nci.cananolab.util.ClassUtils;
import gov.nih.nci.cananolab.util.Comparators;
import gov.nih.nci.cananolab.util.Constants;
import gov.nih.nci.cananolab.util.StringUtils;
import gov.nih.nci.cananolab.util.TextMatchMode;
import gov.nih.nci.cananolab.system.applicationservice.client.ApplicationServiceProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.hibernate.FetchMode;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Helper class providing implementations of advanced sample search methods
 * needed for both local implementation of SampleService and grid service *
 * 
 * @author pansu
 * 
 */
@Component("advancedSampleServiceHelper")
public class AdvancedSampleServiceHelper
{
	private static Logger logger = Logger.getLogger(AdvancedSampleServiceHelper.class);

	@Autowired
	private SpringSecurityAclService springSecurityAclService;
	
	/**
	 * Find sample names based on advanced search parameters
	 * 
	 * @param searchBean
	 * @return
	 * @throws Exception
	 */
	public List<String> findSampleIdsByAdvancedSearch(
			AdvancedSampleSearchBean searchBean) throws Exception {
		List<String> sampleIds = new ArrayList<String>();
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();
		// AND or all empty
		if (searchBean.getLogicalOperator().equals("and")
				|| searchBean.getSampleQueries().isEmpty()
				&& searchBean.getCharacterizationQueries().isEmpty()
				&& searchBean.getCompositionQueries().isEmpty()) {
			DetachedCriteria crit = DetachedCriteria.forClass(Sample.class,
					"rootCrit").setProjection(
					Projections.distinct(Property.forName("id")));
			setSampleCriteria(searchBean, crit);
			setCompositionCriteria(searchBean, crit);
			setCharacterizationCriteria(searchBean, crit);
			List results = appService.query(crit);
			for(int i = 0; i < results.size(); i++){
				String sampleId = results.get(i).toString();
				sampleIds.add(sampleId);
			}
		}
		// OR union the results
		else {
			Set<String> sampleIdSet = new HashSet<String>();
			// sample
			if (!searchBean.getSampleQueries().isEmpty()) {
				DetachedCriteria crit = DetachedCriteria.forClass(Sample.class,
						"rootCrit").setProjection(
						Projections.distinct(Property.forName("id")));
				setSampleCriteria(searchBean, crit);
				List results = appService.query(crit);
				for (int i = 0; i < results.size(); i++) {
					String sampleId = results.get(i).toString();
					sampleIds.add(sampleId);
				}
			}
			// composition
			if (!searchBean.getCompositionQueries().isEmpty()) {
				if( searchBean.getCompositionLogicalOperator().equals("and") ) {
					for (CompositionQueryBean query : searchBean.getCompositionQueries()) {
						List<String> subSampleIds = new ArrayList<String>();
						DetachedCriteria crit = DetachedCriteria.forClass(Sample.class,
								"rootCrit").setProjection(
								Projections.distinct(Property.forName("id")));
						setSampleCriteria(searchBean, crit);
						setCharacterizationCriteria(searchBean, crit);
						setCompositionCriteriaBase(searchBean, crit);

						switch (query.getCompositionType()) {
							case "function": {
								DetachedCriteria subCrit = getFunctionSubquery(query,
										"inherentFunction.", "function.", "id");
								crit.add(Subqueries.exists(subCrit));
								break;
							}
							case "nanomaterial entity": {
								DetachedCriteria subCrit = getNanomaterialEntitySubquery(query,
										"nanoEntity.", "id");
								crit.add(Subqueries.exists(subCrit));
								break;
							}
							case "functionalizing entity": {
								DetachedCriteria subCrit = getFunctionalizingEntitySubquery(
										query, "funcEntity.", "id");
								crit.add(Subqueries.exists(subCrit));
								break;
							}
						}
						
						List results = appService.query(crit);
						for (int i = 0; i < results.size(); i++) {
							String sampleId = results.get(i).toString();
							subSampleIds.add(sampleId);
						}
						
						if ( sampleIds.size() > 0 )
							sampleIds.retainAll(subSampleIds);
						else
							sampleIds.addAll(subSampleIds);
					}
				}
				else {
					DetachedCriteria crit = DetachedCriteria.forClass(Sample.class,
							"rootCrit").setProjection(
							Projections.distinct(Property.forName("id")));
					setCompositionCriteria(searchBean, crit);
					List results=appService.query(crit);
					for (int i = 0; i < results.size(); i++) {
						String sampleId = results.get(i).toString();
						sampleIds.add(sampleId);
					}
				}
			}
			if (!searchBean.getCharacterizationQueries().isEmpty()) {
				// characterization
				DetachedCriteria crit = DetachedCriteria.forClass(Sample.class,
						"rootCrit").setProjection(
						Projections.distinct(Property.forName("id")));
				setCharacterizationCriteria(searchBean, crit);
				List results=appService.query(crit);
				for (int i = 0; i < results.size(); i++) {
					String sampleId = results.get(i).toString();
					sampleIds.add(sampleId);
				}
			}
		}
		
		//filter out redundant ones and non-accessible ones
		List<String>filteredSampleIds=new ArrayList<String>();
		for (String sampleId: sampleIds) {
			if (!filteredSampleIds.contains(sampleId) && 
				(springSecurityAclService.currentUserHasReadPermission(Long.valueOf(sampleId), SecureClassesEnum.SAMPLE.getClazz()) ||
				 springSecurityAclService.currentUserHasWritePermission(Long.valueOf(sampleId), SecureClassesEnum.SAMPLE.getClazz()))) {
				filteredSampleIds.add(sampleId);
			} else { // ignore no access exception
				logger.debug("User doesn't have access to sample with id " + sampleId);
			}
		}
		return filteredSampleIds;
	}

	/**
	 * Find sample names based on advanced search parameters
	 * 
	 * @param searchBean
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> findSampleIdNamesByAdvancedSearch(
			AdvancedSampleSearchBean searchBean) throws Exception {
		List<String> sampleIds = new ArrayList<String>();
		
		Map<String, String> sampleIdNameMap = new HashMap<String, String>();
		
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();
		
		ProjectionList projList = Projections.projectionList();
		projList.add(Property.forName("id"));
		projList.add(Property.forName("name"));
		
		// AND or all empty
		if (searchBean.getLogicalOperator().equals("and")
				|| searchBean.getSampleQueries().isEmpty()
				&& searchBean.getCharacterizationQueries().isEmpty()
				&& searchBean.getCompositionQueries().isEmpty()) {
			DetachedCriteria crit = DetachedCriteria.forClass(Sample.class,
					"rootCrit").setProjection(
							Projections.distinct(projList));
					//Projections.distinct(Property.forName("id")));
			setSampleCriteria(searchBean, crit);
			setCompositionCriteria(searchBean, crit);
			setCharacterizationCriteria(searchBean, crit);
			List results = appService.query(crit);
			for (int i = 0; i < results.size(); i++) {
				
				Object[] row = (Object[]) results.get(i);
				String id = row[0].toString();
				String name = row[1].toString();
				
				logger.debug("id is: " + id);
				logger.debug("name is: " + name);

                sampleIds.add(id);
				
				sampleIdNameMap.put(id, name);
				
			}	
		}
		// OR union the results
		else {
			Set<String> sampleIdSet = new HashSet<String>();
			// sample
			if (!searchBean.getSampleQueries().isEmpty()) {
				
//				ProjectionList projList = Projections.projectionList();
//				projList.add(Property.forName("id"));
//				projList.add(Property.forName("name"));
				
				DetachedCriteria crit = DetachedCriteria.forClass(Sample.class,
						"rootCrit").setProjection(
								Projections.distinct(projList));		
						//Projections.distinct(Property.forName("id")));
				setSampleCriteria(searchBean, crit);
				List results = appService.query(crit);
				for (int i = 0; i < results.size(); i++) {
					
					Object[] row = (Object[]) results.get(i);
					String id = row[0].toString();
					String name = row[1].toString();
					logger.debug("id is: " + id);
					logger.debug("name is: " + name);
                    sampleIds.add(id);
					
					sampleIdNameMap.put(id, name);
				}
			}
			
			
//			// composition
			if (!searchBean.getCompositionQueries().isEmpty()) {
				if( searchBean.getCompositionLogicalOperator().equals("and") ) {
					for (CompositionQueryBean query : searchBean.getCompositionQueries()) {
						List<String> subSampleIds = new ArrayList<String>();
						DetachedCriteria crit = DetachedCriteria.forClass(Sample.class,
								"rootCrit").setProjection(
										Projections.distinct(projList));
								//Projections.distinct(Property.forName("id")));
						setSampleCriteria(searchBean, crit);
						setCharacterizationCriteria(searchBean, crit);
						setCompositionCriteriaBase(searchBean, crit);

						switch (query.getCompositionType()) {
							case "function": {
								DetachedCriteria subCrit = getFunctionSubquery(query,
										"inherentFunction.", "function.", "id");
								crit.add(Subqueries.exists(subCrit));
								break;
							}
							case "nanomaterial entity": {
								DetachedCriteria subCrit = getNanomaterialEntitySubquery(query,
										"nanoEntity.", "id");
								crit.add(Subqueries.exists(subCrit));
								break;
							}
							case "functionalizing entity": {
								DetachedCriteria subCrit = getFunctionalizingEntitySubquery(
										query, "funcEntity.", "id");
								crit.add(Subqueries.exists(subCrit));
								break;
							}
						}
						
						List results = appService.query(crit);
						for (int i = 0; i < results.size(); i++) {
							Object[] row = (Object[]) results.get(i);
							String id = row[0].toString();
							String name = row[1].toString();
							logger.debug("id is: " + id);
							logger.debug("name is: " + name);
                            //String sampleId = obj.toString();
							subSampleIds.add(id);
							
							sampleIdNameMap.put(id, name);
						}
						
						if ( sampleIds.size() > 0 )
							sampleIds.retainAll(subSampleIds);
						else
							sampleIds.addAll(subSampleIds);
					}
				}
				else {
					DetachedCriteria crit = DetachedCriteria.forClass(Sample.class,
							"rootCrit").setProjection(
									Projections.distinct(projList));
							//Projections.distinct(Property.forName("id")));
					setCompositionCriteria(searchBean, crit);
					List results=appService.query(crit);
					for (int i = 0; i < results.size(); i++) {
						
						Object[] row = (Object[]) results.get(i);
						String id = row[0].toString();
						String name = row[1].toString();
						logger.debug("id is: " + id);
						logger.debug("name is: " + name);
                        //String sampleId = obj.toString();
						sampleIds.add(id);
						
						sampleIdNameMap.put(id, name);
					}
				}
			}
			if (!searchBean.getCharacterizationQueries().isEmpty()) {
				// characterization
				DetachedCriteria crit = DetachedCriteria.forClass(Sample.class,
						"rootCrit").setProjection(
								Projections.distinct(projList));
						//Projections.distinct(Property.forName("id")));
				setCharacterizationCriteria(searchBean, crit);
				List results=appService.query(crit);
				for (int i = 0; i < results.size(); i++) {
					Object[] row = (Object[]) results.get(i);
					String id = row[0].toString();
					String name = row[1].toString();
					logger.debug("id is: " + id);
					logger.debug("name is: " + name);
                    //String sampleId = obj.toString();
					sampleIds.add(id);
					sampleIdNameMap.put(id, name);
				}
			}
		}
		
		Iterator<String> ite = sampleIdNameMap.keySet().iterator();
		while (ite.hasNext()) {
			Long sampleId = Long.valueOf(ite.next());
			if (!springSecurityAclService.currentUserHasReadPermission(sampleId, SecureClassesEnum.SAMPLE.getClazz()) &&
				!springSecurityAclService.currentUserHasWritePermission(sampleId, SecureClassesEnum.SAMPLE.getClazz())) { 
				logger.debug("User doesn't have access to sample with id " + sampleId);
				ite.remove();
			}
		}
		
		//filter out redundant ones and non-accessible ones
//		List<String>filteredSampleIds=new ArrayList<String>();
//		for (String sampleId: sampleIds) {
//			if (!filteredSampleIds.contains(sampleId)
//					&& StringUtils.containsIgnoreCase(getAccessibleData(),
//							sampleId)) {
//				filteredSampleIds.add(sampleId);
//			} else { // ignore no access exception
//				logger.debug("User doesn't have access to sample with id "
//						+ sampleId);
//			}
//		}
		
		return sampleIdNameMap;
	}

	/**
	 * Find sample details as an AdvancedSampleBean for the given sample name
	 * and advanced search parameters
	 * 
	 * @param searchBean
	 * @return
	 * @throws Exception
	 */
	public AdvancedSampleBean findAdvancedSampleByAdvancedSearch(String sampleId, AdvancedSampleSearchBean searchBean) throws Exception
	{
		if (!springSecurityAclService.currentUserHasReadPermission(Long.valueOf(sampleId), SecureClassesEnum.SAMPLE.getClazz()) &&
			!springSecurityAclService.currentUserHasWritePermission(Long.valueOf(sampleId), SecureClassesEnum.SAMPLE.getClazz())) {
			throw new NoAccessException();
		}
		// load sample first with point of contact info and function info and
		// datum info
		DetachedCriteria crit = DetachedCriteria.forClass(Sample.class).add(
				Restrictions.eq("id", new Long(sampleId)));
		crit.setFetchMode("primaryPointOfContact", FetchMode.JOIN);
		crit.setFetchMode("primaryPointOfContact.organization", FetchMode.JOIN);
		crit.setFetchMode("otherPointOfContactCollection", FetchMode.JOIN);
		crit.setFetchMode("otherPointOfContactCollection.organization",
				FetchMode.JOIN);
		crit.setFetchMode("sampleComposition.nanomaterialEntityCollection",
				FetchMode.JOIN);
		crit
				.setFetchMode(
						"sampleComposition.nanomaterialEntityCollection.composingElementCollection",
						FetchMode.JOIN);
		crit
				.setFetchMode(
						"sampleComposition.nanomaterialEntityCollection.composingElementCollection.inherentFunctionCollection",
						FetchMode.JOIN);

		crit.setFetchMode("sampleComposition.functionalizingEntityCollection",
				FetchMode.JOIN);
		crit
				.setFetchMode(
						"sampleComposition.functionalizingEntityCollection.functionCollection",
						FetchMode.JOIN);
		crit.setFetchMode("characterizationCollection", FetchMode.JOIN);
		crit.setFetchMode("characterizationCollection.findingCollection",
				FetchMode.JOIN);
		crit.setFetchMode(
				"characterizationCollection.findingCollection.datumCollection",
				FetchMode.JOIN);
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();
		crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		List result = appService.query(crit);
		Sample sample = null;
		if (!result.isEmpty()) {
			sample = (Sample) result.get(0);
		}

		List<PointOfContact> pocs = findPointOfContactsBy(sample, searchBean);
		List<Function> functions = findFunctionsBy(sampleId, searchBean);
		List<NanomaterialEntity> nanoEntities = findNanomaterialEntitiesBy(
				sampleId, searchBean);
		List<FunctionalizingEntity> funcEntities = findFunctionalizingEntitiesBy(
				sampleId, searchBean);
		List<Characterization> charas = findCharacterizationsBy(sampleId,
				searchBean);
		List<Datum> data = findDataBy(sample, searchBean);
        return new AdvancedSampleBean(
                sampleId, pocs, functions, nanoEntities, funcEntities, charas,
                data, searchBean, sample);
	}

	private List<Characterization> findCharacterizationsBy(String sampleId,
			AdvancedSampleSearchBean searchBean) throws Exception {
		List<Characterization> chars = new ArrayList<Characterization>();
		if (searchBean.getCharacterizationQueries().isEmpty()) {
			return chars;
		}
		Long id = new Long(sampleId);
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();
		if (searchBean.getCharacterizationQueries().size() == 1
				|| searchBean.getCharacterizationLogicalOperator().equals("or")) {
			DetachedCriteria crit = DetachedCriteria.forClass(
					Characterization.class, "rootCrit");
			crit.createAlias("sample", "sample");
			// join finding and datum
			if (searchBean.getHasDatum()) {
				crit.createAlias("findingCollection", "finding",
						CriteriaSpecification.LEFT_JOIN);
				crit.createAlias("finding.datumCollection", "datum",
						CriteriaSpecification.LEFT_JOIN);
			}
			crit.add(Restrictions.eq("sample.id", id));
			Disjunction charDisjunction = getCharacterizationDisjunction(
					searchBean, crit, "");
			crit.add(charDisjunction);
			crit
					.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
			List results = appService.query(crit);
			for (int i = 0; i < results.size(); i++) {
				Characterization achar = (Characterization) results.get(i);
				chars.add(achar);
			}
		} else {
			// hibernate doesn't support union have to execute the query one at
			// a time union the result in Java
			for (CharacterizationQueryBean charQuery : searchBean
					.getCharacterizationQueries()) {
				DetachedCriteria crit = DetachedCriteria.forClass(
						Characterization.class, "rootCrit");
				crit.createAlias("sample", "sample");
				crit.add(Restrictions.eq("sample.id", id));
				DetachedCriteria subCrit = getCharacterizationSubquery(
						charQuery, "id");
				crit.add(Subqueries.exists(subCrit));
				crit
						.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
				List results = appService.query(crit);
				for (int i = 0; i < results.size(); i++) {
					Characterization achar = (Characterization) results.get(i);
					if (!chars.contains(achar)) {
						chars.add(achar);
					}
				}
			}
		}
		chars.sort(new Comparators.CharacterizationNameAssayTypeDateComparator());
		return chars;
	}

	private Junction getDatumJunction(AdvancedSampleSearchBean searchBean) {
		// if AND and more than one type of datum, don't use junction
		if (searchBean.getCharacterizationLogicalOperator().equals("and")
				&& searchBean.getDatumTypeCount() > 1) {
			return null;
		}
		Disjunction datumDisjunction = Restrictions.disjunction();
		Conjunction datumConjunction = Restrictions.conjunction();
		for (CharacterizationQueryBean charQuery : searchBean
				.getCharacterizationQueries()) {
			Criterion datumCrit = getDatumCriterion(charQuery);
			if (datumCrit != null) {
				datumDisjunction.add(datumCrit);
				if (searchBean.getDatumTypeCount() == 1) {
					datumConjunction.add(datumCrit);
				}
			}
		}
		// default to or if only one query
        return (searchBean.getCharacterizationLogicalOperator()
                .equals("or") || searchBean.getCharacterizationQueries().size() == 1) ? datumDisjunction
                : datumConjunction;
	}

	private DetachedCriteria getDatumSubquery(
			CharacterizationQueryBean charQuery) {
		DetachedCriteria subCrit = DetachedCriteria.forClass(Datum.class,
				"subCrit");
		subCrit.setProjection(Projections.distinct(Property
				.forName("id")));
		Criterion datumCrit = getDatumCriterion(charQuery);
		subCrit.add(datumCrit);
		subCrit.add(Restrictions.eqProperty("subCrit." + "id",
				"rootCrit.id"));
		return subCrit;
	}

	private Criterion getDatumCriterion(CharacterizationQueryBean charQuery) {
		Criterion datumCrit = null;
		// datum name
		if (!StringUtils.isEmpty(charQuery.getDatumName())) {
			datumCrit = Restrictions.eq("name", charQuery.getDatumName());
		}
		// datum value
		if (!StringUtils.isEmpty(charQuery.getDatumValue())) {
			Float datumValue = new Float(charQuery.getDatumValue());
			datumCrit = Restrictions.and(datumCrit, Restrictions.eq(
					"valueUnit", charQuery.getDatumValueUnit()));
			if ("=".equals(charQuery.getOperand())) {
				datumCrit = Restrictions.and(datumCrit, Expression.eq("value",
						datumValue));
			} else if (">".equals(charQuery.getOperand())) {
				datumCrit = Restrictions.and(datumCrit, Expression.gt("value",
						datumValue));
			} else if (">=".equals(charQuery.getOperand())) {
				datumCrit = Restrictions.and(datumCrit, Expression.ge("value",
						datumValue));
			} else if ("<".equals(charQuery.getOperand())) {
				datumCrit = Restrictions.and(datumCrit, Expression.lt("value",
						datumValue));
			} else if ("<=".equals(charQuery.getOperand())) {
				datumCrit = Restrictions.and(datumCrit, Expression.le("value",
						datumValue));
			}
		}
		return datumCrit;
	}

	private List<Datum> findDataBy(Sample sample,
			AdvancedSampleSearchBean searchBean) throws Exception {
		List<Datum> data = new ArrayList<Datum>();
		Boolean hasDatum = searchBean.getHasDatum();
		if (!hasDatum) {
			return data;
		}
		List<Datum> sampleData = new ArrayList<Datum>();
		for (Characterization achar : sample.getCharacterizationCollection()) {
			for (Finding finding : achar.getFindingCollection()) {
				sampleData.addAll(finding.getDatumCollection());
			}
		}
		DetachedCriteria crit = DetachedCriteria.forClass(Datum.class);
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();
		Junction datumJunction = getDatumJunction(searchBean);
		if (datumJunction != null) {
			crit.add(datumJunction);
			crit
					.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
			List results = appService.query(crit);
			for (int i = 0; i < results.size(); i++) {
				Datum datum = (Datum) results.get(i);
				if (sampleData.contains(datum)) {
					data.add(datum);
				}
			}
		} else {
			// hibernate doesn't support union have to execute the query one at
			// a time union the result in Java
			for (CharacterizationQueryBean charQuery : searchBean
					.getCharacterizationQueries()) {
				// query for datum only when datum is specified as a search
				// criterion
				if (!StringUtils.isEmpty(charQuery.getDatumName())) {
					crit = DetachedCriteria.forClass(Datum.class, "rootCrit");
					DetachedCriteria subCrit = getDatumSubquery(charQuery);
					crit.add(Subqueries.exists(subCrit));
					crit
							.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
					List results = appService.query(crit);
					for (int i = 0; i < results.size(); i++) {
						Datum datum = (Datum) results.get(i);
						if (sampleData.contains(datum)) {
							data.add(datum);
						}
					}
				}
			}
		}

		return data;
	}

	private List<FunctionalizingEntity> findFunctionalizingEntitiesBy(
			String sampleId, AdvancedSampleSearchBean searchBean)
			throws Exception {
		List<FunctionalizingEntity> entities = new ArrayList<FunctionalizingEntity>();
		if (!searchBean.getHasAgentMaterial()) {
			return entities;
		}
		Long id = new Long(sampleId);
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();
		DetachedCriteria crit = DetachedCriteria
				.forClass(FunctionalizingEntity.class);
		Junction junction = getFunctionalizingEntityJunction(searchBean, crit);
		if (junction != null) {
			crit.createAlias("sampleComposition", "comp",
					CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("comp.sample", "sample",
					CriteriaSpecification.LEFT_JOIN);
			crit.add(Restrictions.eq("sample.id", id));
			crit.add(junction);
			crit.setFetchMode("functionCollection", FetchMode.JOIN);
			crit.setFetchMode("functionCollection.targetCollection",
					FetchMode.JOIN);
			crit.setFetchMode("fileCollection", FetchMode.JOIN);
			crit.setFetchMode("fileCollection.keywordCollection",
					FetchMode.JOIN);
			crit
					.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
			List results = appService.query(crit);
			for (int i = 0; i < results.size(); i++) {
				FunctionalizingEntity entity = (FunctionalizingEntity) results.get(i);
				entities.add(entity);
			}
		} else if (searchBean.getFuncEntityCount() > 1) {
			// Hibernate Criteria API doesn't support union, union in java
			for (CompositionQueryBean query : searchBean
					.getCompositionQueries()) {
				if (query.getCompositionType().equals("functionalizing entity")) {
					crit = DetachedCriteria
							.forClass(FunctionalizingEntity.class);
					crit.createAlias("sampleComposition", "comp",
							CriteriaSpecification.LEFT_JOIN);
					crit.createAlias("comp.sample", "sample",
							CriteriaSpecification.LEFT_JOIN);
					crit.add(Restrictions.eq("sample.id", id));
					DetachedCriteria subCrit = getFunctionalizingEntitySubquery(
							query, "", "id");
					crit.add(Subqueries.exists(subCrit));
					crit.setFetchMode("functionCollection", FetchMode.JOIN);
					crit.setFetchMode("functionCollection.targetCollection",
							FetchMode.JOIN);
					crit.setFetchMode("fileCollection", FetchMode.JOIN);
					crit.setFetchMode("fileCollection.keywordCollection",
							FetchMode.JOIN);
					crit
							.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
					List results = appService.query(crit);
					for (int i = 0; i < results.size(); i++) {
						FunctionalizingEntity entity = (FunctionalizingEntity) results.get(i);
						if (!entities.contains(entity)) {
							entities.add(entity);
						}
					}
				}
			}
		}
		return entities;
	}

	private List<Function> findFunctionsBy(String sampleId,
			AdvancedSampleSearchBean searchBean) throws Exception {
		List<Function> functions = new ArrayList<Function>();
		if (!searchBean.getHasFunction()) {
			return functions;
		}
		Long id = new Long(sampleId);
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();
		DetachedCriteria crit = DetachedCriteria.forClass(Function.class);
		Junction junction = getFunctionJunction(searchBean, crit);
		if (junction != null) {
			crit.createAlias("composingElement", "ce",
					CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("ce.nanomaterialEntity", "nanoEntity",
					CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("nanoEntity.sampleComposition", "comp",
					CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("comp.sample", "sample",
					CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("functionalizingEntity", "funcEntity",
					CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("funcEntity.sampleComposition", "comp2",
					CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("comp2.sample", "sample2",
					CriteriaSpecification.LEFT_JOIN);
			crit.add(Restrictions.or(Restrictions.eq("sample.id", id),
					Restrictions.eq("sample2.id", id)));
			crit.add(junction);
			crit.setFetchMode("targetCollection", FetchMode.JOIN);

			List results = appService.query(crit);
			for (int i = 0; i < results.size(); i++) {
				Function function = (Function) results.get(i);
				functions.add(function);
			}
		} else if (searchBean.getFuncCount() > 1) {
			// Hibernate Criteria API doesn't support union, union in java
			for (CompositionQueryBean query : searchBean
					.getCompositionQueries()) {
				if (query.getCompositionType().equals("function")) {
					crit = DetachedCriteria.forClass(Function.class);
					crit.createAlias("composingElement", "ce",
							CriteriaSpecification.LEFT_JOIN);
					crit.createAlias("ce.nanomaterialEntity", "nanoEntity",
							CriteriaSpecification.LEFT_JOIN);
					crit.createAlias("nanoEntity.sampleComposition", "comp",
							CriteriaSpecification.LEFT_JOIN);
					crit.createAlias("comp.sample", "sample",
							CriteriaSpecification.LEFT_JOIN);
					crit.createAlias("functionalizingEntity", "funcEntity",
							CriteriaSpecification.LEFT_JOIN);
					crit.createAlias("funcEntity.sampleComposition", "comp2",
							CriteriaSpecification.LEFT_JOIN);
					crit.createAlias("comp2.sample", "sample2",
							CriteriaSpecification.LEFT_JOIN);
					crit.add(Restrictions.or(Restrictions.eq("sample.id", id),
							Restrictions.eq("sample2.id", id)));
					DetachedCriteria subCrit = getFunctionSubquery(query, "",
							"", "id");
					crit.add(Subqueries.exists(subCrit));
					crit.setFetchMode("targetCollection", FetchMode.JOIN);
					crit
							.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
					List results = appService.query(crit);
					for (int i = 0; i < results.size(); i++) {
						Function function = (Function) results.get(i);
						if (!functions.contains(function)) {
							functions.add(function);
						}
					}
				}
			}
		}
		return functions;
	}

	private List<NanomaterialEntity> findNanomaterialEntitiesBy(
			String sampleId, AdvancedSampleSearchBean searchBean)
			throws Exception {
		List<NanomaterialEntity> entities = new ArrayList<NanomaterialEntity>();
		Long id = new Long(sampleId);
		if (!searchBean.getHasNanomaterial()) {
			return entities;
		}
		DetachedCriteria crit = DetachedCriteria
				.forClass(NanomaterialEntity.class);
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();
		Junction junction = getNanomaterialEntityJunction(searchBean, crit);
		if (junction != null) {
			// join nanomaterialEntity
			if (searchBean.getHasNanomaterial() && !searchBean.getHasFunction()) {
				if (searchBean.getHasChemicalName()) {
					crit.createAlias("composingElementCollection",
							"compElement", CriteriaSpecification.LEFT_JOIN);
				}
			}
			crit.createAlias("sampleComposition", "comp");
			crit.createAlias("comp.sample", "sample");
			crit.add(Restrictions.eq("sample.id", id));
			crit.add(junction);
			crit.setFetchMode("fileCollection", FetchMode.JOIN);
			crit.setFetchMode("fileCollection.keywordCollection",
					FetchMode.JOIN);
			crit.setFetchMode("composingElementCollection", FetchMode.JOIN);
			crit.setFetchMode(
					"composingElementCollection.inherentFunctionCollection",
					FetchMode.JOIN);
			crit
					.setFetchMode(
							"composingElementCollection.inherentFunctionCollection.targetCollection",
							FetchMode.JOIN);
			crit
					.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
			List results = appService.query(crit);
			for (int i = 0; i < results.size(); i++) {
				NanomaterialEntity entity = (NanomaterialEntity) results.get(i);
				entities.add(entity);
			}
		} else if (searchBean.getNanoEntityCount() > 1) {
			// Hibernate Criteria API doesn't support union, union in java
			for (CompositionQueryBean query : searchBean
					.getCompositionQueries()) {
				if (query.getCompositionType().equals("nanomaterial entity")) {
					crit = DetachedCriteria.forClass(NanomaterialEntity.class);
					crit.createAlias("sampleComposition", "comp",
							CriteriaSpecification.LEFT_JOIN);
					crit.createAlias("comp.sample", "sample",
							CriteriaSpecification.LEFT_JOIN);
					if (!StringUtils.isEmpty(query.getChemicalName())) {
						crit.createAlias("composingElementCollection",
								"compElement");
					}
					crit.add(Restrictions.eq("sample.id", id));
					crit.setFetchMode("fileCollection", FetchMode.JOIN);
					crit.setFetchMode("fileCollection.keywordCollection",
							FetchMode.JOIN);
					crit.setFetchMode("composingElementCollection",
							FetchMode.JOIN);
					crit
							.setFetchMode(
									"composingElementCollection.inherentFunctionCollection",
									FetchMode.JOIN);
					crit
							.setFetchMode(
									"composingElementCollection.inherentFunctionCollection.targetCollection",
									FetchMode.JOIN);
					DetachedCriteria subCrit = getNanomaterialEntitySubquery(
							query, "", "id");
					crit.add(Subqueries.exists(subCrit));
					crit
							.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
					List results = appService.query(crit);
					for (int i = 0; i < results.size(); i++) {
						NanomaterialEntity entity = (NanomaterialEntity) results.get(i);
						if (!entities.contains(entity)) {
							entities.add(entity);
						}
					}
				}
			}
		}
		return entities;
	}

	private List<PointOfContact> findPointOfContactsBy(Sample sample,
			AdvancedSampleSearchBean searchBean) throws Exception {
		if (!searchBean.getHasPOC()) {
			return null;
		}

		// get POCs associated with the sample
		List<PointOfContact> samplePOCs = new ArrayList<PointOfContact>();
		samplePOCs.add(sample.getPrimaryPointOfContact());
		samplePOCs.addAll(sample.getOtherPointOfContactCollection());
		List<PointOfContact> pocs = new ArrayList<PointOfContact>(samplePOCs);

		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();
		if (searchBean.getCharacterizationQueries().size() == 1
				|| searchBean.getSampleLogicalOperator().equals("or")) {
			DetachedCriteria crit = DetachedCriteria
					.forClass(PointOfContact.class);
			crit.createAlias("organization", "organization");
			crit.add(getPointOfContactDisjunction(searchBean, "", ""));
			crit.setFetchMode("organization", FetchMode.JOIN);
			List results = appService.query(crit);
			for (int i = 0; i < results.size(); i++) {
				PointOfContact poc = (PointOfContact) results.get(i);
				// check if in sample POCs
				if (!samplePOCs.contains(poc)) {
					pocs.remove(poc);
				}
			}
		} else {
			for (SampleQueryBean query : searchBean.getSampleQueries()) {
				if (query.getNameType().equals("point of contact name")) {
					DetachedCriteria crit = DetachedCriteria
							.forClass(PointOfContact.class);
					crit.createAlias("organization", "organization");
					DetachedCriteria subCrit = getPointOfContactSubquery(query,
							"", "");
					crit.add(Subqueries.exists(subCrit));
					crit.setFetchMode("organization", FetchMode.JOIN);
					List results = appService.query(crit);
					for (int i = 0; i < results.size(); i++) {
						PointOfContact poc = (PointOfContact) results.get(i);
						// check if in sample POCs
						if (!samplePOCs.contains(poc)) {
							pocs.remove(poc);
						}
					}
				}
			}
		}
		return pocs;
	}

	private Criterion getCharacterizationCriterion(
			CharacterizationQueryBean charQuery, String charAlias) {
		String charClassName = ClassUtils
				.getShortClassNameFromDisplayName(charQuery
						.getCharacterizationName());
		Criterion charCrit;
		if (charClassName == null) {
			Criterion otherCharCrit1 = Restrictions.eq(charAlias + "class",
					"OtherCharacterization");
			Criterion otherCharCrit2 = Restrictions.eq(charAlias + "name",
					charQuery.getCharacterizationName());
			charCrit = Restrictions.and(otherCharCrit1, otherCharCrit2);
		} else {
			charCrit = Restrictions.eq(charAlias + "class", charClassName);
		}
		// assay type
		if (!StringUtils.isEmpty(charQuery.getAssayType())) {
			charCrit = Restrictions.and(charCrit, Restrictions.eq(charAlias
					+ "assayType", charQuery.getAssayType()));
		}
		// datum name
		if (!StringUtils.isEmpty(charQuery.getDatumName())) {
			charCrit = Restrictions.and(charCrit, Restrictions.eq("datum.name",
					charQuery.getDatumName()));
		}
		// datum value
		if (!StringUtils.isEmpty(charQuery.getDatumValue())) {
			Float datumValue = new Float(charQuery.getDatumValue());
			charCrit = Restrictions.and(charCrit, Restrictions.eq(
					"datum.valueUnit", charQuery.getDatumValueUnit()));
			switch (charQuery.getOperand()) {
				case "=":
				case "is":
					charCrit = Restrictions.and(charCrit, Expression.eq(
							"datum.value", datumValue));
					break;
				case ">":
					charCrit = Restrictions.and(charCrit, Expression.gt(
							"datum.value", datumValue));
					break;
				case ">=":
					charCrit = Restrictions.and(charCrit, Expression.ge(
							"datum.value", datumValue));
					break;
				case "<":
					charCrit = Restrictions.and(charCrit, Expression.lt(
							"datum.value", datumValue));
					break;
				case "<=":
					charCrit = Restrictions.and(charCrit, Expression.le(
							"datum.value", datumValue));
					break;
			}
			// ignore the bogus place holder entered for emtpy datum/condition
			// cells
			charCrit = Restrictions.and(charCrit, Expression.not(Expression
					.ilike("datum.createdBy",
							Constants.PLACEHOLDER_DATUM_CONDITION_CREATED_BY)));
		}
		return charCrit;
	}

	private Disjunction getCharacterizationDisjunction(
			AdvancedSampleSearchBean searchBean, DetachedCriteria crit,
			String charAlias) {
		Disjunction charDisjunction = Restrictions.disjunction();
		for (CharacterizationQueryBean charQuery : searchBean
				.getCharacterizationQueries()) {
			Criterion charCrit = getCharacterizationCriterion(charQuery,
					charAlias);
			if (charCrit != null) {
				charDisjunction.add(charCrit);
			}
		}
		return charDisjunction;
	}

	private DetachedCriteria getCharacterizationSubquery(
			CharacterizationQueryBean charQuery, String projectionProperty) {
		DetachedCriteria subCrit = DetachedCriteria.forClass(
				Characterization.class, "subCrit");
		subCrit.setProjection(Projections.distinct(Property
				.forName(projectionProperty)));
		// join finding and datum
		if (!StringUtils.isEmpty(charQuery.getDatumName())) {
			subCrit.createAlias("subCrit.findingCollection", "finding",
					CriteriaSpecification.LEFT_JOIN);
			subCrit.createAlias("finding.datumCollection", "datum",
					CriteriaSpecification.LEFT_JOIN);
		}
		Criterion charCrit = getCharacterizationCriterion(charQuery, "");
		subCrit.add(charCrit);
		subCrit.add(Restrictions.eqProperty("subCrit." + projectionProperty,
				"rootCrit.id"));
		return subCrit;
	}

	/**
	 * Get the junction used in composition queries
	 * 
	 * @param searchBean
	 * @param crit
	 * @return
	 * @throws Exception
	 */
	private Junction getCompositionJunction(
			AdvancedSampleSearchBean searchBean, DetachedCriteria crit)
			throws Exception {
		// if AND and more than one query per type, don't use junction
		if (searchBean.getCompositionLogicalOperator().equals("and")
				&& searchBean.getFuncCount() > 1
				&& searchBean.getNanoEntityCount() > 1
				&& searchBean.getFuncEntityCount() > 1) {
			return null;
		}
		Disjunction compDisjunction = Restrictions.disjunction();
		Conjunction compConjunction = Restrictions.conjunction();
		for (CompositionQueryBean compQuery : searchBean
				.getCompositionQueries()) {
			// function
			switch (compQuery.getCompositionType()) {
				case "function":
					Criterion funcCrit = this.getFunctionCriterion(compQuery,
							"inherentFunction.", "function.");
					if (funcCrit != null) {
						compDisjunction.add(funcCrit);
						// only add to conjunction if there is only one query for
						// the type
						if (searchBean.getFuncCount() == 1) {
							compConjunction.add(funcCrit);
						}
					}
					break;
				// nanomaterial entity
				case "nanomaterial entity":
					Criterion nanoEntityCrit = getNanomaterialEntityCriterion(
							compQuery, "nanoEntity.");
					if (nanoEntityCrit != null) {
						compDisjunction.add(nanoEntityCrit);
						// only add to conjunction if there is only one query for
						// the type
						if (searchBean.getNanoEntityCount() == 1) {
							compConjunction.add(nanoEntityCrit);
						}
					}
					break;
				// functionalizing entity
				case "functionalizing entity":
					Criterion funcEntityCrit = getFunctionalizingEntityCriterion(
							compQuery, "funcEntity.");
					if (funcEntityCrit != null) {
						compDisjunction.add(funcEntityCrit);
						// only add to conjunction if there is only one query for
						// the type
						if (searchBean.getFuncEntityCount() == 1) {
							compConjunction.add(funcEntityCrit);
						}
					}
					break;
			}
		}
		// default to or if only one query
        return (searchBean.getCompositionLogicalOperator().equals(
                "or") || searchBean.getCompositionQueries().size() == 1) ? compDisjunction
                : compConjunction;
	}

	/**
	 * Get the junction used in function queries
	 * 
	 * @param searchBean
	 * @param crit
	 * @return
	 * @throws Exception
	 */
	private Junction getFunctionJunction(AdvancedSampleSearchBean searchBean,
			DetachedCriteria crit) throws Exception {
		// if AND and more than one query per type, don't use junction
		if (searchBean.getCompositionLogicalOperator().equals("and")
				&& searchBean.getFuncCount() > 1) {
			return null;
		}
		Disjunction compDisjunction = Restrictions.disjunction();
		Conjunction compConjunction = Restrictions.conjunction();
		for (CompositionQueryBean compQuery : searchBean
				.getCompositionQueries()) {
			// function
			if (compQuery.getCompositionType().equals("function")) {
				Criterion funcCrit = this.getFunctionCriterion(compQuery, "",
						"");
				if (funcCrit != null) {
					compDisjunction.add(funcCrit);
					// only add to conjunction if there is only one query for
					// the type
					if (searchBean.getFuncCount() == 1) {
						compConjunction.add(funcCrit);
					}
				}
			}
		}
		// default to or if only one query
        return (searchBean.getCompositionLogicalOperator().equals(
                "or") || searchBean.getCompositionQueries().size() == 1) ? compDisjunction
                : compConjunction;
	}

	/**
	 * Get the junction used in nanomaterial entity queries
	 * 
	 * @param searchBean
	 * @param crit
	 * @return
	 * @throws Exception
	 */
	private Junction getNanomaterialEntityJunction(
			AdvancedSampleSearchBean searchBean, DetachedCriteria crit)
			throws Exception {
		// if AND and more than one query per type, don't use junction
		if (searchBean.getCompositionLogicalOperator().equals("and")
				&& searchBean.getNanoEntityCount() > 1) {
			return null;
		}
		Disjunction compDisjunction = Restrictions.disjunction();
		Conjunction compConjunction = Restrictions.conjunction();
		for (CompositionQueryBean compQuery : searchBean
				.getCompositionQueries()) {
			// nanomaterial entity
			if (compQuery.getCompositionType().equals("nanomaterial entity")) {
				Criterion nanoEntityCrit = getNanomaterialEntityCriterion(
						compQuery, "");
				if (nanoEntityCrit != null) {
					compDisjunction.add(nanoEntityCrit);
					// only add to conjunction if there is only one query for
					// the type
					if (searchBean.getNanoEntityCount() == 1) {
						compConjunction.add(nanoEntityCrit);
					}
				}
			}
		}
		// default to or if only one query
        return (searchBean.getCompositionLogicalOperator().equals(
                "or") || searchBean.getCompositionQueries().size() == 1) ? compDisjunction
                : compConjunction;
	}

	/**
	 * Get the junction used in functionalizing entity queries
	 * 
	 * @param searchBean
	 * @param crit
	 * @return
	 * @throws Exception
	 */
	private Junction getFunctionalizingEntityJunction(
			AdvancedSampleSearchBean searchBean, DetachedCriteria crit)
			throws Exception {
		// if AND and more than one query per type, don't use junction
		if (searchBean.getCompositionLogicalOperator().equals("and")
				&& searchBean.getFuncEntityCount() > 1) {
			return null;
		}
		Disjunction compDisjunction = Restrictions.disjunction();
		Conjunction compConjunction = Restrictions.conjunction();
		for (CompositionQueryBean compQuery : searchBean
				.getCompositionQueries()) {
			// functionalizing entity
			if (compQuery.getCompositionType().equals("functionalizing entity")) {
				Criterion funcEntityCrit = getFunctionalizingEntityCriterion(
						compQuery, "");
				if (funcEntityCrit != null) {
					compDisjunction.add(funcEntityCrit);
					// only add to conjunction if there is only one query for
					// the type
					if (searchBean.getFuncEntityCount() == 1) {
						compConjunction.add(funcEntityCrit);
					}
				}
			}
		}
		// default to or if only one query
        return (searchBean.getCompositionLogicalOperator().equals(
                "or") || searchBean.getCompositionQueries().size() == 1) ? compDisjunction
                : compConjunction;
	}

	private Criterion getFunctionalizingEntityCriterion(
			CompositionQueryBean compQuery, String entityAlias)
			throws Exception {
		TextMatchMode chemicalNameMatchMode = null;
		if (compQuery.getOperand().equals("equals")) {
			chemicalNameMatchMode = new TextMatchMode(compQuery
					.getChemicalName());
		} else if (compQuery.getOperand().equals(
				Constants.STRING_OPERAND_CONTAINS)) {
			chemicalNameMatchMode = new TextMatchMode("*"
					+ compQuery.getChemicalName() + "*");
		}
		String funcEntityClassName = ClassUtils
				.getShortClassNameFromDisplayName(compQuery.getEntityType());
		Class clazz = ClassUtils.getFullClass("agentmaterial."
				+ funcEntityClassName);
		Criterion funcEntityCrit;
		// other entity type
		if (clazz == null) {
			/*Criterion otherFuncCrit1 = Restrictions.eq(entityAlias + "class",
					"OtherFunctionalizingEntity");
			Criterion otherFuncCrit2 = Restrictions.eq(entityAlias + "type",
					compQuery.getEntityType());
			funcEntityCrit = Restrictions.and(otherFuncCrit1, otherFuncCrit2);*/
		
			Integer funcClassNameInteger = Constants.FUNCTIONALIZING_ENTITY_SUBCLASS_ORDER_MAP
					.get("OtherFunctionalizingEntity");
			funcEntityCrit = Restrictions.eq(entityAlias + "class",
							funcClassNameInteger);
		
		} else {
			Integer funcClassNameInteger = Constants.FUNCTIONALIZING_ENTITY_SUBCLASS_ORDER_MAP
					.get(funcEntityClassName);
			funcEntityCrit = Restrictions.eq(entityAlias + "class",
					funcClassNameInteger);
		}
		if (!StringUtils.isEmpty(compQuery.getChemicalName())
				&& !StringUtils.isEmpty(compQuery.getOperand())) {
			Criterion chemicalNameCrit = Restrictions.ilike(entityAlias
					+ "name", chemicalNameMatchMode.getUpdatedText(),
					chemicalNameMatchMode.getMatchMode());
			funcEntityCrit = Restrictions.and(funcEntityCrit, chemicalNameCrit);
		}
		return funcEntityCrit;
	}

	private DetachedCriteria getFunctionalizingEntitySubquery(
			CompositionQueryBean query, String funcEntityAlias,
			String projectionProperty) throws Exception {
		DetachedCriteria subCrit = DetachedCriteria.forClass(
				FunctionalizingEntity.class, "subCrit");
		subCrit.setProjection(Projections.distinct(Property
				.forName(projectionProperty)));
		Criterion funcCrit = getFunctionalizingEntityCriterion(query, "");
		subCrit.add(funcCrit);
		subCrit.add(Restrictions.eqProperty("subCrit." + projectionProperty,
				funcEntityAlias + "id"));
		return subCrit;
	}

	private DetachedCriteria getFunctionSubquery(CompositionQueryBean query,
			String funcAlias1, String funcAlias2, String projectionProperty)
			throws Exception {
		DetachedCriteria subCrit = DetachedCriteria.forClass(Function.class,
				"subCrit");
		subCrit.setProjection(Projections.distinct(Property
				.forName(projectionProperty)));
		Criterion funcCrit = getFunctionCriterion(query, "", "");
		subCrit.add(funcCrit);
		if (funcAlias1.equals(funcAlias2)) {
			subCrit.add(Restrictions.eqProperty(
					"subCrit." + projectionProperty, funcAlias1 + "id"));
		} else {
			subCrit.add(Restrictions.or(Restrictions.eqProperty("subCrit."
					+ projectionProperty, funcAlias1 + "id"), Restrictions
					.eqProperty("subCrit." + projectionProperty, funcAlias2
							+ "id")));
		}
		return subCrit;
	}

	private Criterion getFunctionCriterion(CompositionQueryBean compQuery,
			String functionAlias1, String functionAlias2) throws Exception {
		String funcClassName = ClassUtils
				.getShortClassNameFromDisplayName(compQuery.getEntityType());
		Class clazz = ClassUtils.getFullClass("function." + funcClassName);
		Criterion funcCrit, funcCrit1, funcCrit2;
		// other function type
		if (clazz == null) {
			if (!functionAlias1.equals(functionAlias2)) {
				// inherent function
				Criterion otherFuncCrit1 = Restrictions.eq(functionAlias1
						+ "class", "OtherFunction");
				Criterion otherFuncCrit2 = Restrictions.eq(functionAlias1
						+ "type", compQuery.getEntityType());
				funcCrit1 = Restrictions.and(otherFuncCrit1, otherFuncCrit2);
				// function
				Criterion otherFuncCrit3 = Restrictions.eq(functionAlias2
						+ "class", "OtherFunction");
				Criterion otherFuncCrit4 = Restrictions.eq(functionAlias2
						+ "type", compQuery.getEntityType());
				funcCrit2 = Restrictions.and(otherFuncCrit3, otherFuncCrit4);
				funcCrit = Restrictions.or(funcCrit1, funcCrit2);
			} else {
				Criterion otherFuncCrit1 = Restrictions.eq(functionAlias1
						+ "class", "OtherFunction");
				Criterion otherFuncCrit2 = Restrictions.eq(functionAlias1
						+ "type", compQuery.getEntityType());
				funcCrit = Restrictions.and(otherFuncCrit1, otherFuncCrit2);
			}
		} else {
			if (!functionAlias1.equals(functionAlias2)) {
				funcCrit1 = Restrictions.eq(functionAlias1 + "class",
						funcClassName);
				funcCrit2 = Restrictions.eq(functionAlias2 + "class",
						funcClassName);
				funcCrit = Restrictions.and(funcCrit1, funcCrit2);
			} else {
				funcCrit = Restrictions.eq(functionAlias1 + "class",
						funcClassName);
			}
		}
		return funcCrit;
	}

	private Criterion getNanomaterialEntityCriterion(
			CompositionQueryBean compQuery, String entityAlias)
			throws Exception {
		TextMatchMode chemicalNameMatchMode = null;
		if (compQuery.getOperand().equals("equals")) {
			chemicalNameMatchMode = new TextMatchMode(compQuery
					.getChemicalName());
		} else if (compQuery.getOperand().equals(
				Constants.STRING_OPERAND_CONTAINS)) {
			chemicalNameMatchMode = new TextMatchMode("*"
					+ compQuery.getChemicalName() + "*");
		}
		String nanoEntityClassName = ClassUtils
				.getShortClassNameFromDisplayName(compQuery.getEntityType());
		Class clazz = ClassUtils.getFullClass("nanomaterial."
				+ nanoEntityClassName);
		Criterion nanoEntityCrit;
		// other entity type
		if (clazz == null) {
			Criterion otherNanoCrit1 = Restrictions.eq(entityAlias + "class",
					"OtherNanomaterialEntity");
			Criterion otherNanoCrit2 = Restrictions.eq(entityAlias + "type",
					compQuery.getEntityType());
			nanoEntityCrit = Restrictions.and(otherNanoCrit1, otherNanoCrit2);
		} else {
			nanoEntityCrit = Restrictions.eq(entityAlias + "class",
					nanoEntityClassName);
		}
		if (!StringUtils.isEmpty(compQuery.getChemicalName())
				&& !StringUtils.isEmpty(compQuery.getOperand())) {
			Criterion chemicalNameCrit = Restrictions.ilike("compElement.name",
					chemicalNameMatchMode.getUpdatedText(),
					chemicalNameMatchMode.getMatchMode());
			nanoEntityCrit = Restrictions.and(nanoEntityCrit, chemicalNameCrit);
		}
		return nanoEntityCrit;
	}

	/**
	 * Set the disjunction used in point of contact queries
	 * 
	 * @return
	 */
	private Disjunction getPointOfContactDisjunction(
			AdvancedSampleSearchBean searchBean, String pocAlias,
			String otherPOCAlias) {
		Disjunction disjunction = Restrictions.disjunction();
		for (SampleQueryBean query : searchBean.getSampleQueries()) {
			if (query.getNameType().equals("point of contact name")) {
				Disjunction pocDisjunction = getPointOfContactDisjunctionPerQuery(
						query, pocAlias, otherPOCAlias);
				if (pocDisjunction != null)
					disjunction.add(pocDisjunction);
			}
		}
		return disjunction;
	}

	private Disjunction getPointOfContactDisjunctionPerQuery(
			SampleQueryBean query, String pocAlias, String otherPOCAlias) {
		String[] pocCritStrs;
		if (StringUtils.isEmpty(otherPOCAlias)) {
			pocCritStrs = new String[] { pocAlias + "lastName",
					pocAlias + "firstName", "organization.name" };
		} else {
			pocCritStrs = new String[] { pocAlias + "lastName",
					pocAlias + "firstName", "organization.name",
					otherPOCAlias + "lastName", otherPOCAlias + "firstName",
					"otherOrg.name" };
		}
		TextMatchMode nameMatchMode = null;
		if (query.getOperand().equals("equals")) {
			nameMatchMode = new TextMatchMode(query.getName());
		} else if (query.getOperand().equals(Constants.STRING_OPERAND_CONTAINS)) {
			nameMatchMode = new TextMatchMode("*" + query.getName() + "*");
		}
		Disjunction pocDisjunction = Restrictions.disjunction();
		for (String critStr : pocCritStrs) {
			Criterion pocCrit = Restrictions.ilike(critStr, nameMatchMode
					.getUpdatedText(), nameMatchMode.getMatchMode());
			pocDisjunction.add(pocCrit);
		}
		return pocDisjunction;
	}

	/**
	 * Get the sample name junction used in sample queries
	 * 
	 * @param searchBean
	 * @return
	 * @throws Exception
	 */
	private Junction getSampleNameJunction(AdvancedSampleSearchBean searchBean)
			throws Exception {
		Disjunction sampleDisjunction = Restrictions.disjunction();
		Conjunction sampleConjunction = Restrictions.conjunction();
		for (SampleQueryBean query : searchBean.getSampleQueries()) {
			if (query.getNameType().equals("sample name")) {
				Criterion sampleNameCrit = getSampleNameCriterion(query);
				if (sampleNameCrit != null) {
					sampleDisjunction.add(sampleNameCrit);
					sampleConjunction.add(sampleNameCrit);
				}
			}
		}
        return (searchBean.getSampleLogicalOperator().equals("or") || searchBean
                .getSampleQueries().size() == 1) ? sampleDisjunction
                : sampleConjunction;
	}

	/**
	 * Get the sample disjunction used in sample queries
	 * 
	 * @param searchBean
	 * @return
	 * @throws Exception
	 */
	private Junction getSampleJunction(AdvancedSampleSearchBean searchBean)
			throws Exception {
		// if there are more than one POC query in AND, don't use junction.
		if (searchBean.getSampleLogicalOperator().equals("and")
				&& searchBean.getPocCount() > 1) {
			return null;
		}
		Disjunction sampleDisjunction = Restrictions.disjunction();
		Conjunction sampleConjunction = Restrictions.conjunction();
		for (SampleQueryBean query : searchBean.getSampleQueries()) {
			if (query.getNameType().equals("sample name")) {
				Criterion sampleNameCrit = getSampleNameCriterion(query);
				if (sampleNameCrit != null) {
					sampleDisjunction.add(sampleNameCrit);
					sampleConjunction.add(sampleNameCrit);
				}
			}
			if (query.getNameType().equals("point of contact name")) {
				Disjunction pocDisjunction = getPointOfContactDisjunction(
						searchBean, "poc.", "otherPOC.");
				if (pocDisjunction != null) {
					sampleDisjunction.add(pocDisjunction);
					if (searchBean.getPocCount() == 1) {
						sampleConjunction.add(pocDisjunction);
					}
				}
			}
		}
        return (searchBean.getSampleLogicalOperator().equals("or") || searchBean
                .getSampleQueries().size() == 1) ? sampleDisjunction
                : sampleConjunction;
	}

	/**
	 * Get the criterion used for sample name query
	 * 
	 * @param query
	 * @return
	 */
	private Criterion getSampleNameCriterion(SampleQueryBean query) {
		if (query.getNameType().equals("sample name")) {
			TextMatchMode nameMatchMode = null;
			if (query.getOperand().equals("equals")) {
				nameMatchMode = new TextMatchMode(query.getName());
			} else if (query.getOperand().equals(
					Constants.STRING_OPERAND_CONTAINS)) {
				nameMatchMode = new TextMatchMode("*" + query.getName() + "*");
			}
            return Restrictions.ilike("name", nameMatchMode
                    .getUpdatedText(), nameMatchMode.getMatchMode());
		} else {
			return null;
		}
	}

	private void setCharacterizationAndCriteria(
			AdvancedSampleSearchBean searchBean, DetachedCriteria crit) {
		if (searchBean.getCharacterizationQueries().isEmpty()) {
			return;
		}
		for (CharacterizationQueryBean charQuery : searchBean
				.getCharacterizationQueries()) {
			DetachedCriteria subCrit = getCharacterizationSubquery(charQuery,
					"sample.id");
			crit.add(Subqueries.exists(subCrit));
		}
	}

	private void setCharacterizationCriteria(
			AdvancedSampleSearchBean searchBean, DetachedCriteria crit) {
		if (searchBean.getCharacterizationQueries().isEmpty()) {
			return;
		}
		// if only one query default to disjunction
		if (searchBean.getCharacterizationQueries().size() == 1
				|| searchBean.getCharacterizationLogicalOperator().equals("or")) {
			// join characterization
			crit.createAlias("characterizationCollection", "chara",
					CriteriaSpecification.LEFT_JOIN);

			// join finding and datum
			if (searchBean.getHasDatum()) {
				crit.createAlias("chara.findingCollection", "finding",
						CriteriaSpecification.LEFT_JOIN);
				crit.createAlias("finding.datumCollection", "datum",
						CriteriaSpecification.LEFT_JOIN);
			}
			crit
					.add(getCharacterizationDisjunction(searchBean, crit,
							"chara."));
		} else {
			setCharacterizationAndCriteria(searchBean, crit);
		}
	}

	/**
	 * Set the DetachedCriteria used for composition queries
	 * 
	 * @param searchBean
	 * @param crit
	 * @throws Exception
	 */
	private void setCompositionCriteria(AdvancedSampleSearchBean searchBean,
			DetachedCriteria crit) throws Exception {
		if (searchBean.getCompositionQueries().isEmpty()) {
			return;
		}
		
		setCompositionCriteriaBase(searchBean, crit);
		
		if( searchBean.getCompositionLogicalOperator().equals("and") ) {
			if (searchBean.getNanoEntityCount() > 1) {
				setNanomaterialEntityAndCriteria(searchBean, crit);
			}
			if (searchBean.getFuncEntityCount() > 1) {
				setFunctionalizingEntityAndCriteria(searchBean, crit);
			}
			if (searchBean.getFuncCount() > 1) {
				setFunctionAndCriteria(searchBean, crit);
			}
		}
	}
	
	/**
	 * Set the DetachedCriteria used for composition queries
	 * 
	 * @param searchBean
	 * @param crit
	 * @throws Exception
	 */
	private void setCompositionCriteriaBase(AdvancedSampleSearchBean searchBean,
			DetachedCriteria crit) throws Exception {
		if (searchBean.getCompositionQueries().isEmpty()) {
			return;
		}
		// composition queries
		crit.createAlias("sampleComposition", "comp",
				CriteriaSpecification.LEFT_JOIN);
		Boolean hasChemicalName = searchBean.getHasChemicalName();
		// join function
		if (searchBean.getHasFunction()) {
			crit.createAlias("comp.nanomaterialEntityCollection", "nanoEntity",
					CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("nanoEntity.composingElementCollection",
					"compElement", CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("compElement.inherentFunctionCollection",
					"inherentFunction", CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("comp.functionalizingEntityCollection",
					"funcEntity", CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("funcEntity.functionCollection", "function",
					CriteriaSpecification.LEFT_JOIN);
		}
		// join nanomaterialEntity
		if (searchBean.getHasNanomaterial() && !searchBean.getHasFunction()) {
			crit.createAlias("comp.nanomaterialEntityCollection", "nanoEntity",
					CriteriaSpecification.LEFT_JOIN);
			if (hasChemicalName) {
				crit.createAlias("nanoEntity.composingElementCollection",
						"compElement", CriteriaSpecification.LEFT_JOIN);
			}
		}
		// join functionalizing entity
		if (searchBean.getHasAgentMaterial() && !searchBean.getHasFunction()) {
			crit.createAlias("comp.functionalizingEntityCollection",
					"funcEntity", CriteriaSpecification.LEFT_JOIN);
		}
		Junction junction = getCompositionJunction(searchBean, crit);
		if (junction != null) {
			crit.add(junction);
		}
/*		if (searchBean.getNanoEntityCount() > 1) {
			setNanomaterialEntityAndCriteria(searchBean, crit, "id");
		}
		if (searchBean.getFuncEntityCount() > 1) {
			setFunctionalizingEntityAndCriteria(searchBean, crit, "id");
		}
		if (searchBean.getFuncCount() > 1) {
			setFunctionAndCriteria(searchBean, crit, "id");
		}  */
	}

	private void setFunctionalizingEntityAndCriteria(
			AdvancedSampleSearchBean searchBean, DetachedCriteria crit) throws Exception {
		for (CompositionQueryBean query : searchBean.getCompositionQueries()) {
			if (query.getCompositionType().equals("functionalizing entity")) {
				DetachedCriteria subCrit = getFunctionalizingEntitySubquery(
						query, "funcEntity.", "id");
				crit.add(Subqueries.exists(subCrit));
			}
		}
	}

	private void setFunctionAndCriteria(AdvancedSampleSearchBean searchBean,
										DetachedCriteria crit) throws Exception {
		for (CompositionQueryBean query : searchBean.getCompositionQueries()) {
			if (query.getCompositionType().equals("function")) {
				DetachedCriteria subCrit = getFunctionSubquery(query,
						"inherentFunction.", "function.", "id");
				crit.add(Subqueries.exists(subCrit));
			}
		}
	}

	private void setNanomaterialEntityAndCriteria(
			AdvancedSampleSearchBean searchBean, DetachedCriteria crit) throws Exception {
		for (CompositionQueryBean query : searchBean.getCompositionQueries()) {
			if (query.getCompositionType().equals("nanomaterial entity")) {
				DetachedCriteria subCrit = getNanomaterialEntitySubquery(query,
						"nanoEntity.", "id");
				crit.add(Subqueries.exists(subCrit));
			}
		}
	}

	private DetachedCriteria getNanomaterialEntitySubquery(
			CompositionQueryBean query, String nanoEntityAlias,
			String projectionProperty) throws Exception {
		DetachedCriteria subCrit = DetachedCriteria.forClass(
				NanomaterialEntity.class, "subCrit");
		subCrit.setProjection(Projections.distinct(Property
				.forName(projectionProperty)));
		// join composing element
		if (!StringUtils.isEmpty(query.getChemicalName())) {
			subCrit.createAlias("subCrit.composingElementCollection",
					"compElement", CriteriaSpecification.LEFT_JOIN);
		}
		Criterion nanoCrit = getNanomaterialEntityCriterion(query, "");
		subCrit.add(nanoCrit);
		subCrit.add(Restrictions.eqProperty("subCrit." + projectionProperty,
				nanoEntityAlias + "id"));
		return subCrit;
	}

	private DetachedCriteria getPointOfContactSubquery(SampleQueryBean query,
													   String pocAlias1, String pocAlias2) {
		DetachedCriteria subCrit = DetachedCriteria.forClass(
				PointOfContact.class, "subCrit");
		subCrit.createAlias("subCrit.organization", "organization",
				CriteriaSpecification.LEFT_JOIN);
		subCrit.setProjection(Projections.distinct(Property.forName("id")));
		Disjunction pocDisjunction = getPointOfContactDisjunctionPerQuery(
				query, "", "");
		subCrit.add(pocDisjunction);
		if (pocAlias1.equals(pocAlias2)) {
			subCrit.add(Restrictions.eqProperty(
					"subCrit." + "id", pocAlias1 + "id"));
		} else {
			subCrit.add(Restrictions.or(Restrictions.eqProperty("subCrit."
					+ "id", pocAlias1 + "id"), Restrictions
					.eqProperty("subCrit." + "id", pocAlias2
							+ "id")));
		}
		return subCrit;
	}

	/**
	 * Set the DetachedCriteria for sample queries
	 * 
	 * @param searchBean
	 * @param crit
	 * @throws Exception
	 */
	private void setSampleCriteria(AdvancedSampleSearchBean searchBean,
			DetachedCriteria crit) throws Exception {
		if (searchBean.getSampleQueries().isEmpty()) {
			return;
		}
		// join POC
		if (searchBean.getHasPOC()) {
			crit.createAlias("primaryPointOfContact", "poc");
			crit.createAlias("poc.organization", "organization");
			crit.createAlias("otherPointOfContactCollection", "otherPOC",
					CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("otherPOC.organization", "otherOrg",
					CriteriaSpecification.LEFT_JOIN);
		}
		Junction junction = getSampleJunction(searchBean);
		if (junction != null) {
			crit.add(junction);
		}
		// more than one POC and AND
		else {
			crit.add(getSampleNameJunction(searchBean));
			for (SampleQueryBean query : searchBean.getSampleQueries()) {
				if (query.getNameType().equals("point of contact name")) {
					DetachedCriteria subCrit = getPointOfContactSubquery(query,
							"poc.", "otherPOC.");
					crit.add(Subqueries.exists(subCrit));
				}
			}
		}
	}

}