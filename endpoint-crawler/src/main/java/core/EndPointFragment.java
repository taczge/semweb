package core;

import java.io.FileWriter;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.val;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class EndPointFragment {

	@NonNull private final Model model;
	@NonNull private final Crawler crawler;
	
	public static EndPointFragment fromDBPedia() {
		return EndPointFragment.of( Crawler.DBPEDIA );
	}
	
	public static EndPointFragment of(Crawler crawler) {
		val m  = ModelFactory.createDefaultModel();
		
		return new EndPointFragment(m, crawler);
	}
	
	public void expand(String clazz, int depth) {
		val c = ResourceFactory.createResource( clazz );
		
		expand(c, depth);
	}
	
	public void expand(Resource clazz, int depth) {
		val instances = crawler.listInstanceOf(clazz);
		val fragment  = crawler.tracePropertyPath(instances, depth);

		model.add(fragment);
	}
	
	public void output(String fileName) {
		FileWriter out = null;
		try {
			out = new FileWriter( fileName );
			model.write( out, "TTL" );
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
