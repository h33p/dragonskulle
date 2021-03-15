package org.dragonskulle.game.player;

/**
 * Creates a class which contains the request to be sent
 * @author low101043
 *
 */
public class RequestClass {

	
	private RequestType requestType;
	private String request;
	private String parameters;  //TODO Maybe have this as an array -- How attack works
	private String id;
	
	public RequestClass(RequestType requestTypeGiven) {
		requestType = requestTypeGiven;
	}
	
	public void method(String method) {
		request = method;
	}
	
	
}
