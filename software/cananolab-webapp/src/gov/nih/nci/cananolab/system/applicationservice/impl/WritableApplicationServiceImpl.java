package gov.nih.nci.cananolab.system.applicationservice.impl;

import gov.nih.nci.cananolab.system.dao.DAO;
import gov.nih.nci.cananolab.system.dao.Request;
import gov.nih.nci.cananolab.system.dao.Response;
import gov.nih.nci.system.query.SDKQuery;
import gov.nih.nci.system.query.SDKQueryResult;
import gov.nih.nci.system.query.example.ExampleQuery;
import gov.nih.nci.system.query.example.SearchExampleQuery;
import gov.nih.nci.cananolab.system.query.hibernate.HQLCriteria;
import gov.nih.nci.system.query.hql.SearchHQLQuery;
import gov.nih.nci.cananolab.system.query.nestedcriteria.NestedCriteriaPath;
import gov.nih.nci.cananolab.system.util.ClassCache;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;
import gov.nih.nci.cananolab.system.applicationservice.ApplicationException;
import gov.nih.nci.cananolab.system.applicationservice.WritableApplicationService;
import gov.nih.nci.cananolab.system.dao.WritableDAO;


public class WritableApplicationServiceImpl extends ApplicationServiceImpl implements WritableApplicationService
{
	private static Logger log = Logger.getLogger(WritableApplicationServiceImpl.class.getName());
	public WritableApplicationServiceImpl(ClassCache classCache) {
		super(classCache);
	}

	public SDKQueryResult executeQuery(SDKQuery query) throws ApplicationException {
		String classname = getTargetClassName(query);
		WritableDAO dao = getWritableDAO(classname);
		Request request = prepareRequest(query, classname);

		try {
			Response resp =  dao.query(request);
            return prepareResult(request,resp);
		}   catch(Exception exception) {
			log.error("Error while querying DAO ", exception);
			throw new ApplicationException("Error while querying DAO: ", exception);
		}
	}
	
	public List<SDKQueryResult> executeBatchQuery(List<SDKQuery> batchQuery) throws ApplicationException {
		List<SDKQueryResult> sdkQueryResults=new ArrayList<SDKQueryResult>();
		
		for (SDKQuery query : batchQuery) {
			SDKQueryResult queryResult = executeQuery(query);
			sdkQueryResults.add(queryResult);
		}
		return sdkQueryResults;
	}
	
	protected SDKQueryResult prepareResult(Request request, Response resp) {
		Object result = resp.getResponse();
		SDKQueryResult queryResult = null;
		//Pagination of the results if it was a search query
		if(result!=null && !(result instanceof SDKQueryResult))
			queryResult = new SDKQueryResult(convertToListProxy((Collection)result,request.getRequest(),request.getDomainObjectName()));
		else
			queryResult = (SDKQueryResult) result;
		return queryResult;
	}

	protected Request prepareRequest(SDKQuery query, String classname) {
		Request request = new Request();
		request.setIsCount(Boolean.FALSE);
		request.setClassCache(getClassCache());
		request.setDomainObjectName(classname);
		
		Object requestObject = query;
		
		//Needed to make sure the pagination and the count queries works.
		if (query instanceof SearchExampleQuery)
		{
			Object obj = ((SearchExampleQuery)query).getExample();
			List<Object> objList = new ArrayList<Object>();
			objList.add(obj);
            requestObject = new NestedCriteriaPath(obj.getClass().getName(), objList);
		}
		else if(query instanceof SearchHQLQuery)
		{
			HQLCriteria oldCriteria = (HQLCriteria)query;
            requestObject = new HQLCriteria(oldCriteria.getHqlString(),oldCriteria.getParameters());
		}
		
		request.setRequest(requestObject);
		
		return request;
	}

	protected WritableDAO getWritableDAO(String classname) throws ApplicationException
	{
		DAO dao = getDAO(classname);
		if(dao instanceof WritableDAO)
			return (WritableDAO)dao;

		throw new ApplicationException("Can not execute query on non-writable DAO");
	}

	protected String getTargetClassName(SDKQuery query)
	{
		String classname = null;
		if(query instanceof ExampleQuery)
		{
			classname = ((ExampleQuery)query).getExample().getClass().getName();
		}
		else if (query instanceof HQLCriteria)
		{
			String hql = ((HQLCriteria)query).getHqlString();
			
			String upperHQL = hql.toUpperCase();
			int index = upperHQL.indexOf(" FROM ");
			
			hql = hql.substring(index + " FROM ".length()).trim()+" ";
			classname = hql.substring(0,hql.indexOf(' ')).trim();
		}
		return classname;
	}
}