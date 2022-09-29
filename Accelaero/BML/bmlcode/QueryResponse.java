package com.isa.thinair.paymentbroker.core.bl.bml;

public class QueryResponse {
	public static final String TRANSACTION_APPROVED = "CONFIRMED";

	public static final String VALID_HASH = "VALID_HASH";
	public static final String INVALID_HASH = "INVALID_HASH";
	public static final String INVALID_RESPONSE = "INVALID_RESPONSE";
	public static final String NO_VALUE = "";

	public static final String AMOUNT = "amount";
	public static final String CURRENCY = "currency";
	private String id;
	private String currency;
	private String state;
	private String amountAsDecimal;
	private String paddedCardNumber;
	private String merchantId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getAmountAsDecimal() {
		return amountAsDecimal;
	}

	public void setAmountAsDecimal(String amountAsDecimal) {
		this.amountAsDecimal = amountAsDecimal;
	}

	public String getPaddedCardNumber() {
		return paddedCardNumber;
	}

	public void setPaddedCardNumber(String paddedCardNumber) {
		this.paddedCardNumber = paddedCardNumber;
	}

	public String getMerchantId() {
		return merchantId;
	}

	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}
}
