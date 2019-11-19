/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.dto.particle;

import gov.nih.nci.cananolab.exception.BaseException;
import gov.nih.nci.cananolab.util.ClassUtils;
import gov.nih.nci.cananolab.util.DateUtils;
import gov.nih.nci.cananolab.util.SortableName;
import gov.nih.nci.cananolab.util.StringUtils;

import java.util.SortedSet;
import java.util.TreeSet;

import org.displaytag.decorator.TableDecorator;

/**
 * This decorator is used to for decorate different properties of a sample to be
 * shown properly in the view page using display tag lib.
 *
 * @author pansu
 *
 */
public class SampleDecorator extends TableDecorator {

	public String getDetailURL() {
		SampleBean sample = (SampleBean) getCurrentRowObject();
		String sampleId = sample.getDomain().getId().toString();
		String sampleName = sample.getDomain().getName();
		String dispatch = "summaryView";
		String linkLabel = "View";
		if (sample.getUserUpdatable()) {
			dispatch = "summaryEdit";
			linkLabel = "Edit";
		}
		StringBuilder sb = new StringBuilder("<a href=");
		sb.append("'sample.do?dispatch=").append(dispatch).append(
				"&page=0&sampleId=");
		sb.append(sampleId).append("'");
		sb.append(" title='summary for sample ").append(sampleName).append("'>");
		sb.append(linkLabel).append("</a>");
        return sb.toString();
	}

	public SortableName getSampleName() {
		SampleBean sample = (SampleBean) getCurrentRowObject();
		String sampleName = sample.getDomain().getName();
		return new SortableName(sampleName);
	}

	public String getKeywordStr() {
		SampleBean sample = (SampleBean) getCurrentRowObject();
		String keywordsStr = sample.getKeywordsStr();
		return StringUtils.escapeXmlButPreserveLineBreaks(keywordsStr);
	}

	public String getCompositionStr() throws BaseException {
		SampleBean sample = (SampleBean) getCurrentRowObject();
		SortedSet<String> compEntityNames = new TreeSet<String>();
		if (sample.getFunctionalizingEntityClassNames() != null) {
			for (String name : sample.getFunctionalizingEntityClassNames()) {
				String displayName = ClassUtils.getDisplayName(name);
				if (displayName.length() == 0) {
					compEntityNames.add(name);
				} else {
					compEntityNames.add(displayName);
				}
			}
		}
		if (sample.getNanomaterialEntityClassNames() != null) {
			for (String name : sample.getNanomaterialEntityClassNames()) {
				String displayName = ClassUtils.getDisplayName(name);
				if (displayName.length() == 0) {
					compEntityNames.add(name);
				} else {
					compEntityNames.add(displayName);
				}
			}
		}
		String str = StringUtils.join(compEntityNames, "\r\n");
		return StringUtils.escapeXmlButPreserveLineBreaks(str);
	}

	public String getFunctionStr() throws BaseException {
		SampleBean sample = (SampleBean) getCurrentRowObject();
		SortedSet<String> functionNames = new TreeSet<String>();
		if (sample.getFunctionClassNames() != null) {
			for (String name : sample.getFunctionClassNames()) {
				String displayName = ClassUtils.getDisplayName(name);
				if (displayName.length() == 0) {
					functionNames.add(name);
				} else {
					functionNames.add(displayName);
				}
			}
		}
		String str = StringUtils.join(functionNames, "\r\n");
		return StringUtils.escapeXmlButPreserveLineBreaks(str);
	}

	public String getCharacterizationStr() throws BaseException {
		SampleBean sample = (SampleBean) getCurrentRowObject();
		SortedSet<String> charNames = new TreeSet<String>();
		if (sample.getCharacterizationClassNames() != null) {
			for (String name : sample.getCharacterizationClassNames()) {
				String displayName = ClassUtils.getDisplayName(name);
				charNames.add(displayName);
			}
		}
		String str = StringUtils.join(charNames, "\r\n");
		return StringUtils.escapeXmlButPreserveLineBreaks(str);
	}

	public String getPointOfContactName() throws BaseException {
		SampleBean sample = (SampleBean) getCurrentRowObject();
		return sample.getPrimaryPOCBean().getDisplayName();
	}
}
