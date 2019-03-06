package feign.converter;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import feign.util.BytesConversionUtils;
import feign.util.ProtocolUtils;

public class RpcMessageConverter<T>{
	
	protected boolean supports(Class<?> clazz) {
		//对于任何类型的Class都支持转换
		return true;
	}
	
	public boolean canRead(Class<?> clazz, String protocolValue) {
		return supports(clazz) && canRead(protocolValue);
	}
	
	protected boolean canRead(String protocolValue) {
		if (protocolValue == null) {
			return true;
		}
		if (ProtocolUtils.containsSupportedProtocol(protocolValue))
			return true;
		return false;
	}
	
	public boolean canWrite(Class<?> clazz, String protocolValue) {
		return supports(clazz) && canWrite(protocolValue);
	}
	
	protected boolean canWrite(String protocolValue) {
		if (protocolValue == null) {
			return true;
		}
		if (ProtocolUtils.containsSupportedProtocol(protocolValue))
			return true;
		return false;
	}
	
	public final T read(Class<? extends T> clazz, HttpInputMessage inputMessage) throws IOException {
		return readInternal(clazz, inputMessage);
	}

	protected T readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		InputStream input = inputMessage.getBody();
		T obj = (T) BytesConversionUtils.toObject(input);
		return obj;
	}

	public final void write(final T t, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		writeInternal(t, outputMessage);
		
//		final HttpHeaders headers = outputMessage.getHeaders();
//		addDefaultHeaders(headers, t, contentType);
//
//		if (outputMessage instanceof StreamingHttpOutputMessage) {
//			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
//			streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
//				@Override
//				public void writeTo(final OutputStream outputStream) throws IOException {
//					writeInternal(t, new HttpOutputMessage() {
//						@Override
//						public OutputStream getBody() throws IOException {
//							return outputStream;
//						}
//						@Override
//						public HttpHeaders getHeaders() {
//							return headers;
//						}
//					});
//				}
//			});
//		}
//		else {
//			writeInternal(t, outputMessage);
//			outputMessage.getBody().flush();
//		}
	}
	
	protected void writeInternal(Object t, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		// TODO Auto-generated method stub
		
	}
	
}
