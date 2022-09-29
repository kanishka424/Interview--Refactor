package com.isa.thinair.paymentbroker.core.bl.bml;

import com.isa.thinair.commons.api.exception.ModuleException;
import com.isa.thinair.commons.core.framework.DefaultServiceResponse;
import com.isa.thinair.commons.core.util.AppIndicatorEnum;
import com.isa.thinair.commons.core.util.CalendarUtil;
import com.isa.thinair.paymentbroker.api.dto.*;
import com.isa.thinair.paymentbroker.api.model.CreditCardPayment;
import com.isa.thinair.paymentbroker.api.model.CreditCardTransaction;
import com.isa.thinair.paymentbroker.api.util.PaymentConstants;
import com.isa.thinair.paymentbroker.api.util.TnxModeEnum;
import com.isa.thinair.paymentbroker.core.PaymentBrokerInternalConstants;
import com.isa.thinair.paymentbroker.core.bl.PaymentBroker;
import com.isa.thinair.paymentbroker.core.bl.PaymentBrokerTemplate;
import com.isa.thinair.paymentbroker.core.config.PaymentBrokerModuleUtil;
import com.isa.thinair.paymentbroker.core.util.PaymentBrokerUtils;
import com.isa.thinair.platform.api.ServiceResponce;
import com.isa.thinair.platform.api.util.PlatformUtiltiies;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.isa.thinair.paymentbroker.api.util.PaymentConstants.*;

public class PaymentBrokerBmlTemplate extends PaymentBrokerTemplate implements PaymentBroker {
	private static Log log = LogFactory.getLog(PaymentBrokerBmlTemplate.class);

	protected static final String ERROR_CODE_PREFIX = "bml.";
	protected static final String CARD_TYPE_GENERIC = "GC";
	protected static final String DEFAULT_ERROR_CODE = "payment.failed";
	protected static final String USER_CANCELED = "bml.1";
	protected static final String BACK_TO_MERCHANT = "payment.user.backToMerchant";

	private String merchantName;
	private String currency;
	private String acquirerId;
	private String signMethod;
	private String apiKey;
	private String amount;

	protected static ResourceBundle bundle = PaymentBrokerModuleUtil.getResourceBundle();

	@Override
	public Properties getProperties() {
		Properties props = super.getProperties();

		props.put(BmlRequest.ACQUIRER_ID, PlatformUtiltiies.nullHandler(acquirerId));
		props.put(BmlRequest.CURRENCY, PlatformUtiltiies.nullHandler(currency));
		props.put(BmlRequest.API_KEY, PlatformUtiltiies.nullHandler(apiKey));
		props.put(BmlRequest.SIGNATURE_METHOD, PlatformUtiltiies.nullHandler(signMethod));
		props.put(BmlRequest.MERCHANT_ID, PlatformUtiltiies.nullHandler(merchantId));
		props.put(BmlRequest.MERCHANT_NAME, PlatformUtiltiies.nullHandler(merchantName));
		props.put(BmlRequest.PASSWORD, PlatformUtiltiies.nullHandler(password));
		props.put(BmlRequest.AMOUNT, PlatformUtiltiies.nullHandler(amount));

		return props;
	}

	public Properties getPropertiesFromQuery(String currency, String apiKey) {
		Properties props = super.getProperties();
		props.put(BmlRequest.CURRENCY, PlatformUtiltiies.nullHandler(currency));
		props.put(BmlRequest.API_KEY, PlatformUtiltiies.nullHandler(apiKey));
		return props;
	}

	@Override
	public ServiceResponce refund(CreditCardPayment creditCardPayment, String pnr,
			AppIndicatorEnum appIndicator, TnxModeEnum tnxMode) throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceResponce charge(CreditCardPayment creditCardPayment, String pnr,
			AppIndicatorEnum appIndicator, TnxModeEnum tnxMode,
			List <CardDetailConfigDTO> cardDetailConfigData) throws ModuleException {

		CreditCardTransaction creditCardTransaction = PaymentBrokerUtils.getPaymentBrokerDAO()
				.loadTransaction(creditCardPayment.getPaymentBrokerRefNo());

		if (creditCardTransaction != null && !PlatformUtiltiies.nullHandler(
				creditCardTransaction.getRequestText()).equals("")) {
			DefaultServiceResponse sr = new DefaultServiceResponse(true);
			sr.setResponseCode(String.valueOf(creditCardTransaction.getTransactionRefNo()));
			sr.addResponceParam(PaymentConstants.PAYMENTBROKER_AUTHORIZATION_ID,
					creditCardTransaction.getAidCccompnay());
			return sr;
		}

		throw new ModuleException("airreservations.temporyPayment.corruptedParameters");
	}

	@Override
	public ServiceResponce reverse(CreditCardPayment creditCardPayment, String pnr,
			AppIndicatorEnum appIndicator, TnxModeEnum tnxMode) throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceResponce capturePayment(CreditCardPayment creditCardPayment, String pnr,
			AppIndicatorEnum appIndicator, TnxModeEnum tnxMode) throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPGRequestResultsDTO getRequestData(IPGRequestDTO ipgRequestDTO) throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPGRequestResultsDTO getRequestData(IPGRequestDTO ipgRequestDTO,
			List <CardDetailConfigDTO> configDataList) throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPGResponseDTO getReponseData(Map receiptMap, IPGResponseDTO ipgResponseDTO)
			throws ModuleException {
		log.debug("[PaymentBrokerBmlTemplate::getReponseData()] Begin ");

		int tnxResultCode = 0;
		String status = "";
		String errorSpecification = "";
		String responseMismatch = "";
		DefaultServiceResponse bmlServiceResponse = null;

		int cardType = getStandardCardType(CARD_TYPE_GENERIC); // default card type
		CreditCardTransaction creditCardTransaction = loadAuditTransactionByTnxRefNo(
				ipgResponseDTO.getPaymentBrokerRefNo());
		// Calculate response time
		String strResponseTime = CalendarUtil.getTimeDifference(ipgResponseDTO.getRequestTimsStamp(),
				ipgResponseDTO.getResponseTimeStamp());
		long timeDiffInMillis = CalendarUtil.getTimeDifferenceInMillis(ipgResponseDTO.getRequestTimsStamp(),
				ipgResponseDTO.getResponseTimeStamp());

		// Update response time
		creditCardTransaction.setComments(strResponseTime);
		creditCardTransaction.setResponseTime(timeDiffInMillis);
		String receiptMapString = convertMapToString(receiptMap);
		creditCardTransaction.setUrlResParams(receiptMapString);
		PaymentBrokerUtils.getPaymentBrokerDAO().saveTransaction(creditCardTransaction);

		BmlResponse bmlResponse = new BmlResponse();
		bmlResponse.setReqSignature((String) receiptMap.get("signature"));
		bmlResponse.setState((String) receiptMap.get("state"));
		bmlResponse.setTransactionId((String) receiptMap.get("transactionId"));
		log.debug("[PaymentBrokerBmlTemplate::getReponseData()] Mid Response -" + bmlResponse);

		CreditCardTransactionDetailsResponse creditCardTransactionResponse = BmlPaymentUtils.getGson()
				.fromJson(creditCardTransaction.getResponseText(),
						CreditCardTransactionDetailsResponse.class);
		String formattedAmount = BmlPaymentUtils.formatAmount(
				creditCardTransactionResponse.getAmountAsDecimal());

		PaymentBrokerUtils.getPaymentBrokerDAO().saveTransaction(creditCardTransaction);

		if (isTransactionApproved(bmlResponse, creditCardTransaction)) {
			ApprovedTransactionStatus approvedTransactionStatus = approvedTransaction(bmlResponse, formattedAmount,
					creditCardTransaction);
			bmlServiceResponse = approvedTransactionStatus.getBmlServiceResponse();
			status = approvedTransactionStatus.getStatus();
			errorSpecification = approvedTransactionStatus.getErrorSpecification();
			tnxResultCode = approvedTransactionStatus.getTnxResultCode();
		} else if (BmlResponse.TRANSACTION_CANCELLED.equals(bmlResponse.getState())) {
			status = BmlResponse.TRANSACTION_CANCELLED;
			responseMismatch = BmlResponse.TRANSACTION_CANCELLED;
			ipgResponseDTO.setErrorCode(USER_CANCELED);
			errorSpecification = status + " " + responseMismatch;
		} else {
			responseMismatch = BmlResponse.TRANSACTION_REJECTED.equals(bmlResponse.getState()) ?
					BmlResponse.TRANSACTION_REJECTED :
					QueryResponse.INVALID_RESPONSE;
			status = IPGResponseDTO.STATUS_REJECTED;
			errorSpecification = status + " " + responseMismatch;
		}
		if (bmlServiceResponse != null) {
			updateAuditTransactionByTnxRefNo(creditCardTransaction.getTransactionRefNo(),
					creditCardTransaction.getResponseText(), errorSpecification,
					(String) bmlServiceResponse.getResponseParam(BML_ID),
					creditCardTransaction.getTransactionId(), tnxResultCode);
			log.debug("[PaymentBrokerBmlTemplateImpl::getReponseData()] End -"
					+ bmlServiceResponse.getResponseParam(BML_ID) + "");
			ipgResponseDTO.setCcLast4Digits((String) bmlServiceResponse.getResponseParam(CC_LAST4_DIGITS));
		} else {
			errorSpecification = errorSpecification.equals("") ?
					BmlResponse.RESPONSE_QUERY_NULL :
					errorSpecification;
			updateAuditTransactionByTnxRefNo(creditCardTransaction.getTransactionRefNo(),
					creditCardTransaction.getResponseText(), errorSpecification, null,
					creditCardTransaction.getTransactionId(), tnxResultCode);
			log.debug(
					"[PaymentBrokerBmlTemplateImpl::getReponseData()] End -" + BmlResponse.RESPONSE_QUERY_NULL
							+ "");
		}
		ipgResponseDTO.setStatus(status);
		ipgResponseDTO.setApplicationTransactionId(creditCardTransaction.getTempReferenceNum());
		ipgResponseDTO.setCardType(cardType);
		return ipgResponseDTO;
	}

	private boolean isTransactionApproved(BmlResponse bmlResponse,
			CreditCardTransaction creditCardTransaction) {
		return BmlResponse.TRANSACTION_APPROVED.equals(bmlResponse.getState())
				&& creditCardTransaction.getTransactionId().equals(bmlResponse.getTransactionId());
	}

	private ApprovedTransactionStatus approvedTransaction(BmlResponse bmlResponse, String formattedAmount,
			CreditCardTransaction creditCardTransaction) throws ModuleException {
		String status = null;
		String responseMismatch = null;
		int tnxResultCode = 0;
		DefaultServiceResponse bmlServiceResponse = null;
		IPGConfigsDTO ipgConfigs = BmlPaymentUtils.getIPGConfigs(getMerchantId(), getPassword());
		ipgConfigs.setAccessCode(getApiKey());
		ipgConfigs.setIpgURL(getIpgURL());
		String errorSpecification;
		if (BmlPaymentUtils.validateHASH(bmlResponse, getProperties(), formattedAmount)) {
			PaymentBrokerBmlQueryDR paymentBrokerBmlQueryDR = new PaymentBrokerBmlQueryDR();
			bmlServiceResponse = (DefaultServiceResponse) paymentBrokerBmlQueryDR.query(creditCardTransaction,
					ipgConfigs, PaymentBrokerInternalConstants.QUERY_CALLER.SCHEDULER);
			Properties propsFromQuery = getPropertiesFromQuery(
					(String) bmlServiceResponse.getResponseParam(BmlRequest.CURRENCY),
					(String) bmlServiceResponse.getResponseParam(BmlRequest.API_KEY));

			if (isPaymentSuccess(bmlServiceResponse, bmlResponse, propsFromQuery)) {
				status = IPGResponseDTO.STATUS_ACCEPTED;
				responseMismatch = BmlResponse.VALID_HASH;
				tnxResultCode = 1;
				errorSpecification = status + " " + responseMismatch;
			} else {
				responseMismatch = BmlResponse.VALID_HASH;
				String queryMessage = BmlResponse.TRANSACTION_REJECTED;
				status = IPGResponseDTO.STATUS_REJECTED;
				tnxResultCode = 0;
				errorSpecification = status + " " + responseMismatch + " " + queryMessage;
			}

		} else {

			errorSpecification = BmlResponse.INVALID_HASH;
		}

		ApprovedTransactionStatus approvedTransactionStatus = new ApprovedTransactionStatus();
		approvedTransactionStatus.setBmlServiceResponse(bmlServiceResponse);
		approvedTransactionStatus.setStatus(status);
		approvedTransactionStatus.setErrorSpecification(errorSpecification);
		approvedTransactionStatus.setTnxResultCode(tnxResultCode);
		approvedTransactionStatus.setResponseMismatch(responseMismatch);
		return approvedTransactionStatus;
	}

	private boolean isPaymentSuccess(DefaultServiceResponse bmlServiceResponse, BmlResponse bmlResponse,
			Properties propsFromQuery) {
		return bmlServiceResponse.isSuccess() && BmlResponse.TRANSACTION_APPROVED.equals(
				bmlResponse.getState()) && BmlPaymentUtils.validateHASH(bmlResponse, propsFromQuery,
				BmlPaymentUtils.formatAmount(((QueryResponse) bmlServiceResponse.getResponseParam(
						QUERY_DR_RESPONSE_OBJECT)).getAmountAsDecimal()));
	}

	@Override
	public ServiceResponce captureStatusResponse(Map receiptMap) throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceResponce resolvePartialPayments(Collection creditCardPayment, boolean refundFlag)
			throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceResponce preValidatePayment(PCValiPaymentDTO params) throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PaymentCollectionAdvise advisePaymentCollection(IPGPrepPaymentDTO params) throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServiceResponce postCommitPayment(IPGCommitPaymentDTO params) throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public XMLResponseDTO getXMLResponse(Map postDataMap) throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void handleDeferredResponse(Map <String, String> receiptyMap, IPGResponseDTO ipgResponseDTO,
			IPGIdentificationParamsDTO ipgIdentificationParamsDTO) throws ModuleException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getPaymentGatewayName() throws ModuleException {
		return this.ipgIdentificationParamsDTO.getFQIPGConfigurationName();
	}

	/**
	 * @return the currency
	 */
	public String getCurrency() {
		return currency;
	}

	/**
	 * @param currency the currency to set
	 */
	public void setCurrency(String currency) {
		this.currency = currency;
	}

	/**
	 * @return the acquirerId
	 */
	public String getAcquirerId() {
		return acquirerId;
	}

	/**
	 * @param acquirerId the acquirerId to set
	 */
	public void setAcquirerId(String acquirerId) {
		this.acquirerId = acquirerId;
	}

	/**
	 * @return the signMethod
	 */
	public String getSignMethod() {
		return signMethod;
	}

	/**
	 * @param signMethod the signatureMethod to set
	 */
	public void setSignMethod(String signMethod) {
		this.signMethod = signMethod;
	}

	/**
	 * @return the merchantName
	 */
	public String getMerchantName() {
		return merchantName;
	}

	/**
	 * @param merchantName the merchantName to set
	 */
	public void setMerchantName(String merchantName) {
		this.merchantName = merchantName;
	}

	private int getStandardCardType(String card) {
		// [1-MASTER, 2-VISA, 3-AMEX, 4-DINNERS, 5-GENERIC]
		int mapType = 5; // default card type
		if (card != null && !card.equals("") && mapCardType.get(card) != null) {
			String mapTypeStr = (String) mapCardType.get(card);
			mapType = Integer.parseInt(mapTypeStr);
		}
		return mapType;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	public IPGRequestResultsDTO getRequestDataForAliasPayment(IPGRequestDTO ipgRequestDTO)
			throws ModuleException {
		throw new ModuleException("paymentbroker.generic.operation.notsupported");
	}

	public String convertMapToString(Map <String, ?> map) {
		return map.keySet().stream().map(key -> key + "=" + map.get(key))
				.collect(Collectors.joining(", ", "{", "}"));
	}

}
