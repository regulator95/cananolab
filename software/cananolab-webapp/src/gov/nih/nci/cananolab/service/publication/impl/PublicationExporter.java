/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.service.publication.impl;

import gov.nih.nci.cananolab.domain.common.Author;
import gov.nih.nci.cananolab.domain.common.Publication;
import gov.nih.nci.cananolab.dto.common.PublicationBean;
import gov.nih.nci.cananolab.dto.common.PublicationSummaryViewBean;
import gov.nih.nci.cananolab.util.ExportUtils;
import gov.nih.nci.cananolab.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * Local implementation of PublicationService
 *
 * @author tanq, pansu, houy
 *
 */
public class PublicationExporter {
	/**
	 * Constants for generating Excel report for summary view.
	 */
	public static final String BIBLIOBRAPHY_INFO = "Bibliography Info";
	public static final String RESEARCH_CATEGORY = "Research Category";
	public static final String DESCRIPTION = "Description";
	public static final String PUB_STATUS = "Publication Status";

	private static Logger logger = Logger.getLogger(PublicationExporter.class);

	public static void exportDetail(PublicationBean aPub, OutputStream out)
			throws Exception {
		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = wb.createSheet("detailSheet");
		HSSFPatriarch patriarch = sheet.createDrawingPatriarch();
		int startRow = 0;
		setDetailSheet(aPub, wb, sheet, patriarch, startRow);
		wb.write(out);
		if (out != null) {
			out.flush();
			out.close();
		}
	}

	/**
	 * Export sample publication summary report as Excel spread sheet.
	 *
	 * @param summaryBean
	 * @param out
	 * @throws IOException
	 */
	public static void exportSummary(PublicationSummaryViewBean summaryBean,
			OutputStream out) throws IOException {
		if (out != null) {
			HSSFWorkbook wb = new HSSFWorkbook();
			exportSummarySheet(summaryBean, wb);
			wb.write(out);
			out.flush();
			out.close();
		}
	}

	/**
	 * Output Excel report for sample publication summary report.
	 *
	 * @param summaryBean
	 * @param wb
	 */
	private static void exportSummarySheet(
			PublicationSummaryViewBean summaryBean, HSSFWorkbook wb) {
		HSSFRow row = null;
		HSSFFont headerFont = wb.createFont();
		headerFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		HSSFCellStyle headerStyle = wb.createCellStyle();
		headerStyle.setFont(headerFont);

		Set<String> categories = summaryBean.getPublicationCategories();
		if (categories != null && !categories.isEmpty()) {
			for (String category : categories) {
				int rowIndex = 0;

				// Create one work sheet for each category.
				HSSFSheet sheet = wb.createSheet(category);
				row = sheet.createRow(rowIndex++);

				// Output header of report
				ExportUtils.createCell(row, 0, headerStyle, BIBLIOBRAPHY_INFO);
				ExportUtils.createCell(row, 1, headerStyle, RESEARCH_CATEGORY);
				ExportUtils.createCell(row, 2, headerStyle, DESCRIPTION);
				ExportUtils.createCell(row, 3, headerStyle, PUB_STATUS);

				// Output data of report
				SortedMap<String, List<PublicationBean>> pubs = summaryBean
						.getCategory2Publications();
				if (pubs != null && !pubs.isEmpty()) {
					List<PublicationBean> pubBeans = pubs.get(category);
					if (pubBeans != null && !pubBeans.isEmpty()) {
						for (PublicationBean pubBean : pubBeans) {
							Publication pub = (Publication) pubBean
									.getDomainFile();
							row = sheet.createRow(rowIndex++);

							// Bibliography Info: cell index = 0.
							ExportUtils.createCell(row, 0,
									getBibliographyInfo(pubBean));

							// Research Category: cell index = 1.
							ExportUtils.createCell(row, 1, pub
									.getResearchArea());

							// Description: cell index = 2.
							if (!StringUtils.isEmpty(pub.getDescription())) {
								ExportUtils.createCell(row, 2, pub
										.getDescription());
							}

							// Publication Status: cell index = 3.
							ExportUtils.createCell(row, 3, pub.getStatus());
						}
					}
				}
			}
		}
	}

	/**
	 * Return Bibliography Info String for summary sheet.
	 *
	 * @param pubBean
	 * @return
	 */
	private static String getBibliographyInfo(PublicationBean pubBean) {
		Publication pub = (Publication) pubBean.getDomainFile();
		List<String> strs = new ArrayList<String>();

		// 1.Author name.
		if (!pubBean.getAuthors().isEmpty()) {
			List<String> strList = new ArrayList<String>();
			for (Author author : pubBean.getAuthors()) {
				List<String> authorStrs = new ArrayList<String>(2);
				authorStrs.add(author.getLastName());
				authorStrs.add(author.getInitial());
				strList.add(StringUtils.join(authorStrs, ", "));
			}
			strs.add(StringUtils.join(strList, ", "));
		}
		// 2.Title.
		if (!StringUtils.isEmpty(pub.getTitle())) {
			if (pub.getTitle().endsWith(".")) {
				strs.add(pub.getTitle().substring(0,
						pub.getTitle().length() - 1));
			} else {
				strs.add(pub.getTitle());
			}
		}
		// 3.Journal Name.
		if (!StringUtils.isEmpty(pub.getJournalName())) {
			strs.add(pub.getJournalName());
		}
		// 4.Publish Info Name.
		String publishInfo = "";
		if (pub.getYear() != null) {
			publishInfo += pub.getYear().toString() + "; ";
		}
		if (!StringUtils.isEmpty((pub.getVolume()))) {
			publishInfo += pub.getVolume() + ":";
		}
		if (!StringUtils.isEmpty(pub.getVolume())
				&& !StringUtils.isEmpty(pub.getStartPage())
				&& !StringUtils.isEmpty(pub.getEndPage())) {
			publishInfo += pub.getStartPage() + '-' + pub.getEndPage();
		}
		strs.add(publishInfo);

		// 5.PMID.
		if (pub.getPubMedId() != null && pub.getPubMedId() != 0) {
			strs.add("PMID: " + pub.getPubMedId());
		}
		// 6.DOI.
		if (!StringUtils.isEmpty(pub.getDigitalObjectId())) {
			strs.add("DOI: " + pub.getDigitalObjectId());
		}
		// 7.Publication Name.
		strs.add(pub.getName());

		return StringUtils.join(strs, ". ") + '.';
	}

	private static int setDetailSheet(PublicationBean aPub, HSSFWorkbook wb,
			HSSFSheet sheet, HSSFPatriarch patriarch, int rowIndex) {
		HSSFFont headerFont = wb.createFont();
		headerFont.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		HSSFCellStyle headerStyle = wb.createCellStyle();
		headerStyle.setFont(headerFont);

		Publication publication = (Publication) aPub.getDomainFile();

		HSSFRow row = null;
		HSSFCell cell = null;
		// PubMedID
		Long pubMedId = publication.getPubMedId();
		row = sheet.createRow(rowIndex++);
		int cellIndex = 0;
		cell = row.createCell(cellIndex++);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(new HSSFRichTextString("Publication Identifier"));
		if (pubMedId != null && pubMedId.intValue() > 0) {
			row.createCell(cellIndex++).setCellValue(
					new HSSFRichTextString(pubMedId.toString()));
		} else {
			String oid = publication.getDigitalObjectId();
			if (!StringUtils.isEmpty(oid)) {
				row.createCell(cellIndex++).setCellValue(
						new HSSFRichTextString(oid));
			} else {
				// row.createCell(cellIndex++).setCellValue(
				// new HSSFRichTextString(""));
			}
		}
		// publication type
		row = sheet.createRow(rowIndex++);
		cellIndex = 0;
		cell = row.createCell(cellIndex++);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(new HSSFRichTextString("Publication Type"));
		row.createCell(cellIndex++).setCellValue(
				new HSSFRichTextString(publication.getCategory()));

		// publication status
		row = sheet.createRow(rowIndex++);
		cellIndex = 0;
		cell = row.createCell(cellIndex++);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(new HSSFRichTextString(PUB_STATUS));
		row.createCell(cellIndex++).setCellValue(
				new HSSFRichTextString(publication.getStatus()));

		// Authors
		String rowHeader = "Authors";
		StringBuffer sb = new StringBuffer();
		if (publication.getAuthorCollection() != null) {
			List<Author> authorslist = new ArrayList<Author>(publication
					.getAuthorCollection());
			Collections.sort(authorslist, new Comparator<Author>() {
				public int compare(Author o1, Author o2) {
					return (int) (o1.getCreatedDate().compareTo(o2
							.getCreatedDate()));
				}
			});
			for (Author author : authorslist) {
				sb.append(author.getFirstName());
				sb.append(' ');
				sb.append(author.getInitial());
				sb.append(' ');
				sb.append(author.getLastName());

				row = sheet.createRow(rowIndex++);
				cellIndex = 0;
				cell = row.createCell(cellIndex++);
				cell.setCellStyle(headerStyle);
				cell.setCellValue(new HSSFRichTextString(rowHeader));
				row.createCell(cellIndex++).setCellValue(
						new HSSFRichTextString(sb.toString()));
				rowHeader = "";
				sb.setLength(0);
			}
		}

		// research area
		row = sheet.createRow(rowIndex++);
		cellIndex = 0;
		cell = row.createCell(cellIndex++);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(new HSSFRichTextString(RESEARCH_CATEGORY));
		row.createCell(cellIndex++).setCellValue(
				new HSSFRichTextString(publication.getResearchArea()));

		// Title
		row = sheet.createRow(rowIndex++);
		cellIndex = 0;
		cell = row.createCell(cellIndex++);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(new HSSFRichTextString("Title"));
		row.createCell(cellIndex++).setCellValue(
				new HSSFRichTextString(publication.getTitle()));

		// Journal
		row = sheet.createRow(rowIndex++);
		cellIndex = 0;
		cell = row.createCell(cellIndex++);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(new HSSFRichTextString("Journal"));
		row.createCell(cellIndex++).setCellValue(
				new HSSFRichTextString(publication.getJournalName()));

		// Year
		row = sheet.createRow(rowIndex++);
		cellIndex = 0;
		cell = row.createCell(cellIndex++);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(new HSSFRichTextString("Year"));
		int year = 0;
		if (publication.getYear() != null)
			year = publication.getYear();
		if (year > 0) {
			row.createCell(cellIndex++).setCellValue(
					new HSSFRichTextString(Integer.toString(year)));
		}

		// Volume
		row = sheet.createRow(rowIndex++);
		cellIndex = 0;
		cell = row.createCell(cellIndex++);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(new HSSFRichTextString("Volume"));
		row.createCell(cellIndex++).setCellValue(
				new HSSFRichTextString(publication.getVolume()));

		// Pages
		row = sheet.createRow(rowIndex++);
		cellIndex = 0;
		cell = row.createCell(cellIndex++);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(new HSSFRichTextString("Pages"));
		String startPage = publication.getStartPage();
		String endPage = publication.getEndPage();
		if ((!StringUtils.isEmpty(startPage))
				|| (!StringUtils.isEmpty(endPage))) {
			row.createCell(cellIndex++).setCellValue(
					new HSSFRichTextString(publication.getJournalName()));
		}

		// Description
		row = sheet.createRow(rowIndex++);
		cellIndex = 0;
		cell = row.createCell(cellIndex++);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(new HSSFRichTextString(DESCRIPTION));
		row.createCell(cellIndex++).setCellValue(
				new HSSFRichTextString(publication.getDescription()));

		// Uploaded Publication URI
		row = sheet.createRow(rowIndex++);
		cellIndex = 0;
		cell = row.createCell(cellIndex++);
		cell.setCellStyle(headerStyle);
		cell.setCellValue(new HSSFRichTextString("Publication URI"));
		row.createCell(cellIndex++).setCellValue(
				new HSSFRichTextString(publication.getUri()));

		return rowIndex;
	}
}
