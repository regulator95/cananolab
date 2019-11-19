package gov.nih.nci.cananolab.restful.view;

import gov.nih.nci.cananolab.dto.common.LinkableItem;
import gov.nih.nci.cananolab.dto.particle.AdvancedSampleBean;
import gov.nih.nci.cananolab.dto.particle.AdvancedSampleSearchBean;
import gov.nih.nci.cananolab.restful.bean.LabelValueBean;
import gov.nih.nci.cananolab.restful.bean.SimpleAdvancedResultCellBean;
import gov.nih.nci.cananolab.restful.bean.SimpleAdvancedResultCellUnitBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonIgnore;

public class SimpleAdvancedSearchSampleBean extends SimpleSearchSampleBean {
	private Logger logger = Logger.getLogger(SimpleAdvancedSearchSampleBean.class);

	@JsonIgnore
	List<SimpleAdvancedResultCellBean> columns = new ArrayList<SimpleAdvancedResultCellBean>();
	
//	List<Map<String, Object>> rowCells = new ArrayList<Map<String, Object>>();
//
//	Map<String, List<SimpleAdvancedResultCellUnitBean>> rowCellRefs = new HashMap<String, List<SimpleAdvancedResultCellUnitBean>>();
	
	@JsonIgnore
	SimpleAdvancedResultRow dataRow = new SimpleAdvancedResultRow();
	
	public List<SimpleAdvancedResultCellBean> getColumns() {
		return columns;
	}

	public void setColumns(List<SimpleAdvancedResultCellBean> columns) {
		this.columns = columns;
	}

	public void transferAdvancedSampleBeanForResultView(AdvancedSampleBean sampleBean, AdvancedSampleSearchBean searchBean, List<LabelValueBean> colNames) {

		if (sampleBean == null)
			return;
		
		setSampleId(sampleBean.getDomainSample().getId());
		setSampleName(sampleBean.getDomainSample().getName());
		if(sampleBean.getNanomaterialEntities().size() > 0){
			for(int i =0; i < sampleBean.getNanomaterialEntities().size(); i++){
				if(sampleBean.getNanomaterialEntities().get(i).getDescription()!=null)
					setNanoEntityDesc(sampleBean.getNanomaterialEntities().get(i).getDescription());
			}
		}
		Map<String, List<LinkableItem>> dataMap = sampleBean.getAttributeMap();
		
		for (LabelValueBean col : colNames) {
			Map<String, Object> data = new HashMap<String, Object>();
			String colName = col.getLabel();
			
			if (colName.contains("Sample")) {			
				this.dataRow.setSampleId(this.sampleId);
				this.dataRow.setSampleName(this.sampleName);
				this.dataRow.setNanoEntityDesc(this.getNanoEntityDesc());
			} else {
				List<LinkableItem> items = dataMap.get(colName);
				populateColumnContent(colName, items);
			}
			
		}
	}
	
	public static String getHtmlFieldType(String colName) {
		//map to the type value the html is using.
		if (colName.contains("point of contact"))
			return "pointOfContact";
		else if (colName.contains("nanomaterial entity"))
			return "nanomaterialEntity";
		else if (colName.contains("functionalizing entity"))
			return "functionalizaingEntity";
		else if (colName.equals("function"))
			return "function";
		else if (colName.contains("physico"))
			return "physicalChemicalChar";
		else if (colName.contains("in vivo"))
			return "invivoChar";
		else if (colName.contains("in vitro"))
			return "invitroChar";
		else if (colName.contains("ex vivo"))
			return "exvivoChar";
		
		return "";
	}
	
	protected void populateColumnContent(String colName, List<LinkableItem> items/*, List<Map<String, Object>> displayableData*/) {
		
		List<SimpleAdvancedResultCellUnitBean> units = transferFromLinkableItems(items, colName);
		
		String type = getHtmlFieldType(colName);
		
		List<String> formattedDisplayables = new ArrayList<String>();
		for (SimpleAdvancedResultCellUnitBean unit :units) {
			String formatted = unit.getDisplayName() + "|" + unit.getDataId();
			
			if (type.equals("function")) {
				String relatedType = unit.getRelatedEntityType();
				if (relatedType != null && relatedType.length() > 0)
					formatted += "|" + relatedType;
				else
					continue;
			}
			
			formattedDisplayables.add(formatted);
		}
		
		try { //set data fields by reflection
			PropertyUtils.setProperty(this.dataRow, type, formattedDisplayables);
		} catch (NoSuchMethodException nse) {
			logger.error("Got NoSuchMethodException while setting adv. search result data field: " + nse.getMessage());
		} catch (Exception e) {
			logger.error("Got exception while setting adv. search result data field: " + e.getMessage());
		}

	}
	
	/**
	 * 
	 * @param items
	 * @param colName
	 * @return
	 */
	protected List<SimpleAdvancedResultCellUnitBean> transferFromLinkableItems(List<LinkableItem> items, 
			String colName) {
		
		List<SimpleAdvancedResultCellUnitBean> units = new ArrayList<SimpleAdvancedResultCellUnitBean>();
		//List<String> displayables = new ArrayList<String>();
		Map<String, Object> displayMap = new HashMap<String, Object>();
		
		for (LinkableItem item :items) {
			String dn = item.getDisplayName();
			List<String> dnStrs = item.getDisplayStrings();
			String dataId = item.getAction();
			
			logger.debug(dn + "||" + dataId);
			
			SimpleAdvancedResultCellUnitBean unit = new SimpleAdvancedResultCellUnitBean();
			
			StringBuilder names = new StringBuilder();
			for (String dnStr : dnStrs) {
				if (names.length() > 0)
					names.append("<br>");
				names.append(dnStr);
			}
			
			unit.setDisplayName(names.toString());
			//displayables.add(names);
			
			if (dataId != null) {
				String [] relatedItems = dataId.split(":");
				if (relatedItems.length == 2) { //a hack to handle Function data
					unit.setDataId(Long.parseLong(relatedItems[1]));
					unit.setRelatedEntityType(relatedItems[0]);
				} else
					unit.setDataId(Long.parseLong(dataId));
			}
			
			units.add(unit);
			
		}
		
		//displayMap.put(colName, displayables);
		//dataMap.add(displayMap);
		
		return units;
	}

//	public List<Map<String, Object>> getRowCells() {
//		return rowCells;
//	}
//
//	public void setRowCells(List<Map<String, Object>> rowCells) {
//		this.rowCells = rowCells;
//	}
//
//	public Map<String, List<SimpleAdvancedResultCellUnitBean>> getRowCellRefs() {
//		return rowCellRefs;
//	}
//
//	public void setRowCellRefs(
//			Map<String, List<SimpleAdvancedResultCellUnitBean>> rowCellRefs) {
//		this.rowCellRefs = rowCellRefs;
//	}

	public SimpleAdvancedResultRow getDataRow() {
		return dataRow;
	}

	public void setDataRow(SimpleAdvancedResultRow dataRow) {
		this.dataRow = dataRow;
	}

	
}
