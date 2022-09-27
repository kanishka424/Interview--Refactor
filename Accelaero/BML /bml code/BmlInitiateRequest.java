package com.isa.thinair.paymentbroker.core.bl.bml;

public class BmlInitiateRequest {

	private String localId;
	private String customerReference;
	private String apiVersion;
	private String merID;
	private String acqID;
	private String merRespURL;
	private String currency;
	private String orderID;
	private String signMethod;
	private String amount;
	private String signature;
	private String provider;
	private String appVersion;
	private String redirectUrl;

	public String getLocalId() {
		return localId;
	}

	public void setLocalId(String localId) {
		this.localId = localId;
	}

	public String getCustomerReference() {
		return customerReference;
	}

	public void setCustomerReference(String customerReference) {
		this.customerReference = customerReference;
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}

	public String getMerID() {
		return merID;
	}

	public void setMerID(String merID) {
		this.merID = merID;
	}

	public String getAcqID() {
		return acqID;
	}

	public void setAcqID(String acqID) {
		this.acqID = acqID;
	}

	public String getMerRespURL() {
		return merRespURL;
	}

	public void setMerRespURL(String merRespURL) {
		this.merRespURL = merRespURL;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getOrderID() {
		return orderID;
	}

	public void setOrderID(String orderID) {
		this.orderID = orderID;
	}

	public String getSignMethod() {
		return signMethod;
	}

	public void setSignMethod(String signMethod) {
		this.signMethod = signMethod;
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

}
