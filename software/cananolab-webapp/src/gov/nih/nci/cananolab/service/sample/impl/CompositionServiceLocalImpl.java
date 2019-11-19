/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.service.sample.impl;

import gov.nih.nci.cananolab.domain.common.File;
import gov.nih.nci.cananolab.domain.particle.AssociatedElement;
import gov.nih.nci.cananolab.domain.particle.ChemicalAssociation;
import gov.nih.nci.cananolab.domain.particle.ComposingElement;
import gov.nih.nci.cananolab.domain.particle.Function;
import gov.nih.nci.cananolab.domain.particle.FunctionalizingEntity;
import gov.nih.nci.cananolab.domain.particle.NanomaterialEntity;
import gov.nih.nci.cananolab.domain.particle.Sample;
import gov.nih.nci.cananolab.domain.particle.SampleComposition;
import gov.nih.nci.cananolab.dto.common.FileBean;
import gov.nih.nci.cananolab.dto.particle.SampleBean;
import gov.nih.nci.cananolab.dto.particle.composition.ChemicalAssociationBean;
import gov.nih.nci.cananolab.dto.particle.composition.CompositionBean;
import gov.nih.nci.cananolab.dto.particle.composition.FunctionalizingEntityBean;
import gov.nih.nci.cananolab.dto.particle.composition.NanomaterialEntityBean;
import gov.nih.nci.cananolab.exception.ChemicalAssociationViolationException;
import gov.nih.nci.cananolab.exception.CompositionException;
import gov.nih.nci.cananolab.exception.NoAccessException;
import gov.nih.nci.cananolab.security.enums.SecureClassesEnum;
import gov.nih.nci.cananolab.security.service.SpringSecurityAclService;
import gov.nih.nci.cananolab.security.utils.SpringSecurityUtil;
import gov.nih.nci.cananolab.service.BaseServiceLocalImpl;
import gov.nih.nci.cananolab.service.sample.CompositionService;
import gov.nih.nci.cananolab.service.sample.helper.CompositionServiceHelper;
import gov.nih.nci.cananolab.system.applicationservice.CaNanoLabApplicationService;
import gov.nih.nci.cananolab.system.applicationservice.client.ApplicationServiceProvider;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Local implementation of CompositionService.
 *
 * @author pansu
 *
 */
@Component("compositionService")
public class CompositionServiceLocalImpl extends BaseServiceLocalImpl implements CompositionService {
	private static Logger logger = Logger.getLogger(CompositionServiceLocalImpl.class);

	@Autowired
	private SpringSecurityAclService springSecurityAclService;

	@Autowired
	private CompositionServiceHelper compositionServiceHelper;

	public void saveNanomaterialEntity(SampleBean sampleBean, NanomaterialEntityBean entityBean)
			throws CompositionException, NoAccessException {
		logger.debug("In saveNanomaterialEntity");
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		try {
			Sample sample = sampleBean.getDomain();
			if (!springSecurityAclService.currentUserHasWritePermission(sample.getId(),
					SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
					.getApplicationService();
			NanomaterialEntity entity = entityBean.getDomainEntity();
			logger.debug("NanomaterialEntity " + entity.getCreatedBy());
			Boolean newEntity = true;
			Boolean newComp = true;
			if (entity.getId() != null) {
				newEntity = false;
				try {
					logger.debug("Calling appService.load");
					NanomaterialEntity dbEntity = (NanomaterialEntity) appService.load(NanomaterialEntity.class,
							entity.getId());
					logger.debug("dbEntity retrieved");
				} catch (Exception e) {
					String err = "Object doesn't exist in the database anymore.  Please log in again.";
					logger.error(err);
					throw new CompositionException(err, e);
				}
			}

			if (sample.getSampleComposition() == null) {
				sample.setSampleComposition(new SampleComposition());
				sample.getSampleComposition().setSample(sample);
				// particleSample.getSampleComposition()
				// .setNanomaterialEntityCollection(
				// new HashSet<NanomaterialEntity>());

			}
			if (sample.getSampleComposition().getId() != null) {
				newComp = false;
			}
			entity.setSampleComposition(sample.getSampleComposition());
			// particleSample.getSampleComposition()
			// .getNanomaterialEntityCollection().add(entity);

			// save file and keyword
			for (FileBean fileBean : entityBean.getFiles()) {
				fileUtils.prepareSaveFile(fileBean.getDomainFile());
			}
			appService.saveOrUpdate(entity);
			// save file to the file system
			for (FileBean fileBean : entityBean.getFiles()) {
				fileUtils.writeFile(fileBean);
			}

			// Commented when replacing CSM - saving Composition and
			// NanoMaterial Entity accessibility may not be required
			// since ACL inheritance is implemented
			/*
			 * if (newComp)
			 * springSecurityAclService.saveAccessForChildObject(sample.getId(),
			 * SecureClassesEnum.SAMPLE.getClazz(),
			 * sample.getSampleComposition().getId(),
			 * SecureClassesEnum.COMPOSITION.getClazz());
			 * 
			 * 
			 * if (newEntity)
			 * springSecurityAclService.saveAccessForChildObject(sample.
			 * getSampleComposition().getId(),
			 * SecureClassesEnum.COMPOSITION.getClazz(), entity.getId(),
			 * SecureClassesEnum.NANO.getClazz());
			 * 
			 * // find sample accesses List<AccessibilityBean> sampleAccesses =
			 * super.findSampleAccesses(entity.getSampleComposition().getSample(
			 * ).getId().toString()); // save sample accesses for
			 * (AccessibilityBean access : sampleAccesses) { if (newComp) {
			 * this.saveAccessibility(access,
			 * sample.getSampleComposition().getId().toString()); } if
			 * (newEntity) { this.saveAccessibility(access,
			 * entity.getId().toString()); } }
			 */
		} catch (NoAccessException e) {
			logger.error(e);
			throw e;
		} catch (Exception e) {
			String err = "Error in saving a nanomaterial entity.";
			logger.error(err, e);
			throw new CompositionException(err, e);
		}
	}

	public NanomaterialEntityBean findNanomaterialEntityById(String sampleId, String entityId)
			throws CompositionException, NoAccessException {
		NanomaterialEntityBean entityBean = null;
		try {
			NanomaterialEntity entity = compositionServiceHelper.findNanomaterialEntityById(sampleId, entityId);
			if (entity != null) {
				entityBean = new NanomaterialEntityBean(entity);
			}
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String err = "Problem finding the nanomaterial entity by id: " + entityId;
			logger.error(err, e);
			throw new CompositionException(err, e);
		}
		return entityBean;
	}

	public void saveFunctionalizingEntity(SampleBean sampleBean, FunctionalizingEntityBean entityBean)
			throws CompositionException, NoAccessException {
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		try {
			Sample sample = sampleBean.getDomain();
			if (!springSecurityAclService.currentUserHasWritePermission(sample.getId(),
					SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
					.getApplicationService();
			FunctionalizingEntity entity = entityBean.getDomainEntity();
			Boolean newEntity = true;
			Boolean newComp = true;
			if (entity.getId() != null) {
				newEntity = false;
				try {
					// TODO:Commeted to removeCSM. Revisit for access
					// inheritance.
					/*
					 * if (!securityService.checkCreatePermission(entityBean
					 * .getDomainEntity().getId().toString())) { throw new
					 * NoAccessException(); }
					 */
					FunctionalizingEntity dbEntity = (FunctionalizingEntity) appService
							.load(FunctionalizingEntity.class, entity.getId());
				} catch (Exception e) {
					String err = "Object doesn't exist in the database anymore.  Please log in again.";
					logger.error(err);
					throw new CompositionException(err, e);
				}
			}

			if (sample.getSampleComposition() == null) {
				sample.setSampleComposition(new SampleComposition());
				sample.getSampleComposition().setSample(sample);
				// particleSample.getSampleComposition()
				// .setFunctionalizingEntityCollection(
				// new HashSet<FunctionalizingEntity>());

			}
			if (sample.getSampleComposition().getId() != null) {
				newComp = false;
			}
			entity.setSampleComposition(sample.getSampleComposition());
			// particleSample.getSampleComposition()
			// .getFunctionalizingEntityCollection().add(entity);

			// save file and keyword
			for (FileBean fileBean : entityBean.getFiles()) {
				fileUtils.prepareSaveFile(fileBean.getDomainFile());
			}
			appService.saveOrUpdate(entity);
			// save file to the file system
			for (FileBean fileBean : entityBean.getFiles()) {
				fileUtils.writeFile(fileBean);
			}
			// Commented when replacing CSM - saving Composition and
			// Functionalizing Entity accessibility may not be required
			// since ACL inheritance is implemented
			/*
			 * if (newComp)
			 * springSecurityAclService.saveAccessForChildObject(sample.getId(),
			 * SecureClassesEnum.SAMPLE.getClazz(),
			 * sample.getSampleComposition().getId(),
			 * SecureClassesEnum.COMPOSITION.getClazz());
			 * 
			 * if (newEntity)
			 * springSecurityAclService.saveAccessForChildObject(sample.
			 * getSampleComposition().getId(),
			 * SecureClassesEnum.COMPOSITION.getClazz(), entity.getId(),
			 * SecureClassesEnum.FUNCTIONALIZING.getClazz());
			 * 
			 * 
			 * // find sample accesses List<AccessibilityBean> sampleAccesses =
			 * super.findSampleAccesses(entity.getSampleComposition().getSample(
			 * ).getId().toString()); // save sample accesses for
			 * (AccessibilityBean access : sampleAccesses) { if (newComp) {
			 * this.saveAccessibility(access, sample
			 * .getSampleComposition().getId().toString()); } if (newEntity) {
			 * this.saveAccessibility(access, entity.getId().toString()); } }
			 */
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String err = "Problem saving the functionalizing entity.";
			logger.error(err, e);
			throw new CompositionException(err, e);
		}
	}

	public void saveChemicalAssociation(SampleBean sampleBean, ChemicalAssociationBean assocBean)
			throws CompositionException, NoAccessException {
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		try {
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
					.getApplicationService();
			ChemicalAssociation assoc = assocBean.getDomainAssociation();
			Sample sample = sampleBean.getDomain();
			if (!springSecurityAclService.currentUserHasWritePermission(sample.getId(),
					SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			Boolean newAssoc = true;
			Boolean newComp = true;
			if (assoc.getId() != null) {
				newAssoc = false;
				// TODO:Commeted to removeCSM. Revisit for access inheritance.
				/*
				 * if (!securityService.checkCreatePermission(assocBean
				 * .getDomainAssociation().getId().toString())) { throw new
				 * NoAccessException(); }
				 */
				try {
					ChemicalAssociation dbAssoc = (ChemicalAssociation) appService.load(ChemicalAssociation.class,
							assoc.getId());
				} catch (Exception e) {
					String err = "Object doesn't exist in the database anymore.  Please log in again.";
					logger.error(err);
					throw new CompositionException(err, e);
				}
			}
			if (sample.getSampleComposition() == null) {
				sample.setSampleComposition(new SampleComposition());
				sample.getSampleComposition().setSample(sample);
				// particleSample.getSampleComposition()
				// .setFunctionalizingEntityCollection(
				// new HashSet<FunctionalizingEntity>());

			}
			if (sample.getSampleComposition().getId() != null) {
				newComp = false;
			}
			// composition.getChemicalAssociationCollection().add(assoc);
			assoc.setSampleComposition(sample.getSampleComposition());
			// save file and keyword
			for (FileBean fileBean : assocBean.getFiles()) {
				fileUtils.prepareSaveFile(fileBean.getDomainFile());
			}
			appService.saveOrUpdate(assoc);
			// save file to the file system
			for (FileBean fileBean : assocBean.getFiles()) {
				fileUtils.writeFile(fileBean);
			}

			// Commented when replacing CSM - saving Composition and
			// NanoMaterial Entity accessibility may not be required
			// since access is controlled by access to sample
			/*
			 * if (newComp)
			 * springSecurityAclService.saveAccessForChildObject(sample.getId(),
			 * SecureClassesEnum.SAMPLE.getClazz(),
			 * sample.getSampleComposition().getId(),
			 * SecureClassesEnum.COMPOSITION.getClazz());
			 * 
			 * if (newAssoc)
			 * springSecurityAclService.saveAccessForChildObject(sample.
			 * getSampleComposition().getId(),
			 * SecureClassesEnum.COMPOSITION.getClazz(), assoc.getId(),
			 * SecureClassesEnum.CHEMASSOC.getClazz());
			 * 
			 * // find sample accesses List<AccessibilityBean> sampleAccesses =
			 * super .findSampleAccesses(assoc.getSampleComposition()
			 * .getSample().getId().toString()); // save sample accesses for
			 * (AccessibilityBean access : sampleAccesses) { if (newComp) {
			 * this.saveAccessibility(access, sample
			 * .getSampleComposition().getId().toString()); } if (newAssoc) {
			 * this.saveAccessibility(access, assoc.getId().toString()); } }
			 */
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String err = "Problem saving the chemical assocation.";
			logger.error(err, e);
			throw new CompositionException(err, e);
		}
	}

	public void saveCompositionFile(SampleBean sampleBean, FileBean fileBean)
			throws CompositionException, NoAccessException {
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		try {
			Sample sample = sampleBean.getDomain();
			if (!springSecurityAclService.currentUserHasWritePermission(sample.getId(),
					SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			File file = fileBean.getDomainFile();
			Boolean newFile = true;
			Boolean newComp = true;
			if (file.getId() != null) {
				newFile = false;
			}
			// TODO:Commeted to removeCSM. Revisit for access inheritance.
			/*
			 * &&
			 * !securityService.checkCreatePermission(file.getId().toString())
			 */
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
					.getApplicationService();
			if (!newFile) {
				appService.saveOrUpdate(file);
			} else {
				fileUtils.prepareSaveFile(file);

				SampleComposition comp = sample.getSampleComposition();
				if (comp == null) {
					comp = new SampleComposition();
					comp.setSample(sampleBean.getDomain());
					comp.setFileCollection(new HashSet<File>());
				} else if (comp.getId() == null) {
					comp.setFileCollection(new HashSet<File>());
				} else {
					newComp = false;
					// need to load the composition file collection to save
					// composition
					// because of
					// unidirectional relationship between composition and file

					comp = compositionServiceHelper.findCompositionBySampleId(sample.getId().toString());
				}
				comp.getFileCollection().add(file);
				sample.setSampleComposition(comp);
				if (file.getId() == null) { // because of unidirectional
					// relationship between composition
					// and lab files
					appService.saveOrUpdate(comp);
				} else {
					appService.saveOrUpdate(file);
				}
				// write file to file system
				fileUtils.writeFile(fileBean);
			}
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String err = "Error in saving the composition file.";
			logger.error(err, e);
			throw new CompositionException(err, e);
		}
	}

	public FunctionalizingEntityBean findFunctionalizingEntityById(String sampleId, String entityId)
			throws CompositionException, NoAccessException {
		FunctionalizingEntityBean entityBean = null;
		try {
			FunctionalizingEntity entity = compositionServiceHelper.findFunctionalizingEntityById(sampleId, entityId);
			if (entity != null) {
				entityBean = new FunctionalizingEntityBean(entity);
			} else {
				throw new NoAccessException("User doesn't have access to the sample");
			}
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String err = "Problem finding the functionalizing entity by id: " + entityId;
			logger.error(err, e);
			throw new CompositionException(err, e);
		}
		return entityBean;
	}

	public ChemicalAssociationBean findChemicalAssociationById(String sampleId, String assocId)
			throws CompositionException, NoAccessException {
		ChemicalAssociationBean assocBean = null;
		try {
			ChemicalAssociation assoc = compositionServiceHelper.findChemicalAssociationById(sampleId, assocId);
			if (assoc != null) {
				assocBean = new ChemicalAssociationBean(assoc);
			} else {
				throw new NoAccessException();
			}
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String err = "Problem finding the chemical association by id: " + assocId;
			logger.error(err, e);
			throw new CompositionException(err, e);
		}
		return assocBean;
	}

	public void deleteNanomaterialEntity(NanomaterialEntity entity)
			throws CompositionException, ChemicalAssociationViolationException, NoAccessException {
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		Boolean canDelete = this.checkChemicalAssociationBeforeDelete(entity);
		if (!canDelete) {
			throw new ChemicalAssociationViolationException(
					"The nanomaterial entity is used in a chemical association.  Please delete the chemcial association first before deleting the nanomaterial entity.");
		}
		try {
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
					.getApplicationService();
			appService.delete(entity);
		} catch (Exception e) {
			String err = "Error deleting nanomaterial entity " + entity.getId();
			logger.error(err, e);
			throw new CompositionException(err, e);
		}
	}

	public void deleteFunctionalizingEntity(FunctionalizingEntity entity)
			throws CompositionException, ChemicalAssociationViolationException, NoAccessException {
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		Boolean canDelete = this.checkChemicalAssociationBeforeDelete(entity.getSampleComposition(), entity);
		if (!canDelete) {
			throw new ChemicalAssociationViolationException("The functionalizing entity " + entity.getName()
					+ " is used in a chemical association.  Please delete the chemcial association first before deleting the functionalizing entity.");
		}
		try {
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
					.getApplicationService();
			appService.delete(entity);
		} catch (Exception e) {
			String err = "Error deleting functionalizing entity " + entity.getId();
			logger.error(err, e);
			throw new CompositionException(err, e);
		}
	}

	public void deleteChemicalAssociation(ChemicalAssociation assoc) throws CompositionException, NoAccessException {
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		try {
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
					.getApplicationService();
			appService.delete(assoc);
		} catch (Exception e) {
			String err = "Error deleting chemical association " + assoc.getId();
			logger.error(err, e);
			throw new CompositionException(err, e);
		}
	}

	public void deleteCompositionFile(SampleComposition comp, File file)
			throws CompositionException, NoAccessException {
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		try {
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
					.getApplicationService();
			// load files first
			List<File> fileList = compositionServiceHelper.findFilesByCompositionInfoId(comp.getSample().getId(),
					comp.getId().toString(), "SampleComposition");
			comp.setFileCollection(new HashSet<File>(fileList));
			comp.getFileCollection().remove(file);
			appService.saveOrUpdate(comp);
		} catch (Exception e) {
			String err = "Error deleting composition file " + file.getUri();
			logger.error(err, e);
			throw new CompositionException(err, e);
		}
	}

	// check if any composing elements of the nanomaterial entity is involved in
	// the chemical association
	public boolean checkChemicalAssociationBeforeDelete(NanomaterialEntity entity) {
		// need to delete chemical associations first if associated elements
		// are composing elements
		Collection<ChemicalAssociation> assocSet = entity.getSampleComposition().getChemicalAssociationCollection();
		if (assocSet != null) {
			for (ChemicalAssociation assoc : assocSet) {
				if (entity.getComposingElementCollection().contains(assoc.getAssociatedElementA())
						|| entity.getComposingElementCollection().contains(assoc.getAssociatedElementB())) {
					return false;
				}
			}
		}
		return true;
	}

	// check if the associated element is involved in the chemical
	// association
	public boolean checkChemicalAssociationBeforeDelete(SampleComposition comp, AssociatedElement assocElement) {
		// need to delete chemical associations first if associated elements
		// are functionalizing entities or composing elements
		Collection<ChemicalAssociation> assocSet = comp.getChemicalAssociationCollection();
		if (assocSet != null) {
			for (ChemicalAssociation assoc : assocSet) {
				if (assocElement.equals(assoc.getAssociatedElementA())
						|| assocElement.equals(assoc.getAssociatedElementB())) {
					return false;
				}
			}
		}
		return true;
	}

	public CompositionBean findCompositionBySampleId(String sampleId) throws CompositionException {
		CompositionBean comp = null;
		try {
			SampleComposition composition = compositionServiceHelper.findCompositionBySampleId(sampleId);
			if (composition != null) {
				comp = new CompositionBean(composition);
			}
		} catch (Exception e) {
			String err = "Error finding composition by sample ID: " + sampleId;
			throw new CompositionException(err, e);
		}
		return comp;
	}

	public void copyAndSaveNanomaterialEntity(NanomaterialEntityBean entityBean, SampleBean oldSampleBean,
			SampleBean[] newSampleBeans) throws CompositionException, NoAccessException {
		try {
			for (SampleBean sampleBean : newSampleBeans) {
				NanomaterialEntityBean copyBean = null;
				NanomaterialEntity copy = entityBean.getDomainCopy(SpringSecurityUtil.getLoggedInUserName());
				try {
					copyBean = new NanomaterialEntityBean(copy);
					// copy file file content
					for (FileBean fileBean : copyBean.getFiles()) {
						fileUtils.updateClonedFileInfo(fileBean, oldSampleBean.getDomain().getName(),
								sampleBean.getDomain().getName());
					}
				} catch (Exception e) {
					String error = "Error in copying the nanomaterial entity.";
					throw new CompositionException(error, e);
				}
				if (copyBean != null) {
					saveNanomaterialEntity(sampleBean, copyBean);
					// Commented while removing CSM
					/*
					 * // save associated accessibility for the copied entity //
					 * save sample accesses
					 * springSecurityAclService.saveAccessForChildObject(
					 * sampleBean.getDomain().getSampleComposition().getId(),
					 * SecureClassesEnum.COMPOSITION.getClazz(), copy.getId(),
					 * SecureClassesEnum.NANO.getClazz());
					 * 
					 * // find sample accesses List<AccessibilityBean>
					 * sampleAccesses = super
					 * .findSampleAccesses(copy.getSampleComposition()
					 * .getSample().getId().toString());
					 * 
					 * for (AccessibilityBean access : sampleAccesses) {
					 * this.accessUtils.assignAccessibility(access, copy); }
					 */
				}
			}
		} catch (NoAccessException e) {
			logger.error("Error while copying and saving nanomaterial entity: ", e);
			throw e;
		} catch (Exception e) {
			String error = "Error in copying the characterization.";
			throw new CompositionException(error, e);
		}
	}

	public void copyAndSaveFunctionalizingEntity(FunctionalizingEntityBean entityBean, SampleBean oldSampleBean,
			SampleBean[] newSampleBeans) throws CompositionException, NoAccessException {
		try {
			for (SampleBean sampleBean : newSampleBeans) {
				FunctionalizingEntityBean copyBean = null;
				FunctionalizingEntity copy = entityBean.getDomainCopy(SpringSecurityUtil.getLoggedInUserName());
				try {
					copyBean = new FunctionalizingEntityBean(copy);
					// copy file visibility and file content
					for (FileBean fileBean : copyBean.getFiles()) {
						fileUtils.updateClonedFileInfo(fileBean, oldSampleBean.getDomain().getName(),
								sampleBean.getDomain().getName());
					}
				} catch (Exception e) {
					String error = "Error in copying the functionalizing entity.";
					throw new CompositionException(error, e);
				}
				if (copyBean != null) {
					saveFunctionalizingEntity(sampleBean, copyBean);
					// Commented while removing CSM
					/*
					 * springSecurityAclService.saveAccessForChildObject(
					 * sampleBean.getDomain().getSampleComposition().getId(),
					 * SecureClassesEnum.COMPOSITION.getClazz(), copy.getId(),
					 * SecureClassesEnum.FUNCTIONALIZING.getClazz());
					 * 
					 * // save associated accessibility for the copied entity //
					 * find sample accesses List<AccessibilityBean>
					 * sampleAccesses = super
					 * .findSampleAccesses(copy.getSampleComposition()
					 * .getSample().getId().toString()); // save sample accesses
					 * for (AccessibilityBean access : sampleAccesses) {
					 * this.accessUtils.assignAccessibility(access, copy); }
					 */
				}
			}
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in saving the copied functionalizing entity.";
			throw new CompositionException(error, e);
		}
	}

	public void deleteComposition(SampleComposition comp)
			throws ChemicalAssociationViolationException, CompositionException, NoAccessException {
		if (SpringSecurityUtil.getPrincipal() == null) {
			throw new NoAccessException();
		}
		Long sampleId = comp.getSample().getId();
		// delete composition files
		if (comp.getFileCollection() != null) {
			for (File file : comp.getFileCollection()) {
				deleteCompositionFile(comp, file);
			}
		}
		// delete chemical association
		if (comp.getChemicalAssociationCollection() != null) {
			for (ChemicalAssociation assoc : comp.getChemicalAssociationCollection()) {
				deleteChemicalAssociation(assoc);
			}
		}
		comp.setChemicalAssociationCollection(null);

		// delete nanomaterial entities
		if (comp.getNanomaterialEntityCollection() != null) {
			for (NanomaterialEntity entity : comp.getNanomaterialEntityCollection()) {
				deleteNanomaterialEntity(entity);
			}
		}
		// delete functionalizing entities
		if (comp.getFunctionalizingEntityCollection() != null) {
			for (FunctionalizingEntity entity : comp.getFunctionalizingEntityCollection()) {
				deleteFunctionalizingEntity(entity);
			}
		}
		// delete composition files
		if (comp.getFileCollection() != null) {
			for (File file : comp.getFileCollection()) {
				deleteCompositionFile(comp, file);
			}
		}
		try {
			CaNanoLabApplicationService appService = (CaNanoLabApplicationService) ApplicationServiceProvider
					.getApplicationService();
			appService.delete(comp);
		} catch (Exception e) {
			String err = "Problem deleting composition by id: " + comp.getId();
			logger.error(err, e);
			throw new CompositionException(err, e);
		}
	}

	public CompositionServiceHelper getHelper() {
		return compositionServiceHelper;
	}

	public void assignAccesses(ComposingElement composingElement) throws CompositionException, NoAccessException {
		try {

			if (!springSecurityAclService.isOwnerOfObject(
					composingElement.getNanomaterialEntity().getSampleComposition().getSample().getId(),
					SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			// Commented while removing CSM
			/*
			 * springSecurityAclService.saveAccessForChildObject(
			 * composingElement.getNanomaterialEntity().getId(),
			 * SecureClassesEnum.NANO.getClazz(), composingElement.getId(),
			 * SecureClassesEnum.COMPOSINGELEMENT.getClazz());
			 * 
			 * // find sample accesses, already contains owner for composing
			 * element List<AccessibilityBean> sampleAccesses = this
			 * .findSampleAccesses(composingElement
			 * .getNanomaterialEntity().getSampleComposition()
			 * .getSample().getId().toString()); for (AccessibilityBean access :
			 * sampleAccesses) { accessUtils.assignAccessibility(access,
			 * composingElement); }
			 */
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in assigning nanomaterial entity accessibility";
			throw new CompositionException(error, e);
		}
	}

	public void removeAccesses(NanomaterialEntity entity, ComposingElement composingElement)
			throws CompositionException, NoAccessException {
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(
					entity.getSampleComposition().getSample().getId(), SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			// springSecurityAclService.deleteAccessObject(composingElement.getId(),
			// SecureClassesEnum.COMPOSINGELEMENT.getClazz());

			// Commented while removing CSM
			/*
			 * // find sample accesses List<AccessibilityBean> sampleAccesses =
			 * super .findSampleAccesses(entity.getSampleComposition()
			 * .getSample().getId().toString()); for (AccessibilityBean access :
			 * sampleAccesses) { accessUtils .removeAccessibility(access,
			 * composingElement, false); }
			 */
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in assigning nanomaterial entity accessibility";
			throw new CompositionException(error, e);
		}
	}

	public void assignAccesses(Function function) throws CompositionException, NoAccessException {
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(
					function.getFunctionalizingEntity().getSampleComposition().getSample().getId(),
					SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			/*
			 * springSecurityAclService.saveAccessForChildObject(function.
			 * getFunctionalizingEntity().getId(),
			 * SecureClassesEnum.FUNCTIONALIZING.getClazz(), function.getId(),
			 * SecureClassesEnum.FUNCTION.getClazz());
			 */
			// Commented while removing CSM
			/*
			 * // find sample accesses List<AccessibilityBean> sampleAccesses =
			 * this .findSampleAccesses(function.getFunctionalizingEntity()
			 * .getSampleComposition().getSample().getId() .toString()); for
			 * (AccessibilityBean access : sampleAccesses) {
			 * accessUtils.assignAccessibility(access, function); }
			 */
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in assigning functionalizing entity accessibility";
			throw new CompositionException(error, e);
		}
	}

	public void removeAccesses(FunctionalizingEntity entity, Function function)
			throws CompositionException, NoAccessException {
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(
					entity.getSampleComposition().getSample().getId(), SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			// springSecurityAclService.deleteAccessObject(function.getId(),
			// SecureClassesEnum.FUNCTION.getClazz());

			// Commented while removing CSM
			/*
			 * // find sample accesses List<AccessibilityBean> sampleAccesses =
			 * super .findSampleAccesses(entity.getSampleComposition()
			 * .getSample().getId().toString()); for (AccessibilityBean access :
			 * sampleAccesses) { accessUtils.removeAccessibility(access,
			 * function, false); }
			 */
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in assigning functionalizing entity accessibility";
			throw new CompositionException(error, e);
		}
	}

	// TODO: Commented while removing CSM - method may not be used.
	/*
	 * public void assignAccessibility(ChemicalAssociation assoc) throws
	 * CompositionException, NoAccessException { try { if
	 * (!springSecurityAclService.isOwnerOfObject(assoc.getId(),
	 * SecureClassesEnum.CHEMASSOC.getClazz())) { throw new NoAccessException();
	 * } // find sample accesses List<AccessibilityBean> sampleAccesses =
	 * super.findSampleAccesses(assoc.getSampleComposition()
	 * .getSample().getId().toString()); for (AccessibilityBean access :
	 * sampleAccesses) { this.saveAccessibility(access,
	 * assoc.getSampleComposition() .getId().toString());
	 * accessUtils.assignAccessibility(access, assoc); } } catch
	 * (NoAccessException e) { throw e; } catch (Exception e) { String error =
	 * "Error in assigning chemical association accessibility"; throw new
	 * CompositionException(error, e); } }
	 */
//TODO assign and remove don't appear to do anything anymore.  Can be removed?
	public void assignAccesses(SampleComposition comp, File file) throws CompositionException, NoAccessException {
		try {
			if (!springSecurityAclService.isOwnerOfObject(comp.getSample().getId(),
					SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			// TODO check if file is in the comp fileCollection
			// Commented while removing CSM
			/*
			 * // find sample accesses
			 * springSecurityAclService.saveAccessForChildObject(comp.getId(),
			 * SecureClassesEnum.COMPOSITION.getClazz(), file.getId(),
			 * SecureClassesEnum.FILE.getClazz()); List<AccessibilityBean>
			 * sampleAccesses = this
			 * .findSampleAccesses(comp.getSample().getId().toString()); for
			 * (AccessibilityBean access : sampleAccesses) {
			 * this.saveAccessibility(access, file.getId().toString()); }
			 */
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in assigning composition file accessibility";
			throw new CompositionException(error, e);
		}
	}

	public void removeAccesses(NanomaterialEntity entity) throws CompositionException, NoAccessException {
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(
					entity.getSampleComposition().getSample().getId(), SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			// springSecurityAclService.deleteAccessObject(entity.getId(),
			// SecureClassesEnum.NANO.getClazz());

			// Commented while removing CSM
			/*
			 * // find sample accesses List<AccessibilityBean> sampleAccesses =
			 * super .findSampleAccesses(entity.getSampleComposition()
			 * .getSample().getId().toString()); for (AccessibilityBean access :
			 * sampleAccesses) { accessUtils.removeAccessibility(access, entity,
			 * false); }
			 */
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in removing nanomaterial entity accessibility";
			throw new CompositionException(error, e);
		}
	}

	public void removeAccesses(FunctionalizingEntity entity) throws CompositionException, NoAccessException {
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(
					entity.getSampleComposition().getSample().getId(), SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			// springSecurityAclService.deleteAccessObject(entity.getId(),
			// SecureClassesEnum.FUNCTIONALIZING.getClazz());

			// Commented while removing CSM
			/*
			 * // find sample accesses List<AccessibilityBean> sampleAccesses =
			 * super
			 * .findSampleAccesses(entity.getSampleComposition().getSample().
			 * getId().toString()); for (AccessibilityBean access :
			 * sampleAccesses) { accessUtils.removeAccessibility(access, entity,
			 * false); }
			 */
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in removing functionalizing entity accessibility";
			throw new CompositionException(error, e);
		}

	}

	public void removeAccesses(ChemicalAssociation assoc) throws CompositionException, NoAccessException {
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(
					assoc.getSampleComposition().getSample().getId(), SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			// springSecurityAclService.deleteAccessObject(assoc.getId(),
			// SecureClassesEnum.CHEMASSOC.getClazz());

			// Commented while removing CSM
			/*
			 * // find sample accesses List<AccessibilityBean> sampleAccesses =
			 * super .findSampleAccesses(assoc.getSampleComposition()
			 * .getSample().getId().toString()); for (AccessibilityBean access :
			 * sampleAccesses) { accessUtils.removeAccessibility(access, assoc,
			 * false); }
			 */
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in removing chemical association accessibility";
			throw new CompositionException(error, e);
		}
	}

	public void removeAccesses(SampleComposition comp, File file) throws CompositionException, NoAccessException {
		try {
			if (!springSecurityAclService.currentUserHasWritePermission(comp.getSample().getId(),
					SecureClassesEnum.SAMPLE.getClazz())) {
				throw new NoAccessException();
			}
			// springSecurityAclService.deleteAccessObject(file.getId(),
			// SecureClassesEnum.FILE.getClazz());

			// Commented while removing CSM
			/*
			 * // TODO check if file is in the comp fileCollection
			 * 
			 * // find sample accesses List<AccessibilityBean> sampleAccesses =
			 * super .findSampleAccesses(comp.getSample().getId().toString());
			 * for (AccessibilityBean access : sampleAccesses) {
			 * super.deleteAccessibility(access, file.getId().toString()); }
			 */
		} catch (NoAccessException e) {
			throw e;
		} catch (Exception e) {
			String error = "Error in removing composition file accessibility";
			throw new CompositionException(error, e);
		}
	}

	@Override
	public SpringSecurityAclService getSpringSecurityAclService() {
		return springSecurityAclService;
	}

}
