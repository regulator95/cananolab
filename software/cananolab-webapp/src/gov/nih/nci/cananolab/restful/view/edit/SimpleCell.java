package gov.nih.nci.cananolab.restful.view.edit;

import gov.nih.nci.cananolab.dto.common.table.TableCell;

import java.util.Date;

public class SimpleCell {
	String value;
	String datumOrCondition;
	//private Datum datum = new Datum();
	//private Condition condition = new Condition();
	Integer columnOrder;
	Date createdDate;
	String operand;
	
	public void transferFromTableCell(TableCell cell) {
		if (cell == null) return;
		value = cell.getValue();
		datumOrCondition = cell.getDatumOrCondition();
		columnOrder = cell.getColumnOrder();
		operand = cell.getOperand();
	}
	
	public void transferToTableCell(TableCell cell) {
		cell.setColumnOrder(columnOrder);
		cell.setDatumOrCondition(datumOrCondition);
		cell.setValue(value);
		cell.setOperand(operand);
	}
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getDatumOrCondition() {
		return datumOrCondition;
	}
	public void setDatumOrCondition(String datumOrCondition) {
		this.datumOrCondition = datumOrCondition;
	}
	public Integer getColumnOrder() {
		return columnOrder;
	}
	public void setColumnOrder(Integer columnOrder) {
		this.columnOrder = columnOrder;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	
	public String getOperand() {
		return operand;
	}
	
	public void setOperand(String operand){
		this.operand = operand;
	}
}
