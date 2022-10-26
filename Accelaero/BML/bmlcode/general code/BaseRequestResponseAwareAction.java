package com.isa.thinair.webplatform.api.base;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.ServletResponseAware;

/**
 * Base Class for S2 HttpServletResponse HttpServletRequest Support
 * 
 * @author Lasantha Pambagoda
 */
public class BaseRequestResponseAwareAction extends BaseRequestAwareAction implements ServletResponseAware {

	protected HttpServletResponse response;
	public static final String REQ_MESSAGE = "reqMessage";
	public static final String MSG_TYPE = "reqMsgType";
	private static Log log = LogFactory.getLog(BaseRequestResponseAwareAction.class);

	@Override
	public void setServletResponse(HttpServletResponse response) {
		this.response = response;
	}

	protected static void saveMessage(HttpServletRequest request, String userMessage, String msgType) {
		request.setAttribute(REQ_MESSAGE, userMessage);
		request.setAttribute(MSG_TYPE, msgType);
	}

	protected static Object getAttribInRequest(HttpServletRequest request, String attributeName) {
		Object objReturn = null;
		try {
			objReturn = request.getAttribute(attributeName);
		} catch (Exception e) {
			log.error("error in retriving attribute" + e.getMessage());
		}
		return objReturn;
	}
}
