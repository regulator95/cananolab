package gov.nih.nci.cananolab.restful.view.characterization.properties;

import gov.nih.nci.cananolab.domain.characterization.physical.Shape;
import gov.nih.nci.cananolab.dto.particle.characterization.CharacterizationBean;
import gov.nih.nci.cananolab.restful.core.InitSetup;
import gov.nih.nci.cananolab.restful.util.CommonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.annotate.JsonTypeName;

@JsonTypeName("SimpleShape")
public class SimpleShape extends SimpleCharacterizationProperty {
	String type; //says required but empty don't give error
	Float minDimension;
	Float maxDimension;
	Float aspectRatio;
	
	String minDimensionUnit;
	String maxDimensionUnit;
	
	List<String> unitOptions = new ArrayList<String>();
	List<String> shapeTypeOptions = new ArrayList<String>();
	
	@Override
	public void setLookups(HttpServletRequest request) 
	throws Exception {
		SortedSet<String> options = InitSetup.getInstance().getDefaultAndOtherTypesByLookup(request,
				"dimensionUnits", "dimension", "unit", "otherUnit", true);
		
		if (options != null)
			unitOptions.addAll(options);
		CommonUtil.addOtherToList(unitOptions);
		
		options = InitSetup.getInstance().getDefaultAndOtherTypesByLookup(request,
				"shapeTypes", "shape", "type", "otherType", true);
		
		if (options != null)
			shapeTypeOptions.addAll(options);
		//TODO this isn't returning the actual list with "other" added.
		CommonUtil.addOtherToList(shapeTypeOptions);
		
	}
	
	@Override
	public void transferFromPropertyBean(HttpServletRequest request, CharacterizationBean charBean, boolean needOptions)
			throws Exception {
		super.transferFromPropertyBean(request, charBean, needOptions);
		
		Shape shape = charBean.getShape();

		this.type = shape.getType();
		this.aspectRatio = shape.getAspectRatio();
		this.maxDimension = shape.getMaxDimension();
		this.maxDimensionUnit = shape.getMaxDimensionUnit();
		this.minDimension = shape.getMinDimension();
		this.minDimensionUnit = shape.getMinDimensionUnit();
		
		if (needOptions)
			this.setLookups(request);
	}

	@Override
	public void transferToPropertyBean(CharacterizationBean charBean)
			throws Exception {
		
		Shape shape = charBean.getShape();
		shape.setType(this.type);
		shape.setAspectRatio(this.aspectRatio);
		shape.setMaxDimension(this.maxDimension);
		shape.setMaxDimensionUnit(this.maxDimensionUnit);
		shape.setMinDimension(this.minDimension);
		shape.setMinDimensionUnit(this.minDimensionUnit);
	}


	@Override
	public List<String> getPropertyViewTitles() {
		List<String> vals = new ArrayList<String>();
		vals.add("Type");
		vals.add("Aspect Ratio");
		vals.add("Minimum Dimension");
		vals.add("Maximum Dimension");
        return vals;
	}

	@Override
	public List<String> getPropertyViewValues() {
		List<String> vals = new ArrayList<String>();
		vals.add(this.type);
		if (this.aspectRatio != null)
			vals.add(String.valueOf(this.aspectRatio));
		else
			vals.add("");
		
		if (this.minDimension != null)
			vals.add(this.minDimension + "  " + this.minDimensionUnit);
		else
			vals.add("");

		if (this.maxDimension != null)
			vals.add(this.maxDimension + "  " + this.maxDimensionUnit);
		else
			vals.add("");

		return vals;
	}

	public List<String> getUnitOptions() {
		return unitOptions;
	}

	public void setUnitOptions(List<String> unitOptions) {
		this.unitOptions = unitOptions;
	}

	public List<String> getShapeTypeOptions() {
		return shapeTypeOptions;
	}

	public void setShapeTypeOptions(List<String> shapeTypeOptions) {
		this.shapeTypeOptions = shapeTypeOptions;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = (type == null) ? "" : type;
	}

	public Float getMinDimension() {
		return minDimension;
	}

	public void setMinDimension(Float minDimension) {
		this.minDimension = minDimension;
	}

	public Float getMaxDimension() {
		return maxDimension;
	}

	public void setMaxDimension(Float maxDimension) {
		this.maxDimension = maxDimension;
	}

	public Float getAspectRatio() {
		return aspectRatio;
	}

	public void setAspectRatio(Float aspectRatio) {
		this.aspectRatio = aspectRatio;
	}

	public String getMinDimensionUnit() {
		return minDimensionUnit;
	}

	public void setMinDimensionUnit(String minDimensionUnit) {
		this.minDimensionUnit = minDimensionUnit;
	}

	public String getMaxDimensionUnit() {
		return maxDimensionUnit;
	}

	public void setMaxDimensionUnit(String maxDimensionUnit) {
		this.maxDimensionUnit = maxDimensionUnit;
	}
	
	
}
