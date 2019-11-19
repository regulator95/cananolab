/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.restful.sample;

import gov.nih.nci.cananolab.dto.particle.SampleBean;
import gov.nih.nci.cananolab.restful.core.InitSetup;
import gov.nih.nci.cananolab.service.common.LookupService;
import gov.nih.nci.cananolab.service.sample.SampleService;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import javax.servlet.http.HttpServletRequest;

/**
 * This class sets up information required for canano forms.
 *
 * @author pansu, cais
 *
 */
public class InitSampleSetup {
	
	private InitSampleSetup() {
	}

	public static InitSampleSetup getInstance() {
		return new InitSampleSetup();
	}

	public void setLocalSearchDropdowns(HttpServletRequest request)
			throws Exception {
		InitSetup.getInstance().getDefaultAndOtherTypesByReflection(request,
				"defaultFunctionTypes", "functionTypes",
				"gov.nih.nci.cananolab.domain.particle.Function",
				"gov.nih.nci.cananolab.domain.function.OtherFunction", true);
		InitSetup
				.getInstance()
				.getDefaultAndOtherTypesByReflection(
						request,
						"defaultNanomaterialEntityTypes",
						"nanomaterialEntityTypes",
						"gov.nih.nci.cananolab.domain.particle.NanomaterialEntity",
						"gov.nih.nci.cananolab.domain.nanomaterial.OtherNanomaterialEntity",
						true);
		InitSetup
				.getInstance()
				.getDefaultAndOtherTypesByReflection(
						request,
						"defaultFunctionalizingEntityTypes",
						"functionalizingEntityTypes",
						"gov.nih.nci.cananolab.domain.particle.FunctionalizingEntity",
						"gov.nih.nci.cananolab.domain.agentmaterial.OtherFunctionalizingEntity",
						true);
		InitCharacterizationSetup.getInstance().getCharacterizationTypes(
				request);
	}
	
	public SortedSet<String> getDefaultTypesByReflection(HttpServletRequest request, String fullClassName) 
	throws Exception {

        return InitSetup.getInstance().getDefaultTypesByReflection(
                request.getSession().getServletContext(),
                "defaultFunctionalizingEntityTypes", fullClassName);
	}
	
	public List<String> getFunctionTypes(HttpServletRequest request)
			throws Exception {

        SortedSet<String> defaultTypes = InitSetup.getInstance().getDefaultTypesByReflection(
				request.getSession().getServletContext(), 
				"defaultFunctionTypes", "gov.nih.nci.cananolab.domain.particle.Function");
        List<String> sortedFormatted = new ArrayList<String>(defaultTypes);

		SortedSet<String> otherTypes = LookupService
				.getAllOtherObjectTypes("gov.nih.nci.cananolab.domain.function.OtherFunction");

		for (String type : otherTypes) {
			sortedFormatted.add("[" + type + "]");
		}

		return sortedFormatted;
	}
	
	public List<String> getNanomaterialEntityTypes(HttpServletRequest request)
	throws Exception {

        SortedSet<String> defaultTypes = InitSetup.getInstance().getDefaultTypesByReflection(
				request.getSession().getServletContext(), 
				"defaultNanomaterialEntityTypes", "gov.nih.nci.cananolab.domain.particle.NanomaterialEntity");
        List<String> sortedFormatted = new ArrayList<String>(defaultTypes);
		
		SortedSet<String> otherTypes = LookupService
				.getAllOtherObjectTypes("gov.nih.nci.cananolab.domain.nanomaterial.OtherNanomaterialEntity");
		
		for (String type : otherTypes) {
			sortedFormatted.add("[" + type + "]");
		}
		
		return sortedFormatted;
	}
	
	public List<String> getFunctionalizingEntityTypes(HttpServletRequest request) 
	throws Exception {
        SortedSet<String> defaultTypes = InitSetup.getInstance().getDefaultTypesByReflection(
				request.getSession().getServletContext(), 
				"defaultFunctionalizingEntityTypes", "gov.nih.nci.cananolab.domain.particle.FunctionalizingEntity");
        List<String> sortedFormatted = new ArrayList<String>(defaultTypes);
		
		SortedSet<String> otherTypes = LookupService
				.getAllOtherObjectTypes("gov.nih.nci.cananolab.domain.agentmaterial.OtherFunctionalizingEntity");
		
		for (String type : otherTypes) {
			sortedFormatted.add("[" + type + "]");
		}
		
		return sortedFormatted;
	}

	public List<String> getOtherSampleNames(HttpServletRequest request, String sampleId, SampleService service) throws Exception
	{
		List<String> names = service.findOtherSampleNamesFromSamePrimaryOrganization(sampleId);
		request.getSession().setAttribute("otherSampleNames", names);
		return names;
	}

	public void setSharedDropdowns(HttpServletRequest request) throws Exception {
		InitSetup.getInstance().getDefaultAndOtherTypesByLookup(request,
				"fileTypes", "file", "type", "otherType", true);
	}

	public void setPOCDropdowns(HttpServletRequest request, SampleService service) throws Exception {
		InitSetup.getInstance().getDefaultAndOtherTypesByLookup(request,
				"contactRoles", "point of contact", "role", "otherRole", true);
		SortedSet<String> organizationNames = service.getAllOrganizationNames();
		request.getSession().setAttribute("allOrganizationNames", organizationNames);
	}

	public void persistPOCDropdowns(HttpServletRequest request, SampleBean sampleBean, SampleService service) throws Exception
	{
		InitSetup.getInstance().persistLookup(request, "point of contact", "role", "otherRole", sampleBean.getThePOC().getDomain().getRole());
		setPOCDropdowns(request, service);
	}

/*	public void updateCSMCleanupEntriesInContext(Sample sample, HttpServletRequest request, SampleService service) throws Exception
	{
		CSMCleanupJob job = new CSMCleanupJob();
		Set<String> secureObjects = job.getAllSecureObjectsToRemove();
		List<String> csmEntriesToRemove = service.removeAccesses(sample, true);
		secureObjects.addAll(csmEntriesToRemove);
		request.getSession().getServletContext().setAttribute("allCSMEntriesToRemove", secureObjects);
	}*/
}
