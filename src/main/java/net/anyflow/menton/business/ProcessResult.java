/**
 * 
 */
package net.anyflow.menton.business;

/**
 * @author anyflow
 */
public class ProcessResult<Return> {

	public enum Error {

		NONE(0) {

			@Override
			public String toString() {
				return "No error.";
			}
		},
		UNKNOWN(1), ERROR_IN_BUSINESS_LOGIC(2), ERROR_IN_LIBRARY(3);

		final int value;

		Error(int value) {
			this.value = value;
		}
	}

	Return returnValue;
	Error error;

	public ProcessResult() {
		this(null, Error.NONE);
	}

	public ProcessResult(Return returnVal) {
		this(returnVal, Error.NONE);
	}

	public ProcessResult(Return returnVal, Error error) {
		this.returnValue = returnVal;
		this.error = error;
	}

	/**
	 * @return the error
	 */
	public Error getError() {
		return error;
	}

	/**
	 * @param error
	 *            the error to set
	 */
	public void setError(Error error) {
		this.error = error;
	}

	/**
	 * @return the returnValue
	 */
	public Return getReturnValue() {
		return returnValue;
	}

	/**
	 * @param returnValue
	 *            the returnValue to set
	 */
	public void setReturnValue(Return returnValue) {
		this.returnValue = returnValue;
	}
}
