package tdb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import lombok.val;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class AbstractEliminatorTest {
	
	private static final Resource a = createResource( "a" );
	private static final Resource b = createResource( "b" );
	private static final Property p = createProperty( "p" );
	private static final Resource blank = createBlankNode();
	
	private static final String NAME_SPACE = "http://www.exmaple.org/"; 
	private static Resource createResource(String s) {
		return ResourceFactory.createResource( NAME_SPACE + s ); 
	}
	
	public static Property createProperty(String s) {
		return ResourceFactory.createProperty( NAME_SPACE + s ); 
	}
	
	private static Resource createBlankNode() {
		return ResourceFactory.createResource();
	}

	private Model model;
	private Eliminator sut;
	
	@Before
	public void setUp() {
		model = createModel();
		sut   = createEliminator(model); 
	}

	private Eliminator createEliminator(Model m) {
		return new ModelEliminator(m);
	}

	private Eliminator createEmptyEliminator() {
		return new ModelEliminator( createModel() );
	}

	private Model createModel() {
		return ModelFactory.createDefaultModel();
	}
	
	@Test
	public void deleteNonResource_retainTripleThatConsistsOfOnlyURI() throws Exception {
		model.add(a, p, b);
		
		val expected = createEliminator(model);

		sut.deleteNonResource();
		assertThat( sut, is(expected) );
		
	}
	
	@Test
	public void deleteNonResource_deleteLiteralInObject() throws Exception {
		model.add(a, p, "literal");
		
		val expected = createEmptyEliminator();

		sut.deleteNonResource();
		assertThat( sut, is(expected) );
	}

	@Test
	public void deleteNonResource_deleteBlankNodeInObject() throws Exception {
		model.add(a, p, blank);
		
		val expected = createEmptyEliminator();

		sut.deleteNonResource();
		assertThat( sut, is(expected) );
	}

	@Test
	public void deleteNonResource_deleteBlankNodeInSubject() throws Exception {
		model.add(blank, p, a);
		
		val expected = createEmptyEliminator();

		sut.deleteNonResource();
		assertThat( sut, is(expected) );
	}

}
