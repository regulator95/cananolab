package gov.nih.nci.cananolab.service.publication.helper;

import gov.nih.nci.cananolab.domain.common.Publication;
import gov.nih.nci.cananolab.domain.particle.Sample;
import gov.nih.nci.cananolab.exception.NoAccessException;
import gov.nih.nci.cananolab.security.dao.AclDao;
import gov.nih.nci.cananolab.security.enums.CaNanoRoleEnum;
import gov.nih.nci.cananolab.security.enums.SecureClassesEnum;
import gov.nih.nci.cananolab.security.service.SpringSecurityAclService;
import gov.nih.nci.cananolab.system.applicationservice.CaNanoLabApplicationService;
import gov.nih.nci.cananolab.util.Comparators;
import gov.nih.nci.cananolab.util.Constants;
import gov.nih.nci.cananolab.util.StringUtils;
import gov.nih.nci.cananolab.util.TextMatchMode;
import gov.nih.nci.cananolab.system.applicationservice.client.ApplicationServiceProvider;
import gov.nih.nci.cananolab.system.query.hibernate.HQLCriteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.hibernate.FetchMode;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Helper class providing implementations of search methods needed for both
 * local implementation of PublicationService and grid service *
 * 
 * @author tanq, pansu
 */
@Component("publicationServiceHelper")
public class PublicationServiceHelper
{
	private Logger logger = Logger.getLogger(PublicationServiceHelper.class);
	
	@Autowired
	private SpringSecurityAclService springSecurityAclService;
	
	@Autowired
	private AclDao aclDao;

	public List<String> findPublicationIdsBy(String title, String category,
			String sampleName, String[] researchArea, String[] keywords,
			String pubMedId, String digitalObjectId, String[] authors,
			String[] nanomaterialEntityClassNames,
			String[] otherNanomaterialEntityTypes,
			String[] functionalizingEntityClassNames,
			String[] otherFunctionalizingEntityTypes,
			String[] functionClassNames, String[] otherFunctionTypes)
			throws Exception {

		// Set<String> samplePublicationIds = new HashSet<String>();
		// Set<String> compositionPublicationIds = new HashSet<String>();
		// Set<String> otherPublicationIds = new HashSet<String>();
		// Set<String> allPublicationIds = new HashSet<String>();
		Set<Publication> samplePublications = new HashSet<Publication>();
		Set<Publication> compositionPublications = new HashSet<Publication>();
		Set<Publication> otherPublications = new HashSet<Publication>();
		Set<Publication> allPublications = new HashSet<Publication>();

		// check if sample is accessible
		if (!StringUtils.isEmpty(sampleName)) {
			List<Publication> publications = this
					.findPublicationsBySampleName(sampleName);
			samplePublications.addAll(publications);
			// allPublicationIds.addAll(samplePublicationIds);
			allPublications.addAll(samplePublications);
		}

		if (nanomaterialEntityClassNames != null
				&& nanomaterialEntityClassNames.length > 0
				|| otherNanomaterialEntityTypes != null
				&& otherNanomaterialEntityTypes.length > 0
				|| functionalizingEntityClassNames != null
				&& functionalizingEntityClassNames.length > 0
				|| functionClassNames != null && functionClassNames.length > 0) {
			compositionPublications = this.findPublicationsBySampleComposition(
					sampleName, nanomaterialEntityClassNames,
					otherNanomaterialEntityTypes,
					functionalizingEntityClassNames,
					otherFunctionalizingEntityTypes, functionClassNames,
					otherFunctionTypes);
			// allPublicationIds.addAll(compositionPublicationIds);
			allPublications.addAll(compositionPublications);
		}

		// can't query for the entire Publication object due to limitations in
		// pagination in SDK
		// DetachedCriteria crit = DetachedCriteria.forClass(Publication.class)
		// .setProjection(Projections.distinct(Property.forName("id")));
		//
		// added created date and title to allow sorting on date and title
		DetachedCriteria crit = DetachedCriteria.forClass(Publication.class)
				.setProjection(Projections.projectionList()
								.add(Projections.property("id"))
								.add(Projections.property("title"))
								.add(Projections.property("createdDate")));

		if (!StringUtils.isEmpty(title)) {
			TextMatchMode titleMatchMode = new TextMatchMode(title);
			crit.add(Restrictions.ilike("title",
					titleMatchMode.getUpdatedText(),
					titleMatchMode.getMatchMode()));
		}
		if (!StringUtils.isEmpty(category)) {
			TextMatchMode categoryMatchMode = new TextMatchMode(category);
			crit.add(Restrictions.ilike("category",
					categoryMatchMode.getUpdatedText(),
					categoryMatchMode.getMatchMode()));
		}

		// pubMedId
		if (!StringUtils.isEmpty(pubMedId)) {
			TextMatchMode pubMedIdMatchMode = new TextMatchMode(pubMedId);
			Long pubMedIdLong = null;
			try {
				pubMedIdLong = new Long(pubMedIdMatchMode.getUpdatedText());
			} catch (Exception ex) {
				// ignore
				pubMedIdLong = new Long(0);
			}
			crit.add(Restrictions.eq("pubMedId", pubMedIdLong));
		}
		if (!StringUtils.isEmpty(digitalObjectId)) {
			TextMatchMode digitalObjectIdMatchMode = new TextMatchMode(
					digitalObjectId);
			crit.add(Restrictions.ilike("digitalObjectId",
					digitalObjectIdMatchMode.getUpdatedText(),
					digitalObjectIdMatchMode.getMatchMode()));
		}

		// researchArea
		if (researchArea != null && researchArea.length > 0) {

			Disjunction disjunction = Restrictions.disjunction();
			for (String research : researchArea) {
				Criterion crit1 = Restrictions.like("researchArea", research,
						MatchMode.ANYWHERE);
				disjunction.add(crit1);
			}
			crit.add(disjunction);
		}

		// keywords
		if (keywords != null && keywords.length > 0) {
			Disjunction disjunction = Restrictions.disjunction();
			crit.createAlias("keywordCollection", "keyword");
			for (String keyword : keywords) {
				Criterion keywordCrit1 = Restrictions.ilike("keyword.name",
						keyword, MatchMode.ANYWHERE);
				disjunction.add(keywordCrit1);
			}
			crit.add(disjunction);
		}

		// authors
		if (authors != null && authors.length > 0) {
			Disjunction disjunction = Restrictions.disjunction();
			crit.createAlias("authorCollection", "author");
			for (String author : authors) {
				//The column in MySQL is VARCHAR so search will be case insensitive, whether we use like or ilike
				Criterion crit1 = Restrictions.ilike("author.lastName", author,
						MatchMode.ANYWHERE);
				disjunction.add(crit1);
				
				//Searching by first name and middle initial creates lots of false positives. ilike creates '%xxx%'
//				Criterion crit2 = Restrictions.ilike("author.firstName",
//						author, MatchMode.ANYWHERE);
//				disjunction.add(crit2);
//				Criterion crit3 = Restrictions.ilike("author.initial", author,
//						MatchMode.ANYWHERE);
//				disjunction.add(crit3);
			}
			crit.add(disjunction);
		}

		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
		List results = appService.query(crit);
		for(int i = 0; i < results.size(); i++){
			Object[] row = (Object[]) results.get(i);
			// otherPublicationIds.add(obj.toString());
			Publication publication = new Publication();
			publication.setId((Long) row[0]);
			publication.setTitle((String) row[1]);
			publication.setCreatedDate((Date) row[2]);
			otherPublications.add(publication);
		}
		// allPublicationIds.addAll(otherPublicationIds);
		allPublications.addAll(otherPublications);

		// find the union of all publication Ids
		// if (!samplePublicationIds.isEmpty()) {
		// allPublicationIds.retainAll(samplePublicationIds);
		// }
		// if (!compositionPublicationIds.isEmpty()) {
		// allPublicationIds.retainAll(compositionPublicationIds);
		// }
		// if (!otherPublicationIds.isEmpty()) {
		// allPublicationIds.retainAll(otherPublicationIds);
		// }
		//
		if (!samplePublications.isEmpty()) {
			allPublications.retainAll(samplePublications);
		}
		if (!compositionPublications.isEmpty()) {
			allPublications.retainAll(compositionPublications);
		}
		if (!otherPublications.isEmpty()) {
			allPublications.retainAll(otherPublications);
		}
		// order publications by createdDate
		List<Publication> orderedPubs = new ArrayList<Publication>(
				allPublications);
		Collections.sort(orderedPubs,
				new Comparators.PublicationDateComparator());
		// get ordered ids
		List<String> orderedPubIds = new ArrayList<String>();
		for (Publication pub : orderedPubs) {
			if (springSecurityAclService.currentUserHasReadPermission(pub.getId(), SecureClassesEnum.PUBLICATION.getClazz()) ||
				springSecurityAclService.currentUserHasWritePermission(pub.getId(), SecureClassesEnum.PUBLICATION.getClazz())) {
				orderedPubIds.add(pub.getId().toString());
			} else {
				// ignore no access exception
				logger.debug("User doesn't have access to publication with id " + pub.getId());
			}
		}
		return orderedPubIds;
	}

	public Publication findPublicationById(String publicationId) throws Exception
	{
		Long pubId = Long.valueOf(publicationId);
		if (!springSecurityAclService.currentUserHasReadPermission(pubId, SecureClassesEnum.PUBLICATION.getClazz()) &&
			!springSecurityAclService.currentUserHasWritePermission(pubId, SecureClassesEnum.PUBLICATION.getClazz())) {
			throw new NoAccessException("User has no access to the publication " + publicationId);
		}
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();

		DetachedCriteria crit = DetachedCriteria.forClass(Publication.class).add(Property.forName("id").eq(pubId));
		crit.setFetchMode("authorCollection", FetchMode.JOIN);
		crit.setFetchMode("keywordCollection", FetchMode.JOIN);
		crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		List result = appService.query(crit);
		Publication publication = null;
		if (!result.isEmpty()) {
			publication = (Publication) result.get(0);
		}
		return publication;
	}

	public Publication findPublicationByKey(String keyName, Object keyValue)
			throws Exception {
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();

		DetachedCriteria crit = DetachedCriteria.forClass(Publication.class)
				.add(Property.forName(keyName).eq(keyValue));
		crit.setFetchMode("authorCollection", FetchMode.JOIN);
		crit.setFetchMode("keywordCollection", FetchMode.JOIN);
		crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		List result = appService.query(crit);
		Publication publication = null;
		if (!result.isEmpty()) {
			publication = (Publication) result.get(0);
			if (springSecurityAclService.currentUserHasReadPermission(publication.getId(), SecureClassesEnum.PUBLICATION.getClazz()) ||
				springSecurityAclService.currentUserHasWritePermission(publication.getId(), SecureClassesEnum.PUBLICATION.getClazz())) {
				return publication;
			} else {
				throw new NoAccessException();
			}
		}
		return publication;
	}

	public int getNumberOfPublicPublications() throws Exception {
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
		HQLCriteria crit = new HQLCriteria("select id from gov.nih.nci.cananolab.domain.common.Publication");
		List results = appService.query(crit);
		int count = 0;
		for(int i = 0; i< results.size(); i++)
		{
			Long id = Long.valueOf(results.get(i).toString());
			if (springSecurityAclService.checkObjectPublic(id, SecureClassesEnum.PUBLICATION.getClazz()))
				count++;
		}
		return count;
	}

	public int getNumberOfPublicPublicationsForJob() throws Exception
	{
		List<Long> publicData = aclDao.getIdsOfClassForSid(SecureClassesEnum.PUBLICATION.getClazz().getName(), CaNanoRoleEnum.ROLE_ANONYMOUS.toString());
        return (publicData != null) ? publicData.size() : 0;
	}

	public String[] findSampleNamesByPublicationId(String publicationId) throws Exception
	{
		if (!springSecurityAclService.currentUserHasReadPermission(Long.valueOf(publicationId), SecureClassesEnum.PUBLICATION.getClazz()) &&
			!springSecurityAclService.currentUserHasWritePermission(Long.valueOf(publicationId), SecureClassesEnum.PUBLICATION.getClazz())) {
			throw new NoAccessException("User has no access to the publication " + publicationId);
		} 
		
		// check if user have access to publication first
		String query = "select sample.name, sample.id from gov.nih.nci.cananolab.domain.particle.Sample as sample join sample.publicationCollection as pub where pub.id='"
				+ publicationId + "'";
		HQLCriteria crit = new HQLCriteria(query);
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
		
		List results = appService.query(crit);
		SortedSet<String> names = new TreeSet<String>();
		for (int i = 0; i < results.size(); i++) {
			Object[] row = (Object[]) results.get(i);
			String sampleName = row[0].toString();
			String sampleId = row[1].toString();
			if (springSecurityAclService.currentUserHasReadPermission(Long.valueOf(sampleId), SecureClassesEnum.SAMPLE.getClazz()) ||
				springSecurityAclService.currentUserHasWritePermission(Long.valueOf(sampleId), SecureClassesEnum.SAMPLE.getClazz())) {
				names.add(sampleName);
			} else {
				logger.debug("User doesn't have access to sample " + sampleName);
			}
		}
		return names.toArray(new String[0]);
	}

	public List<Publication> findPublicationsBySampleId(String sampleId) throws Exception
	{
		if (!springSecurityAclService.currentUserHasReadPermission(Long.valueOf(sampleId), SecureClassesEnum.SAMPLE.getClazz()) &&
			!springSecurityAclService.currentUserHasWritePermission(Long.valueOf(sampleId), SecureClassesEnum.SAMPLE.getClazz())) {
			throw new NoAccessException("User has no access to the sample " + sampleId);
		}
		
		List<Publication> publications = new ArrayList<Publication>();
		Sample sample = null;
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();

		DetachedCriteria crit = DetachedCriteria.forClass(Sample.class).add(Property.forName("id").eq(new Long(sampleId)));
		crit.setFetchMode("publicationCollection", FetchMode.JOIN);
		crit.setFetchMode("publicationCollection.authorCollection", FetchMode.JOIN);
		crit.setFetchMode("publicationCollection.keywordCollection", FetchMode.JOIN);
		List result = appService.query(crit);
		if (!result.isEmpty()) {
			sample = (Sample) result.get(0);
		}
		for (Object obj : sample.getPublicationCollection()) {
			Publication pub = (Publication) obj;
			if (springSecurityAclService.currentUserHasReadPermission(pub.getId(), SecureClassesEnum.PUBLICATION.getClazz()) ||
				springSecurityAclService.currentUserHasWritePermission(pub.getId(), SecureClassesEnum.PUBLICATION.getClazz())) {
				publications.add(pub);
			} else {
				logger.debug("User doesn't have access ot publication with id " + pub.getId());
			}
		}
		return publications;
	}

	private List<Publication> findPublicationsBySampleName(String sampleName) throws Exception
	{
		List<Publication> publications = new ArrayList<Publication>();
		Sample sample = null;
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();

		DetachedCriteria crit = DetachedCriteria.forClass(Sample.class);
		TextMatchMode nameMatchMode = new TextMatchMode(sampleName);
		crit.add(Restrictions.ilike("name", nameMatchMode.getUpdatedText(), nameMatchMode.getMatchMode()));
		
		crit.setFetchMode("publicationCollection", FetchMode.JOIN);
		List result = appService.query(crit);
		/* CANANOLAB-71 fix below */
		/*		if (!result.isEmpty()) {
			sample = (Sample) result.get(0);
			if (!StringUtils.containsIgnoreCase(getAccessibleData(), sample
					.getId().toString())) {
				return publications;
			}
		}  */
		if (!result.isEmpty()) {
			for( Object robj: result ) {
				sample = (Sample) robj;
				
				if (springSecurityAclService.currentUserHasReadPermission(sample.getId(), SecureClassesEnum.SAMPLE.getClazz()) ||
					springSecurityAclService.currentUserHasWritePermission(sample.getId(), SecureClassesEnum.SAMPLE.getClazz())) {
					for (Object obj : sample.getPublicationCollection()) {
						Publication pub = (Publication) obj;
						if (springSecurityAclService.currentUserHasReadPermission(pub.getId(), SecureClassesEnum.PUBLICATION.getClazz()) ||
							springSecurityAclService.currentUserHasWritePermission(pub.getId(), SecureClassesEnum.PUBLICATION.getClazz())) {
							publications.add(pub);
						} else {
							logger.debug("User doesn't have access to publication with id " + pub.getId());
						}
					}
				} else {
					logger.debug("User doesn't have access ot sample with id " + sample.getId());
				}
				
			}
		}
		return publications;
	}

	private Set<Publication> findPublicationsBySampleComposition(
			String sampleName, String[] nanomaterialEntityClassNames,
			String[] otherNanomaterialEntityTypes,
			String[] functionalizingEntityClassNames,
			String[] otherFunctionalizingEntityTypes,
			String[] functionClassNames, String[] otherFunctionTypes)
			throws Exception {
		Set<Publication> publications = new HashSet<Publication>();
		Sample sample = null;
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();
		DetachedCriteria crit = DetachedCriteria.forClass(Sample.class);
		if (!StringUtils.isEmpty(sampleName)) {
			crit.add(Property.forName("name").eq(sampleName));
		}
		// join composition
		if (nanomaterialEntityClassNames != null
				&& nanomaterialEntityClassNames.length > 0
				|| otherNanomaterialEntityTypes != null
				&& otherNanomaterialEntityTypes.length > 0
				|| functionClassNames != null && functionClassNames.length > 0
				|| otherFunctionTypes != null && otherFunctionTypes.length > 0
				|| functionalizingEntityClassNames != null
				&& functionalizingEntityClassNames.length > 0
				|| otherFunctionalizingEntityTypes != null
				&& otherFunctionalizingEntityTypes.length > 0) {
			crit.createAlias("sampleComposition", "comp",
					CriteriaSpecification.LEFT_JOIN);
		}
		// join nanomaterial entity
		if (nanomaterialEntityClassNames != null
				&& nanomaterialEntityClassNames.length > 0
				|| otherNanomaterialEntityTypes != null
				&& otherNanomaterialEntityTypes.length > 0
				|| functionClassNames != null && functionClassNames.length > 0
				|| otherFunctionTypes != null && otherFunctionTypes.length > 0) {
			crit.createAlias("comp.nanomaterialEntityCollection", "nanoEntity",
					CriteriaSpecification.LEFT_JOIN);
		}

		// join functionalizing entity
		if (functionalizingEntityClassNames != null
				&& functionalizingEntityClassNames.length > 0
				|| otherFunctionalizingEntityTypes != null
				&& otherFunctionalizingEntityTypes.length > 0
				|| functionClassNames != null && functionClassNames.length > 0
				|| otherFunctionTypes != null && otherFunctionTypes.length > 0) {
			crit.createAlias("comp.functionalizingEntityCollection",
					"funcEntity", CriteriaSpecification.LEFT_JOIN);
		}

		// nanomaterial entity
		if (nanomaterialEntityClassNames != null
				&& nanomaterialEntityClassNames.length > 0
				|| otherNanomaterialEntityTypes != null
				&& otherNanomaterialEntityTypes.length > 0
				|| functionClassNames != null && functionClassNames.length > 0
				|| otherFunctionTypes != null && otherFunctionTypes.length > 0) {
			Disjunction disjunction = Restrictions.disjunction();
			if (nanomaterialEntityClassNames != null
					&& nanomaterialEntityClassNames.length > 0) {
				Criterion nanoEntityCrit = Restrictions.in("nanoEntity.class",
						nanomaterialEntityClassNames);
				disjunction.add(nanoEntityCrit);
			}
			if (otherNanomaterialEntityTypes != null
					&& otherNanomaterialEntityTypes.length > 0) {
				Criterion otherNanoCrit1 = Restrictions.eq("nanoEntity.class",
						"OtherNanomaterialEntity");
				Criterion otherNanoCrit2 = Restrictions.in("nanoEntity.type",
						otherNanomaterialEntityTypes);
				Criterion otherNanoCrit = Restrictions.and(otherNanoCrit1,
						otherNanoCrit2);
				disjunction.add(otherNanoCrit);
			}
			crit.add(disjunction);
		}

		// functionalizing entity
		// need to turn class names into integers in order for the .class
		// clause to work
		if (functionalizingEntityClassNames != null
				&& functionalizingEntityClassNames.length > 0
				|| otherFunctionalizingEntityTypes != null
				&& otherFunctionalizingEntityTypes.length > 0
				|| functionClassNames != null && functionClassNames.length > 0
				|| otherFunctionTypes != null && otherFunctionTypes.length > 0) {
			Disjunction disjunction = Restrictions.disjunction();
			if (functionalizingEntityClassNames != null
					&& functionalizingEntityClassNames.length > 0) {
				Integer[] functionalizingEntityClassNameIntegers = this
						.convertToFunctionalizingEntityClassOrderNumber(functionalizingEntityClassNames);
				Criterion funcEntityCrit = Restrictions.in("funcEntity.class",
						functionalizingEntityClassNameIntegers);
				disjunction.add(funcEntityCrit);
			}
			if (otherFunctionalizingEntityTypes != null
					&& otherFunctionalizingEntityTypes.length > 0) {
				Integer classOrderNumber = Constants.FUNCTIONALIZING_ENTITY_SUBCLASS_ORDER_MAP
						.get("OtherFunctionalizingEntity");
				Criterion otherFuncCrit1 = Restrictions.eq("funcEntity.class",
						classOrderNumber);
				Criterion otherFuncCrit2 = Restrictions.in("funcEntity.type",
						otherFunctionalizingEntityTypes);
				Criterion otherFuncCrit = Restrictions.and(otherFuncCrit1,
						otherFuncCrit2);
				disjunction.add(otherFuncCrit);
			}
			crit.add(disjunction);
		}

		// function
		if (functionClassNames != null && functionClassNames.length > 0
				|| otherFunctionTypes != null && otherFunctionTypes.length > 0) {
			Disjunction disjunction = Restrictions.disjunction();
			crit.createAlias("nanoEntity.composingElementCollection",
					"compElement", CriteriaSpecification.LEFT_JOIN)
					.createAlias("compElement.inherentFunctionCollection",
							"inFunc", CriteriaSpecification.LEFT_JOIN);
			crit.createAlias("funcEntity.functionCollection", "func",
					CriteriaSpecification.LEFT_JOIN);
			if (functionClassNames != null && functionClassNames.length > 0) {
				Criterion funcCrit1 = Restrictions.in("inFunc.class",
						functionClassNames);
				Criterion funcCrit2 = Restrictions.in("func.class",
						functionClassNames);
				disjunction.add(funcCrit1).add(funcCrit2);
			}
			if (otherFunctionTypes != null && otherFunctionTypes.length > 0) {
				Criterion otherFuncCrit1 = Restrictions.and(
						Restrictions.eq("inFunc.class", "OtherFunction"),
						Restrictions.in("inFunc.type", otherFunctionTypes));
				Criterion otherFuncCrit2 = Restrictions.and(
						Restrictions.eq("func.class", "OtherFunction"),
						Restrictions.in("func.type", otherFunctionTypes));
				disjunction.add(otherFuncCrit1).add(otherFuncCrit2);
			}
			crit.add(disjunction);
		}
		crit.setFetchMode("publicationCollection", FetchMode.JOIN);
		List results = appService.query(crit);
		for (Object result : results) {
			sample = (Sample) result;
			if (!springSecurityAclService.currentUserHasReadPermission(sample.getId(), SecureClassesEnum.SAMPLE.getClazz()) &&
				!springSecurityAclService.currentUserHasWritePermission(sample.getId(), SecureClassesEnum.SAMPLE.getClazz())) {
				logger.debug("User doesn't have access to sample with id " + sample.getId());
			} else {
				for (Object obj : sample.getPublicationCollection()) {
					Publication pub = (Publication) obj;
					if (springSecurityAclService.currentUserHasReadPermission(pub.getId(), SecureClassesEnum.PUBLICATION.getClazz()) ||
						springSecurityAclService.currentUserHasWritePermission(pub.getId(), SecureClassesEnum.PUBLICATION.getClazz())) {
						publications.add(pub);
					} else {
						logger.debug("User doesn't have access to publication with id " + pub.getId());
					}
				}
			}
		}
		return publications;
	}

	public Integer[] convertToFunctionalizingEntityClassOrderNumber(
			String[] classNames) {
		Integer[] orderNumbers = new Integer[classNames.length];
		int i = 0;
		for (String name : classNames) {
			orderNumbers[i] = Constants.FUNCTIONALIZING_ENTITY_SUBCLASS_ORDER_MAP
					.get(name);
			i++;
		}
		return orderNumbers;
	}

	public Publication findNonPubMedNonDOIPublication(String publicationType,
			String title, String firstAuthorLastName,
			String firstAuthorFirstName) throws Exception {
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();
		DetachedCriteria crit = DetachedCriteria.forClass(Publication.class);
		crit.add(Restrictions.eq("category", publicationType).ignoreCase());
		crit.add(Restrictions.eq("title", title).ignoreCase());
		// had to use either createAlias with LEFT_JOIN on authorCollection and
		// no fetchMode on authorCollection
		crit.createAlias("authorCollection", "author",
				CriteriaSpecification.LEFT_JOIN);
		crit.setFetchMode("keywordCollection", FetchMode.JOIN);
		crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		if (firstAuthorLastName != null) {
			crit.add(Restrictions.eq("author.lastName", firstAuthorLastName)
					.ignoreCase());
		} else {
			crit.add(Restrictions.isNull("author.lastName"));
		}
		if (firstAuthorFirstName != null) {
			crit.add(Restrictions.eq("author.firstName", firstAuthorFirstName)
					.ignoreCase());
		} else {
			crit.add(Restrictions.isNull("author.firstName"));
		}

		crit.add(Restrictions.isNull("pubMedId"));
		crit.add(Restrictions.isNull("digitalObjectId"));
		List results = appService.query(crit);
		Publication publication = null;
		//TODO Why is this a for loop?  It never loops
		for (int i = 0; i < results.size(); i++) {
			publication = (Publication) results.get(i);
			if (springSecurityAclService.currentUserHasReadPermission(publication.getId(), SecureClassesEnum.PUBLICATION.getClazz()) ||
				springSecurityAclService.currentUserHasWritePermission(publication.getId(), SecureClassesEnum.PUBLICATION.getClazz())) {
				return publication;
			} else {
				throw new NoAccessException();
			}
		}
		return publication;
	}

	public List<String> findPublicationIdsByOwner(String currentOwner) throws Exception
	{
		List<String> publicationIds = new ArrayList<String>();
		DetachedCriteria crit = DetachedCriteria.forClass(Publication.class).setProjection(Projections.projectionList().add(Projections.property("id")));
		Criterion crit1 = Restrictions.eq("createdBy", currentOwner);
		// in case of copy createdBy is like lijowskim:COPY
		Criterion crit2 = Restrictions.like("createdBy", currentOwner + ":", MatchMode.START);
		crit.add(Expression.or(crit1, crit2));

		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
		List results = appService.query(crit);
		for(int i = 0; i < results.size(); i++){
			String publicationId = results.get(i).toString();
			Long pubId = Long.valueOf(publicationId);
			if (springSecurityAclService.currentUserHasReadPermission(pubId, SecureClassesEnum.PUBLICATION.getClazz()) ||
				springSecurityAclService.currentUserHasWritePermission(pubId, SecureClassesEnum.PUBLICATION.getClazz())) {
				publicationIds.add(publicationId);
			} else {
				logger.debug("User doesn't have access to publication of ID: " + publicationId);
			}
		}
		return publicationIds;
	}

	private static class PublicationDateComparator implements Comparator<Publication> {
		public int compare(Publication pub1, Publication pub2) {
			return pub1.getCreatedDate().compareTo(pub2.getCreatedDate());
		}
	}
	
	/**
	 * Find samples based on publication id. Currently, it only returns sample with sampleId, sampleName and
	 * createdDate field values
	 * 
	 * @param pubId
	 * @return
	 * @throws Exception
	 */
	public List<Sample> findSamplesByPublicationId(long pubId) 
			throws Exception {

		List<String> sampleIds = new ArrayList<String>();

		DetachedCriteria crit = DetachedCriteria.forClass(Sample.class)
				.setProjection(
						Projections.projectionList()
						.add(Projections.property("id"))
						.add(Projections.property("name"))
						.add(Projections.property("createdDate")));

		crit.createAlias("publicationCollection", "publication");
		crit.add(Restrictions.eq("publication.id", pubId));

		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();

		List results = appService.query(crit);
		Set<Sample> samples = new HashSet<Sample>();
		for (int i = 0; i < results.size(); i++) {

			try {
				Object[] row = (Object[]) results.get(i);

				Long sampleId = (Long) row[0];

				if (springSecurityAclService.currentUserHasReadPermission(sampleId, SecureClassesEnum.SAMPLE.getClazz()) ||
					springSecurityAclService.currentUserHasWritePermission(sampleId, SecureClassesEnum.SAMPLE.getClazz())) {
					Sample sample = new Sample();
					sample.setId(sampleId);
					sample.setName((String) row[1]);
					sample.setCreatedDate((Date) row[2]);
					samples.add(sample);
				} else {
					logger.debug("User doesn't have access to sample of ID: " + sampleId);
				}

			} catch (ClassCastException e) {
				logger.error("Got ClassCastException: " + e.getMessage());
				break;
			}
		}

		List<Sample> orderedSamples = new ArrayList<Sample>(samples);

		Collections
		.sort(orderedSamples, new Comparators.SampleDateComparator());

		return orderedSamples;
	}
	
	public List<String> getAllPublications() throws Exception {
		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
				.getApplicationService();
		HQLCriteria crit = new HQLCriteria(
				"select id from gov.nih.nci.cananolab.domain.common.Publication");
		List results = appService.query(crit);
		List<String> publicationIds = new ArrayList<String>();
		for(int i = 0; i< results.size(); i++){
			String id = (String) results.get(i).toString();
			publicationIds.add(id);
			
		}
		return publicationIds;
	}

}