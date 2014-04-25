/**
 * 
 */
package net.anyflow.menton.example.twitter.model;

/**
 * @author Park Hyunjeong
 */
public class Error {

	private String message;

	public Error(String message) {
		this.message = message;
	}

	/**
	 * @return the message
	 */
	public String message() {
		return message;
	}

	/**
	 * @param message
	 *            the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
}