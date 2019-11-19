/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.exception;

/**
 * @author pansu
 *
 */
public class ChemicalAssociationViolationException extends BaseException {

	private static final long serialVersionUID = 1234567890L;

	public ChemicalAssociationViolationException() {
		super("Exception occurred working with chemical association.");
	}

	public ChemicalAssociationViolationException(String message) {
		super(message);
	}

	public ChemicalAssociationViolationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ChemicalAssociationViolationException(Throwable cause) {
		super(cause);
	}
}
