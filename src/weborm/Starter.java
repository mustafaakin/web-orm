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
import spark.SparkBase;
import weborm.annotations.request.ContentType;
import weborm.annotations.request.Handler;
import weborm.annotations.request.RequestMethod;
import weborm.controllers.RequestHandler;

import com.google.gson.Gson;

public class Starter {
	private static final String RESPONSE_OBJECT_KEY = "^RESPONSE_OBJECT!!";

	public static void init(int port, String packageName) {
		if (port <= 0 || port >= 65536) {
			throw new IllegalArgumentException("The port number should be 1-65535, now: " + port);
		}
		SparkBase.setPort(port);

		Reflections reflections = new Reflections(packageName);
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Handler.class);

		for (Class<?> cls : annotated) {
			Handler handler = cls.getAnnotation(Handler.class);
			// It cannot be null, but whatever
			if (handler != null) {
				Route route = new Route() {
					@Override
					public Object handle(Request request, Response response) {
						long startTime = System.nanoTime();
						Object respObject = null;

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
							request.attribute(RESPONSE_OBJECT_KEY, respObject);

						} catch (SecurityException | InstantiationException | IllegalAccessException e) {
							e.printStackTrace();
						}
						long endTime = System.nanoTime();
						double diffTime = (double) (endTime - startTime) / 1000000;
						// TODO: Make it more beautiful

						String methodStr = handler.method() == RequestMethod.GET ? "GET" : "POST";
						System.out.printf("%s %s %4.3f ms\n", methodStr, handler.path(), diffTime);
						return "";
					}
				};

				System.out.printf("Mapping route: %s %s", handler.method(), handler.path());
				if (RequestMethod.GET == handler.method()) {
					Spark.get(handler.path(), route);
				} else if (RequestMethod.POST == handler.method()) {
					System.out.println("Here Post");
					Spark.post(handler.path(), route);
				} else {

				}

				Spark.after(handler.path(), new Filter() {
					public void handle(Request request, Response response) throws Exception {
						Object respObject = request.attribute(RESPONSE_OBJECT_KEY);
						if (respObject == null) {

						} else {
							// TODO: Check if response already has a type
							response.type(handler.contentType().getStr());

							if (handler.contentType() == ContentType.JSON) {
								if (respObject instanceof String) {
									// Maybe you already prepared it as a JSON
									// string
									response.body(respObject.toString());
								} else if (respObject instanceof JSONObject || respObject instanceof JSONArray) {
									response.body(respObject.toString());
								} else {
									Gson gson = new Gson();
									response.body(gson.toJson(respObject));
								}
							} else if (respObject instanceof File) {
								File file = (File) respObject;
								FileInputStream fis = null;
								try {
									HttpServletResponse raw = response.raw();
									ServletOutputStream sos = raw.getOutputStream();
									fis = new FileInputStream(file);
									// TODO: Check if modified or not from the
									// file dates etc
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
								// When all options are exhausted, just try to
								// convert it to
								// a String
								response.body(respObject.toString());
							}
						}
					};
				});

			}
		}

		Spark.exception(Exception.class, (e, req, res) -> {
			res.status(500);
			res.body(e.getMessage());
		});

	}
}
