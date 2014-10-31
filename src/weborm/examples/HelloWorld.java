package weborm.examples;

import spark.Request;
import spark.Response;
import weborm.annotations.inputs.NotNull;
import weborm.annotations.inputs.Range;
import weborm.annotations.request.ContentType;
import weborm.annotations.request.Handler;
import weborm.annotations.request.RequestMethod;
import weborm.controllers.RequestHandler;


@Handler(path = "/:hello", method = RequestMethod.GET, contentType=ContentType.JSON)
public class HelloWorld extends RequestHandler {
	@NotNull
	public String hello;
	
	@NotNull
	public String name;

	@weborm.annotations.inputs.NotNull
	@Range(max = 50, min = 18)
	public Integer age;

	@Override
	public Object handle(Request request, Response response) {
		return hello + " world:" + name + ", " + age;
	}

}
