/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.restful.publication;

import gov.nih.nci.cananolab.domain.common.Publication;
import gov.nih.nci.cananolab.dto.common.PublicationBean;
import gov.nih.nci.cananolab.restful.core.InitSetup;

import javax.servlet.http.HttpServletRequest;

/**
 * This class sets up information required for publication forms.
 *
 * @author tanq
 *
 */
public class InitPublicationSetup {
	private InitPublicationSetup() {
	}

	public static InitPublicationSetup getInstance() {
		return new InitPublicationSetup();
	}

	public void setPublicationDropdowns(HttpServletRequest request) throws Exception {
		InitSetup.getInstance()
				.getDefaultAndOtherTypesByLookup(request, "publicationCategories",
						"publication", "category", "otherCategory", true);
		InitSetup.getInstance()
				.getDefaultAndOtherTypesByLookup(request, "publicationStatuses",
						"publication", "status", "otherStatus", true);
		InitSetup.getInstance()
			.getDefaultAndOtherTypesByLookup(request, "publicationResearchAreas",
				"publication", "researchArea", "otherResearchArea", true);
	}

	public void persistPublicationDropdowns(HttpServletRequest request,
			PublicationBean publication) throws Exception {

		InitSetup.getInstance().persistLookup(request, "publication", "category",
				"otherCategory",
				((Publication) (publication.getDomainFile())).getCategory());
		InitSetup.getInstance().persistLookup(request, "publication", "status",
				"otherStatus",
				((Publication) (publication.getDomainFile())).getStatus());
		setPublicationDropdowns(request);
	}

	public void setDefaultResearchAreas(
			HttpServletRequest request) throws Exception {
		InitSetup.getInstance()
			.getDefaultAndOtherTypesByLookup(request, "publicationResearchAreas",
			"publication", "researchArea", "otherResearchArea", true);
	}
}
