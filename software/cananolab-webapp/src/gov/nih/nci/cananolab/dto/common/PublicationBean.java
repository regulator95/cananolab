/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

/**
 *
 */
package gov.nih.nci.cananolab.dto.common;

import gov.nih.nci.cananolab.domain.common.Author;
import gov.nih.nci.cananolab.domain.common.Publication;
import gov.nih.nci.cananolab.util.Constants;
import gov.nih.nci.cananolab.util.DateUtils;
import gov.nih.nci.cananolab.util.StringUtils;

import java.util.*;

/**
 * Publication view bean
 * 
 * @author tanq, pansu
 * 
 */
public class PublicationBean extends FileBean {
	private static final String delimiter = ";";

	private String[] sampleNames = new String[0];
	private String[] researchAreas;
	private List<Author> authors = new ArrayList<Author>();
	private Author theAuthor = new Author();
	private String sampleNamesStr;
	private Boolean fromSamplePage = false;

	private String displayName = "";

    public PublicationBean() {
		domainFile = new Publication();
		domainFile.setUriExternal(false);
	}

	public PublicationBean(String id) {
		domainFile.setId(new Long(id));
	}

	public PublicationBean(Publication publication) {
		super(publication);
		this.domainFile = publication;
		this.createdBy = publication.getCreatedBy();

		String researchAreasStr = publication.getResearchArea();
		if (!StringUtils.isEmpty(researchAreasStr)) {
			researchAreas = researchAreasStr.split(delimiter);
		} else {
			researchAreas = null;
		}
		Collection<Author> authorCollection = publication.getAuthorCollection();
		if (authorCollection != null && authorCollection.size() > 0) {
			List<Author> authorslist = new ArrayList<Author>(authorCollection);
			Collections.sort(authorslist, new Comparator<Author>() {
				public int compare(Author o1, Author o2) {
					return (int) (o1.getCreatedDate().compareTo(o2
							.getCreatedDate()));
				}
			});
			authors = authorslist;
		}
	}

	public PublicationBean(Publication publication, String[] sampleNames) {
		this(publication);
		this.sampleNames = sampleNames;
		sampleNamesStr = StringUtils.join(sampleNames, "\r\n");
	}

	/**
	 * Copy PubMed data from source PublicationBean to this PublicationBean.
	 * 
	 * @param source
     */
	public void copyPubMedFieldsFromPubMedXML(PublicationBean source) {
		Publication oldPub = (Publication) this.getDomainFile();
		Publication xmlPub = (Publication) source.getDomainFile();

		oldPub.setPubMedId(xmlPub.getPubMedId());
		oldPub.setDescription(xmlPub.getDescription());
		oldPub.setDigitalObjectId(xmlPub.getDigitalObjectId());
		oldPub.setTitle(xmlPub.getTitle());
		oldPub.setJournalName(xmlPub.getJournalName());
		oldPub.setStartPage(xmlPub.getStartPage());
		oldPub.setEndPage(xmlPub.getEndPage());
		oldPub.setVolume(xmlPub.getVolume());
		oldPub.setYear(xmlPub.getYear());
		oldPub.setKeywordCollection(xmlPub.getKeywordCollection());
		this.setAuthors(source.getAuthors());
	}

	public void copyNonPubMedFieldsFromDatabase(PublicationBean source) {
		Publication oldPub = (Publication) this.getDomainFile();
		Publication dbPub = (Publication) source.getDomainFile();
		oldPub.setId(dbPub.getId());
		oldPub.setCreatedBy(dbPub.getCreatedBy());
		oldPub.setCreatedDate(dbPub.getCreatedDate());
		oldPub.setCategory(dbPub.getCategory());
		oldPub.setDescription(dbPub.getDescription());
		oldPub.setKeywordCollection(dbPub.getKeywordCollection());
		oldPub.setResearchArea(dbPub.getResearchArea());
		oldPub.setStatus(dbPub.getStatus());
		oldPub.setType(dbPub.getType());
		this.setSampleNamesStr(source.getSampleNamesStr());
		this.setSampleNames(source.getSampleNames());
		this.setUserAccesses(source.getUserAccesses());
		this.setGroupAccesses(source.getGroupAccesses());
	}

	public void copyFromDatabase(PublicationBean source) {
		Publication oldPub = (Publication) this.getDomainFile();
		Publication dbPub = (Publication) source.getDomainFile();

		oldPub.setPubMedId(dbPub.getPubMedId());
		oldPub.setDescription(dbPub.getDescription());
		oldPub.setDigitalObjectId(dbPub.getDigitalObjectId());
		oldPub.setTitle(dbPub.getTitle());
		oldPub.setJournalName(dbPub.getJournalName());
		oldPub.setStartPage(dbPub.getStartPage());
		oldPub.setEndPage(dbPub.getEndPage());
		oldPub.setVolume(dbPub.getVolume());
		oldPub.setYear(dbPub.getYear());
		oldPub.setKeywordCollection(dbPub.getKeywordCollection());
		this.setAuthors(source.getAuthors());
		oldPub.setId(dbPub.getId());
		oldPub.setCreatedBy(dbPub.getCreatedBy());
		oldPub.setCreatedDate(dbPub.getCreatedDate());
		oldPub.setCategory(dbPub.getCategory());
		oldPub.setResearchArea(dbPub.getResearchArea());
		oldPub.setStatus(dbPub.getStatus());
		oldPub.setType(dbPub.getType());
		this.setSampleNamesStr(source.getSampleNamesStr());
		this.setSampleNames(source.getSampleNames());
		this.setUserAccesses(source.getUserAccesses());
		this.setGroupAccesses(source.getGroupAccesses());
		oldPub.setUri(dbPub.getUri());
		oldPub.setUriExternal(dbPub.getUriExternal());
	}

	public boolean equals(Object obj) {
		boolean eq = false;
		if (obj instanceof PublicationBean) {
			PublicationBean c = (PublicationBean) obj;
			Long thisId = this.domainFile.getId();
			if (thisId != null && thisId.equals(c.getDomainFile().getId())) {
				eq = true;
			}
		}
		return eq;
	}

	public String[] getSampleNames() {
		if (sampleNamesStr != null) {
			sampleNames = sampleNamesStr.split("\r\n");
		}
		if (sampleNames.length == 1 && StringUtils.isEmpty(sampleNames[0])) {
			sampleNames = new String[0];
		}
		return sampleNames;
	}

	public void setSampleNames(String[] sampleNames) {
		this.sampleNames = sampleNames;
		this.sampleNamesStr = StringUtils.join(sampleNames, "\r\n");
	}

	/**
	 * @return the researchAreas
	 */
	public String[] getResearchAreas() {
		String researchAreasStr = ((Publication) getDomainFile())
				.getResearchArea();
		if (!StringUtils.isEmpty(researchAreasStr)) {
			researchAreas = researchAreasStr.split(delimiter);
		} else {
			researchAreas = null;
		}
		if (!StringUtils.isEmpty(researchAreasStr)) {
			researchAreas = researchAreasStr.split(delimiter);
		} else {
			researchAreas = null;
		}
		return researchAreas;
	}

	/**
	 * @param researchAreas
	 *            the researchAreas to set
	 */
	public void setResearchAreas(String[] researchAreas) {
		this.researchAreas = researchAreas;
	}

	/**
	 * @return the authors
	 */
	public List<Author> getAuthors() {
		return authors;
	}

	/**
	 * @param authors
	 *            the authors to set
	 */
	public void setAuthors(List<Author> authors) {
		this.authors = authors;
	}

	// /**
	// * @return the authorsStr
	// */
	// public String getAuthorsStr() {
	// return authorsStr;
	// }
	//
	// /**
	// * @param authorsStr the authorsStr to set
	// */
	// public void setAuthorsStr(String authorsStr) {
	// this.authorsStr = authorsStr;
	// }

	public void addAuthor() {
		authors.add(new Author());
	}

	public void removeAuthor(int ind) {
		authors.remove(ind);
	}

	public String getAuthorsDisplayName() {
		List<String> strs = new ArrayList<String>();
		for (Author author : authors) {
			List<String> authorStrs = new ArrayList<String>();
			authorStrs.add(author.getLastName());
			authorStrs.add(author.getInitial());
			strs.add(StringUtils.join(authorStrs, ", "));
		}
		return StringUtils.join(strs, ", ");
	}

	private String getPublishInfoDisplayName() {
		Publication pub = (Publication) domainFile;
		String publishInfo = "";
		if (pub.getYear() != null) {
			publishInfo += pub.getYear().toString() + "; ";
		}
		if (!StringUtils.isEmpty((pub.getVolume()))) {
			publishInfo += pub.getVolume();
		}
		if (!StringUtils.isEmpty(pub.getVolume())
				&& !StringUtils.isEmpty(pub.getStartPage())
				&& !StringUtils.isEmpty(pub.getEndPage())) {
			publishInfo += ":" + pub.getStartPage() + "-" + pub.getEndPage();
		}
		return publishInfo;
	}

	public String getPubMedDisplayName() {
		Publication pub = (Publication) domainFile;
		if (pub.getPubMedId() != null) {
			StringBuilder sb = new StringBuilder("<a target='_abstract' href=");
			sb.append(Constants.PUBMED_PREFIX);
			sb.append(pub.getPubMedId());
			sb.append(">");
			sb.append("PMID: " + pub.getPubMedId());
			sb.append("</a>");
			return sb.toString();
		} else {
			return null;
		}
	}

	private String getDOIDisplayName() {
		Publication pub = (Publication) domainFile;
		if (!StringUtils.isEmpty(pub.getDigitalObjectId())) {
			StringBuilder sb = new StringBuilder("<a target='_abstract' href=");
			sb.append(Constants.DOI_PREFIX);
			sb.append(pub.getDigitalObjectId());
			sb.append(">");
			sb.append("DOI: " + pub.getDigitalObjectId());
			sb.append("</a>");
			sb.append(Constants.EXTERNAL_SITE_DISCLAIMER_LINK);
			return sb.toString();
		} else {
			return null;
		}
	}

	private String getUriDisplayName() {
		Publication pub = (Publication) domainFile;
		String link = "rest/publication/download?fileId=" + pub.getId();
	//	String link = "publication.do?dispatch=download&fileId=" + pub.getId();
		if (!StringUtils.isEmpty(pub.getUri())) {
			if (pub.getUriExternal()) {
				link = pub.getUri();
			}
			StringBuilder sb = new StringBuilder("<a href=");
			sb.append(link);
			sb.append(" target='");
			sb.append(getUrlTarget());
			sb.append("'>");
			// sb.append(pub.getName()); some times the name is null or too long
			sb.append("View");
			sb.append("</a>");
			// if Uri is external
			if (pub.getUriExternal()) {
				sb.append(Constants.EXTERNAL_SITE_DISCLAIMER_LINK);
			}
			return sb.toString();
		} else {
			return null;
		}
	}

	public String getDisplayName() {
		// standard PubMed journal citation format
		// e.g. Freedman SB, Adler M, Seshadri R, Powell EC. Oral ondansetron
		// for gastroenteritis in a pediatric emergency department. N Engl J
		// Med. 2006 Apr 20;354(16):1698-705. PubMed PMID: 12140307.
		Publication pub = (Publication) domainFile;
		List<String> strs = new ArrayList<String>();
		strs.add(getAuthorsDisplayName());
		// remove last . in the title
		if ((pub.getTitle() != null) && (pub.getTitle().endsWith("."))) {
			strs.add(pub.getTitle().substring(0, pub.getTitle().length() - 1));
		} else {
			strs.add(pub.getTitle());
		}
		strs.add(pub.getJournalName());
		strs.add(getPublishInfoDisplayName());
		strs.add(getPubMedDisplayName());

		if (pub.getPubMedId() == null) {
			strs.add(getDOIDisplayName());
		}
		if (pub.getPubMedId() == null
				&& StringUtils.isEmpty(pub.getDigitalObjectId())) {
			strs.add(getUriDisplayName());
		}

		displayName = StringUtils.join(strs, ". ") + ".";

		return displayName;
	}

	public void setupDomain(String internalUriPath, String createdBy)
			throws Exception {
		super.setupDomainFile(internalUriPath, createdBy);
		Publication domain = (Publication) domainFile;
		if (domain.getPubMedId() != null && domain.getPubMedId() == 0) {
			domain.setPubMedId(null);
		}
		if (domain.getYear() != null && domain.getYear() == 0) {
			domain.setYear(null);
		}
		if (StringUtils.isEmpty(domain.getDigitalObjectId())) {
			domain.setDigitalObjectId(null);
		}
		if (researchAreas != null && researchAreas.length > 0) {
			String researchAreasStr = StringUtils.join(researchAreas, ";");
			domain.setResearchArea(researchAreasStr);
		}

		if (domain.getAuthorCollection() != null
				&& !domain.getAuthorCollection().isEmpty()) {
			domain.getAuthorCollection().clear();
		} else {
			domain.setAuthorCollection(new HashSet<Author>());
		}
		for (int i = 0; i < authors.size(); i++) {
			Author author = authors.get(i);
			if (!StringUtils.isEmpty(author.getFirstName())
					|| !StringUtils.isEmpty(author.getLastName())
					|| !StringUtils.isEmpty(author.getInitial())) {
				if (author.getCreatedDate() == null) {
					author.setCreatedDate(DateUtils.addSecondsToCurrentDate(i));
				}
				if (author.getCreatedBy() == null
						|| author.getCreatedBy().trim().length() == 0) {
					author.setCreatedBy(createdBy);
				}
				if (author.getId() != null && author.getId() <= 0) {
					author.setId(null);
				}
			} else {
				author = null;
			}
			if (author != null) {
				domain.getAuthorCollection().add(author);
			}
		}
	}

	public Author getTheAuthor() {
		return theAuthor;
	}

	public void setTheAuthor(Author theAuthor) {
		this.theAuthor = theAuthor;
	}

	public void addAuthor(Author author) {
		// if an old one exists, remove it first
		int index = authors.indexOf(author);
		if (index != -1) {
			authors.remove(author);
			// retain the original order
			authors.add(index, author);
		} else {
			authors.add(author);
		}
	}

	public void removeAuthor(Author author) {
		authors.remove(author);
	}

	//TODO remove?
	public void resetDomainCopy(String createdBy, Publication copy) {
		// don't need to reset anything because publications can be shared
	}

	public String getSampleNamesStr() {
		return sampleNamesStr;
	}

	public void setSampleNamesStr(String sampleNamesStr) {
		this.sampleNamesStr = sampleNamesStr;
	}

	public Boolean getFromSamplePage() {
		return fromSamplePage;
	}

	public void setFromSamplePage(Boolean fromSamplePage) {
		this.fromSamplePage = fromSamplePage;
	}

    @Override
    public String toString()
    {
        return "{\"PublicationBean\":"
                + super.toString()
                + ",                         \"sampleNames\":" + Arrays.toString( sampleNames )
                + ",                         \"researchAreas\":" + Arrays.toString( researchAreas )
                + ",                         \"authors\":" + authors
                + ",                         \"theAuthor\":" + theAuthor
                + ",                         \"sampleNamesStr\":\"" + sampleNamesStr + "\""
                + ",                         \"fromSamplePage\":\"" + fromSamplePage + "\""
                + ",                         \"displayName\":\"" + displayName + "\""
                + ",                         \"domainFile\":" + domainFile
                + ",                         \"createdBy\":\"" + createdBy + "\""
                + "}";
    }
}
