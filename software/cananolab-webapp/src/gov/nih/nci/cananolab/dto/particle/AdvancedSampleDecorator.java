/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.dto.particle;

import gov.nih.nci.cananolab.util.SortableName;

import org.displaytag.decorator.TableDecorator;

/**
 * This decorator is used to for decorate different properties of a sample
 * resulted from advanced search to be shown properly in the view page using
 * display tag lib.
 *
 * @author pansu
 *
 */
public class AdvancedSampleDecorator extends TableDecorator {

	public SortableName getEditSampleURL() {
		AdvancedSampleBean sample = (AdvancedSampleBean) getCurrentRowObject();
		String sampleName = sample.getDomainSample().getName();
		StringBuilder sb = new StringBuilder("<a href=");
		sb.append("sample.do?dispatch=summaryEdit&page=0&sampleId=");
		sb.append(sample.getSampleId());
		sb.append("&from=advanced").append('>');
		sb.append(sampleName).append("</a>");
		String link = sb.toString();

        return new SortableName(sampleName, link);
	}

	public SortableName getViewSampleURL() {
		AdvancedSampleBean sample = (AdvancedSampleBean) getCurrentRowObject();
		String sampleName = sample.getDomainSample().getName();
		StringBuilder sb = new StringBuilder("<a href=");
		sb.append("sample.do?dispatch=summaryView&page=0&sampleId=");
		sb.append(sample.getSampleId());
		sb.append("&from=advanced").append('>');
		sb.append(sampleName).append("</a>");
		String link = sb.toString();

        return new SortableName(sampleName, link);
	}
}
