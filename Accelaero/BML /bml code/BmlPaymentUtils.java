package com.isa.thinair.paymentbroker.core.bl.bml;

import com.google.gson.Gson;
import com.isa.thinair.paymentbroker.api.dto.IPGConfigsDTO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Properties;

public class BmlPaymentUtils {

	private BmlPaymentUtils() {
	}

	private static Log log = LogFactory.getLog(BmlPaymentUtils.class);

	public static String computeSHA1(String amount, String currency, String apiKey) {

		String candidate = amount + currency + apiKey;
		try {
			return computeSHA1(candidate);
		} catch (Exception e) {
			log.debug("[computeSHA1] Error computing SHA-1 : " + e.toString());
		}
		return null;
	}

	public static String computeSHA1(String text)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {

		MessageDigest md;
		md = MessageDigest.getInstance("SHA-1");
		md.update(text.getBytes("iso-8859-1"), 0, text.length());
		byte[] sha1hash = md.digest();
		return convertToHex(sha1hash);

	}

	public static String formatAmount(String finalAmount) {

		StringBuilder sb = new StringBuilder(finalAmount);
		int decimalPointindex = 0;
		if (sb.indexOf(".") > 0) {
			decimalPointindex = finalAmount.indexOf(".");
			sb = sb.replace(decimalPointindex, decimalPointindex + 1, "");
		}
		while (sb.length() < 12) {
			sb = sb.insert(0, "0");
		}
		return sb.toString();
	}

	public static boolean validateHASH(BmlResponse resp, Properties props, String amount) {
		//(amount+currency+APIKEY)
		String candidate =
				amount + props.getProperty(BmlRequest.CURRENCY) + props.getProperty(BmlRequest.API_KEY);
		String hash = "";
		try {

			hash = computeSHA1(candidate);

		} catch (Exception e) {
			log.debug("[validateHASH] Error computing SHA-1 : " + e.toString());
		}

		return hash.equals(resp.getReqSignature());
	}

	private static String convertToHex(byte[] data) {

		StringBuilder buf = new StringBuilder();

		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();

	}

	public static IPGConfigsDTO getIPGConfigs(String merchantID, String password) {

		IPGConfigsDTO iPGConfigsDTO = new IPGConfigsDTO();
		iPGConfigsDTO.setMerchantID(merchantID);
		iPGConfigsDTO.setPassword(password);
		return iPGConfigsDTO;
	}

	public static <T> ResponseEntity <T> getHttpRequest(String json, MediaType mediaType,
			Map <String, String> headerValues, String url, HttpMethod method, Class <T> responseType)
			throws HttpClientErrorException {

		HttpHeaders headers = new HttpHeaders();
		for (Map.Entry <String, String> headerEntry : headerValues.entrySet()) {
			headers.set(headerEntry.getKey(), headerEntry.getValue());
		}
		RestTemplate restTemplate = new RestTemplate();
		HttpEntity <Object> requestEntity = new HttpEntity <>(json, headers);
		return restTemplate.exchange(url, method, requestEntity, responseType);
	}

	public static Gson getGson() {
		return new Gson();
	}
}