package at.ac.tuwien.infosys.praktikum;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Contains example hostTests.
 * 
 * TODO: Note that the hostTests are just small examples of what your implementation should be able to deal with
 */
public class Example {
	@Test
	public void test1() throws InterruptedException {
		Thread.sleep(8000);
		assertTrue(true);
	}

	@Test
	public void test2() throws InterruptedException {
		Thread.sleep(10000);
		assertTrue(true);
	}

	@Test
	public void test3() throws InterruptedException {
		Thread.sleep(10000);
		assertTrue(true);
	}

	@Ignore
	@Test
	public void testIgnore() {
	}

    @Test
    public void testTestClass() {
        TestClass testClass = new TestClass();
        assertEquals(testClass.foo, 3);
    }

	@Test
	public void testFailure() {
		// This test should always fail by throwing an AssertionError
		fail();
	}

	@Test
	public void testError() {
		// This test should always fail by throwing an expected exception
		throw new ThreadDeath();
	}

	@Test(expected = IOException.class)
	public void testNotImplemented() throws IOException {
		throw new IOException();
	}

	/**
	 * Uses the {@link javax.script.ScriptEngine} to check whether the time retrieved using JavaScript is within a certain range.
	 * @throws javax.script.ScriptException if error occurrs in script.
	 * @throws java.io.IOException if an I/O error occurs
	 */
	@Test
	public void testScriptEngine() throws ScriptException, IOException {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine scriptEngine = manager.getEngineByExtension("js");

		InputStream script = ScriptManager.load("dateTest.js");

		long before = System.currentTimeMillis();
		Double time = (Double) scriptEngine.eval(IOUtils.toString(script));
		long after = System.currentTimeMillis();

		assertTrue(before <= time.longValue());
		assertTrue(time.longValue() <= after);
	}
}
