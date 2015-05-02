package utilities;

public class MathUtils {
	
	// http://en.wikipedia.org/wiki/Julian_day#Converting_Julian_or_Gregorian_calendar_date_to_Julian_Day_Number
	public static int julianDay(int year, int month, int day) {
	  int a = (14 - month) / 12;
	  int y = year + 4800 - a;
	  int m = month + 12 * a - 3;
	  int jdn = day + floorDiv(153 * m + 2, 5) + (365*y) + floorDiv(y, 4) - floorDiv(y, 100) + floorDiv(y, 400) - 32045;
	  return jdn;
	}
	
	// Used for calculating born dates
	public static int numberDaysSince(int y1, int m1, int d1, int y2, int m2, int d2) {
		  return julianDay(y1, m1, d1) - julianDay(y2, m2, d2);
	}
	
	/**
	 * To keep compatible with Java7, this uses Oracle's Implementation of Math.floorDiv
	 * 
	 * Returns the largest (closest to positive infinity)
     * {@code int} value that is less than or equal to the algebraic quotient.
     * There is one special case, if the dividend is the
     * {@linkplain Integer#MIN_VALUE Integer.MIN_VALUE} and the divisor is {@code -1},
     * then integer overflow occurs and
     * the result is equal to the {@code Integer.MIN_VALUE}.
     * <p>
     * Normal integer division operates under the round to zero rounding mode
     * (truncation).  This operation instead acts under the round toward
     * negative infinity (floor) rounding mode.
     * The floor rounding mode gives different results than truncation
     * when the exact result is negative.
     * <ul>
     *   <li>If the signs of the arguments are the same, the results of
     *       {@code floorDiv} and the {@code /} operator are the same.  <br>
     *       For example, {@code floorDiv(4, 3) == 1} and {@code (4 / 3) == 1}.</li>
     *   <li>If the signs of the arguments are different,  the quotient is negative and
     *       {@code floorDiv} returns the integer less than or equal to the quotient
     *       and the {@code /} operator returns the integer closest to zero.<br>
     *       For example, {@code floorDiv(-4, 3) == -2},
     *       whereas {@code (-4 / 3) == -1}.
     *   </li>
     * </ul>
     * <p>
     *
     * @param x the dividend
     * @param y the divisor
     * @return the largest (closest to positive infinity)
     * {@code int} value that is less than or equal to the algebraic quotient.
     * @throws ArithmeticException if the divisor {@code y} is zero
     * @see #floorMod(int, int)
     * @see #floor(double)
     * @since 1.8
	 */
	private static int floorDiv(int x, int y) {
		int r = x / y;
        // if the signs are different and modulo not zero, round down
        if ((x ^ y) < 0 && (r * y != x)) {
            r--;
        }
        return r;
	}
}
