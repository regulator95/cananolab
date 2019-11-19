/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.restful.core;

import gov.nih.nci.cananolab.restful.sample.InitCharacterizationSetup;
import gov.nih.nci.cananolab.util.SampleConstants;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import javax.servlet.ServletContext;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;


/**
 * Creates default CSM groups and sample types and initialize Hibernate
 * configurations as soon as server starts up.
 * 
 * @author pansu
 * 
 */
@Component("customPlugInBO")
public class CustomPlugInBO implements ServletContextAware { //implements PlugIn {
	Logger logger = Logger.getLogger(CustomPlugInBO.class);
	
	ServletContext servletContext;

	// This method will be called at application startup time
	public CustomPlugInBO() {
		//init(servletContext);
	}
 	
	public void init(ServletContext actionServlet)
			{//throws ServletException {
		logger.info("Entering CustomPlugIn.init()");
		try {
			// set default lookup valules in the servlet context
            InitSetup.getInstance().getDefaultLookupTable(actionServlet);
			InitSetup
					.getInstance()
					.getDefaultTypesByReflection(actionServlet,
							"defaultFunctionalizingEntityTypes",
							"gov.nih.nci.cananolab.domain.particle.FunctionalizingEntity");
			InitSetup.getInstance().getDefaultTypesByReflection(actionServlet,
					"defaultNanomaterialEntityTypes",
					"gov.nih.nci.cananolab.domain.particle.NanomaterialEntity");
			InitSetup.getInstance().getDefaultTypesByReflection(actionServlet,
					"defaultFunctionTypes",
					"gov.nih.nci.cananolab.domain.particle.Function");
			InitSetup.getInstance().getDefaultTypesByReflection(actionServlet,
					"defaultTargetTypes",
					"gov.nih.nci.cananolab.domain.function.Target");
			InitSetup
					.getInstance()
					.getDefaultTypesByReflection(actionServlet,
							"defaultChemicalAssociationTypes",
							"gov.nih.nci.cananolab.domain.particle.ChemicalAssociation");

			InitSetup.getInstance().getDefaultTypesByLookup(actionServlet,
					"speciesTypes", "species", "type");
			InitSetup.getInstance().getDefaultTypesByLookup(actionServlet,
					"wallTypes", "carbon nanotube", "wallType");

			InitCharacterizationSetup.getInstance()
					.getDefaultCharacterizationTypes(actionServlet);
			// For PubChem data sources drop-down list.
			actionServlet.setAttribute("pubChemDataSources",
					SampleConstants.PUBCHEM_DS_LIST);

			InitSetup.getInstance().setStaticOptions(actionServlet);

			// InitSecuritySetup.getInstance().setupDefaultCSM();

			// load mapping for data availability
			InitSetup.getInstance().getDefaultTypesByLookup(actionServlet,
					"MINChar", "MINChar", "entity");
			InitSetup.getInstance().getLookupByName(actionServlet,
					"caNano2MINChar", "caNano2MINChar");
			InitSetup
					.getInstance()
					.getDefaultTypesByReflection(
                            actionServlet,
							"physicoChars",
							"gov.nih.nci.cananolab.domain.characterization.physical.PhysicoChemicalCharacterization");
			InitSetup
					.getInstance()
					.getDefaultTypesByReflection(actionServlet, "invitroChars",
							"gov.nih.nci.cananolab.domain.characterization.invitro.InvitroCharacterization");

			InitSetup
					.getInstance()
					.getDefaultTypesByReflection(actionServlet, "invivoChars",
							"gov.nih.nci.cananolab.domain.characterization.invivo.InvivoCharacterization");

			InitSetup
					.getInstance()
					.getDefaultTypesByReflection(actionServlet, "chemicalAssocs",
							"gov.nih.nci.cananolab.domain.particle.ChemicalAssociation");

			//obtain online help url from properties file
			actionServlet.setAttribute("webHelp", getWikiHelpUrl());

		} catch (Exception e) {
			this.logger.error("Servlet initialization error", e);
		}
		logger.info("Exiting CustomPlugIn.init()");
	}
	
	private String getWikiHelpUrl() {
		Properties wikihelpProperties = new Properties();
		String wikiSiteBegin = "";
		String wikiSiteBeginKey = "wiki_help_main";
		try {

			String wikihelpPropertiesFileName = null;

			wikihelpPropertiesFileName = System.getProperty("gov.nih.nci.cananolab.wikihelpProperties");
			
			try {
			
				FileInputStream in = new FileInputStream(wikihelpPropertiesFileName);
				wikihelpProperties.load(in);
				}
			catch (IOException e) {
				e.printStackTrace();
			}
			wikiSiteBegin =  wikihelpProperties.getProperty(wikiSiteBeginKey);
		}

		// Default to 100 on an exception
		catch (Exception e) {
			System.err.println("Error loading system.properties file");
			e.printStackTrace();
		}
		return wikiSiteBegin;
	}

	// This method will be called at application shutdown time
	public void destroy() {
		logger.info("Entering CustomPlugIn.destroy()");
		logger.info("Exiting CustomPlugIn.destroy()");
		// be sure to clean up CSM entries that need to be removed before
		// shutting down the server.
	}

	@Override
	public void setServletContext(ServletContext arg0) {
		servletContext = arg0;
		init(servletContext);
	}

	public ServletContext getServletContext() {
		return servletContext;
	}
	
	
}
