package com.isa.thinair.webplatform.api.base;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.interceptor.ServletRequestAware;

import com.isa.thinair.airreservation.api.dto.TrackInfoDTO;
import com.isa.thinair.commons.api.dto.ClientCommonInfoDTO;
import com.isa.thinair.commons.core.security.UserPrincipal;
import com.isa.thinair.commons.core.util.AppSysParamsUtil;
import com.isa.thinair.webplatform.api.util.Constants;
import com.isa.thinair.webplatform.api.v2.util.TrackInfoUtil;

/**
 * Base Class for S2 HttpServletRequest Support
 * 
 * @author Lasantha Pambagoda
 */
public class BaseRequestAwareAction implements ServletRequestAware {

	protected HttpServletRequest request;

	@Override
	public void setServletRequest(HttpServletRequest request) {
		this.request = request;
	}

	public ClientCommonInfoDTO getClientInfoDTO() {
		ClientCommonInfoDTO clientInfoDTO = new ClientCommonInfoDTO();
		// clientIps = clientIP, proxyIP1, proxyIP2
		String clientIps = request.getHeader("X-Forwarded-For");
		String clientIp = null;

		if (clientIps != null && !clientIps.trim().isEmpty()) {
			String[] clientIpArray = clientIps.split(Constants.COMMA_SEPARATOR);
			if (clientIpArray.length > 0) {
				for (String ipAddress : clientIpArray) {
					if (ipAddress != null && !ipAddress.trim().isEmpty()) {
						clientIp = ipAddress.trim();
						break;
					}
				}
			}

		}

		if (clientIp == null || clientIp.equals("")) {
			clientIp = request.getRemoteAddr();
		}
		clientInfoDTO.setIpAddress(clientIp);
		clientInfoDTO.setFwdIpAddress(request.getHeader(""));
		clientInfoDTO.setUrl(request.getRequestURL().toString());
		clientInfoDTO.setBrowser(request.getHeader("User-Agent"));
		UserPrincipal userPrincipal = (UserPrincipal) request.getUserPrincipal();
		// DO not pass the defaultCarrier directly here. It will affect the dry user functionalities.Insted use the
		// userPrincipal.getDefaultCarrierCode
		// if it's not null
		String defaultCArrier = (userPrincipal == null) ? AppSysParamsUtil.getDefaultCarrierCode() : userPrincipal
				.getDefaultCarrierCode();
		clientInfoDTO.setCarrierCode(defaultCArrier);
		return clientInfoDTO;
	}

	protected TrackInfoDTO getTrackInfo() {
		TrackInfoDTO trackInfoDTO = new TrackInfoDTO();
		trackInfoDTO.setIpAddress(getClientInfoDTO().getIpAddress());
		trackInfoDTO.setCarrierCode(AppSysParamsUtil.getDefaultCarrierCode());
		trackInfoDTO = TrackInfoUtil.updateTrackInfo(trackInfoDTO, request);
		return trackInfoDTO;
	}

}
