package at.ac.tuwien.infosys.test;

import at.ac.tuwien.infosys.praktikum.cloudsuite.CloudSuite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(CloudSuite.class)
@Suite.SuiteClasses({
		Example.class,
	    at.ac.tuwien.infosys.test.Pi.class
})
public class ExampleSuiteTest {
}
