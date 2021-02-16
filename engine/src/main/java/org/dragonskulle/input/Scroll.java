package org.dragonskulle.input;

import lombok.Getter;

/**
 * Records the amount of mouse scrolling and can be used to access the assigned button {@link Integer} values used in {@link Buttons}.
 * @author Craig Wilbourne
 */
class Scroll {
	
	/** An arbitrary value that is used as a key code for scrolling up, to treat scrolling like a button press. */
	static final Integer UP = -777;
	/** An arbitrary value that is used as a key code for scrolling down, to treat scrolling like a button press. */
	static final Integer DOWN = -776;
	
	/** The total sum of the amount of scrolling done since the last call to {@link reset()}.*/
	@Getter private double amount = 0;

	/**
	 * Add to the total amount of scrolling done.
	 * @param value The value to add.
	 */
	void add(double value) {
		amount += value;
	}
	
	/**
	 * Resets the value stored in {@link #amount}. <br>
	 * Needs to be called frequently to ensure that the system understands when no mouse scrolling is occurring.
	 */
	void reset() {
		amount = 0;
	}
	
}
