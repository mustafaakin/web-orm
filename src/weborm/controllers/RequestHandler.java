package weborm.controllers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import spark.Request;
import spark.Response;
import weborm.annotations.inputs.Length;
import weborm.annotations.inputs.NotNull;
import weborm.annotations.inputs.Range;

public abstract class RequestHandler {
	private HashMap<String, ArrayList<String>> validation_messages = new HashMap<String, ArrayList<String>>();

	public HashMap<String, ArrayList<String>> getValidation_messages() {
		return validation_messages;
	}

	public boolean isValid() {
		validation_messages.clear();

		Class<? extends RequestHandler> cls = this.getClass();
		Field[] fields = cls.getFields();

		for (Field field : fields) {
			String fieldName = field.getName();
			Object obj = null;
			if (fieldName.equals("validation_messages")) {
				continue;
			}

			try {
				obj = field.get(this);
			} catch (IllegalAccessException ex) {
				ex.printStackTrace();
			}

			NotNull nn = field.getAnnotation(NotNull.class);
			if (nn != null) {
				if (obj == null) {
					addMessage(fieldName, nn.message(), obj);
				}
			}

			Range range = field.getAnnotation(Range.class);
			if (range != null) {
				// TODO: Check if it is a int, double, etc.. (number)
				try {
					double value = Double.parseDouble(obj.toString());
					if (range.max() < value) {
						addMessage(fieldName, "The value is larger than max value: " + range.max(), obj);
					} else if (range.min() > value) {
						addMessage(fieldName, "The value is smaller than min value: " + range.min(), obj);
					}
				} catch (Exception ex) {
					// Do not mind yet..
				}
			}

			Length length = field.getAnnotation(Length.class);
			if (length != null) {
				String str = obj.toString();
				if (str != null) {
					int value = str.length();
					if (length.max() < value) {
						addMessage(fieldName, "The value is larger than max value: " + range.max(), obj);
					} else if (length.min() > value) {
						addMessage(fieldName, "The value is smaller than min value: " + length.min(), obj);
					}
				} else {

				}
			}
		}

		return validation_messages.size() == 0;
	}

	private void addMessage(String fieldName, String message, Object actualValue) {
		if (!validation_messages.containsKey(fieldName)) {
			validation_messages.put(fieldName, new ArrayList<String>());
		}
		ArrayList<String> fieldMessages = validation_messages.get(fieldName);
		fieldMessages.add(message + " , value: " + actualValue);
	}

	private void putToField(Field field, String value) {
		try {
			Class<?> fieldCls = field.getType();
			if (fieldCls.equals(String.class)) {
				field.set(this, value);
			} else if (fieldCls.equals(Integer.class)) {
				field.set(this, Integer.parseInt(value));
			} else if (fieldCls.equals(Double.class)) {
				field.set(this, Double.parseDouble(value));
			} else {
				// TODO: all the types, or non parsable things Handle
				// later
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void build(Request request) {
		Class<? extends RequestHandler> cls = this.getClass();

		// Map to existing fields, therefore don't fail when there extra
		// parameters arrives
		Field[] fields = cls.getFields();

		// The
		Map<String, String[]> queryMap = request.queryMap().toMap();
		Map<String, String> requestParams = request.params();

		for (Field field : fields) {
			String name = field.getName();
			// Because they are mapped as /someurl/path/:param/get/info/etc
			String reqParamName = ":" + name;
			if (requestParams.containsKey(reqParamName)) {
				String value = requestParams.get(reqParamName);
				putToField(field, value);
			} else if (queryMap.containsKey(name)) {
				String[] value = queryMap.get(name);
				if (value != null) {
					if (value.length == 1) {
						putToField(field, value[0]);
					} else {
						// TODO: Handle later, get class check if list etc
						throw new UnsupportedOperationException("The array values not implemented yet.");
					}
				}
			} else {
				System.out.println("The field does not exist anywhere" + name);
			}
		}
	}

	public abstract Object handle(Request request, Response response);
}