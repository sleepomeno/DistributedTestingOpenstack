package at.ac.tuwien.infosys.praktikum;

import java.io.InputStream;

/**
 * Loads scripts from the classpath.
 */
public class ScriptManager {
	
	static ClassLoader classLoader;

	/**
	 * Returns an input stream for reading the specified resource.
	 *
	 * @param fileName the resource name
	 * @return an input stream for reading the resource, or {@code null} if the resource could not be found
	 * @see ClassLoader#getResourceAsStream(String)
	 */
	public static InputStream load(String fileName) {
		return classLoader.getResourceAsStream(fileName);
	}
}
