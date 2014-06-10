package core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import lombok.val;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class EndPointFragmentTest {
	
	private static final String NAME_SPACE = "http://www.example.org#";
	private EndPointFragment sut;
	
	private Resource createResource(String str) {
		return ResourceFactory.createResource( NAME_SPACE + str );
	}
	
	@Before
	public void setUp() {
		//sut = EndPointFragment.of( new Crawler() );
	}
	
	@Test
	public void expand() throws Exception {
		
		val c = createResource( "c" );
		
		EndPointFragment expected = null;
		

		assertThat( sut, is(expected) );
	}

}
