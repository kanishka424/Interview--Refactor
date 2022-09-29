package com.isa.thinair.paymentbroker.core.bl.bml;

public class BmlResponse {

	public static final String TRANSACTION_APPROVED = "CONFIRMED";
	public static final String TRANSACTION_CANCELLED = "CANCELLED";
	public static final String TRANSACTION_REJECTED = "TRANSACTION_REJECTED";

	public static final String VALID_HASH = "VALID_HASH";
	public static final String INVALID_HASH = "INVALID_HASH";

	public static final String INVALID_RESPONSE = "INVALID_RESPONSE";
	public static final String NO_VALUE = "";

	public static final String RESPONSE_QUERY_NULL = "RESPONSE_QUERY_NULL";
	private String transactionId;
	private String reqSignature;
	private String ipgSessionId;
	private String state;

	@Override
	public String toString() {
		StringBuilder strLog = new StringBuilder();
		strLog.append("BmlResponse{");
		strLog.append("transactionId='").append(transactionId).append('\'');
		strLog.append(", reqSignature='").append(reqSignature).append('\'');
		strLog.append(", ipgSessionId='").append(ipgSessionId).append('\'');
		strLog.append(", state='").append(state).append('\'');
		return strLog.toString();
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}

	public String getReqSignature() {
		return reqSignature;
	}

	public void setReqSignature(String reqSignature) {
		this.reqSignature = reqSignature;
	}

	public String getIpgSessionId() {
		return ipgSessionId;
	}

	public void setIpgSessionId(String ipgSessionId) {
		this.ipgSessionId = ipgSessionId;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
}
