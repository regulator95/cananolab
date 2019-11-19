/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.service.publication;

import gov.nih.nci.cananolab.domain.common.Publication;
import gov.nih.nci.cananolab.dto.common.PublicationBean;
import gov.nih.nci.cananolab.exception.NoAccessException;
import gov.nih.nci.cananolab.exception.PublicationException;
import gov.nih.nci.cananolab.security.AccessControlInfo;
import gov.nih.nci.cananolab.security.CananoUserDetails;
import gov.nih.nci.cananolab.service.BaseService;
import gov.nih.nci.cananolab.service.publication.helper.PublicationServiceHelper;

import java.util.List;

/**
 * Interface defining methods invovled in submiting and searching publications.
 *
 * @author tanq
 *
 */
public interface PublicationService extends BaseService {

	/**
	 * Persist a new publication or update an existing publication
	 *
	 * @throws Exception
	 */
    void savePublication(PublicationBean publicationBean)
			throws PublicationException, NoAccessException;

	PublicationBean findPublicationById(String publicationId,
                                        Boolean loadAccessInfo) throws PublicationException,
			NoAccessException;

	PublicationBean findPublicationByKey(String keyName,
                                         Object keyValue, Boolean loadAccessInfo)
			throws PublicationException, NoAccessException;

	List<PublicationBean> findPublicationsBySampleId(String sampleId)
			throws PublicationException;

	int getNumberOfPublicPublications() throws PublicationException;
	
	int getNumberOfPublicPublicationsForJob() throws PublicationException;

	List<String> findPublicationIdsBy(String title, String category,
                                      String sampleName, String[] researchAreas, String[] keywords,
                                      String pubMedId, String digitalObjectId, String[] authors,
                                      String[] nanomaterialEntityClassNames,
                                      String[] otherNanomaterialEntityTypes,
                                      String[] functionalizingEntityClassNames,
                                      String[] otherFunctionalizingEntityTypes,
                                      String[] functionClassNames, String[] otherFunctionTypes)
			throws PublicationException;

	void deletePublication(Publication publication)
			throws PublicationException, NoAccessException;

	/**
	 * Parse PubMed XML file and store the information into a PublicationBean
	 *
	 * @param pubMedId
	 * @return
	 * @throws PublicationException
	 */
    PublicationBean getPublicationFromPubMedXML(String pubMedId)
			throws PublicationException;

	PublicationBean findNonPubMedNonDOIPublication(
            String publicationType, String title, String firstAuthorLastName,
            String firstAuthorFirstName) throws PublicationException;

	void removePublicationFromSample(String sampleName,
                                     Publication publication) throws PublicationException,
			NoAccessException;

	void assignAccessibility(AccessControlInfo access,
                             Publication publication) throws PublicationException,
			NoAccessException;

	void removeAccessibility(AccessControlInfo access,
                             Publication publication) throws PublicationException,
			NoAccessException;

	List<String> findPublicationIdsByOwner(String currentOwner) throws PublicationException;
	
	List<String> findPublicationIdsSharedWithUser(CananoUserDetails userDetails) throws PublicationException;

	PublicationBean findPublicationByIdWorkspace(String id, boolean b)
			throws PublicationException;
	
	PublicationServiceHelper getPublicationServiceHelper();
}
