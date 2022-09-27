package com.isa.thinair.paymentbroker.core.bl.bml;

import com.isa.thinair.commons.core.framework.DefaultServiceResponse;

public class ApprovedTransactionStatus {

	int tnxResultCode = 0;
	String status = "";
	String errorSpecification = "";
	String responseMismatch = "";
	DefaultServiceResponse bmlServiceResponse = null;

	public int getTnxResultCode() {
		return tnxResultCode;
	}

	public void setTnxResultCode(int tnxResultCode) {
		this.tnxResultCode = tnxResultCode;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getErrorSpecification() {
		return errorSpecification;
	}

	public void setErrorSpecification(String errorSpecification) {
		this.errorSpecification = errorSpecification;
	}

	public String getResponseMismatch() {
		return responseMismatch;
	}

	public void setResponseMismatch(String responseMismatch) {
		this.responseMismatch = responseMismatch;
	}

	public DefaultServiceResponse getBmlServiceResponse() {
		return bmlServiceResponse;
	}

	public void setBmlServiceResponse(DefaultServiceResponse bmlServiceResponse) {
		this.bmlServiceResponse = bmlServiceResponse;
	}

}
