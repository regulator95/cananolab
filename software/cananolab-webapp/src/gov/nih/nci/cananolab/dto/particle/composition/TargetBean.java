/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.dto.particle.composition;

import gov.nih.nci.cananolab.domain.function.Antigen;
import gov.nih.nci.cananolab.domain.function.OtherTarget;
import gov.nih.nci.cananolab.domain.function.Target;
import gov.nih.nci.cananolab.util.ClassUtils;
import gov.nih.nci.cananolab.util.Constants;
import gov.nih.nci.cananolab.util.DateUtils;
import gov.nih.nci.cananolab.util.StringUtils;

/**
 * Represents the view bean for the Target domain object
 *
 * @author pansu
 *
 */
public class TargetBean {
	private String id;

	private String type;

	private String name;

	private String description;

	private Antigen antigen = new Antigen();

	private String className;

	private Target domainTarget;

	public TargetBean() {
	}

	public TargetBean(Target target) {
		//when the target is copied, its id is intentionally set to nul
		if (target.getId() != null) {
			id = target.getId().toString();
		}
		domainTarget = target;
		name = target.getName();
		description = target.getDescription();
		className = ClassUtils.getShortClassName(target.getClass()
				.getCanonicalName());
		if (target instanceof Antigen) {
			antigen = (Antigen) target;
		}
		updateType();
	}

	public Antigen getAntigen() {
		return antigen;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public String getClassName() {
		return className;
	}

	public Target getDomainTarget() {
		return domainTarget;
	}

	public void setupDomainTarget(String createdBy, int index) throws Exception {
		className = ClassUtils.getShortClassNameFromDisplayName(type);
		Class clazz = ClassUtils.getFullClass(className);
		if (clazz == null) {
			clazz = OtherTarget.class;
		}
		if (domainTarget == null
				|| domainTarget != null
				&& !clazz.getCanonicalName().equals(
						domainTarget.getClass().getCanonicalName())) {
			try {
				domainTarget = (Target) clazz.newInstance();
			} catch (ClassCastException ex) {
				String tmpType = type;
				this.setType(null);
				throw new ClassCastException(tmpType);
			}
		}
		if (domainTarget instanceof OtherTarget) {
			((OtherTarget) domainTarget).setType(type);
		} else if (domainTarget instanceof Antigen) {
			domainTarget = antigen;
		}
		domainTarget.setName(name);
		domainTarget.setDescription(description);
		//updated created_date and created_by if id is null
		if (domainTarget.getId() == null) {
			domainTarget.setCreatedBy(createdBy);
			// domainTarget.setCreatedDate(new Date());
			// fix for MySQL database, which supports precision only up to
			// seconds
			domainTarget.setCreatedDate(DateUtils
					.addSecondsToCurrentDate(index));
		}
		//updated created_by if created_by contains copy, but keep the original created_date
		if (domainTarget.getId() != null
				|| !StringUtils.isEmpty(domainTarget.getCreatedBy())
				&& domainTarget.getCreatedBy().contains(
						Constants.AUTO_COPY_ANNOTATION_PREFIX)) {
			domainTarget.setCreatedBy(createdBy);
		}
	}

	private void updateType() {
		if (domainTarget instanceof OtherTarget) {
			type = ((OtherTarget) domainTarget).getType();
		} else {
			type = ClassUtils.getDisplayName(className);
		}
	}

	public void setAntigen(Antigen antigen) {
		this.antigen = antigen;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDisplayName() {
		StringBuffer buffer = new StringBuffer();
		if (!StringUtils.isEmpty(type)) {
			buffer.append(type);
			if (!StringUtils.isEmpty(name)) {
				buffer.append(" " + name);
			}
			if (!StringUtils.isEmpty(description)) {
				buffer.append(": ");
				buffer.append(description);
			}
			if (!StringUtils.isEmpty(antigen.getSpecies())) {
				buffer.append(", species: ");
				buffer.append(antigen.getSpecies());
			}
		}
		return buffer.toString();
	}

	public boolean equals(Object obj) {
		boolean eq = false;
		if (obj instanceof TargetBean) {
			TargetBean t = (TargetBean) obj;
			String thisId = getId();
			if (thisId != null && thisId.equals(t.getId())) {
				eq = true;
			}
		}
		return eq;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
