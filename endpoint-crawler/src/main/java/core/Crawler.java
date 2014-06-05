package core;

import java.util.Collection;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.val;

import com.google.common.annotations.VisibleForTesting;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;

@AllArgsConstructor
public class Crawler {
	
	private static final String LS = System.lineSeparator();
	private static final String PREFIXES = "" + 
			"prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" + LS + 
			"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>";
	
	private final String endpointURL;
	
	@VisibleForTesting
	public QueryExecution createQuery(String query) {
		return QueryExecutionFactory.sparqlService(endpointURL, query);
	}
	
	private String normalize(Resource r) {
		return "<" + r + ">";
	}
	
	public Set<Resource> listInstanceOf(Resource clazz) {
		val c = normalize( clazz );
		val query = PREFIXES + LS +  
				"construct { ?x rdf:type " + c + " . } " + 
				"where { ?x rdf:type/rdfs:subClassOf* " + c + " . }";
		
		val result = createQuery(query).execConstruct();
		
		return result.listSubjects().toSet();
	}
	
	public Model listDirectPathFrom(Resource subject) {
		val s = normalize( subject );
		val query = "construct { " + s + " ?p ?o . } where { " + s + " ?p ?o . }"; 
		val execution = createQuery(query);
		
		return execution.execConstruct();
	}

	public Model tracePropertyPathFrom(Resource subject, int depth) {
		if ( depth <= 0 ) {
			return ModelFactory.createDefaultModel(); 
		}
		
		Model current = listDirectPathFrom( subject );
		Model rest    = ModelFactory.createDefaultModel();
		for ( NodeIterator it = current.listObjects(); it.hasNext(); ) {
			val object = it.next();
			
			if ( !object.isURIResource() ) {
				continue;
			}
			
			val o = object.asResource();
			
			rest.add( tracePropertyPathFrom(o, depth - 1) );
		}
		
		return current.add(rest);
	}
	
	public Model tracePropertyPath(Collection<Resource> subjects, int depth) {
		return subjects.stream()
				.map( subject -> tracePropertyPathFrom(subject, depth) )
				.reduce(ModelFactory.createDefaultModel(), (a, b) -> a.add(b));
	}
	
}
