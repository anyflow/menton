/**
 * 
 */
package net.anyflow.menton.example.twitter.model;

import java.util.Date;

/**
 * @author Park Hyunjeong
 */
public class Tweet {

	/**
	 * @return the id
	 */
	public String id() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
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

	private String id;
	private String message;
	private Date date;

	/**
	 * @return the date
	 */
	public Date date() {
		return date;
	}

	/**
	 * @param date
	 *            the date to set
	 */
	public void setDate(Date date) {
		this.date = date;
	}
}