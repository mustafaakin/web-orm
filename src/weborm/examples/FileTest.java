package weborm.examples;

import java.io.File;

import spark.Request;
import spark.Response;
import weborm.annotations.inputs.NotNull;
import weborm.annotations.inputs.Range;
import weborm.annotations.request.ContentType;
import weborm.annotations.request.Handler;
import weborm.annotations.request.RequestMethod;
import weborm.controllers.RequestHandler;

@Handler(path = "/fileTest", method = RequestMethod.GET, contentType = ContentType.FILE)
public class FileTest extends RequestHandler {
	
	@Override
	public Object handle(Request request, Response response) {		
		File file = new File("C:/setup.log"); 
		return file;
	}
}
