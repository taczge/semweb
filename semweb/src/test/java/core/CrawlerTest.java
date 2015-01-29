package core;

import static core.IsModel.modelOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import lombok.val;

import org.junit.Before;
import org.junit.Test;

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

	private static final String NAME_SPACE = "http://www.example.org/";
	
	private static final Resource a = createResource("a");
	private static final Resource b = createResource("b");
	private static final Resource c = createResource("c");
	private static final Resource d = createResource("d");
	private static final Resource e = createResource("e");
	private static final Resource f = createResource("f");
	
	private static final Resource i = createResource("i");
	
	private static final Resource x = createResource("x");
	private static final Resource y = createResource("y");
	private static final Resource z = createResource("z");
	
	private static final Property p = createProperty("p");
	private static final Property q = createProperty("q");
	private static final Property r = createProperty("r");
	private static final Property s = createProperty("s");
	
	@Before
	public void setUp() {
		endpointMock = ModelFactory.createDefaultModel();
		sut = new Crawler("mock of endpoint") {
			@Override
			public QueryExecution createQuery(String query) {
				super.createQuery(query);
				return QueryExecutionFactory.create(
						insertPrefixList(query), endpointMock);
			}
		};
	}

	private static Resource createResource(String str) {
		return ResourceFactory.createResource( NAME_SPACE + str );
	}
	
	private static Property createProperty(String str) {
		return ResourceFactory.createProperty( NAME_SPACE + str );
	}

	@Test
	public void tracePropertyPathFrom() throws Exception {
		endpointMock.add(a, p, b);
		endpointMock.add(b, p, c);
		endpointMock.add(c, p, d);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(a, p, b);
		expected.add(b, p, c);
		
		assertThat( sut.tracePropertyPathFrom(a, 2), is(modelOf(expected)) );
	}

	@Test
	public void exists() throws Exception {
		endpointMock.add(a, p, b);
				
		assertThat( sut.exists(a), is(true) );
		assertThat( sut.exists(p), is(true) );
		assertThat( sut.exists(b), is(true) );
		assertThat( sut.exists(x), is(false) );
	}
	
	@Test
	public void listPropertyInfo_inferDomains() throws Exception {
		val model = ModelFactory.createDefaultModel();
		model.add(a, p, b);
		
		endpointMock.add(p, RDFS.domain, c);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.domain, c);

		assertThat( sut.extractPropertyInfo(model), is(modelOf(expected)) ); 
	}

	@Test
	public void listPropertyInfo_inferRanges() throws Exception {
		val model = ModelFactory.createDefaultModel();
		model.add(a, p, b);
		
		endpointMock.add(p, RDFS.range, c);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.range, c);

		assertThat( sut.extractPropertyInfo(model), is(modelOf(expected)) ); 
	}
	
	@Test
	public void extractPropertyInfo_getDomain() throws Exception {
		endpointMock.add(a, p, b);
		endpointMock.add(p, RDFS.domain, c);
		
		val model = ModelFactory.createDefaultModel();
		model.add(a, p, b);

		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.domain, c);

		assertThat( sut.extractPropertyInfo(model), is(modelOf(expected)) );
		
	}

	@Test
	public void extractPropertyInfo_getRange() throws Exception {
		endpointMock.add(a, p, b);
		endpointMock.add(p, RDFS.range, c);
		
		val model = ModelFactory.createDefaultModel();
		model.add(a, p, b);

		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.range, c);

		assertThat( sut.extractPropertyInfo(model), is(modelOf(expected)) );
	}
	
	@Test
	public void extraceTypeOfInstanceIn() throws Exception {
		endpointMock.add(x, RDF.type, c);
		
		val model = ModelFactory.createDefaultModel();
		model.add(x, p, y);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(x, RDF.type, c);
		
		assertThat( sut.extractTypeIn(model), is(modelOf(expected)) );
	}
	
	@Test
	public void extractTypeOfInstanceIn_returnEmpty() throws Exception {
		endpointMock.add(z, RDF.type, c);
		
		val model = ModelFactory.createDefaultModel();
		model.add(x, p, y);
		
		val expected = ModelFactory.createDefaultModel();
		
		assertThat( sut.extractTypeIn(model), is(modelOf(expected)) );
	}
	
	@Test
	public void inferSuperPropertyIn_byProperty() throws Exception {
		endpointMock.add(p, RDFS.subPropertyOf, q);
		
		val model = ModelFactory.createDefaultModel();
		model.add(a, p, b);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.subPropertyOf, q);
		
		assertThat( sut.inferSuperPropertyIn(model), is(modelOf(expected)) );
	}
	
	@Test
	public void inferSuperPropertyIn_byRdfsSubPropertyOf() throws Exception {
		endpointMock.add(p, RDFS.subPropertyOf, r);
		endpointMock.add(q, RDFS.subPropertyOf, s);
		
		val model = ModelFactory.createDefaultModel();
		model.add(p, RDFS.subPropertyOf, q);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.subPropertyOf, r);
		expected.add(q, RDFS.subPropertyOf, s);
		
		assertThat( sut.inferSuperPropertyIn(model), is(modelOf(expected)) );
	}
	
	@Test
	public void inferSuperPropertyIn_byRdfsDomain() throws Exception {
		endpointMock.add(p, RDFS.subPropertyOf, q);
		
		val model = ModelFactory.createDefaultModel();
		model.add(p, RDFS.domain, c);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.subPropertyOf, q);
		
		assertThat( sut.inferSuperPropertyIn(model), is(modelOf(expected)) );
	}

	@Test
	public void inferSuperPropertyIn_byRdfsRange() throws Exception {
		endpointMock.add(p, RDFS.subPropertyOf, q);
		
		val model = ModelFactory.createDefaultModel();
		model.add(p, RDFS.range, c);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(p, RDFS.subPropertyOf, q);
		
		assertThat( sut.inferSuperPropertyIn(model), is(modelOf(expected)) );
	}

	@Test
	public void inferSuperClassIn_byRdfType() throws Exception {
		endpointMock.add(c, RDFS.subClassOf, d);
		
		val model = ModelFactory.createDefaultModel();
		model.add(i, RDF.type, c);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(c, RDFS.subClassOf, d);
		
		assertThat( sut.inferSuperClassIn(model), is(modelOf(expected)) );
	}
	
	@Test
	public void inferSuperClassIn_byRdfsSubClassOf() throws Exception {
		endpointMock.add(c, RDFS.subClassOf, e);
		endpointMock.add(d, RDFS.subClassOf, f);
		
		val model = ModelFactory.createDefaultModel();
		model.add(c, RDFS.subClassOf, d);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(c, RDFS.subClassOf, e);
		expected.add(d, RDFS.subClassOf, f);
		
		assertThat( sut.inferSuperClassIn(model), is(modelOf(expected)) );
	}

	@Test
	public void inferSuperClassIn_byRdfsDomain() throws Exception {
		endpointMock.add(c, RDFS.subClassOf, d);
		
		val model = ModelFactory.createDefaultModel();
		model.add(p, RDFS.domain, c);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(c, RDFS.subClassOf, d);
		
		assertThat( sut.inferSuperClassIn(model), is(modelOf(expected)) );
	}
	
	@Test
	public void inferSuperClassIn_byRdfsRange() throws Exception {
		endpointMock.add(c, RDFS.subClassOf, d);
		
		val model = ModelFactory.createDefaultModel();
		model.add(p, RDFS.range, c);
		
		val expected = ModelFactory.createDefaultModel();
		expected.add(c, RDFS.subClassOf, d);
		
		assertThat( sut.inferSuperClassIn(model), is(modelOf(expected)) );
	}
}
