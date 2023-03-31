package br.com.brforgers.mods.disfabric.utils;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class StringUtils {

	/**
	 * Checks to see if the given string is an integer.
	 *
	 * @param str The string to test.
	 * @return {@code true} if it is an integer, false otherwise.
	 */
	public static boolean isNumber(String str) {
		for (int i = 0, l = str.length(); i < l; i++) {
			if (!Character.isDigit(str.charAt(i))) return false;
		}
		return true;
	}

	/**
	 * Counts all instances of {@code {}} in the given string.
	 *
	 * @param str The string to count parameters in.
	 * @return The parameter count.
	 */
	public static int countParameters(String str) {
		int c = 0;
		for (int i = str.indexOf("{}"); i >= 0; i = str.indexOf("{}", i + 2)) {
			c++;
		}
		return c;
	}

	/**
	 * Fills all parameters in order.
	 * <p>
	 * If {@code args} is smaller than the number of parameters available,
	 * the remaining parameters are left as is.
	 * <p>
	 * If {@code args} is larger than the number of parameters available,
	 * the tail end of the array is never inserted into the resulting string.
	 *
	 * @param s    The string to do basic replacement on.
	 * @param args An array of strings to replace each {@code {}} with.
	 * @return The processed string.
	 */
	public static String fillParameters(String s, String... args) {
		var sb = new StringBuilder(s.length());
		int b = 0;
		for (int i = 0, l = args.length, a = s.indexOf("{}"); a >= 0 && i < l; a = s.indexOf("{}", b = a + 2), i++) {
			sb.append(s, b, a).append(args[i]);
		}
		sb.append(s, b, s.length());
		return sb.toString();
	}
}
