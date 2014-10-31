package weborm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.Reflections;

import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import weborm.annotations.request.ContentType;
import weborm.annotations.request.Handler;
import weborm.annotations.request.RequestMethod;
import weborm.controllers.FileRequestHandler;
import weborm.controllers.RequestHandler;

import com.google.gson.Gson;

public class Starter {
	public static void init(int port, String packageName){
		if ( port <= 0 || port >= 65536){
			throw new IllegalArgumentException("The port number should be 1-65535, now: " + port);
		}
		Spark.setPort(port);
				
		Reflections reflections = new Reflections(packageName);
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Handler.class);

		for (Class<?> cls : annotated) {
			Handler handler = cls.getAnnotation(Handler.class);
			if (handler != null) {
				if (handler.contentType() == ContentType.FILE) {
					// TODO: Handle later by direct interaction to Jetty
					Spark.after(handler.path(), new Filter() {
						@Override
						public void handle(Request request, Response response) throws Exception {
							Object respObject = null;
							HttpServletResponse raw = response.raw();
							ServletOutputStream sos = raw.getOutputStream();

							FileRequestHandler requestHandler = (FileRequestHandler) cls.newInstance();
							if (requestHandler != null) {
								requestHandler.build(request);
								if (requestHandler.isValid()) {
									respObject = requestHandler.handle(request, response);
								} else {
									response.status(400); // Bad Request
									respObject = requestHandler.getValidation_messages();
								}
							}
							// TODO: Check stream
							if (respObject instanceof File) {
								File file = (File) respObject;
								FileInputStream fis = null;
								try {
									fis = new FileInputStream(file);									
									// TODO: Check if modified or not from the file dates etc
									// TODO: Put mime type from file, or create new class extending requesthandler
									response.body("");
									if ( requestHandler.contentType != null){
										response.type(requestHandler.contentType);										
									} else {
										response.type("text/html");
									}
									response.status(200);
									IOUtils.copy(fis, sos);									
								} catch (IOException e) {
									e.printStackTrace();
								} finally {
									try {
										if (fis != null)
											fis.close();
									} catch (IOException ex) {
										ex.printStackTrace();
									}
								}
							} else {
								response.body(respObject.toString());
							}
						}
					});
				} else {
					Route route = new Route() {
						@Override
						public Object handle(Request request, Response response) {
							long startTime = System.nanoTime();
							Object finalObject = null;
							Object respObject = null;
							
							response.type(handler.contentType().getStr());
							try {
								RequestHandler requestHandler = (RequestHandler) cls.newInstance();
								if (requestHandler != null) {
									requestHandler.build(request);
									if (requestHandler.isValid()) {
										respObject = requestHandler.handle(request, response);
									} else {
										response.status(400); // Bad Request
										respObject = requestHandler.getValidation_messages();
									}
								}
								if (respObject == null) {
									// TODO: Causes an error!!
									return null;
								} else {
									// TODO: Check our mongodb entity types for
									// converting to JSON also
									if (handler.contentType() == ContentType.JSON) {
										if (respObject instanceof JSONObject || respObject instanceof JSONArray) {
											finalObject = respObject.toString();
										} else {
											Gson gson = new Gson();
											finalObject = gson.toJson(respObject);
										}
									} else {
										finalObject = respObject.toString();
									}
								}
							} catch (SecurityException | InstantiationException | IllegalAccessException e) {
								e.printStackTrace();
							}
							long endTime = System.nanoTime();
							double diffTime = (double) (endTime - startTime) / 1000000;
							// TODO: Make it more beautiful
							String methodStr = handler.method() == RequestMethod.GET ? "GET" : "POST";
							System.out.printf("%s %s %4.3f ms\n", methodStr, handler.path(), diffTime);
							return finalObject;
						}
					};

					switch (handler.method()) {
					case GET:
						Spark.get(handler.path(), route);
						break;
					case POST:
						Spark.post(handler.path(), route);
						break;
					default:
						break;

					}
				}
			}
		}

		Spark.exception(Exception.class, (e, req, res) -> {
			res.status(500);
			res.body(e.getMessage());
		});
		
	}
}
