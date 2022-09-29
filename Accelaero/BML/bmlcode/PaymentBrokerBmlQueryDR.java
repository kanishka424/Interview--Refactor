package com.isa.thinair.paymentbroker.core.bl.bml;

import com.google.gson.Gson;
import com.isa.thinair.commons.api.exception.ModuleException;
import com.isa.thinair.commons.core.framework.DefaultServiceResponse;
import com.isa.thinair.paymentbroker.api.dto.IPGConfigsDTO;
import com.isa.thinair.paymentbroker.api.model.CreditCardTransaction;
import com.isa.thinair.paymentbroker.core.PaymentBrokerInternalConstants;
import com.isa.thinair.paymentbroker.core.bl.PaymentBrokerTemplate;
import com.isa.thinair.paymentbroker.core.bl.PaymentQueryDR;
import com.isa.thinair.platform.api.ServiceResponce;
import com.isa.thinair.platform.api.util.PlatformUtiltiies;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;

import static com.isa.thinair.paymentbroker.api.util.PaymentConstants.*;

public class PaymentBrokerBmlQueryDR extends PaymentBrokerTemplate implements PaymentQueryDR {

	private static Log log = LogFactory.getLog(PaymentBrokerBmlQueryDR.class);

	@Override
	public ServiceResponce query(CreditCardTransaction oCreditCardTransaction, IPGConfigsDTO ipgConfigsDTO,
			PaymentBrokerInternalConstants.QUERY_CALLER caller) throws ModuleException {
		try {
			Map <String, String> headerMap = new HashMap <>();
			headerMap.put(AUTHORIZATION, ipgConfigsDTO.getAccessCode());
			final ResponseEntity <String> response = BmlPaymentUtils.getHttpRequest(null,
					MediaType.APPLICATION_JSON, headerMap,
					ipgConfigsDTO.getIpgURL() + "/" + oCreditCardTransaction.getTransactionId(),
					HttpMethod.GET, String.class);
			Gson gson = BmlPaymentUtils.getGson();
			QueryResponse queryResponseObject = gson.fromJson(response.getBody(), QueryResponse.class);
			DefaultServiceResponse bmlServiceResponse = new DefaultServiceResponse(true);
			bmlServiceResponse.addResponceParam(QUERY_DR_RESPONSE_OBJECT, queryResponseObject);
			bmlServiceResponse.addResponceParam(BML_ID, queryResponseObject.getId());
			bmlServiceResponse.addResponceParam(BmlRequest.CURRENCY,
					PlatformUtiltiies.nullHandler(queryResponseObject.getCurrency()));
			bmlServiceResponse.addResponceParam(BmlRequest.API_KEY,
					PlatformUtiltiies.nullHandler(ipgConfigsDTO.getAccessCode()));
			if (queryResponseObject.getPaddedCardNumber() != null
					&& queryResponseObject.getPaddedCardNumber().length() > 4) {
				bmlServiceResponse.addResponceParam(CC_LAST4_DIGITS, queryResponseObject.getPaddedCardNumber()
						.substring(queryResponseObject.getPaddedCardNumber().length() - 4));
			}
			bmlServiceResponse.addResponceParam(TRANSACTION_REF_COMPANY, queryResponseObject.getMerchantId());
			auditTransaction(oCreditCardTransaction.getTransactionReference(),
					queryResponseObject.getMerchantId(), oCreditCardTransaction.getTempReferenceNum(),
					oCreditCardTransaction.getTemporyPaymentId(), bundle.getString(SERVICETYPE_QUERY),
					ipgConfigsDTO.getIpgURL() + "/" + oCreditCardTransaction.getTransactionId(),
					response.getBody(), oCreditCardTransaction.getPaymentGatewayConfigurationName(),
					oCreditCardTransaction.getTransactionId(), true);
			bmlServiceResponse.setSuccess(
					queryResponseObject.TRANSACTION_APPROVED.equals(queryResponseObject.getState()));
			return bmlServiceResponse;
		} catch (HttpClientErrorException e) {
			log.error("responseBody --->" + e.getResponseBodyAsString() + "...............response status: "
					+ e.getStatusCode().toString() + "...............response message " + e.getMessage());
			auditTransactionAtOnce(null, oCreditCardTransaction.getTransactionId(),
					bundle.getString(SERVICETYPE_QUERY),
					ipgConfigsDTO.getIpgURL() + "/" + oCreditCardTransaction.getTransactionId(),
					e.getResponseBodyAsString(), getPaymentGatewayName(), null,
					"HttpClientErrorException occured wile getting query response", null, false);
			throw new ModuleException("paymentbroker.result.null");

		} catch (Exception ignored) {
			auditTransactionAtOnce(null, oCreditCardTransaction.getTransactionId(),
					bundle.getString(SERVICETYPE_QUERY),
					ipgConfigsDTO.getIpgURL() + "/" + oCreditCardTransaction.getTransactionId(),
					ignored.getMessage(), getPaymentGatewayName(), null,
					"HttpClientErrorException occured wile getting query response", null, false);
			throw new ModuleException("paymentbroker.result.null");
		}
	}

	@Override
	public String getPaymentGatewayName() throws ModuleException {
		// TODO Auto-generated method stub
		return null;
	}

}
