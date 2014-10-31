package weborm.annotations.request;

public enum ContentType {
	JSON("application/json"), TEXT("text/plain"), HTML("text/html"), FILE("application/octetstream");
	
	String str;
	ContentType(String str){
		this.str = str;
	}
	
	public String getStr() {
		return str;
	}
}
