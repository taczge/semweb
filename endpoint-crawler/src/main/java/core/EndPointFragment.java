package core;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

@AllArgsConstructor
public class EndPointFragment {

	@NonNull private final Model model;
	@NonNull private final Crawler crawler;
	
	@NonNull private final Set<Resource> classes;
	@NonNull private final Set<Resource> instances;
	@NonNull private final Set<Resource> properties;
	
	public static EndPointFragment of(Crawler crawler) {
		val m  = ModelFactory.createDefaultModel();
		val cs = new HashSet<Resource>();
		val is = new HashSet<Resource>();
		val ps = new HashSet<Resource>();
		
		return new EndPointFragment(m, crawler, cs, is, ps);
	}
	
	public void expand(Resource clazz, int depth) {
		val ins = crawler.listInstanceOf(clazz);
		val mod = crawler.tracePropertyPath(ins, depth);

		add(mod);
	}
	
	private void add(Model other) {
		other.listStatements().toList().forEach( stmt -> add(stmt) );
	}
	
	private void add(Statement stmt) {
		val s = stmt.getSubject();
		val p = stmt.getPredicate();
		val o = stmt.getObject();
		
		if ( s.isURIResource() && o.isURIResource() ) {
			add( s, p, o.asResource() );
		}
	}
	
	public void add(Resource s, Property p, Resource o) {
		model.add(s, p, o);
		
		if ( p.equals(RDF.type) ) {
			instances.add(s);
			classes.add(o);
			
			return;
		}
		
		if ( p.equals(RDFS.subClassOf) ) {
			classes.add(s);
			classes.add(o);
			
			return;
		}
		
		if ( p.equals(RDFS.subPropertyOf) ) {
			properties.add(s);
			properties.add(o);
			
			return;
		}
		
		if ( p.equals(RDFS.domain) || p.equals(RDFS.range) ) {
			properties.add(s);
			classes.add(o);
			
			return;
		}
		
		instances.add(s);
		properties.add(p);
		instances.add(o);
	}
	
	public Set<Resource> listClass() {
		return classes;
	}
	
	public Set<Resource> listInstance() {
		return instances;
	}
	
	public Set<Resource> listProeprty() {
		return properties;
	}
	
	@Override
	public int hashCode() {
		return model.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( !(obj instanceof Model) ) {
			return false;
		}

		val other = (Model) obj;

		return model.isIsomorphicWith(other);
	}

}
