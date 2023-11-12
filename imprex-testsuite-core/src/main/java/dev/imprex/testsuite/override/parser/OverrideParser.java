package dev.imprex.testsuite.override.parser;

import java.io.BufferedReader;
import java.io.BufferedWriter;

public interface OverrideParser {

	 /**
	  * 
	  * @param inputStream
	  * @return true when the input stream is valid
	  */
	boolean load(BufferedReader inputStream);

	boolean save(BufferedWriter outputStream);

	void setValue(String key, Object value);
}
