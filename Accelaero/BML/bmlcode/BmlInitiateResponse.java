package com.isa.thinair.paymentbroker.core.bl.bml;

public class BmlInitiateResponse {

	private String url;
	private String id;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		StringBuilder strLog = new StringBuilder();
		strLog.append("BmlInitiateResponse{");
		strLog.append("url='").append(url).append('\'');
		strLog.append(", id='").append(id).append('\'');
		strLog.append("'}'");
		return strLog.toString();

	}

}