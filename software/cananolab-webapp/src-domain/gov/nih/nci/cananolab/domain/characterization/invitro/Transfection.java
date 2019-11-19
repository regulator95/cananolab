package gov.nih.nci.cananolab.domain.characterization.invitro;


import java.io.Serializable;
/**
	* 
	**/

public class Transfection extends InvitroCharacterization implements Serializable
{
	/**
	* An attribute to allow serialization of the domain objects
	*/
	private static final long serialVersionUID = 1234567890L;

	
	/**
	* 
	**/
	
	private String cellLine;
	/**
	* Retrieves the value of the cellLine attribute
	* @return cellLine
	**/

	public String getCellLine(){
		return cellLine;
	}

	/**
	* Sets the value of cellLine attribute
	**/

	public void setCellLine(String cellLine){
		this.cellLine = cellLine;
	}
	
	/**
	* Compares <code>obj</code> to it self and returns true if they both are same
	*
	* @param obj
	**/
	public boolean equals(Object obj)
	{
		if(obj instanceof Transfection) 
		{
			Transfection c =(Transfection)obj;
            return getId() != null && getId().equals(c.getId());
		}
		return false;
	}
		
	/**
	* Returns hash code for the primary key of the object
	**/
	public int hashCode()
	{
		if(getId() != null)
			return getId().hashCode();
		return 0;
	}
	
}