package com.isa.thinair.paymentbroker.core.bl.bml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.isa.thinair.commons.api.exception.ModuleException;
import com.isa.thinair.paymentbroker.api.dto.IPGConfigsDTO;
import com.isa.thinair.paymentbroker.api.dto.IPGRequestDTO;
import com.isa.thinair.paymentbroker.api.dto.IPGRequestResultsDTO;
import com.isa.thinair.paymentbroker.api.model.CreditCardTransaction;
import com.isa.thinair.paymentbroker.api.util.PaymentConstants;
import com.isa.thinair.paymentbroker.core.util.PaymentBrokerUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;

import static com.isa.thinair.paymentbroker.api.util.PaymentConstants.AUTHORIZATION;
import static com.isa.thinair.paymentbroker.api.util.PaymentConstants.SERVICETYPE_AUTHORIZE;

public class PaymentBrokerBmlImpl extends PaymentBrokerBmlTemplate {
	private static Log log = LogFactory.getLog(PaymentBrokerBmlImpl.class);

	public IPGRequestResultsDTO getRequestData(IPGRequestDTO ipgRequestDTO) throws ModuleException {

		final String RETURN_URL = ipgRequestDTO.getReturnUrl();
		String finalAmount = BmlPaymentUtils.formatAmount(ipgRequestDTO.getAmount());
		String merchantTxnId = composeMerchantTransactionId(ipgRequestDTO.getApplicationIndicator(),//debug and see the values of all parmeters
				ipgRequestDTO.getApplicationTransactionId(), PaymentConstants.MODE_OF_SERVICE.PAYMENT);
		IPGRequestResultsDTO ipgRequestResultsDTO = new IPGRequestResultsDTO();

		BmlInitiateRequest bmlInitiateRequest = new BmlInitiateRequest();
		bmlInitiateRequest.setApiVersion(getVersion());
		bmlInitiateRequest.setMerID(getMerchantId());
		bmlInitiateRequest.setAcqID(getAcquirerId());
		bmlInitiateRequest.setCurrency(getCurrency());
		bmlInitiateRequest.setSignMethod(getSignMethod());
		bmlInitiateRequest.setAmount(finalAmount);
		bmlInitiateRequest.setSignature(BmlPaymentUtils.computeSHA1(finalAmount, getCurrency(), getApiKey()));
		bmlInitiateRequest.setProvider(getMerchantId());
		bmlInitiateRequest.setRedirectUrl(RETURN_URL);

		IPGConfigsDTO ipgConfigsDTO = new IPGConfigsDTO();
		ipgConfigsDTO.setAccessCode(getApiKey());
		ObjectMapper oMapper = new ObjectMapper();
		Map <String, String> postDataMap = oMapper.convertValue(bmlInitiateRequest, Map.class);
		String strRequestParams = PaymentBrokerUtils.getRequestDataAsString(postDataMap);
		String sessionID = ipgRequestDTO.getSessionID() == null ? "" : ipgRequestDTO.getSessionID();

		if (log.isDebugEnabled()) {
			log.debug("BML Request Data : " + strRequestParams + ", sessionID : " + sessionID);
		}
		Gson gson = BmlPaymentUtils.getGson();
		Map <String, String> headerMap = new HashMap <>();
		headerMap.put(AUTHORIZATION, getApiKey());
		try {
			final ResponseEntity <String> response = BmlPaymentUtils.getHttpRequest(
					gson.toJson(bmlInitiateRequest), MediaType.APPLICATION_JSON, headerMap, getIpgURL(),
					HttpMethod.POST, String.class);
			if (response != null) {
				log.debug("responseBody --->" + response.getBody() + "...............response status: "
						+ response.getStatusCode());
				BmlInitiateResponse bmlInitiateResponseObject = gson.fromJson(response.getBody(),
						BmlInitiateResponse.class);
				CreditCardTransaction ccTransaction = auditTransaction(ipgRequestDTO.getPnr(),
						getMerchantId(), merchantTxnId,
						new Integer(ipgRequestDTO.getApplicationTransactionId()),
						bundle.getString(SERVICETYPE_AUTHORIZE), strRequestParams + "," + sessionID,
						response.getBody(), getPaymentGatewayName(), bmlInitiateResponseObject.getId(),
						false);
				ipgRequestResultsDTO.setRequestData(bmlInitiateResponseObject.getUrl());
				ipgRequestResultsDTO.setPaymentBrokerRefNo(ccTransaction.getTransactionRefNo());
				ipgRequestResultsDTO.setAccelAeroTransactionRef(merchantTxnId);
				ipgRequestResultsDTO.setPostDataMap(postDataMap);
				return ipgRequestResultsDTO;
			} else {
				throw new NullPointerException("the response is null");
			}
		} catch (HttpClientErrorException e) {
			log.error("responseBody --->" + e.getResponseBodyAsString() + "...............response status: "
					+ e.getStatusCode().toString() + "...............response message " + e.getMessage());
			auditTransactionAtOnce(getMerchantId(), merchantTxnId, bundle.getString(SERVICETYPE_AUTHORIZE),
					strRequestParams + "," + sessionID, e.getResponseBodyAsString(), getPaymentGatewayName(),
					null, "HttpClientErrorException occured", null, false);
			ipgRequestResultsDTO.setErrorCode(e.getStatusCode().toString());
			throw new ModuleException("paymentbroker.result.null");
		} catch (Exception ignored) {
			auditTransactionAtOnce(getMerchantId(), merchantTxnId, bundle.getString(SERVICETYPE_AUTHORIZE),
					strRequestParams + "," + sessionID, ignored.getMessage(), getPaymentGatewayName(), null,
					"Exception Occured", null, false);
			log.error("Error when postForObject", ignored);
			throw new ModuleException("paymentbroker.result.null");
		}
	}

}
