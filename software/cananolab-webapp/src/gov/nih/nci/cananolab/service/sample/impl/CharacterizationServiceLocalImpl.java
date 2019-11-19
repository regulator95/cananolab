/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.service.sample.impl;

import gov.nih.nci.cananolab.domain.characterization.OtherCharacterization;
import gov.nih.nci.cananolab.domain.common.ExperimentConfig;
import gov.nih.nci.cananolab.domain.common.Finding;
import gov.nih.nci.cananolab.domain.common.Instrument;
import gov.nih.nci.cananolab.domain.common.Technique;
import gov.nih.nci.cananolab.domain.particle.Characterization;
import gov.nih.nci.cananolab.domain.particle.Sample;
import gov.nih.nci.cananolab.dto.common.ExperimentConfigBean;
import gov.nih.nci.cananolab.dto.common.FileBean;
import gov.nih.nci.cananolab.dto.common.FindingBean;
import gov.nih.nci.cananolab.dto.particle.SampleBean;
import gov.nih.nci.cananolab.dto.particle.characterization.CharacterizationBean;
import gov.nih.nci.cananolab.exception.CharacterizationException;
import gov.nih.nci.cananolab.exception.ExperimentConfigException;
import gov.nih.nci.cananolab.exception.NoAccessException;
import gov.nih.nci.cananolab.security.enums.SecureClassesEnum;
import gov.nih.nci.cananolab.security.service.SpringSecurityAclService;
import gov.nih.nci.cananolab.security.utils.SpringSecurityUtil;
import gov.nih.nci.cananolab.service.BaseServiceLocalImpl;
import gov.nih.nci.cananolab.service.sample.CharacterizationService;
import gov.nih.nci.cananolab.service.sample.helper.CharacterizationServiceHelper;
import gov.nih.nci.cananolab.system.applicationservice.CaNanoLabApplicationService;
import gov.nih.nci.cananolab.util.Constants;
import gov.nih.nci.cananolab.util.DateUtils;
import gov.nih.nci.cananolab.system.applicationservice.client.ApplicationServiceProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service methods involving local characterizations
 *
 * @author tanq, pansu
 *
 */
@Component("characterizationService")
public class CharacterizationServiceLocalImpl extends BaseServiceLocalImpl implements CharacterizationService 
{
	private static Logger logger = Logger.getLogger(CharacterizationServiceLocalImpl.class);
	
	@Autowired
	private SpringSecurityAclService springSecurityAclService;
	
	@Autowired
	private CharacterizationServiceHelper characterizationServiceHelper;

	public void saveCharacterization(SampleBean sampleBean, CharacterizationBean charBean) throws CharacterizationException, NoAccessException
	{
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		try {
			Sample sample = sampleBean.getDomain();
			Characterization achar = charBean.getDomainChar();
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
			Boolean newChar = true;
			if (achar.getId() != null) {
				newChar = false;
				if (!springSecurityAclService.currentUserHasWritePermission(achar.getId(), SecureClassesEnum.CHAR.getClazz())) {
					throw new NoAccessException();
				}
				try {
					Characterization dbChar = (Characterization) appService.load(Characterization.class, achar.getId());
				} catch (Exception e) {
					String err = "Object doesn't exist in the database anymore.  Please log in again.";
					logger.error(err);
					throw new CharacterizationException(err, e);
				}
			}
			achar.setSample(sample);

			// save file data to file system
			List<FindingBean> findingBeans = charBean.getFindings();
			if (findingBeans != null && !findingBeans.isEmpty()) {
				for (FindingBean findingBean : findingBeans) {
					for (FileBean fileBean : findingBean.getFiles()) {
						fileUtils.prepareSaveFile(fileBean.getDomainFile());
						fileUtils.writeFile(fileBean);
					}
				}
			}
			appService.saveOrUpdate(achar);
			if (newChar)
				springSecurityAclService.saveAccessForChildObject(achar.getSample().getId(),
																  SecureClassesEnum.SAMPLE.getClazz(),
																  achar.getId(),
																  SecureClassesEnum.CHAR.getClazz());
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String err = "Problem in saving the characterization.";
			logger.error(err, e);
			throw new CharacterizationException(err, e);
		}
	}

	public CharacterizationBean findCharacterizationById(String charId) throws CharacterizationException, NoAccessException
	{
		CharacterizationBean charBean = null;
		try {
			Characterization achar = characterizationServiceHelper.findCharacterizationById(charId);
			if (achar != null) {
				charBean = new CharacterizationBean(achar);
			}
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Problem finding the characterization by id: "
					+ charId, e);
			throw new CharacterizationException();
		}
		return charBean;
	}

	public void deleteCharacterization(Characterization chara) throws CharacterizationException, NoAccessException
	{
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		try {
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
			appService.delete(chara);
		} catch (Exception e) {
			String err = "Error deleting characterization " + chara.getId();
			logger.error(err, e);
			throw new CharacterizationException(err, e);
		}
	}

	public List<CharacterizationBean> findCharacterizationsBySampleId(String sampleId) throws CharacterizationException
	{
		List<CharacterizationBean> charBeans = new ArrayList<CharacterizationBean>();
		try {
			List<Characterization> chars = characterizationServiceHelper.findCharacterizationsBySampleId(sampleId);
			for (Characterization achar : chars) {
				CharacterizationBean charBean = new CharacterizationBean(achar);
				charBeans.add(charBean);
			}
			return charBeans;
		} catch (Exception e) {
			String err = "Error finding characterization by sample ID " + sampleId;
			logger.error(err, e);
			throw new CharacterizationException(err);
		}
	}

	/*public FindingBean findFindingById(String findingId) throws CharacterizationException, NoAccessException
	{
		FindingBean findingBean = null;
		try {
			Finding finding = characterizationServiceHelper.findFindingById(findingId);
			if (finding != null) {
				findingBean = new FindingBean(finding);
			}
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String err = "Error getting finding of ID " + findingId;
			logger.error(err, e);
			throw new CharacterizationException(err, e);
		}
		return findingBean;
	}*/

	public void saveFinding(FindingBean finding) throws CharacterizationException, NoAccessException
	{
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		try {
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
			for (FileBean fileBean : finding.getFiles()) {
				fileUtils.prepareSaveFile(fileBean.getDomainFile());
			}
			appService.saveOrUpdate(finding.getDomain());
			// save file data to file system
			for (FileBean fileBean : finding.getFiles()) {
				fileUtils.writeFile(fileBean);
			}

		} catch (Exception e) {
			String err = "Error saving characterization result finding. ";
			logger.error(err, e);
			throw new CharacterizationException(err, e);
		}
	}

	public void deleteFinding(Finding finding)
			throws CharacterizationException, NoAccessException {
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		try {
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
			appService.delete(finding);
		} catch (Exception e) {
			String err = "Error deleting finding " + finding.getId();
			logger.error(err, e);
			throw new CharacterizationException(err, e);
		}
	}

	public void saveExperimentConfig(String sampleId, ExperimentConfigBean configBean)
			throws ExperimentConfigException, NoAccessException {
		if (SpringSecurityUtil.getPrincipal() == null)
			throw new NoAccessException();

		try {
			ExperimentConfig config = configBean.getDomain();
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
			// get existing createdDate and createdBy
			if (config.getId() != null) {
				ExperimentConfig dbConfig = characterizationServiceHelper.findExperimentConfigById(sampleId, config.getId().toString());
				if (dbConfig != null) {
					// reuse original createdBy if it is not COPY
					if (!dbConfig.getCreatedBy().contains(Constants.AUTO_COPY_ANNOTATION_PREFIX)) {
						config.setCreatedBy(dbConfig.getCreatedBy());
					}
					// reuse original created date
					config.setCreatedDate(dbConfig.getCreatedDate());
				}
			}
			Technique technique = config.getTechnique();
			// check if technique already exists;
			Technique dbTechnique = findTechniqueByType(technique.getType());
			if (dbTechnique != null) {
				technique.setId(dbTechnique.getId());
				technique.setCreatedBy(dbTechnique.getCreatedBy());
				technique.setCreatedDate(dbTechnique.getCreatedDate());
			} else {
				technique.setId(null);
				technique.setCreatedBy(config.getCreatedBy());
				technique.setCreatedDate(new Date());
			}
			if (config.getInstrumentCollection() != null) {
				Collection<Instrument> instruments = new HashSet<Instrument>(config.getInstrumentCollection());
				config.setInstrumentCollection(new HashSet<Instrument>());
				for (Instrument instrument : instruments) {
					Instrument dbInstrument = findInstrumentBy(instrument.getType(), instrument.getManufacturer(),
							instrument.getModelName());
					if (dbInstrument != null) {
						instrument.setId(dbInstrument.getId());
						instrument.setCreatedBy(dbInstrument.getCreatedBy());
						instrument.setCreatedDate(dbInstrument.getCreatedDate());
					} else {
						instrument.setId(null);
					}
					config.getInstrumentCollection().add(instrument);
				}
			}
			appService.saveOrUpdate(config);
		} catch (Exception e) {
			String err = "Error in saving the technique and associated instruments.";
			logger.error(err, e);
			throw new ExperimentConfigException(err, e);
		}
	}

	public void deleteExperimentConfig(String sampleId, ExperimentConfig config) throws ExperimentConfigException, NoAccessException {
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		try {
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
			// get existing createdDate and createdBy
			if (config.getId() != null) {
				ExperimentConfig dbConfig = characterizationServiceHelper.findExperimentConfigById(sampleId, config.getId().toString());
				if (dbConfig != null) {
					config.setCreatedBy(dbConfig.getCreatedBy());
					config.setCreatedDate(dbConfig.getCreatedDate());
				}
			}
			Technique technique = config.getTechnique();
			// check if technique already exists;
			Technique dbTechnique = findTechniqueByType(technique.getType());
			if (dbTechnique != null) {
				technique.setId(dbTechnique.getId());
				technique.setCreatedBy(dbTechnique.getCreatedBy());
				technique.setCreatedDate(dbTechnique.getCreatedDate());
			} else {
				technique.setCreatedBy(config.getCreatedBy());
				technique.setCreatedDate(new Date());
				// need to save the transient object before deleting the
				// experiment config
				appService.saveOrUpdate(technique);
			}
			// check if instrument already exists;
			if (config.getInstrumentCollection() != null) {
				Collection<Instrument> instruments = new HashSet<Instrument>(config.getInstrumentCollection());
				config.getInstrumentCollection().clear();
				int i = 0;
				for (Instrument instrument : instruments) {
					Instrument dbInstrument = findInstrumentBy(instrument.getType(), instrument.getManufacturer(),
							instrument.getModelName());
					if (dbInstrument != null) {
						instrument.setId(dbInstrument.getId());
						instrument.setCreatedBy(dbInstrument.getCreatedBy());
						instrument.setCreatedDate(dbInstrument.getCreatedDate());
					} else {
						instrument.setCreatedBy(config.getCreatedBy());
						instrument.setCreatedDate(DateUtils.addSecondsToCurrentDate(i));
						// need to save the transient object before deleting the
						// experiment config
						appService.saveOrUpdate(instrument);
					}
					config.getInstrumentCollection().add(instrument);
				}
			}
			appService.delete(config);
		} catch (Exception e) {
			String err = "Error in deleting the technique and associated instruments";
			logger.error(err, e);
			throw new ExperimentConfigException(err, e);
		}
	}

	private Technique findTechniqueByType(String type) throws ExperimentConfigException
	{
		Technique technique = null;
		try {
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
			DetachedCriteria crit = DetachedCriteria.forClass(Technique.class)
					.add(Property.forName("type").eq(new String(type)).ignoreCase());
			List results = appService.query(crit);
			for (int i = 0; i < results.size(); i++) {
				technique = (Technique) results.get(i);
			}
		} catch (Exception e) {
			String err = "Problem to retrieve technique by type.";
			logger.error(err, e);
			throw new ExperimentConfigException(err);
		}
		return technique;
	}

	private Instrument findInstrumentBy(String type, String manufacturer,
			String modelName) throws Exception 
	{
		Instrument instrument = null;

		CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
		DetachedCriteria crit = DetachedCriteria.forClass(Instrument.class);
		crit.add(Restrictions.eq("type", type).ignoreCase());
		crit.add(Restrictions.eq("manufacturer", manufacturer).ignoreCase());
		crit.add(Restrictions.eq("modelName", modelName).ignoreCase());
		List results = appService.query(crit);
		for (int i = 0; i < results.size(); i++) {
			instrument = (Instrument) results.get(i);
		}
		return instrument;
	}

	public void copyAndSaveCharacterization(CharacterizationBean charBean,
			SampleBean oldSampleBean, SampleBean[] newSampleBeans,
			boolean copyData) throws CharacterizationException,
			NoAccessException {
		try {
			for (SampleBean sampleBean : newSampleBeans) {
				// create a deep copy
				Characterization copy = charBean.getDomainCopy(SpringSecurityUtil.getLoggedInUserName(), copyData);
				CharacterizationBean copyBean = new CharacterizationBean(copy);
				// try {
				// // copy file visibility
				// for (FindingBean findingBean : copyBean.getFindings()) {
				// for (FileBean fileBean : findingBean.getFiles()) {
				// fileHelper
				// .retrieveVisibilityAndContentForCopiedFile(
				// fileBean, user);
				// }
				// }
				// } catch (Exception e) {
				// String error = "Error setting visibility of the copy.";
				// throw new CharacterizationException(error, e);
				// }
				/**
				 * Need to save associate Config & Finding in copy bean first,
				 * otherwise will get "transient object" Hibernate exception.
				 */
				List<ExperimentConfigBean> expConfigs = copyBean.getExperimentConfigs();
				if (expConfigs != null && !expConfigs.isEmpty()) {
					for (ExperimentConfigBean configBean : expConfigs) {
						this.saveExperimentConfig(sampleBean.getDomain().getId() + "", configBean);
					}
				}
				List<FindingBean> findings = copyBean.getFindings();
				// copy file visibility and replace file URI with new sample
				// name
				if (findings != null && !findings.isEmpty()) {
					for (FindingBean findingBean : findings) {
						for (FileBean fileBean : findingBean.getFiles()) {
							fileUtils.updateClonedFileInfo(fileBean,
									oldSampleBean.getDomain().getName(),
									sampleBean.getDomain().getName());
						}
						this.saveFinding(findingBean);
					}
				}
				this.saveCharacterization(sampleBean, copyBean);
				// save associated accessibility for the copied characterization
				
				//Commented while removing CSM - already done in saveCharacterization method call
				/*// find sample accesses
				List<AccessibilityBean> sampleAccesses = super.findSampleAccesses(copy.getSample().getId().toString());
				// save sample accesses
				for (AccessibilityBean access : sampleAccesses) {
					this.accessUtils.assignAccessibility(access, copy);
				}*/
			}
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Error in copying characterization: ", e);
			String error = "Error saving the copy of characterization.";
			throw new CharacterizationException(error, e);
		}
	}

	public int getNumberOfPublicCharacterizations(String characterizationClassName) throws CharacterizationException {
		try {
            return characterizationServiceHelper.getNumberOfPublicCharacterizations(characterizationClassName);
		} catch (Exception e) {
			String err = "Error finding counts of public characterizations of type " + characterizationClassName;
			logger.error(err, e);
			throw new CharacterizationException(err, e);

		}
	}
	
	public int getNumberOfPublicCharacterizationsForJob(List<String> characterizationClassNames) throws CharacterizationException {
		try {
            return characterizationServiceHelper.getNumberOfPublicCharacterizationsForJob(characterizationClassNames);
		} catch (Exception e) {
			String err = "Error finding counts of public characterizations of type " + characterizationClassNames;
			logger.error(err, e);
			throw new CharacterizationException(err, e);

		}
	}

	public List<String> findOtherCharacterizationByAssayCategory(
			String assayCategory) throws CharacterizationException {
		List<String> charNames = new ArrayList<String>();
		try {
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider.getApplicationService();
			DetachedCriteria crit = DetachedCriteria.forClass(OtherCharacterization.class).add(
					Property.forName("assayCategory").eq(assayCategory).ignoreCase());
			List result = appService.query(crit);
			for (Object obj : result) {
				String charName = ((OtherCharacterization) obj).getName();
				if (!charNames.contains(charName)) {
					charNames.add(charName);
				}
			}
		} catch (Exception e) {
			String err = "Error finding other characterizations by assay cateogry " + assayCategory;
			logger.error(err, e);
			throw new CharacterizationException(err, e);
		}
		return charNames;
	}

	public CharacterizationServiceHelper getHelper() {
		return characterizationServiceHelper;
	}

	public void assignAccesses(Characterization achar) throws CharacterizationException, NoAccessException
	{
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(achar.getSample().getId(), SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			// find sample accesses
			springSecurityAclService.saveAccessForChildObject(achar.getSample().getId(), SecureClassesEnum.SAMPLE.getClazz(), 
															  achar.getId(), SecureClassesEnum.CHAR.getClazz());
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			throw new CharacterizationException("Error in assigning characterization accessibility", e);
		}
	}

	public void removeAccesses(Characterization achar) throws CharacterizationException, NoAccessException
	{
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(achar.getId(), SecureClassesEnum.CHAR.getClazz())) {
				throw new NoAccessException();
			}
			springSecurityAclService.deleteAccessObject(achar.getId(), SecureClassesEnum.CHAR.getClazz());
			
			/*// find sample accesses
			List<AccessibilityBean> sampleAccesses = super.findSampleAccesses(achar.getSample().getId().toString());
			for (AccessibilityBean access : sampleAccesses) {
				accessUtils.removeAccessibility(access, achar, false);
			}*/
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in assigning characterization accessibility";
			throw new CharacterizationException(error, e);
		}
	}

	public void assignAccesses(Characterization achar, ExperimentConfig config) throws CharacterizationException, NoAccessException
	{
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(achar.getId(), SecureClassesEnum.CHAR.getClazz())) {
				throw new NoAccessException();
			}
			
			/*springSecurityAclService.saveAccessForChildObject(achar.getId(), SecureClassesEnum.CHAR.getClazz(),
															  config.getId(), SecureClassesEnum.CONFIG.getClazz());*/

			/*// find sample accesses, already contains owner for config
			List<AccessibilityBean> sampleAccesses = this.findSampleAccesses(achar.getSample().getId().toString());
			for (AccessibilityBean access : sampleAccesses) {
				accessUtils.assignAccessibility(access, config);
			}*/
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in assigning experiment config accessibility";
			throw new CharacterizationException(error, e);
		}
	}

	public void assignAccesses(Characterization achar, Finding finding) throws CharacterizationException, NoAccessException
	{
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(achar.getId(), SecureClassesEnum.CHAR.getClazz())) {
				throw new NoAccessException();
			}
			
			/*springSecurityAclService.saveAccessForChildObject(achar.getId(), SecureClassesEnum.CHAR.getClazz(),
															  finding.getId(), SecureClassesEnum.FINDING.getClazz());*/
			
			
			/*// find sample accesses, already contains owner for finding
			List<AccessibilityBean> sampleAccesses = this.findSampleAccesses(achar.getSample().getId().toString());
			for (AccessibilityBean access : sampleAccesses) {
				accessUtils.assignAccessibility(access, finding);
			}*/
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in assigning finding accessibility";
			throw new CharacterizationException(error, e);
		}
	}

	public void removeAccesses(Characterization achar, ExperimentConfig config) throws CharacterizationException, NoAccessException
	{
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(achar.getId(), SecureClassesEnum.CHAR.getClazz())) {
				throw new NoAccessException();
			}
			//springSecurityAclService.deleteAccessObject(config.getId(), SecureClassesEnum.CONFIG.getClazz());

			
			/*// find sample accesses, already contains owner for config
			List<AccessibilityBean> sampleAccesses = this.findSampleAccesses(achar.getSample().getId().toString());
			for (AccessibilityBean access : sampleAccesses) {
				accessUtils.removeAccessibility(access, config, false);
			}*/
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in removing experiment config accessibility";
			throw new CharacterizationException(error, e);
		}
	}

	public void removeAccesses(Characterization achar, Finding finding) throws CharacterizationException, NoAccessException
	{
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(achar.getId(), SecureClassesEnum.CHAR.getClazz())) {
				throw new NoAccessException();
			}
			//springSecurityAclService.deleteAccessObject(finding.getId(), SecureClassesEnum.FINDING.getClazz());

			/*// find sample accesses, already contains owner for finding
			List<AccessibilityBean> sampleAccesses = this.findSampleAccesses(achar.getSample().getId().toString());
			for (AccessibilityBean access : sampleAccesses) {
				accessUtils.removeAccessibility(access, finding, false);
			}*/
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in removing finding accessibility";
			throw new CharacterizationException(error, e);
		}
	}

	@Override
	public SpringSecurityAclService getSpringSecurityAclService() {
		return springSecurityAclService;
	}
	
}