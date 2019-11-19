/*L
 *  Copyright Leidos
 *  Copyright Leidos Biomedical
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cananolab/LICENSE.txt for details.
 */

package gov.nih.nci.cananolab.restful.core;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import gov.nih.nci.cananolab.exception.BaseException;
import gov.nih.nci.cananolab.exception.InvalidSessionException;
import gov.nih.nci.cananolab.exception.NoAccessException;
import gov.nih.nci.cananolab.exception.SecurityException;
import gov.nih.nci.cananolab.security.utils.SpringSecurityUtil;
import gov.nih.nci.cananolab.util.Constants;
import gov.nih.nci.cananolab.util.StringUtils;

public abstract class AbstractDispatchBO
{

	public void execute(HttpServletRequest request) throws Exception
	{
		HttpSession session = request.getSession();
		String dispatch = (String) request.getParameter("dispatch");
		String page = request.getParameter("page");
		// per app scan, validate dispatch and page parameters
		if (page != null && !page.matches(Constants.NUMERIC_PATTERN)) {
			throw new BaseException("Invalid value for the page parameter");
		}
		if (dispatch == null) {
			throw new BaseException("The dispatch parameter can not be null");
		}
		if (!dispatch.matches(Constants.STRING_PATTERN)) {
			throw new BaseException("Invalid value for the dispatch parameter");
		}
		// private dispatch and session expired
		boolean privateDispatch = checkDispatch(dispatch, Constants.PRIVATE_DISPATCHES, "startsWith");
		if (session.isNew() && privateDispatch) {
			throw new InvalidSessionException();
		}
		// if dispatched methods require validation but page=0 throw error
		if (checkDispatch(dispatch, Constants.DISPATCHES_WITH_VALIDATIONS, "equals") && 
			(page == null || Integer.parseInt(page) <= 0))
		{
			throw new BaseException("The value for the page parameter is invalid for the given dispatch");
		}
		String protectedData = request.getParameter("sampleId");
		if (protectedData == null) {
			protectedData = request.getParameter("publicationId");
		}
		if (protectedData == null) {
			protectedData = request.getParameter("protocolId");
		}
		boolean executeStatus = canUserExecute(request, dispatch, protectedData);
		if (executeStatus) {
			
			//TODO
			//return null;
			//return super.execute(mapping, form, request, response);
			
			
		} else {
			if (!SpringSecurityUtil.isUserLoggedIn()) {
				throw new NoAccessException("Log in is required");
			}
			throw new NoAccessException();
		}
	}

	/**
	 * Check whether the current user can execute the action with the specified
	 * dispatch
	 * 
	 * @return
	 * @throws SecurityException
	 */
	public boolean canUserExecute(HttpServletRequest request, String dispatch, String protectedData) throws Exception
	{
		// private dispatch in public actions
		boolean privateDispatch = checkDispatch(dispatch, Constants.PRIVATE_DISPATCHES, "startsWith");
		if (!privateDispatch) {
			return true;
		} else if (!SpringSecurityUtil.isUserLoggedIn()) {
			return false;
		} else {
			return canUserExecutePrivateDispatch(request, protectedData);
		}
	}

	public Boolean canUserExecutePrivateDispatch(HttpServletRequest request, String protectedData) throws Exception
	{
		return SpringSecurityUtil.isUserLoggedIn();
	}

	private boolean checkDispatch(String dispatch, String[] dispatchGroup, String compareString)
	{
		if (dispatch != null) {
			for (String theDispatch : dispatchGroup) {
				if (compareString.equals("startsWith")) {
					if (dispatch.startsWith(theDispatch)) {
						return true;

					} else {
						if (dispatch.equals(theDispatch)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Get the page number used in display tag library pagination
	 * 
	 * @param request
	 * @return
	 */
	public int getDisplayPage(HttpServletRequest request)
	{
		int page = 0;
		Enumeration paramNames = request.getParameterNames();
		while (paramNames.hasMoreElements()) {
			String name = (String) paramNames.nextElement();
			if (name != null && name.startsWith("d-") && name.endsWith("-p")) {
				String pageValue = request.getParameter(name);
				if (pageValue != null) {
					page = Integer.parseInt(pageValue) - 1;
				}
			}
		}
		return page;
	}

	public String getBrowserDispatch(HttpServletRequest request) {
		String dispatch = request.getParameter("dispatch");
		// get the dispatch value from the URL in the browser address bar
		// used in case of validation
		if (dispatch != null && request.getAttribute("javax.servlet.forward.query_string") != null) {
			String browserQueryString = request.getAttribute("javax.servlet.forward.query_string").toString();
			if (!StringUtils.isEmpty(browserQueryString)) {
				return browserQueryString.replaceAll("dispatch=(.+)&(.+)", "$1");
			}
		}
		return "";
	}

	/**
	 * Retrieve a value from request by name in the order of Parameter - Request
	 * Attribute - Session Attribute
	 * 
	 * @param request
	 * @param name
	 * @return
	 */
	public Object getValueFromRequest(HttpServletRequest request, String name) {
		Object value = request.getParameter(name);
		if (value == null) {
			value = request.getAttribute(name);
		}
		if (value == null) {
			value = request.getSession().getAttribute(name);
		}
		return value;
	}

}