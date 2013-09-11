package at.ac.tuwien.infosys.praktikum;

import at.ac.tuwien.infosys.praktikum.cloudsuite.CloudSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Contains example hostTests.
 * 
 * // TODO: you should add more hostTests i.e., by copying ExampleTest and modifying the duration so that the test classes are executed on several hosts in parallel
 */  
@RunWith(CloudSuite.class)
@Suite.SuiteClasses({
		Example.class,
	    Pi.class
})
public class ExampleSuiteTest {
}
