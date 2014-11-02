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
import org.slf4j.LoggerFactory;

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
    final static org.slf4j.Logger log = LoggerFactory.getLogger(Starter.class);

	private static final String RESPONSE_OBJECT_KEY = "RES_OBJECT";
	private static final String RESPONSE_TIME_KEY = "RES_TIME";
	
	private static Route getRoute(Class<?> cls, Handler handler) {
		Route route = new Route() {
			@Override
			public Object handle(Request request, Response response) {
				request.attribute(RESPONSE_TIME_KEY, System.nanoTime());
				
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
				
				return "";
			}
		};
		return route;
	}

	private static Filter getFilter(Handler handler) {
		Filter filter = new Filter() {
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
				Object time = request.attribute(RESPONSE_TIME_KEY);
				if ( time instanceof Long){
					Long start = (Long) time;
					Long end = System.nanoTime();
					double  diff = (double) ( (end - start) / 1000000.0);
					String msg = String.format("%s %s %.2f ms", handler.method(), handler.path(), diff);
					log.info(msg);
				}
			};
		};
		return filter;
	}

	public static void init(int port, String packageName) {

		if (port <= 0 || port >= 65536) {
			throw new IllegalArgumentException("The port number should be 1-65535, now: " + port);
		}
		SparkBase.setPort(port);

		Reflections reflections = new Reflections(packageName);
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Handler.class);

		for (Class<?> cls : annotated) {
			Handler handler = cls.getAnnotation(Handler.class);
			log.info("Mapping route: " + handler.method()  +  " " + handler.path());
			
			Route route = getRoute(cls, handler);
			if (RequestMethod.GET == handler.method()) {
				Spark.get(handler.path(), route);
			} else if (RequestMethod.POST == handler.method()) {
				Spark.post(handler.path(), route);
			} else {

			}
			Spark.after(handler.path(), getFilter(handler));
		}

		Spark.exception(Exception.class, (e, req, res) -> {
			res.status(500);
			// TODO: Show stack trace also
			e.printStackTrace();
			res.body(e.getCause() + " " + e.getMessage());
		});
	}
}
