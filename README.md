web-orm
=======

A Java web framework that allows you map request with parameters to actual Plain Java Objects

## Example

A simple handler class in the `weborm.examples` package.

```java
@Handler(path = "/test/:hello", method = RequestMethod.GET, contentType = ContentType.TEXT)
public class HelloWorld extends RequestHandler {
	@NotNull
	public String hello;

	@NotNull
	public String name;

	@NotNull
	@Range(max = 50, min = 18)
	public Integer age;

	@Override
	public Object handle(Request request, Response response) {
		return hello + " world:" + name + ", " + age;
	}	
}
```

When you execute the following in a main method, it will parse the all classes annotated with `Handler` and extends the `RequestHandler` class and initialize them using the (SparkJava)[sparkjava.com] framework.

```java
Starter.init(4000, "weborm.examples");
```
		
## TODO

- Handle file uploads