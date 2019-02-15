package feign.util;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

public class URLUtils {
	//解析出URL中指定参数的值
	public static String resolveParam(String url,String paramKey) {
		//定义正则表达式
		String pattern = "(\\?|&){1}#{0,1}" + paramKey + "=[a-zA-Z0-9.]*(&{0,1})";
	    Pattern r = Pattern.compile(pattern);
	    Matcher m = r.matcher(url);
	    //如果匹配到相应字段
	    if (m.find( )) {
	        return m.group(0).split("=")[1].replace("&", "");
	    } else {
	        return "";
	    }
	}
	
	//解析出URL中的所有参数
	public static Map<String,String> resolveAllParams(String url) {
		Map<String,String> params = null;
		//判断url是否为空字符串
		if (!StringUtils.isEmpty(url.trim())) {
			//使用?对URL参数进行分割
			String[] urlSplitedByQUES = url.trim().split("[?]");
			//如果在使用?分割url之后，在？后面的字符串不为空，那么可以获取参数
			if (urlSplitedByQUES.length > 1 && !StringUtils.isEmpty(urlSplitedByQUES[1])) {
				params = Arrays.asList(urlSplitedByQUES[1].split("[&]")).stream()
						.map(paramStr -> paramStr.split("[=]"))
						.filter(param -> param.length>1)
						.collect(Collectors.toMap(param->param[0], param->param[1], (k,v)->v));
			}
		}
		return params;
	}
	
	
	public static void main(String[] args) {
		String url1 = "dubbo://service?interface=com.migu.test&version=1.0&test";
		String url2 = "dubbo://service?interface=com.migu.test&version=1.0&test=";
		String url3 = "dubbo://service?interface=com.migu.test";
		Map<String,String> resutl1 = URLUtils.resolveAllParams(url1);
		Map<String,String> resutl2 = URLUtils.resolveAllParams(url2);
		Map<String,String> resutl3 = URLUtils.resolveAllParams(url3);
		//resutl1.forEach((k,v)->System.out.println("Key:"+k+", Value:" +v));
		//resutl2.forEach((k,v)->System.out.println("Key:"+k+", Value:" +v));
		
		System.out.println(URLUtils.resolveParam(url3,"interface"));
	}
}
