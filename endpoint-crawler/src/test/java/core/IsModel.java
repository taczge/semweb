package core;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import com.hp.hpl.jena.rdf.model.Model;

public class IsModel extends BaseMatcher<Model> {

	private final Model expected;
	Object actual;
	
	public IsModel(Model expected) {
		this.expected = expected;
	}
	
	public static Matcher<Model> modelOf(Model m) {
		return new IsModel( m );
	}

	@Override
	public boolean matches(Object item) {
		this.actual = item;
		
		if ( !(item instanceof Model) ) {
			return false;
		}

		Model other = (Model) item;
		
		return expected.isIsomorphicWith( other );
	}

	@Override
	public void describeTo(Description description) {
		description.appendValue(expected);
		description.appendValue(" but actual is " + actual);
	}
}
