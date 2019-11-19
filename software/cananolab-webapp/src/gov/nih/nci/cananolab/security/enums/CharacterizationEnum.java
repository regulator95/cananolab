package gov.nih.nci.cananolab.security.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum CharacterizationEnum
{
	OTHER(Collections.singletonList("OtherCharacterization")),
	INVITRO(Arrays.asList("MetabolicStability", "OxidativeStress", "EnzymeInduction", "Sterility", "ImmuneCellFunction", "Cytotoxicity", "Targeting", "BloodContact", "Transfection")),
	INVIVO(Arrays.asList("Pharmacokinetics", "Toxicology")),
	PHYSICO(Arrays.asList("Purity", "Size", "Shape", "PhysicalState", "Solubility", "Relaxivity", "MolecularWeight", "Surface"));
	
	private List<String> subChars;
	
	CharacterizationEnum(List<String> subChars) {
		this.subChars = subChars;
	}

	public List<String> getSubChars() {
		return subChars;
	}

	public void setSubChars(List<String> subChars) {
		this.subChars = subChars;
	}

}
