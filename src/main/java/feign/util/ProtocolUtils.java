package feign.util;

import feign.enumerate.ProtocolType;

public class ProtocolUtils {

	public static String appendToUrl(String protocolName,String url) {
		for(ProtocolType protocolTypeEnum : ProtocolType.values()) {
			if (protocolTypeEnum.getName().equals(protocolName) && !url.startsWith(protocolTypeEnum.getName())) {
				return protocolName + "://" + url;
			}
		}
		return url;
	}
	
	public static String appendToUrl(ProtocolType protocolType,String url) {
		for(ProtocolType protocolTypeEnum : ProtocolType.values()) {
			if (protocolTypeEnum == protocolType && !url.startsWith(protocolTypeEnum.getName())) {
				return protocolType.getName() + "://" + url;
			}
		}
		return url;
	}
	
	public static boolean isSupportedProtocol(ProtocolType protocolType) {
		for (ProtocolType protocolTypeEnum : ProtocolType.values()) {
			if (protocolTypeEnum == protocolType) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean containsSupportedProtocol(String url) {
		for (ProtocolType protocolTypeEnum : ProtocolType.values()) {
			if (url.startsWith(protocolTypeEnum.getName())) {
				return true;
			}
		}
		return false;
	}
	
	public static ProtocolType getProtocol(String url) {
		for (ProtocolType protocolTypeEnum : ProtocolType.values()) {
			if (url.substring(0, url.indexOf("://")).equals(protocolTypeEnum.getName()))
				return protocolTypeEnum;
		}
		return null;
	}
	
}
