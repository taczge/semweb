package core;

import static core.IsModel.modelOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class CrawlerTest {
	
	private Model endpointMock;
	private Crawler sut;
	
	private static final Resource a = createResource("a");
	private static final Resource b = createResource("b");
	private static final Resource c = createResource("c");
	private static final Resource d = createResource("d");
	
	private static final Resource i = createResource("i");
	private static final Resource j = createResource("j");
	
	private static final Resource x = createResource("x");
	private static final Resource y = createResource("y");
	private static final Resource z = createResource("z");
	
	private static final Property p = createProperty("p");
	private static final Property q = createProperty("q");
	
	@Before
	public void setUp() {
		endpointMock = ModelFactory.createDefaultModel();
		sut = new Crawler("") {
			@Override
			public QueryExecution createQuery(String query) {
				return QueryExecutionFactory.create(query, endpointMock);
			}
		};
	}
	
	private static final String NS = "http://www.example.org/";
	private static Resource createResource(String str) {
		return ResourceFactory.createResource( NS + str );
	}
	
	private static Property createProperty(String str) {
		return ResourceFactory.createProperty( NS + str );
	}

	@Test
	public void listInstanceOf() throws Exception {
		endpointMock.add(c, RDFS.subClassOf, d);
		endpointMock.add(i, RDF.type, c);
		endpointMock.add(j, RDF.type, d);

		Set<Resource> expected = Sets.newHashSet(i, j);
		
		assertThat( sut.listInstanceOf(d), is(expected) );
	}

	@Test
	public void tracePropertyPathFrom() throws Exception {
		endpointMock.add(a, p, b);
		endpointMock.add(b, p, c);
		endpointMock.add(c, p, d);
		
		Model expected = ModelFactory.createDefaultModel();
		expected.add(a, p, b);
		expected.add(b, p, c);
		
		assertThat( sut.tracePropertyPathFrom(a, 2), is(modelOf(expected)) );
	}

	@Test
	public void tracePropertyPath() throws Exception {
		endpointMock.add(a, p, b);
		endpointMock.add(b, p, c);
		endpointMock.add(c, p, d);
		endpointMock.add(x, q, y);
		endpointMock.add(y, q, z);
		
		Set<Resource> subjects = Sets.newHashSet(a, x);
		
		Model expected = ModelFactory.createDefaultModel();
		expected.add(a, p, b);
		expected.add(b, p, c);
		expected.add(x, q, y);
		expected.add(y, q, z);
		
		assertThat( sut.tracePropertyPath(subjects, 2), is(modelOf(expected)) );
	}

	
}
