package com.isa.thinair.ibe.core.web.v2.action.payment;

import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.config.Namespace;
import org.apache.struts2.config.Result;
import org.apache.struts2.config.Results;

import com.isa.thinair.airreservation.api.dto.ReservationDTO;
import com.isa.thinair.airreservation.api.utils.BeanUtils;
import com.isa.thinair.airreservation.api.utils.ResponseCodes;
import com.isa.thinair.commons.api.constants.CommonsConstants;
import com.isa.thinair.commons.api.exception.ModuleException;
import com.isa.thinair.commons.core.util.StringUtil;
import com.isa.thinair.commons.core.util.SystemPropertyUtil;
import com.isa.thinair.ibe.api.dto.IBECommonDTO;
import com.isa.thinair.ibe.api.dto.IBEPromotionPaymentDTO;
import com.isa.thinair.ibe.api.dto.IBEReservationPostPaymentDTO;
import com.isa.thinair.ibe.api.dto.PaymentGateWayInfoDTO;
import com.isa.thinair.ibe.core.service.ModuleServiceLocator;
import com.isa.thinair.ibe.core.web.constants.ExceptionConstants;
import com.isa.thinair.ibe.core.web.constants.ReservationWebConstnts;
import com.isa.thinair.ibe.core.web.constants.StrutsConstants;
import com.isa.thinair.ibe.core.web.listener.AAHttpSessionListener;
import com.isa.thinair.ibe.core.web.util.AppParamUtil;
import com.isa.thinair.ibe.core.web.util.SessionUtil;
import com.isa.thinair.ibe.core.web.util.ancilarary.SeatMapUtil;
import com.isa.thinair.ibe.core.web.v2.constants.WebConstants;
import com.isa.thinair.ibe.core.web.v2.util.CommonUtil;
import com.isa.thinair.ibe.core.web.v2.util.I18NUtil;
import com.isa.thinair.ibe.core.web.v2.util.ReservationUtil;
import com.isa.thinair.ibe.core.web.v2.util.SystemUtil;
import com.isa.thinair.lccclient.api.util.LCCClientApiUtils;
import com.isa.thinair.paymentbroker.api.dto.IPGIdentificationParamsDTO;
import com.isa.thinair.paymentbroker.api.dto.IPGResponseDTO;
import com.isa.thinair.paymentbroker.api.dto.IPGTransactionResultDTO;
import com.isa.thinair.paymentbroker.api.dto.XMLResponseDTO;
import com.isa.thinair.paymentbroker.api.dto.paypal.UserInputDTO;
import com.isa.thinair.paymentbroker.api.dto.qiwi.QiwiPRequest;
import com.isa.thinair.paymentbroker.api.service.PaymentBrokerBD;
import com.isa.thinair.paymentbroker.api.util.PaymentConstants;
import com.isa.thinair.promotion.api.to.PromotionRequestTo;
import com.isa.thinair.webplatform.api.base.BaseRequestAwareAction;
import com.isa.thinair.webplatform.api.util.WebplatformUtil;
import com.opensymphony.xwork2.ActionChainResult;

@Namespace(StrutsConstants.Namespace.PUBLIC)
@Results({
		@Result(name = StrutsConstants.Result.SUCCESS, type = ActionChainResult.class, value = StrutsConstants.Action.SAVE_INTERLINE_RESERVATION),
		@Result(name = StrutsConstants.Action.SAVE_PROMOTION_REQUESTS, type = ActionChainResult.class, value = StrutsConstants.Action.SAVE_PROMOTION_REQUESTS),
		@Result(name = StrutsConstants.Result.SESSION_EXPIRED, value = StrutsConstants.Jsp.Common.SESSION_EXPIRED),
		@Result(name = StrutsConstants.Jsp.ModifyReservation.LOAD_REDIRECTPAGE, value = StrutsConstants.Jsp.ModifyReservation.LOAD_REDIRECTPAGE),
		@Result(name = StrutsConstants.Result.ERROR, value = StrutsConstants.Jsp.Common.ERROR),
		@Result(name = StrutsConstants.Action.PAY_AT_STORE_BACK_TO_MERCHANT, type = ActionChainResult.class, value = StrutsConstants.Action.PAY_AT_STORE_BACK_TO_MERCHANT) })
public class HandleInterlineIPGResponseAction extends BaseRequestAwareAction {

	private static Log log = LogFactory.getLog(HandleInterlineIPGResponseAction.class);
	private IBECommonDTO commonParams = new IBECommonDTO();
	private UserInputDTO userInputDTO = new UserInputDTO();
	private String sessionId;

	private String handleMerchantPost() {
		if (BeanUtils.nullHandler(this.sessionId).length() > 0) {

			HttpSession oldSession = AAHttpSessionListener.getSession(this.sessionId);
			synchronized (oldSession) {

				if (oldSession.getAttribute(WebConstants.IBE_NEW_SESSION_ID) == null) {
					if (log.isDebugEnabled()) {
						log.debug("####### Session ID as recevied from the PG : " + request.getSession().getId());
					}

					AAHttpSessionListener.handleIPGPost(this.sessionId, request.getSession().getId());

					if (log.isDebugEnabled()) {
						log.debug("####### Previous Session Attributess were copied to current Session");
					}
				} else {
					composeCommonError();
					return StrutsConstants.Result.ERROR;
				}
			}
		}

		return null;
	}

	private void composeCommonError() {
		StringBuffer messageBuilder = new StringBuffer();
		messageBuilder.append(I18NUtil.getMessage("msg.booking.session.timeout"));
		messageBuilder.append("<br/>");
		messageBuilder.append(I18NUtil.getMessage("msg.booking.fail.refund"));
		request.setAttribute(ReservationWebConstnts.MSG_ERROR_MESSAGE, messageBuilder.toString());
	}

	public String execute() {

		String forward = StrutsConstants.Result.SUCCESS;
		IBEReservationPostPaymentDTO sessResInfo = null;
		boolean isPaymentSuccess = false;

		try {
			// This should be the first statement always.
			String returnStatus = BeanUtils.nullHandler(this.handleMerchantPost());
			if (returnStatus.length() > 0) {
				return returnStatus;
			}

			String timestamp = CommonsConstants.DATE_FORMAT_FOR_LOGGING.format(new Date());
			// IBEReservationPostPaymentDTO sessResInfo = null;
			Map<String, String> receiptyMap = getReceiptMap(request);
			if (isSessionExpiry(receiptyMap)) {
				return StrutsConstants.Result.ERROR;
			}
			/* Set navigation parameter */
			SystemUtil.setCommonParameters(request, commonParams);
			sessResInfo = SessionUtil.getIBEReservationPostPaymentDTO(request);
			/*
			 * Check whether successful reservation exist if exist we will load the reservation. When user click the
			 * browser back button or any other cases
			 */
			if (isSuccessfulReservationExist()) {
				return StrutsConstants.Jsp.ModifyReservation.LOAD_REDIRECTPAGE;
			}
			// TODO handle proper way
			// FIXME
			if (sessResInfo.getPaymentGateWay().getProviderCode().equals("CYBERPLUS")) {
				receiptyMap = getReceiptMapWithEmptyFields(request);

			} else if (sessResInfo.getPaymentGateWay().getProviderCode().equalsIgnoreCase("CYBERSOURCE")) {
				log.info("##############################################################");
				Enumeration myEnum = request.getParameterNames();
				if (myEnum != null) {
					while (myEnum.hasMoreElements()) {
						String paramName = (String) myEnum.nextElement();
						String paramValue = request.getParameter(paramName);
						log.info("Key[" + paramName + "] Value[" + paramValue + "] ");
					}
				}

				log.info("##############################################################");

			}

			IPGResponseDTO ipgResponseDTO = new IPGResponseDTO();

			if (SystemPropertyUtil.isPaymentBokerDisabled() && AppParamUtil.isInDevMode()) { // only in dev mode
				ipgResponseDTO.setStatus(IPGResponseDTO.STATUS_ACCEPTED);
				ipgResponseDTO.setCardType(2);
			} else {
				synchronized (request.getSession()) {
					String pnr = sessResInfo.getPnr();
					String merchantTxnRef = sessResInfo.getIpgRefenceNo();
					if (!sessResInfo.isResponseReceived()) {
						int paymentBrokerRefNo = LCCClientApiUtils.getLccPaymentBrokerRefNo(sessResInfo.getTemporyPaymentMap());
						int tempPayId = LCCClientApiUtils.getTmpPayIdFromTmpPayMap(sessResInfo.getTemporyPaymentMap());			

						String strPayCurCode = sessResInfo.getPaymentGateWay().getPayCurrency();
						int ipgId = sessResInfo.getPaymentGateWay().getPaymentGateway();
						IPGIdentificationParamsDTO ipgIdentificationParamsDTO = WebplatformUtil
								.validateAndPrepareIPGConfigurationParamsDTO(new Integer(ipgId), strPayCurCode);
						Properties ipgProps = ModuleServiceLocator.getPaymentBrokerBD().getProperties(ipgIdentificationParamsDTO);
						String responseTypeXML = ipgProps
								.getProperty(PaymentBrokerBD.PaymentBrokerProperties.KEY_RESPONSE_TYPE_XML);

						if (responseTypeXML != null && !responseTypeXML.equals("") && responseTypeXML.equals("true")) {
							log.debug("### Respons is of type XML... ");
							XMLResponseDTO xmlResponse = (XMLResponseDTO) request
									.getAttribute(WebConstants.XML_RESPONSE_PARAMETERS);
							if (xmlResponse != null) {
								receiptyMap = getReceiptMap(
										(XMLResponseDTO) request.getAttribute(WebConstants.XML_RESPONSE_PARAMETERS), request
												.getSession().getId());
							}
						}

						Date requestTime = ModuleServiceLocator.getReservationBD().getPaymentRequestTime(tempPayId);
						ipgResponseDTO.setRequestTimsStamp(requestTime);
						ipgResponseDTO.setResponseTimeStamp(Calendar.getInstance().getTime());
						ipgResponseDTO.setPaymentBrokerRefNo(paymentBrokerRefNo);
						ipgResponseDTO.setTemporyPaymentId(tempPayId);
						ipgResponseDTO.setPaypalResponse(sessResInfo.getPaypalResponse());
						ipgResponseDTO.setOnholdCreated(sessResInfo.isOnHoldCreated());
						ipgResponseDTO.setAmeriaBankSessionInfo(sessResInfo.getAmeriaBankSessionInfo());
						userInputDTO.setToken(getToken());
						ipgResponseDTO.setUserInputDTO(userInputDTO);
						ipgResponseDTO.setBrokerType(ipgProps.getProperty("brokerType"));

						log.debug("### Getting response data... ###");
						ipgResponseDTO = ModuleServiceLocator.getPaymentBrokerBD().getReponseData(receiptyMap, ipgResponseDTO,
								ipgIdentificationParamsDTO);
						if (sessResInfo.getPaymentGateWay().getProviderCode().equalsIgnoreCase("BEHPARDAKHT")) {
							ipgResponseDTO = ModuleServiceLocator.getPaymentBrokerBD().getReponseData(receiptyMap,
									ipgResponseDTO, ipgIdentificationParamsDTO);
						}
						addPaymentDetail(sessResInfo.getPaymentGateWay(), ipgResponseDTO);
						if (log.isDebugEnabled()) {
							log.debug("IPG Response Status:" + "|pnr=" + pnr + "|merchantTxnRef=" + merchantTxnRef + "|sessId="
									+ request.getRequestedSessionId() + "|isSuccess=" + ipgResponseDTO.isSuccess()
									+ "|receivedMerchantTxnRef=" + ipgResponseDTO.getApplicationTransactionId());
						}
						if (IPGResponseDTO.STATUS_REJECTED.equals(ipgResponseDTO.getStatus())
								&& ipgResponseDTO.getErrorCode().equals("payfort.payment.user.backToMerchant")) {
							return StrutsConstants.Action.PAY_AT_STORE_BACK_TO_MERCHANT;
						}
						isPaymentSuccess = ipgResponseDTO.isSuccess();

						// Tempory fix for cybersource.
						// TODO Please find ways to make it configurable
						if (BeanUtils.nullHandler(this.sessionId).length() > 0) {
							if (!ipgResponseDTO.isSuccess()) {
								HttpSession oldSession = AAHttpSessionListener.getSession(this.sessionId);
								oldSession.setAttribute(WebConstants.IBE_NEW_SESSION_ID, null);
								composeCommonError();
								return StrutsConstants.Result.ERROR;
							}
						}
						sessResInfo.setResponseReceived(true);
						ModuleServiceLocator.getReservationBD().updateTempPaymentEntryPaymentStatus(
								sessResInfo.getTemporyPaymentMap(), ipgResponseDTO);
					} else {
						if (StringUtil.isNullOrEmpty(pnr)) {
							ipgResponseDTO.setTemporyPaymentId(LCCClientApiUtils.getTmpPayIdFromTmpPayMap(sessResInfo
									.getTemporyPaymentMap()));
							ipgResponseDTO
									.setStatus(SessionUtil.isResponseReceivedFromPaymentGateway(request) == true ? IPGResponseDTO.STATUS_ACCEPTED
											: IPGResponseDTO.STATUS_REJECTED);
						}
						return processMultiplePaymentResponses(ipgResponseDTO, merchantTxnRef, pnr);
					}
				}
			}

			sessResInfo.setIpgResponseDTO(ipgResponseDTO);
			if (IPGResponseDTO.STATUS_ACCEPTED.equals(ipgResponseDTO.getStatus())) {
				sessResInfo.setInvoiceStatus(QiwiPRequest.PAID);
			}

			// redirecting to re-enter qiwi mobile number
			if (!isPaymentSuccess && sessResInfo.getPaymentGateWay().getProviderCode().equalsIgnoreCase("QIWI")) {
				log.debug("[HandleInterlineIPGResponseAction ] Qiwi Payment Is Not Success : Redirecting ");
				return StrutsConstants.Result.SUCCESS;
			}
			// FIXME
			if (!sessResInfo.getPaymentGateWay().getProviderCode().equals("PAYPAL")) {
				validateTransaction(sessResInfo.getIpgRefenceNo(), ipgResponseDTO, isPaymentSuccess,
						SessionUtil.getLanguage(request));
			}
			/* Set Reservation data After response received. */
			SessionUtil.setTempPaymentPNR(request, sessResInfo.getPnr());
			SessionUtil.setResponseReceivedFromPaymentGateway(request, isPaymentSuccess);

			if (SessionUtil.getIbePromotionPaymentPostDTO(request) != null) {
				IBEPromotionPaymentDTO ibePromotionPaymentDTO = SessionUtil.getIbePromotionPaymentPostDTO(request);
				List<PromotionRequestTo> colPromotionRequestTo = ibePromotionPaymentDTO.getColPromotionRequestTo();
				if (colPromotionRequestTo != null) {
					if (isPaymentSuccess) {
						forward = StrutsConstants.Action.SAVE_PROMOTION_REQUESTS;
					} else {
						log.error("Promotion payment was rejected at the payment gateway");
						return StrutsConstants.Result.ERROR;
					}

				} else {
					log.error("Promotion Session data was not found");
					return handleSessionDataNotFoundAfterPayment(request, receiptyMap, false);
				}

			}

			if (log.isDebugEnabled())
				log.debug("### HandleInterlineIPGResponseAction end... ###");
		} catch (Exception ex) {
			log.error("HandleInterlineIPGResponseAction.SessionID:" + request.getRequestedSessionId(), ex);
			/* Multiple Response Handling if fresh reservation exist - load the itinerary page */
			if (SessionUtil.isSuccessfulReservationExit(request)) {
				forward = StrutsConstants.Jsp.ModifyReservation.LOAD_REDIRECTPAGE;
			} else {
				String message = null;
				forward = StrutsConstants.Result.ERROR;
				callReleaseBlockedSeats(request);
				if (isPaymentSuccess) {
					try {
						String pnr = sessResInfo.getPnr();
						ReservationDTO reservationDTO = ReservationUtil.getReservationDTO(pnr);
						if (reservationDTO != null) {
							message = CommonUtil.getExistingReservationMsg(request, pnr);
						} else {
							message = CommonUtil.getRefundMessage(SessionUtil.getLanguage(request));
						}
					} catch (Exception e) {
						log.error(e);
					}
				} else {
					message = I18NUtil.getMessage(ExceptionConstants.SERVER_OPERATION_FAIL, SessionUtil.getLanguage(request));
				}
				request.setAttribute(ReservationWebConstnts.MSG_ERROR_MESSAGE, message);
				SessionUtil.resetSesionDataInError(request);
			}
		}

		return forward;
	}

	private boolean isOnholdReservation() {
		return SessionUtil.isMakePayment(request);
	}

	/**
	 * Handle multiple redirects for same payment request on given session Handle back buttons payments also
	 */
	private String processMultiplePaymentResponses(IPGResponseDTO ipgResponseDTO, String merchantTxnRef, String pnr) {
		String forward = StrutsConstants.Result.ERROR;
		/* Set page navigation parameters */
		SystemUtil.setCommonParameters(request, commonParams);
		String language = SessionUtil.getLanguage(request);
		boolean isOnhold = isOnholdReservation();
		StringBuilder logInfo = new StringBuilder();
		logInfo.append("### Multiple Response process start.. ###").append("\n");
		logInfo.append("& PNR :").append(pnr);
		logInfo.append("& merchantTxnRef :").append(merchantTxnRef);
		logInfo.append("& Payment Status :").append(ipgResponseDTO.isSuccess());
		logInfo.append("& SessionID : ").append(request.getRequestedSessionId());

		log.info(logInfo.toString());

		StringBuffer messageBuilder = new StringBuffer();
		messageBuilder.append(I18NUtil.getMessage("msg.booking.browser.backbutton", language));
		messageBuilder.append("<br/>");

		if (isOnhold) {
			messageBuilder.append(I18NUtil.getMessage("msg.please.contact", language));
			messageBuilder.append("<br/>");
			messageBuilder.append(I18NUtil.getMessage("msg.reference.number", language)).append(" : ").append(pnr);
			messageBuilder.append("<br/>");
		}

		if (ipgResponseDTO.isSuccess()) {
			boolean isUpdate = false;
			try {
				isUpdate = ModuleServiceLocator.getReservationBD().processMultiplePaymentResponses(
						ipgResponseDTO.getTemporyPaymentId());
			} catch (Exception ex) {
				isUpdate = false;
				log.error("Couldn't update payment status:", ex);
			}
			logInfo = new StringBuilder();
			logInfo.append("Multiple response process status : ").append(isUpdate);
			logInfo.append("& merchantTxnRef : ").append(merchantTxnRef);
			logInfo.append("& SessionID : ").append(request.getRequestedSessionId());
			log.info(logInfo.toString());

			if (isUpdate && !isOnhold) {
				messageBuilder.append(I18NUtil.getMessage("msg.booking.fail.refund", language));
			}

		} else {
			if (!isOnhold) {
				messageBuilder.append(I18NUtil.getMessage("server.default.operation.fail", language));
			}
		}
		callReleaseBlockedSeats(request);
		request.setAttribute(ReservationWebConstnts.MSG_ERROR_MESSAGE, messageBuilder.toString());
		SessionUtil.resetSesionDataInError(request);

		return forward;
	}

	private boolean isSuccessfulReservationExist() {
		boolean isExist = false;
		if (SessionUtil.getIbeReservationPostDTO(request) != null) {
			if (!StringUtil.isNullOrEmpty(SessionUtil.getIbeReservationPostDTO(request).getPnr())) {
				isExist = true;
			}
		}
		return isExist;
	}

	private boolean isSessionExpiry(Map<String, String> receiptyMap) {
		boolean isError = false;
		if (SessionUtil.isSessionExpired(request) || SessionUtil.getIBEReservationPostPaymentDTO(request) == null) {
			// Present error page with appropriate message
			isError = true;
			handleSessionDataNotFoundAfterPayment(request, receiptyMap, true);
		}

		return isError;
	}

	private void addPaymentDetail(PaymentGateWayInfoDTO gateWayInfoDTO, IPGResponseDTO ipgResponseDTO) {
		if (ipgResponseDTO != null && ipgResponseDTO.getIpgTransactionResultDTO() != null && gateWayInfoDTO != null) {
			IPGTransactionResultDTO ipgTransactionResultDTO = ipgResponseDTO.getIpgTransactionResultDTO();
			ipgTransactionResultDTO.setAmount(gateWayInfoDTO.getPayCurrencyAmount());
			ipgTransactionResultDTO.setCurrency(gateWayInfoDTO.getPayCurrency());
		}
	}

	private String getToken() {
		String[] queryStrings;
		String[] tokenKeyValuePair;
		String tokenFromQueryString = null;
		if (request.getQueryString() != null && request.getQueryString().contains("token")
				&& request.getQueryString().contains("PayerID")) {
			queryStrings = request.getQueryString().split("&");
			for (String queryString : queryStrings) {
				if (queryString.contains("token")) {
					tokenKeyValuePair = queryString.split("=");
					tokenFromQueryString = tokenKeyValuePair[1];
				}
			}
		}
		return tokenFromQueryString;
	}

	private static void callReleaseBlockedSeats(HttpServletRequest request) {
		try {
			SeatMapUtil.releaseBlockedSeats(request);
		} catch (Exception ex) {
			log.error("Blocked seats release failed", ex);
		}
	}

	private static String handleSessionDataNotFoundAfterPayment(HttpServletRequest request, Map receiptyMap, boolean relaseSeats) {

		if (relaseSeats) {
			callReleaseBlockedSeats(request);
		}
		// TODO - invoke query and refund the payment if successful payment exists
		StringBuffer messageBuilder = new StringBuffer();
		messageBuilder.append(I18NUtil.getMessage("msg.booking.session.timeout"));
		messageBuilder.append("<br/>");
		messageBuilder.append(I18NUtil.getMessage("msg.booking.fail.refund"));
		request.setAttribute(ReservationWebConstnts.MSG_ERROR_MESSAGE, messageBuilder.toString());
		return StrutsConstants.Result.ERROR;
	}

	private static String handleMultipleRedirects(HttpServletRequest request, String pnr, boolean isPaySucces)
			throws ModuleException {
		// Not invoking releasing seat as previous payment response already handles it
		String msg = "";
		String forward = StrutsConstants.Result.ERROR;

		if (pnr != null && !"".equals(pnr)) {
			if (isPaySucces) {
				forward = StrutsConstants.Jsp.ModifyReservation.LOAD_REDIRECTPAGE;
			} else {
				msg = CommonUtil.getExistingReservationMsg(request, "");
			}
		} else {
			log.error("Handle IPG response - expected sess data not found.");
			msg = "Invalid operation."; // Expected session data not found
		}
		request.setAttribute(ReservationWebConstnts.MSG_ERROR_MESSAGE, msg);

		return forward;
	}

	/**
	 * 
	 * @param ipgTransactionId
	 * @param ipgResponseDTO
	 */
	private static void validateTransaction(String ipgTransactionId, IPGResponseDTO ipgResponseDTO, boolean isPaymentSuccess,
			String language) {

		if (!ipgTransactionId.equals(ipgResponseDTO.getApplicationTransactionId())) {
			log.error("Application Transaction Ids not match[AccelAero TransactionId Sent=" + ipgTransactionId
					+ ",AccelAero TransactionId Received" + ipgResponseDTO.getApplicationTransactionId() + "]");
			String msg = "";
			if (isPaymentSuccess) {
				msg = CommonUtil.getRefundMessage(language);
			} else {
				msg = I18NUtil.getMessage("msg.invalid.booking", language);
			}
			throw new RuntimeException(msg);
		}
	}

	/**
	 * Returns ReceiptMap. Map will holds all parameters send by the payment gateway. It is up the payment broker
	 * implementation extract the required parameter and values
	 * 
	 * @param request
	 * @return
	 */
	private static Map<String, String> getReceiptMap(HttpServletRequest request) {
		Map<String, String> fields = new LinkedHashMap<String, String>();
		for (Enumeration<String> enumeration = request.getParameterNames(); enumeration.hasMoreElements();) {
			String fieldName = (String) enumeration.nextElement();
			String fieldValue = request.getParameter(fieldName);

			if (fieldValue != null && fieldValue.length() > 0) {
				fields.put(fieldName, fieldValue);
			}
		}
		fields.put(PaymentConstants.IPG_SESSION_ID, request.getSession().getId());//check how this session id is
		//where we get session check how it is used check if this is same as transaction id
		return fields;
	}

	/**
	 * Returns ReceiptMap. Map will holds all parameters send by the payment gateway including fields with empty values.
	 * 
	 * @param request
	 * @return
	 */
	private static Map<String, String> getReceiptMapWithEmptyFields(HttpServletRequest request) {
		Map<String, String> fields = new LinkedHashMap<String, String>();
		for (Enumeration<String> enumeration = request.getParameterNames(); enumeration.hasMoreElements();) {
			String fieldName = (String) enumeration.nextElement();
			String fieldValue = request.getParameter(fieldName);

			if (fieldValue != null) {
				fields.put(fieldName, fieldValue);
			}
		}
		fields.put(PaymentConstants.IPG_SESSION_ID, request.getSession().getId());
		return fields;
	}

	/**
	 * Returns ReceiptMap. Map will holds all parameters send by the payment gateway.
	 * 
	 * @param xmlResponse
	 * @return
	 */
	private static Map<String, String> getReceiptMap(XMLResponseDTO xmlResponse, String sessionId) {
		Map<String, String> fields = new LinkedHashMap<String, String>();

		if (log.isDebugEnabled()) {
			log.debug("### Started generating receipt map...");
			log.debug("### Order ID : " + StringUtil.getNotNullString(xmlResponse.getOrderId()));
		}

		fields.put(PaymentConstants.XML_RESPONSE.ORDERID.getValue(), StringUtil.getNotNullString(xmlResponse.getOrderId()));
		fields.put(PaymentConstants.XML_RESPONSE.PAYID.getValue(), StringUtil.getNotNullString(xmlResponse.getPayId()));
		fields.put(PaymentConstants.XML_RESPONSE.NCSTATUS.getValue(), StringUtil.getNotNullString(xmlResponse.getNcStatus()));
		fields.put(PaymentConstants.XML_RESPONSE.NCERROR.getValue(), StringUtil.getNotNullString(xmlResponse.getNcError()));
		fields.put(PaymentConstants.XML_RESPONSE.NCERRORPLUS.getValue(),
				StringUtil.getNotNullString(xmlResponse.getNcErrorPlus()));
		fields.put(PaymentConstants.XML_RESPONSE.ACCEPTANCE.getValue(), StringUtil.getNotNullString(xmlResponse.getAcceptance()));
		fields.put(PaymentConstants.XML_RESPONSE.STATUS.getValue(), StringUtil.getNotNullString(xmlResponse.getStatus()));
		fields.put(PaymentConstants.XML_RESPONSE.ECI.getValue(), StringUtil.getNotNullString(xmlResponse.getEci()));
		fields.put(PaymentConstants.XML_RESPONSE.AMOUNT.getValue(), StringUtil.getNotNullString(xmlResponse.getAmount()));
		fields.put(PaymentConstants.XML_RESPONSE.CURRENCY.getValue(), StringUtil.getNotNullString(xmlResponse.getCurrency()));
		fields.put(PaymentConstants.XML_RESPONSE.PAYMENT_METHOD.getValue(),
				StringUtil.getNotNullString(xmlResponse.getPaymentMethod()));
		fields.put(PaymentConstants.XML_RESPONSE.BRAND.getValue(), StringUtil.getNotNullString(xmlResponse.getBrand()));
		fields.put(PaymentConstants.XML_RESPONSE.SHAREQUIRED.getValue(), "N");

		fields.put(PaymentConstants.IPG_SESSION_ID, sessionId);

		log.debug("### Returning the receipt map...");
		return fields;
	}

	/**
	 * 
	 * @param params
	 * @return
	 */
	private static String getRequestParamsString(Map<String, String> params) {
		StringBuffer requestParams = new StringBuffer();

		if (params != null) {
			String key;
			for (Iterator<String> paramsIt = params.keySet().iterator(); paramsIt.hasNext();) {
				key = (String) paramsIt.next();
				if ("".equals(requestParams)) {
					requestParams.append(key + "=" + params.get(key));
				} else {
					requestParams.append(", " + key + "=" + params.get(key));
				}
			}
		}
		return requestParams.toString();
	}

	private String getHTTPHeaderValue(HttpServletRequest request, String headerName) {
		String headerParamValue = "";
		try {
			headerParamValue = request.getHeader(headerName);
		} catch (Exception e) {
			headerParamValue = "-";
		}
		return headerParamValue;
	}

	private String getNullSafeValue(String value) {
		return StringUtils.trimToEmpty(value);
	}

	public IBECommonDTO getCommonParams() {
		return commonParams;
	}

	public void setCommonParams(IBECommonDTO commonParams) {
		this.commonParams = commonParams;
	}

	private static ModuleException getStandardPaymentBrokerError(String paymentErrorCode) {

		ModuleException me = new ModuleException(ResponseCodes.STANDARD_PAYMENT_BROKER_ERROR);
		Map<String, String> errorMap = new HashMap<String, String>();

		errorMap.put(paymentErrorCode, "External payment broker error:" + paymentErrorCode);
		me.setExceptionDetails(errorMap);

		return me;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

}
